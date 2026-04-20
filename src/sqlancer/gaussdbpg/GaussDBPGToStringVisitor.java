package sqlancer.gaussdbpg;

import sqlancer.common.visitor.ToStringVisitor;
import sqlancer.gaussdbpg.ast.GaussDBPGAggregate;
import sqlancer.gaussdbpg.ast.GaussDBPGBetweenOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryComparisonOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGBinaryLogicalOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGCaseWhen;
import sqlancer.gaussdbpg.ast.GaussDBPGCastOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnReference;
import sqlancer.gaussdbpg.GaussDBPGSchema.GaussDBPGColumn;
import sqlancer.gaussdbpg.ast.GaussDBPGColumnValue;
import sqlancer.gaussdbpg.ast.GaussDBPGConstant;
import sqlancer.gaussdbpg.ast.GaussDBPGDataType;
import sqlancer.gaussdbpg.ast.GaussDBPGExpression;
import sqlancer.gaussdbpg.ast.GaussDBPGInOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGJoin;
import sqlancer.gaussdbpg.ast.GaussDBPGLikeOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGSelect;
import sqlancer.gaussdbpg.ast.GaussDBPGTableReference;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPostfixOperation;
import sqlancer.gaussdbpg.ast.GaussDBPGUnaryPrefixOperation;

public class GaussDBPGToStringVisitor extends ToStringVisitor<GaussDBPGExpression> {

    @Override
    public void visitSpecific(GaussDBPGExpression expr) {
        if (expr instanceof GaussDBPGSelect) {
            visit((GaussDBPGSelect) expr);
        } else if (expr instanceof GaussDBPGConstant) {
            visit((GaussDBPGConstant) expr);
        } else if (expr instanceof GaussDBPGColumnReference) {
            visit((GaussDBPGColumnReference) expr);
        } else if (expr instanceof GaussDBPGColumnValue) {
            visit((GaussDBPGColumnValue) expr);
        } else if (expr instanceof GaussDBPGBinaryLogicalOperation) {
            visit((GaussDBPGBinaryLogicalOperation) expr);
        } else if (expr instanceof GaussDBPGBinaryComparisonOperation) {
            visit((GaussDBPGBinaryComparisonOperation) expr);
        } else if (expr instanceof GaussDBPGBetweenOperation) {
            visit((GaussDBPGBetweenOperation) expr);
        } else if (expr instanceof GaussDBPGTableReference) {
            visit((GaussDBPGTableReference) expr);
        } else if (expr instanceof GaussDBPGJoin) {
            visit((GaussDBPGJoin) expr);
        } else if (expr instanceof GaussDBPGUnaryPrefixOperation) {
            visit((GaussDBPGUnaryPrefixOperation) expr);
        } else if (expr instanceof GaussDBPGUnaryPostfixOperation) {
            visit((GaussDBPGUnaryPostfixOperation) expr);
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
        } else {
            throw new AssertionError(expr);
        }
    }

