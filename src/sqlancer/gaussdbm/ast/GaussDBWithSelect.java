package sqlancer.gaussdbm.ast;

import java.util.List;

public class GaussDBWithSelect implements GaussDBExpression {

    private final List<GaussDBCteDefinition> ctes;
    private final GaussDBSelect mainQuery;

    public GaussDBWithSelect(List<GaussDBCteDefinition> ctes, GaussDBSelect mainQuery) {
        this.ctes = ctes;
        this.mainQuery = mainQuery;
    }

    public List<GaussDBCteDefinition> getCtes() {
        return ctes;
    }

    public GaussDBSelect getMainQuery() {
        return mainQuery;
    }
}
