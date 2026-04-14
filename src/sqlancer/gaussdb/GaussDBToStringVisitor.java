package sqlancer.gaussdb;

import sqlancer.common.visitor.ToStringVisitor;
import sqlancer.gaussdb.ast.GaussDBBetweenOperation;
import sqlancer.gaussdb.ast.GaussDBBinaryComparisonOperation;
import sqlancer.gaussdb.ast.GaussDBBinaryLogicalOperation;
import sqlancer.gaussdb.ast.GaussDBColumnReference;
import sqlancer.gaussdb.ast.GaussDBConstant;
import sqlancer.gaussdb.ast.GaussDBExpression;
import sqlancer.gaussdb.ast.GaussDBJoin;
import sqlancer.gaussdb.ast.GaussDBSelect;
import sqlancer.gaussdb.ast.GaussDBTableReference;
import sqlancer.gaussdb.ast.GaussDBUnaryPostfixOperation;
import sqlancer.gaussdb.ast.GaussDBUnaryPrefixOperation;

public class GaussDBToStringVisitor extends ToStringVisitor<GaussDBExpression> {

    @Override
    public void visitSpecific(GaussDBExpression expr) {
        if (expr instanceof GaussDBSelect) {
            visit((GaussDBSelect) expr);
        } else if (expr instanceof GaussDBConstant) {
            visit((GaussDBConstant) expr);
        } else if (expr instanceof GaussDBColumnReference) {
            visit((GaussDBColumnReference) expr);
        } else if (expr instanceof GaussDBBinaryLogicalOperation) {
            visit((GaussDBBinaryLogicalOperation) expr);
        } else if (expr instanceof GaussDBBinaryComparisonOperation) {
            visit((GaussDBBinaryComparisonOperation) expr);
        } else if (expr instanceof GaussDBBetweenOperation) {
            visit((GaussDBBetweenOperation) expr);
        } else if (expr instanceof GaussDBTableReference) {
            visit((GaussDBTableReference) expr);
        } else if (expr instanceof GaussDBJoin) {
            visit((GaussDBJoin) expr);
        } else if (expr instanceof GaussDBUnaryPrefixOperation) {
            visit((GaussDBUnaryPrefixOperation) expr);
        } else if (expr instanceof GaussDBUnaryPostfixOperation) {
            visit((GaussDBUnaryPostfixOperation) expr);
        } else {
            throw new AssertionError(expr);
        }
    }

    public void visit(GaussDBSelect s) {
        sb.append("SELECT ");
        if (s.getFetchColumns() == null) {
            sb.append("*");
        } else {
            super.visit(s.getFetchColumns());
        }
        if (!s.getFromList().isEmpty()) {
            sb.append(" FROM ");
            super.visit(s.getFromList());
        }
        if (!s.getJoinList().isEmpty()) {
            sb.append(" ");
            super.visit(s.getJoinList());
        }
        if (s.getWhereClause() != null) {
            sb.append(" WHERE ");
            visit(s.getWhereClause());
        }
        if (!s.getGroupByExpressions().isEmpty()) {
            sb.append(" GROUP BY ");
            super.visit(s.getGroupByExpressions());
        }
        if (s.getHavingClause() != null) {
            sb.append(" HAVING ");
            visit(s.getHavingClause());
        }
        if (!s.getOrderByClauses().isEmpty()) {
            sb.append(" ORDER BY ");
            super.visit(s.getOrderByClauses());
        }
        if (s.getLimitClause() != null) {
            sb.append(" LIMIT ");
            visit(s.getLimitClause());
        }
    }

    public void visit(GaussDBConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    public void visit(GaussDBColumnReference column) {
        sb.append(column.getColumn().getTable().getName());
        sb.append(".");
        sb.append(column.getColumn().getName());
    }

    public void visit(GaussDBBinaryLogicalOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(" ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBBinaryComparisonOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(" ");
        sb.append(op.getOp().getTextRepr());
        sb.append(" ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBBetweenOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(op.isNegated() ? " NOT BETWEEN " : " BETWEEN ");
        // Important restriction from GaussDB M guide: x/y must not be expressions like (1 < 1).
        // Our generator enforces leaf nodes here; we still render whatever is provided.
        visit(op.getLeft());
        sb.append(" AND ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBTableReference ref) {
        sb.append(ref.getTable().getName());
    }

    public void visit(GaussDBJoin join) {
        switch (join.getJoinType()) {
        case INNER:
            sb.append("JOIN ");
            break;
        case LEFT:
            sb.append("LEFT JOIN ");
            break;
        case RIGHT:
            sb.append("RIGHT JOIN ");
            break;
        case CROSS:
            sb.append("CROSS JOIN ");
            break;
        default:
            throw new AssertionError(join.getJoinType());
        }
        visit(join.getTableReference());
        if (join.getJoinType() != GaussDBJoin.JoinType.CROSS) {
            sb.append(" ON ");
            visit(join.getOnCondition());
        }
    }

    public void visit(GaussDBUnaryPrefixOperation op) {
        sb.append("(");
        sb.append(op.getOp().getText());
        sb.append(" ");
        visit(op.getExpr());
        sb.append(")");
    }

    public void visit(GaussDBUnaryPostfixOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(" ");
        sb.append(op.getOp().getText());
        sb.append(")");
    }

    public static String asString(GaussDBExpression expr) {
        GaussDBToStringVisitor v = new GaussDBToStringVisitor();
        v.visit(expr);
        return v.get();
    }
}