    public void visit(GaussDBPGSelect s) {
        sb.append("SELECT ");
        if (s.getSelectType() == GaussDBPGSelect.GaussDBPGSelectType.DISTINCT) {
            sb.append("DISTINCT ");
        }
        if (s.getFetchColumns() == null || s.getFetchColumns().isEmpty()) {
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
        if (s.getOffsetClause() != null) {
            sb.append(" OFFSET ");
            visit(s.getOffsetClause());
        }
    }

    public void visit(GaussDBPGConstant constant) {
        sb.append(constant.getTextRepresentation());
    }

    public void visit(GaussDBPGColumnReference column) {
        GaussDBPGColumn c = column.getColumn();
        if (c.getTable() != null) {
            sb.append(c.getTable().getName());
            sb.append(".");
        }
        sb.append(c.getName());
    }

    public void visit(GaussDBPGColumnValue columnValue) {
        GaussDBPGColumn c = columnValue.getColumn();
        if (c.getTable() != null) {
            sb.append(c.getTable().getName());
            sb.append(".");
        }
        sb.append(c.getName());
    }

    public void visit(GaussDBPGBinaryLogicalOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(" ");
        sb.append(op.getOp().getTextRepresentation());
        sb.append(" ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBPGBinaryComparisonOperation op) {
        sb.append("(");
        visit(op.getLeft());
        sb.append(" ");
        sb.append(op.getOp().getTextRepr());
        sb.append(" ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBPGBetweenOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(op.isNegated() ? " NOT BETWEEN " : " BETWEEN ");
        visit(op.getLeft());
        sb.append(" AND ");
        visit(op.getRight());
        sb.append(")");
    }

    public void visit(GaussDBPGTableReference ref) {
        sb.append(ref.getTable().getName());
    }

    public void visit(GaussDBPGJoin join) {
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
        if (join.getJoinType() != GaussDBPGJoin.GaussDBPGJoinType.CROSS) {
            sb.append(" ON ");
            visit(join.getOnCondition());
        }
    }

    public void visit(GaussDBPGUnaryPrefixOperation op) {
        sb.append("(");
        sb.append(op.getOp().getText());
        sb.append(" ");
        visit(op.getExpr());
        sb.append(")");
    }

    public void visit(GaussDBPGUnaryPostfixOperation op) {
        sb.append("(");
        visit(op.getExpr());
        sb.append(" ");
        sb.append(op.getOp().getText());
        sb.append(")");
    }

    public void visit(GaussDBPGCaseWhen caseWhen) {
        sb.append("CASE");
        for (int i = 0; i < caseWhen.getConditions().size(); i++) {
            sb.append(" WHEN ");
            visit(caseWhen.getConditions().get(i));
            sb.append(" THEN ");
            visit(caseWhen.getThenExpressions().get(i));
        }
        if (caseWhen.getElseExpression() != null) {
            sb.append(" ELSE ");
            visit(caseWhen.getElseExpression());
        }
        sb.append(" END");
    }

    public void visit(GaussDBPGCastOperation cast) {
        sb.append("CAST(");
        visit(cast.getExpr());
        sb.append(" AS ");
        sb.append(getTypeString(cast.getType()));
        sb.append(")");
    }

    private String getTypeString(GaussDBPGDataType type) {
        switch (type) {
        case INT:
            return "INTEGER";
        case BOOLEAN:
            return "BOOLEAN";
        case TEXT:
            return "TEXT";
        case DECIMAL:
            return "DECIMAL";
        case FLOAT:
            return "DOUBLE PRECISION";
        case REAL:
            return "REAL";
        case DATE:
            return "DATE";
        case TIME:
            return "TIME";
        case TIMESTAMP:
            return "TIMESTAMP";
        case TIMESTAMPTZ:
            return "TIMESTAMP WITH TIME ZONE";
        case INTERVAL:
            return "INTERVAL";
        default:
            throw new AssertionError(type);
        }
    }

    public void visit(GaussDBPGAggregate aggregate) {
        sb.append(aggregate.getFunc().toString());
        sb.append("(");
        if (!aggregate.getArgs().isEmpty()) {
            super.visit(aggregate.getArgs());
        } else {
            sb.append("*");
        }
        sb.append(")");
    }

    public void visit(GaussDBPGInOperation inOp) {
        sb.append("(");
        visit(inOp.getExpr());
        sb.append(inOp.isNegated() ? " NOT IN (" : " IN (");
        super.visit(inOp.getListElements());
        sb.append("))");
    }

    public void visit(GaussDBPGLikeOperation likeOp) {
        sb.append("(");
        visit(likeOp.getLeft());
        sb.append(likeOp.isNegated() ? " NOT LIKE " : " LIKE ");
        visit(likeOp.getRight());
        sb.append(")");
    }

    public static String asString(GaussDBPGExpression expr) {
        GaussDBPGToStringVisitor v = new GaussDBPGToStringVisitor();
        v.visit(expr);
        return v.get();
    }
}