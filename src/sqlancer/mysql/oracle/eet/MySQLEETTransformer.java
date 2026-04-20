package sqlancer.mysql.oracle.eet;

import java.util.List;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.mysql.ast.MySQLBinaryComparisonOperation;
import sqlancer.mysql.ast.MySQLBinaryLogicalOperation;
import sqlancer.mysql.ast.MySQLBinaryLogicalOperation.MySQLBinaryLogicalOperator;
import sqlancer.mysql.ast.MySQLCaseOperator;
import sqlancer.mysql.ast.MySQLColumnReference;
import sqlancer.mysql.ast.MySQLConstant;
import sqlancer.mysql.ast.MySQLConstant.MySQLDoubleConstant;
import sqlancer.mysql.ast.MySQLExists;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.ast.MySQLInOperation;
import sqlancer.mysql.ast.MySQLPrintedExpression;
import sqlancer.mysql.ast.MySQLTableReference;
import sqlancer.mysql.ast.MySQLText;
import sqlancer.mysql.ast.MySQLUnaryPostfixOperation;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation;
import sqlancer.mysql.ast.MySQLUnaryPrefixOperation.MySQLUnaryPrefixOperator;
import sqlancer.mysql.gen.MySQLExpressionGenerator;
import sqlancer.mysql.MySQLSchema.MySQLDataType;

/**
 * EET equivalent transformations (Rules 1–7) on MySQL AST nodes.
 */
public class MySQLEETTransformer {

    private static final int MAX_TRANSFORM_RETRIES = 200;

    private final MySQLExpressionGenerator gen;

    public MySQLEETTransformer(MySQLExpressionGenerator gen) {
        this.gen = gen;
    }

    public MySQLExpression transformExpression(MySQLExpression expr) {
        if (expr == null) {
            return null;
        }
        if (isRule7NoChange(expr)) {
            return expr;
        }
        // §3.2: recurse into CASE / bool_binop / IN / etc. first (EET-main order), then transform this node.
        MySQLExpression exprRec = MySQLEETExpressionTree.mapChildren(this::transformExpression, expr);
        MySQLExpression constHandled = tryConstBoolTransform(exprRec);
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

    /**
     * Nodes that must not be wrapped by EET rules (would break scoping). {@link MySQLText} is used by
     * {@link MySQLEETQueryGenerator} for CTE/derived outer columns and WHERE; transforming it would call
     * {@link #transformValueExpr} and pull in base-table columns (e.g. {@code t0.c0}) outside their scope.
     */
    private static boolean isRule7NoChange(MySQLExpression expr) {
        return expr instanceof MySQLTableReference || expr instanceof MySQLText;
    }

    static boolean isBooleanLike(MySQLExpression expr) {
        return expr instanceof MySQLBinaryLogicalOperation || expr instanceof MySQLBinaryComparisonOperation
                || expr instanceof MySQLUnaryPrefixOperation || expr instanceof MySQLUnaryPostfixOperation
                || expr instanceof MySQLInOperation || expr instanceof MySQLExists;
    }

    MySQLExpression tryConstBoolTransform(MySQLExpression expr) {
        if (!(expr instanceof MySQLConstant)) {
            return null;
        }
        MySQLConstant c = (MySQLConstant) expr;
        if (c.isNull()) {
            return null;
        }
        if (!isZeroOrOneInt(c)) {
            return null;
        }
        MySQLExpression extend;
        try {
            extend = gen.generateBooleanExpression();
        } catch (IgnoreMeException e) {
            // Generating the extension expression may legitimately fail for some shapes/operators.
            // Returning null allows the caller to retry with alternative EET rules.
            return null;
        }
        if (c.asBooleanNotNull()) {
            return new MySQLBinaryLogicalOperation(c, extend, MySQLBinaryLogicalOperator.OR);
        } else {
            return new MySQLBinaryLogicalOperation(c, extend, MySQLBinaryLogicalOperator.AND);
        }
    }

    private static boolean isZeroOrOneInt(MySQLConstant c) {
        if (!c.isInt()) {
            return false;
        }
        long v = c.getInt();
        return v == 0 || v == 1;
    }

    public MySQLExpression transformBoolExpr(MySQLExpression expr) {
        MySQLExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 6);
        boolean useTrueExpr = choice <= 2;
        MySQLExpression notRand = new MySQLUnaryPrefixOperation(randomBool, MySQLUnaryPrefixOperator.NOT);
        MySQLExpression randIsNull = new MySQLUnaryPostfixOperation(randomBool,
                MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, false);
        MySQLExpression randIsNotNull = new MySQLUnaryPostfixOperation(randomBool,
                MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, true);

        if (useTrueExpr) {
            MySQLExpression part1 = new MySQLBinaryLogicalOperation(randomBool, notRand, MySQLBinaryLogicalOperator.OR);
            MySQLExpression base = new MySQLBinaryLogicalOperation(part1, randIsNull, MySQLBinaryLogicalOperator.OR);
            return new MySQLBinaryLogicalOperation(base, expr, MySQLBinaryLogicalOperator.AND);
        } else {
            MySQLExpression part1 = new MySQLBinaryLogicalOperation(randomBool, notRand, MySQLBinaryLogicalOperator.AND);
            MySQLExpression base = new MySQLBinaryLogicalOperation(part1, randIsNotNull, MySQLBinaryLogicalOperator.AND);
            return new MySQLBinaryLogicalOperation(base, expr, MySQLBinaryLogicalOperator.OR);
        }
    }

