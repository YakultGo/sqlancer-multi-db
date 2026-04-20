package sqlancer.gaussdba;

import sqlancer.IgnoreMeException;
import sqlancer.gaussdba.ast.GaussDBAAggregate;
import sqlancer.gaussdba.ast.GaussDBABetweenOperation;
import sqlancer.gaussdba.ast.GaussDBABinaryLogicalOperation;
import sqlancer.gaussdba.ast.GaussDBACaseWhen;
import sqlancer.gaussdba.ast.GaussDBACastOperation;
import sqlancer.gaussdba.ast.GaussDBAColumnReference;
import sqlancer.gaussdba.ast.GaussDBAColumnValue;
import sqlancer.gaussdba.ast.GaussDBAConstant;
import sqlancer.gaussdba.ast.GaussDBAExpression;
import sqlancer.gaussdba.ast.GaussDBAInOperation;
import sqlancer.gaussdba.ast.GaussDBALikeOperation;
import sqlancer.gaussdba.ast.GaussDBASelect;
import sqlancer.gaussdba.ast.GaussDBATableReference;
import sqlancer.gaussdba.ast.GaussDBAUnaryPostfixOperation;
import sqlancer.gaussdba.ast.GaussDBAUnaryPrefixOperation;

public final class GaussDBAExpectedValueVisitor {

    private final StringBuilder sb = new StringBuilder();

    private void print(GaussDBAExpression expr) {
        GaussDBAToStringVisitor v = new GaussDBAToStringVisitor();
        v.visit(expr);
        sb.append(v.get());
        sb.append(" -- ");
        sb.append(expr.getExpectedValue());
        sb.append("\n");
    }

    public String get() {
        return sb.toString();
    }

    public void visit(GaussDBAExpression expr) {
        if (expr instanceof GaussDBAConstant) {
            visit((GaussDBAConstant) expr);
        } else if (expr instanceof GaussDBAColumnValue) {
            visit((GaussDBAColumnValue) expr);
        } else if (expr instanceof GaussDBAColumnReference) {
            visit((GaussDBAColumnReference) expr);
        } else if (expr instanceof GaussDBAUnaryPrefixOperation) {
            visit((GaussDBAUnaryPrefixOperation) expr);
        } else if (expr instanceof GaussDBAUnaryPostfixOperation) {
            visit((GaussDBAUnaryPostfixOperation) expr);
        } else if (expr instanceof GaussDBABinaryLogicalOperation) {
            visit((GaussDBABinaryLogicalOperation) expr);
        } else if (expr instanceof GaussDBABetweenOperation) {
            visit((GaussDBABetweenOperation) expr);
        } else if (expr instanceof GaussDBACaseWhen) {
            visit((GaussDBACaseWhen) expr);
        } else if (expr instanceof GaussDBACastOperation) {
            visit((GaussDBACastOperation) expr);
        } else if (expr instanceof GaussDBAAggregate) {
            visit((GaussDBAAggregate) expr);
        } else if (expr instanceof GaussDBAInOperation) {
            visit((GaussDBAInOperation) expr);
        } else if (expr instanceof GaussDBALikeOperation) {
            visit((GaussDBALikeOperation) expr);
        } else if (expr instanceof GaussDBASelect) {
            visit((GaussDBASelect) expr);
        } else if (expr instanceof GaussDBATableReference) {
            visit((GaussDBATableReference) expr);
        } else {
            throw new AssertionError(expr);
        }
    }

    public void visit(GaussDBAConstant constant) {
        print(constant);
    }

    public void visit(GaussDBAColumnValue c) {
        print(c);
    }

    public void visit(GaussDBAColumnReference column) {
        print(column);
    }

    public void visit(GaussDBATableReference tb) {
        // Table reference has no expected value
    }

    public void visit(GaussDBAUnaryPrefixOperation op) {
        print(op);
        visit(op.getExpr());
    }

    public void visit(GaussDBAUnaryPostfixOperation op) {
        print(op);
        visit(op.getExpr());
    }

    public void visit(GaussDBABinaryLogicalOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBABetweenOperation op) {
        print(op);
        visit(op.getExpr());
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBACaseWhen caseWhen) {
        throw new IgnoreMeException();
    }

    public void visit(GaussDBACastOperation cast) {
        print(cast);
        visit(cast.getExpr());
    }

    public void visit(GaussDBAAggregate op) {
        print(op);
        for (GaussDBAExpression expr : op.getArgs()) {
            visit(expr);
        }
    }

    public void visit(GaussDBAInOperation op) {
        print(op);
        visit(op.getExpr());
        for (GaussDBAExpression right : op.getListElements()) {
            visit(right);
        }
    }

    public void visit(GaussDBALikeOperation op) {
        print(op);
        visit(op.getLeft());
        visit(op.getRight());
    }

    public void visit(GaussDBASelect op) {
        visit(op.getWhereClause());
    }

    public static String asExpectedValues(GaussDBAExpression expr) {
        GaussDBAExpectedValueVisitor v = new GaussDBAExpectedValueVisitor();
        v.visit(expr);
        return v.get();
    }
}