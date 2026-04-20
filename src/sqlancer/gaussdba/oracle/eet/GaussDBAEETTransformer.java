package sqlancer.gaussdba.oracle.eet;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.gaussdba.ast.GaussDBABinaryComparisonOperation;
import sqlancer.gaussdba.ast.GaussDBABinaryLogicalOperation;
import sqlancer.gaussdba.ast.GaussDBABinaryLogicalOperation.GaussDBABinaryLogicalOperator;
import sqlancer.gaussdba.ast.GaussDBACaseWhen;
import sqlancer.gaussdba.ast.GaussDBAConstant;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBATableReference;
import sqlancer.gaussdba.ast.GaussDBAUnaryPostfixOperation;
import sqlancer.gaussdba.ast.GaussDBAUnaryPostfixOperation.UnaryPostfixOperator;
import sqlancer.gaussdba.ast.GaussDBAUnaryPrefixOperation;
import sqlancer.gaussdba.ast.GaussDBAUnaryPrefixOperation.UnaryPrefixOperator;
import sqlancer.gaussdba.gen.GaussDBAExpressionGenerator;

/**
 * EET equivalent transformations on GaussDB A AST nodes.
 */
public class GaussDBAEETTransformer {

    private static final int MAX_TRANSFORM_RETRIES = 200;

    private final GaussDBAExpressionGenerator gen;

    public GaussDBAEETTransformer(GaussDBAExpressionGenerator gen) {
        this.gen = gen;
    }

    public GaussDBAExpression transformExpression(GaussDBAExpression expr) {
        if (expr == null) {
            return null;
        }
        if (isRule7NoChange(expr)) {
            return expr;
        }
        GaussDBAExpression exprRec = GaussDBAEETExpressionTree.mapChildren(this::transformExpression, expr);
        GaussDBAExpression constHandled = tryConstBoolTransform(exprRec);
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

    private static boolean isRule7NoChange(GaussDBAExpression expr) {
        return expr instanceof GaussDBATableReference;
    }

    static boolean isBooleanLike(GaussDBAExpression expr) {
        return expr instanceof GaussDBABinaryLogicalOperation || expr instanceof GaussDBABinaryComparisonOperation
                || expr instanceof GaussDBAUnaryPrefixOperation || expr instanceof GaussDBAUnaryPostfixOperation;
    }

    GaussDBAExpression tryConstBoolTransform(GaussDBAExpression expr) {
        if (!(expr instanceof GaussDBAConstant)) {
            return null;
        }
        GaussDBAConstant c = (GaussDBAConstant) expr;
        if (c.isNull()) {
            return null;
        }
        if (!isZeroOrOneInt(c)) {
            return null;
        }
        GaussDBAExpression extend;
        try {
            extend = gen.generateBooleanExpression();
        } catch (IgnoreMeException e) {
            return null;
        }
        long val = c.isNumber() ? c.asNumber() : 0;
        if (val == 1) {
            return new GaussDBABinaryLogicalOperation(c, extend, GaussDBABinaryLogicalOperator.OR);
        } else {
            return new GaussDBABinaryLogicalOperation(c, extend, GaussDBABinaryLogicalOperator.AND);
        }
    }

    private static boolean isZeroOrOneInt(GaussDBAConstant c) {
        if (!c.isNumber()) {
            return false;
        }
        long v = c.asNumber();
        return v == 0 || v == 1;
    }

    public GaussDBAExpression transformBoolExpr(GaussDBAExpression expr) {
        GaussDBAExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 6);
        boolean useTrueExpr = choice <= 2;
        GaussDBAExpression notRand = new GaussDBAUnaryPrefixOperation(randomBool, UnaryPrefixOperator.NOT);
        GaussDBAExpression randIsNull = new GaussDBAUnaryPostfixOperation(randomBool, UnaryPostfixOperator.IS_NULL);
        GaussDBAExpression randIsNotNull = new GaussDBAUnaryPostfixOperation(randomBool, UnaryPostfixOperator.IS_NOT_NULL);

        if (useTrueExpr) {
            GaussDBAExpression part1 = new GaussDBABinaryLogicalOperation(randomBool, notRand,
                    GaussDBABinaryLogicalOperator.OR);
            GaussDBAExpression base = new GaussDBABinaryLogicalOperation(part1, randIsNull,
                    GaussDBABinaryLogicalOperator.OR);
            return new GaussDBABinaryLogicalOperation(base, expr, GaussDBABinaryLogicalOperator.AND);
        } else {
            GaussDBAExpression part1 = new GaussDBABinaryLogicalOperation(randomBool, notRand,
                    GaussDBABinaryLogicalOperator.AND);
            GaussDBAExpression base = new GaussDBABinaryLogicalOperation(part1, randIsNotNull,
                    GaussDBABinaryLogicalOperator.AND);
            return new GaussDBABinaryLogicalOperation(base, expr, GaussDBABinaryLogicalOperator.OR);
        }
    }

    public GaussDBAExpression transformValueExpr(GaussDBAExpression expr) {
        GaussDBAExpression randomBool = gen.generateBooleanExpression();
        int choice = (int) Randomly.getNotCachedInteger(0, 9);
        GaussDBAConstant randVal = GaussDBAConstant.createNumberConstant(0);

        if (choice <= 2) {
            GaussDBAExpression trueExpr = buildTrueExpr(randomBool);
            return GaussDBACaseWhen.create(trueExpr, expr, randVal);
        } else if (choice <= 5) {
            GaussDBAExpression falseExpr = buildFalseExpr(randomBool);
            return GaussDBACaseWhen.create(falseExpr, randVal, expr);
        } else {
            GaussDBAExpression copy = copyExpr(expr);
            if (Randomly.getBoolean()) {
                return GaussDBACaseWhen.create(randomBool, copy, expr);
            } else {
                return GaussDBACaseWhen.create(randomBool, expr, copy);
            }
        }
    }

    public GaussDBAExpression buildTrueExpr(GaussDBAExpression randomBool) {
        GaussDBAExpression notRand = new GaussDBAUnaryPrefixOperation(randomBool, UnaryPrefixOperator.NOT);
        GaussDBAExpression randIsNull = new GaussDBAUnaryPostfixOperation(randomBool, UnaryPostfixOperator.IS_NULL);
        GaussDBAExpression part1 = new GaussDBABinaryLogicalOperation(randomBool, notRand,
                GaussDBABinaryLogicalOperator.OR);
        return new GaussDBABinaryLogicalOperation(part1, randIsNull, GaussDBABinaryLogicalOperator.OR);
    }

    public GaussDBAExpression buildFalseExpr(GaussDBAExpression randomBool) {
        GaussDBAExpression notRand = new GaussDBAUnaryPrefixOperation(randomBool, UnaryPrefixOperator.NOT);
        GaussDBAExpression randIsNotNull = new GaussDBAUnaryPostfixOperation(randomBool,
                UnaryPostfixOperator.IS_NOT_NULL);
        GaussDBAExpression part1 = new GaussDBABinaryLogicalOperation(randomBool, notRand,
                GaussDBABinaryLogicalOperator.AND);
        return new GaussDBABinaryLogicalOperation(part1, randIsNotNull, GaussDBABinaryLogicalOperator.AND);
    }

    public GaussDBAExpression copyExpr(GaussDBAExpression expr) {
        // 简化实现：返回原始表达式（在Oracle语义下）
        return expr;
    }
}