    public MySQLExpression transformValueExpr(MySQLExpression expr) {
        MySQLExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 9);
        MySQLExpression randVal = generateSameTypeAs(expr);

        if (choice <= 2) {
            MySQLExpression trueExpr = buildTrueExpr(randomBool);
            return new MySQLCaseOperator(null, List.of(trueExpr), List.of(expr), randVal);
        } else if (choice <= 5) {
            MySQLExpression falseExpr = buildFalseExpr(randomBool);
            return new MySQLCaseOperator(null, List.of(falseExpr), List.of(randVal), expr);
        } else {
            MySQLExpression copy = copyExpr(expr);
            if (Randomly.getBoolean()) {
                return new MySQLCaseOperator(null, List.of(randomBool), List.of(copy), expr);
            } else {
                return new MySQLCaseOperator(null, List.of(randomBool), List.of(expr), copy);
            }
        }
    }

    public MySQLExpression buildTrueExpr(MySQLExpression randomBool) {
        MySQLExpression notRand = new MySQLUnaryPrefixOperation(randomBool, MySQLUnaryPrefixOperator.NOT);
        MySQLExpression randIsNull = new MySQLUnaryPostfixOperation(randomBool,
                MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, false);
        MySQLExpression part1 = new MySQLBinaryLogicalOperation(randomBool, notRand, MySQLBinaryLogicalOperator.OR);
        return new MySQLBinaryLogicalOperation(part1, randIsNull, MySQLBinaryLogicalOperator.OR);
    }

    public MySQLExpression buildFalseExpr(MySQLExpression randomBool) {
        MySQLExpression notRand = new MySQLUnaryPrefixOperation(randomBool, MySQLUnaryPrefixOperator.NOT);
        MySQLExpression randIsNotNull = new MySQLUnaryPostfixOperation(randomBool,
                MySQLUnaryPostfixOperation.UnaryPostfixOperator.IS_NULL, true);
        MySQLExpression part1 = new MySQLBinaryLogicalOperation(randomBool, notRand, MySQLBinaryLogicalOperator.AND);
        return new MySQLBinaryLogicalOperation(part1, randIsNotNull, MySQLBinaryLogicalOperator.AND);
    }

    public MySQLExpression copyExpr(MySQLExpression expr) {
        return new MySQLPrintedExpression(expr);
    }

    private MySQLExpression generateSameTypeAs(MySQLExpression expr) {
        MySQLDataType t = inferDataType(expr);
        if (t == null) {
            return gen.generateExpression();
        }
        for (int i = 0; i < MAX_TRANSFORM_RETRIES; i++) {
            try {
                MySQLExpression e = gen.generateExpression();
                MySQLDataType t2 = inferDataType(e);
                if (t2 != null && t2 == t) {
                    return e;
                }
            } catch (IgnoreMeException e) {
                // retry
            }
        }
        return sameTypeFallback(t);
    }

    private static MySQLExpression sameTypeFallback(MySQLDataType t) {
        switch (t) {
        case INT:
            return MySQLConstant.createIntConstant(0);
        case VARCHAR:
            return MySQLConstant.createStringConstant("eet");
        case FLOAT:
        case DOUBLE:
            return new MySQLDoubleConstant(0.0);
        case DECIMAL:
            return MySQLConstant.createIntConstant(0);
        case DATE:
            return new MySQLConstant.MySQLDateConstant(2000, 1, 1);
        case TIME:
            return new MySQLConstant.MySQLTimeConstant(0, 0, 0, 0, 0);
        case DATETIME:
            return new MySQLConstant.MySQLDateTimeConstant(2000, 1, 1, 0, 0, 0, 0, 0);
        case TIMESTAMP:
            return new MySQLConstant.MySQLTimestampConstant(2000, 1, 1, 0, 0, 0, 0, 0);
        case YEAR:
            return new MySQLConstant.MySQLYearConstant(2000);
        case BIT:
            return MySQLConstant.createBitConstant(0, 1);
        case ENUM:
            return MySQLConstant.createEnumConstant("e0", 0);
        case SET:
            return MySQLConstant.createSetConstant(java.util.Collections.singleton("s0"), 1);
        case JSON:
            return MySQLConstant.createJSONConstant("{}");
        case BINARY:
        case VARBINARY:
        case BLOB:
            return MySQLConstant.createBinaryConstant(new byte[]{0});
        default:
            // 对于未知类型，抛出 IgnoreMeException 而不是 AssertionError
            throw new sqlancer.IgnoreMeException();
        }
    }

    private static MySQLDataType inferDataType(MySQLExpression expr) {
        try {
            if (expr instanceof MySQLColumnReference) {
                return ((MySQLColumnReference) expr).getColumn().getType();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
