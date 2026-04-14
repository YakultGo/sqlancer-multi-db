package sqlancer.postgres.oracle.ext.eet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import sqlancer.postgres.ast.PostgresBetweenOperation;
import sqlancer.postgres.ast.PostgresBinaryComparisonOperation;
import sqlancer.postgres.ast.PostgresBinaryLogicalOperation;
import sqlancer.postgres.ast.PostgresCaseWhen;
import sqlancer.postgres.ast.PostgresCastOperation;
import sqlancer.postgres.ast.PostgresCollate;
import sqlancer.postgres.ast.PostgresColumnReference;
import sqlancer.postgres.ast.PostgresColumnValue;
import sqlancer.postgres.ast.PostgresConstant;
import sqlancer.postgres.ast.PostgresCteTableReference;
import sqlancer.postgres.ast.PostgresExpression;
import sqlancer.postgres.ast.PostgresInOperation;
import sqlancer.postgres.ast.PostgresLikeOperation;
import sqlancer.postgres.ast.PostgresPOSIXRegularExpression;
import sqlancer.postgres.ast.PostgresPostfixOperation;
import sqlancer.postgres.ast.PostgresPrefixOperation;
import sqlancer.postgres.ast.PostgresPrintedExpression;
import sqlancer.postgres.ast.PostgresSimilarTo;
import sqlancer.postgres.ast.PostgresTableReference;
import sqlancer.postgres.ast.PostgresText;

/**
 * Structural map/copy/replace on Postgres AST (EET recursive traversal + reducer support).
 */
public final class PostgresEETExpressionTree {

    private PostgresEETExpressionTree() {
    }

    public static PostgresExpression copyAst(PostgresExpression e) {
        return mapChildren(PostgresEETExpressionTree::copyAst, e);
    }

    public static PostgresExpression replaceNode(PostgresExpression root, PostgresExpression target,
            PostgresExpression replacement) {
        if (root == target) {
            return replacement;
        }
        return mapChildren(c -> replaceNode(c, target, replacement), root);
    }

    public static List<PostgresExpression> collectDfs(PostgresExpression root) {
        List<PostgresExpression> out = new ArrayList<>();
        collectDfs(root, out);
        return out;
    }

    private static void collectDfs(PostgresExpression e, List<PostgresExpression> out) {
        if (e == null) {
            return;
        }
        out.add(e);
        forEachChild(e, c -> collectDfs(c, out));
    }

    public static PostgresExpression mapChildren(Function<PostgresExpression, PostgresExpression> mapChild,
            PostgresExpression e) {
        if (e == null) {
            return null;
        }
        if (e instanceof PostgresText || e instanceof PostgresTableReference || e instanceof PostgresCteTableReference) {
            return e;
        }
        if (e instanceof PostgresColumnReference || e instanceof PostgresColumnValue || e instanceof PostgresConstant) {
            return e;
        }
        if (e instanceof PostgresBinaryLogicalOperation) {
            PostgresBinaryLogicalOperation b = (PostgresBinaryLogicalOperation) e;
            return new PostgresBinaryLogicalOperation(mapChild.apply(b.getLeft()), mapChild.apply(b.getRight()), b.getOp());
        }
        if (e instanceof PostgresBinaryComparisonOperation) {
            PostgresBinaryComparisonOperation b = (PostgresBinaryComparisonOperation) e;
            return new PostgresBinaryComparisonOperation(mapChild.apply(b.getLeft()), mapChild.apply(b.getRight()),
                    b.getOp());
        }
        if (e instanceof PostgresPrefixOperation) {
            PostgresPrefixOperation u = (PostgresPrefixOperation) e;
            return new PostgresPrefixOperation(mapChild.apply(u.getExpression()), u.getOperator());
        }
        if (e instanceof PostgresPostfixOperation) {
            PostgresPostfixOperation u = (PostgresPostfixOperation) e;
            return new PostgresPostfixOperation(mapChild.apply(u.getExpression()), u.getOperator());
        }
        if (e instanceof PostgresCaseWhen) {
            PostgresCaseWhen c = (PostgresCaseWhen) e;
            return new PostgresCaseWhen(mapChild.apply(c.getWhenExpr()), mapChild.apply(c.getThenExpr()),
                    mapChild.apply(c.getElseExpr()));
        }
        if (e instanceof PostgresBetweenOperation) {
            PostgresBetweenOperation b = (PostgresBetweenOperation) e;
            return new PostgresBetweenOperation(mapChild.apply(b.getExpr()), mapChild.apply(b.getLeft()),
                    mapChild.apply(b.getRight()), b.isSymmetric());
        }
        if (e instanceof PostgresPrintedExpression) {
            PostgresPrintedExpression p = (PostgresPrintedExpression) e;
            return new PostgresPrintedExpression(mapChild.apply(p.getOriginal()));
        }
        if (e instanceof PostgresCastOperation) {
            PostgresCastOperation c = (PostgresCastOperation) e;
            return new PostgresCastOperation(mapChild.apply(c.getExpression()), c.getCompoundType());
        }
        if (e instanceof PostgresInOperation) {
            PostgresInOperation i = (PostgresInOperation) e;
            List<PostgresExpression> list = new ArrayList<>();
            for (PostgresExpression x : i.getListElements()) {
                list.add(mapChild.apply(x));
            }
            return new PostgresInOperation(mapChild.apply(i.getExpr()), list, i.isTrue());
        }
        if (e instanceof PostgresLikeOperation) {
            PostgresLikeOperation l = (PostgresLikeOperation) e;
            return new PostgresLikeOperation(mapChild.apply(l.getLeft()), mapChild.apply(l.getRight()));
        }
        if (e instanceof PostgresSimilarTo) {
            PostgresSimilarTo s = (PostgresSimilarTo) e;
            return new PostgresSimilarTo(mapChild.apply(s.getString()), mapChild.apply(s.getSimilarTo()),
                    mapChild.apply(s.getEscapeCharacter()));
        }
        if (e instanceof PostgresPOSIXRegularExpression) {
            PostgresPOSIXRegularExpression r = (PostgresPOSIXRegularExpression) e;
            return new PostgresPOSIXRegularExpression(mapChild.apply(r.getString()), mapChild.apply(r.getRegex()),
                    r.getOp());
        }
        if (e instanceof PostgresCollate) {
            PostgresCollate c = (PostgresCollate) e;
            return new PostgresCollate(mapChild.apply(c.getExpr()), c.getCollate());
        }
        // Aggregates are complex nodes; keep them unchanged for reducer stability.
        return e;
    }

