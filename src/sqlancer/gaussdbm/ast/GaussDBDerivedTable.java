package sqlancer.gaussdbm.ast;

public class GaussDBDerivedTable implements GaussDBExpression {

    private final GaussDBSelect subquery;
    private final String alias;

    public GaussDBDerivedTable(GaussDBSelect subquery, String alias) {
        this.subquery = subquery;
        this.alias = alias;
    }

    public GaussDBSelect getSubquery() {
        return subquery;
    }

    public String getAlias() {
        return alias;
    }
}
