package sqlancer.postgres.oracle.ext.eet;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.postgres.PostgresSchema.PostgresDataType;
import sqlancer.postgres.ast.PostgresBinaryLogicalOperation;
import sqlancer.postgres.ast.PostgresBinaryLogicalOperation.BinaryLogicalOperator;
import sqlancer.postgres.ast.PostgresCaseWhen;
import sqlancer.postgres.ast.PostgresConstant;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresPostfixOperation;
import sqlancer.postgres.ast.PostgresPostfixOperation.PostfixOperator;
import sqlancer.postgres.ast.PostgresPrefixOperation;
import sqlancer.postgres.ast.PostgresPrefixOperation.PrefixOperator;
import sqlancer.postgres.ast.PostgresPrintedExpression;
import sqlancer.postgres.ast.PostgresTableReference;
import sqlancer.postgres.ast.PostgresText;
import sqlancer.postgres.gen.PostgresExpressionGenerator;

/**
 * EET equivalent transformations (Rule 1–7 style) for PostgreSQL AST.
 *
 * Compared to MySQL/GaussDBM, this implementation focuses on a safe subset:
 * - boolean transforms for predicates (3-valued logic aware)
 * - value transforms via CASE WHEN for typed expressions when type is known
 * - recursive traversal via {@link PostgresEETExpressionTree}
 */
public final class PostgresEETTransformer {

    private static final int MAX_TRANSFORM_RETRIES = 200;

    private final PostgresExpressionGenerator gen;

    public PostgresEETTransformer(PostgresExpressionGenerator gen) {
        this.gen = gen;
    }

    public PostgresExpression transformExpression(PostgresExpression expr) {
        if (expr == null) {
            return null;
        }
        if (isRule7NoChange(expr)) {
            return expr;
        }
        PostgresExpression exprRec = PostgresEETExpressionTree.mapChildren(this::transformExpression, expr);
        PostgresExpression constHandled = tryConstBoolTransform(exprRec);
        if (constHandled != null) {
            return constHandled;
        }
        int attempts = 0;
        while (attempts++ < MAX_TRANSFORM_RETRIES) {
            try {
                if (isBooleanLike(exprRec)) {
                    if (Randomly.getBoolean()) {
                        return transformBoolExpr(exprRec);
                    } else {
                        return transformValueExpr(exprRec);
                    }
                } else {
                    return transformValueExpr(exprRec);
                }
            } catch (IgnoreMeException e) {
                // retry
            }
        }
        throw new IgnoreMeException();
    }

    private static boolean isRule7NoChange(PostgresExpression expr) {
        return expr instanceof PostgresTableReference || expr instanceof PostgresText;
    }

    static boolean isBooleanLike(PostgresExpression expr) {
        // conservative: only treat known boolean-typed nodes as boolean-like
        if (expr == null) {
            return false;
        }
        PostgresDataType t = expr.getExpressionType();
        if (t == PostgresDataType.BOOLEAN) {
            return true;
        }
        return expr instanceof PostgresBinaryLogicalOperation || expr instanceof PostgresPostfixOperation
                || expr instanceof PostgresPrefixOperation;
    }

    PostgresExpression tryConstBoolTransform(PostgresExpression expr) {
        if (!(expr instanceof PostgresConstant)) {
            return null;
        }
        PostgresConstant c = (PostgresConstant) expr;
        if (c.isNull() || !c.isInt()) {
            return null;
        }
        long v = c.asInt();
        if (v != 0 && v != 1) {
            return null;
        }
        PostgresExpression extend;
        try {
            extend = gen.generateBooleanExpression();
        } catch (IgnoreMeException e) {
            return null;
        }
        if (v == 1) {
            return new PostgresBinaryLogicalOperation(c, extend, BinaryLogicalOperator.OR);
        } else {
            return new PostgresBinaryLogicalOperation(c, extend, BinaryLogicalOperator.AND);
        }
    }

    public PostgresExpression transformBoolExpr(PostgresExpression expr) {
        PostgresExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 6);
        boolean useTrueExpr = choice <= 2;

        PostgresExpression notRand = new PostgresPrefixOperation(randomBool, PrefixOperator.NOT);
        PostgresExpression randIsNull = new PostgresPostfixOperation(randomBool, PostfixOperator.IS_NULL);
        PostgresExpression randIsNotNull = new PostgresPostfixOperation(randomBool, PostfixOperator.IS_NOT_NULL);

        if (useTrueExpr) {
            PostgresExpression part1 = new PostgresBinaryLogicalOperation(randomBool, notRand, BinaryLogicalOperator.OR);
            PostgresExpression base = new PostgresBinaryLogicalOperation(part1, randIsNull, BinaryLogicalOperator.OR);
            return new PostgresBinaryLogicalOperation(base, expr, BinaryLogicalOperator.AND);
        } else {
            PostgresExpression part1 = new PostgresBinaryLogicalOperation(randomBool, notRand, BinaryLogicalOperator.AND);
            PostgresExpression base = new PostgresBinaryLogicalOperation(part1, randIsNotNull,
                    BinaryLogicalOperator.AND);
            return new PostgresBinaryLogicalOperation(base, expr, BinaryLogicalOperator.OR);
        }
    }

    public PostgresExpression transformValueExpr(PostgresExpression expr) {
        PostgresDataType type = expr.getExpressionType();
        if (type == null) {
            // Type unknown: still safe to wrap with an equivalent CASE WHEN using identical branches.
            // This increases transform coverage without requiring additional Postgres type inference.
            PostgresExpression randomBool = gen.generateBooleanExpression();
            PostgresExpression copy = new PostgresPrintedExpression(expr);
            return new PostgresCaseWhen(randomBool, copy, expr);
        }
        PostgresExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 9);
        PostgresExpression randVal = gen.generateExpression(0, type);

        if (choice <= 2) {
            PostgresExpression trueExpr = buildTrueExpr(randomBool);
            return new PostgresCaseWhen(trueExpr, new PostgresPrintedExpression(expr), randVal);
        } else if (choice <= 5) {
            PostgresExpression falseExpr = buildFalseExpr(randomBool);
            return new PostgresCaseWhen(falseExpr, randVal, new PostgresPrintedExpression(expr));
        } else {
            PostgresExpression copy = new PostgresPrintedExpression(expr);
            if (Randomly.getBoolean()) {
                return new PostgresCaseWhen(randomBool, copy, expr);
            } else {
                return new PostgresCaseWhen(randomBool, expr, copy);
            }
        }
    }

    private static PostgresExpression buildTrueExpr(PostgresExpression randomBool) {
        PostgresExpression notRand = new PostgresPrefixOperation(randomBool, PrefixOperator.NOT);
        PostgresExpression randIsNull = new PostgresPostfixOperation(randomBool, PostfixOperator.IS_NULL);
        PostgresExpression part1 = new PostgresBinaryLogicalOperation(randomBool, notRand, BinaryLogicalOperator.OR);
        return new PostgresBinaryLogicalOperation(part1, randIsNull, BinaryLogicalOperator.OR);
    }

    private static PostgresExpression buildFalseExpr(PostgresExpression randomBool) {
        PostgresExpression notRand = new PostgresPrefixOperation(randomBool, PrefixOperator.NOT);
        PostgresExpression randIsNotNull = new PostgresPostfixOperation(randomBool, PostfixOperator.IS_NOT_NULL);
        PostgresExpression part1 = new PostgresBinaryLogicalOperation(randomBool, notRand, BinaryLogicalOperator.AND);
        return new PostgresBinaryLogicalOperation(part1, randIsNotNull, BinaryLogicalOperator.AND);
    }
}