    private static void forEachChild(PostgresExpression e, java.util.function.Consumer<PostgresExpression> sink) {
        if (e == null) {
            return;
        }
        if (e instanceof PostgresBinaryLogicalOperation) {
            PostgresBinaryLogicalOperation b = (PostgresBinaryLogicalOperation) e;
            sink.accept(b.getLeft());
            sink.accept(b.getRight());
        } else if (e instanceof PostgresBinaryComparisonOperation) {
            PostgresBinaryComparisonOperation b = (PostgresBinaryComparisonOperation) e;
            sink.accept(b.getLeft());
            sink.accept(b.getRight());
        } else if (e instanceof PostgresPrefixOperation) {
            sink.accept(((PostgresPrefixOperation) e).getExpression());
        } else if (e instanceof PostgresPostfixOperation) {
            sink.accept(((PostgresPostfixOperation) e).getExpression());
        } else if (e instanceof PostgresCaseWhen) {
            PostgresCaseWhen c = (PostgresCaseWhen) e;
            sink.accept(c.getWhenExpr());
            sink.accept(c.getThenExpr());
            sink.accept(c.getElseExpr());
        } else if (e instanceof PostgresBetweenOperation) {
            PostgresBetweenOperation b = (PostgresBetweenOperation) e;
            sink.accept(b.getExpr());
            sink.accept(b.getLeft());
            sink.accept(b.getRight());
        } else if (e instanceof PostgresPrintedExpression) {
            sink.accept(((PostgresPrintedExpression) e).getOriginal());
        } else if (e instanceof PostgresCastOperation) {
            sink.accept(((PostgresCastOperation) e).getExpression());
        } else if (e instanceof PostgresInOperation) {
            PostgresInOperation i = (PostgresInOperation) e;
            sink.accept(i.getExpr());
            for (PostgresExpression x : i.getListElements()) {
                sink.accept(x);
            }
        } else if (e instanceof PostgresLikeOperation) {
            PostgresLikeOperation l = (PostgresLikeOperation) e;
            sink.accept(l.getLeft());
            sink.accept(l.getRight());
        } else if (e instanceof PostgresSimilarTo) {
            PostgresSimilarTo s = (PostgresSimilarTo) e;
            sink.accept(s.getString());
            sink.accept(s.getSimilarTo());
            sink.accept(s.getEscapeCharacter());
        } else if (e instanceof PostgresPOSIXRegularExpression) {
            PostgresPOSIXRegularExpression r = (PostgresPOSIXRegularExpression) e;
            sink.accept(r.getString());
            sink.accept(r.getRegex());
        } else if (e instanceof PostgresCollate) {
            sink.accept(((PostgresCollate) e).getExpr());
        }
    }

    public static boolean isEetReductionLeaf(PostgresExpression e) {
        return e instanceof PostgresText || e instanceof PostgresTableReference || e instanceof PostgresCteTableReference
                || e instanceof PostgresColumnReference || e instanceof PostgresColumnValue || e instanceof PostgresConstant;
    }
}

