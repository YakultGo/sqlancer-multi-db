package sqlancer.gaussdbm.ast;

public class GaussDBCteDefinition {

    private final String name;
    private final GaussDBSelect subquery;

    public GaussDBCteDefinition(String name, GaussDBSelect subquery) {
        this.name = name;
        this.subquery = subquery;
    }

    public String getName() {
        return name;
    }

    public GaussDBSelect getSubquery() {
        return subquery;
    }
}
