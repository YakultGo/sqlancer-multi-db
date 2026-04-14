package sqlancer.gaussdbm.ast;

public class GaussDBCteTableReference implements GaussDBExpression {

    private final String cteName;

    public GaussDBCteTableReference(String cteName) {
        this.cteName = cteName;
    }

    public String getCteName() {
        return cteName;
    }
}
