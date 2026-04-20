package sqlancer.gaussdbpg;

import sqlancer.IgnoreMeException;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate;
import sqlancer.gaussdbpg.ast.GaussDBPGBetweenOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryLogicalOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGCaseWhen;
import sqlancer.gaussdbpg.ast.GaussDBPGCastOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnValue;
import sqlancer.gaussdbpg.ast.GaussDBPGConstant;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGInOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGLikeOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGTableReference;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation;

public final class GaussDBPGExpectedValueVisitor {

    private final StringBuilder sb = new StringBuilder();

    private void print(GaussDBPGExpression expr) {
        GaussDBPGToStringVisitor v = new GaussDBPGToStringVisitor();
        v.visit(expr);
        sb.append(v.get());
        sb.append(" -- ");
        sb.append(expr.getExpectedValue());
        sb.append("\n");
    }

    public String get() {
        return sb.toString();
    }

    public void visit(GaussDBPGExpression expr) {
        if (expr instanceof GaussDBPGConstant) {
            visit((GaussDBPGConstant) expr);
        } else if (expr instanceof GaussDBPGColumnValue) {
            visit((GaussDBPGColumnValue) expr);
        } else if (expr instanceof GaussDBPGColumnReference) {
            visit((GaussDBPGColumnReference) expr);
        } else if (expr instanceof GaussDBPGUnaryPrefixOperation) {
            visit((GaussDBPGUnaryPrefixOperation) expr);
        } else if (expr instanceof GaussDBPGUnaryPostfixOperation) {
            visit((GaussDBPGUnaryPostfixOperation) expr);
        } else if (expr instanceof GaussDBPGBinaryLogicalOperation) {
            visit((GaussDBPGBinaryLogicalOperation) expr);
        } else if (expr instanceof GaussDBPGBetweenOperation) {
            visit((GaussDBPGBetweenOperation) expr);
        } else if (expr instanceof GaussDBPGCaseWhen) {
            visit((GaussDBPGCaseWhen) expr);
        } else if (expr instanceof GaussDBPGCastOperation) {
            visit((GaussDBPGCastOperation) expr);
        } else if (expr instanceof GaussDBPGAggregate) {
            visit((GaussDBPGAggregate) expr);
        } else if (expr instanceof GaussDBPGInOperation) {
            visit((GaussDBPGInOperation) expr);
        } else if (expr instanceof GaussDBPGLikeOperation) {
            visit((GaussDBPGLikeOperation) expr);
        } else if (expr instanceof GaussDBPGSelect) {
            visit((GaussDBPGSelect) expr);
        } else if (expr instanceof GaussDBPGTableReference) {
            visit((GaussDBPGTableReference) expr);
        } else {
            throw new AssertionError(expr);
        }
    }

    public void visit(GaussDBPGConstant constant) {
        print(constant);
    }

    public void visit(GaussDBPGColumnValue c) {
        print(c);
    }

    public void visit(GaussDBPGColumnReference column) {
        print(column);
    }

    public void visit(GaussDBPGTableReference tb) {
        // Table reference has no expected value
    }

    public void visit(GaussDBPGUnaryPrefixOperation op) {
        print(op);
        visit(op.getExpr());
    }

    public void visit(GaussDBPGUnaryPostfixOperation op) {
        print(op);
        visit(op.getExpr());
    }

    public void visit(GaussDBPGBinaryLogicalOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBPGBetweenOperation op) {
        print(op);
        visit(op.getExpr());
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBPGCaseWhen caseWhen) {
        throw new IgnoreMeException();
    }

    public void visit(GaussDBPGCastOperation cast) {
        print(cast);
        visit(cast.getExpr());
    }

    public void visit(GaussDBPGAggregate op) {
        print(op);
        for (GaussDBPGExpression expr : op.getArgs()) {
            visit(expr);
        }
    }

    public void visit(GaussDBPGInOperation op) {
        print(op);
        visit(op.getExpr());
        for (GaussDBPGExpression right : op.getListElements()) {
            visit(right);
        }
    }

    public void visit(GaussDBPGLikeOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBPGSelect op) {
        visit(op.getWhereClause());
    }

    public static String asExpectedValues(GaussDBPGExpression expr) {
        GaussDBPGExpectedValueVisitor v = new GaussDBPGExpectedValueVisitor();
        v.visit(expr);
        return v.get();
    }
}