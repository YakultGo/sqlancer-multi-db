package sqlancer.gaussdbm.ast;

/**
 * Raw SQL fragment (e.g. outer-scope column refs for EET CTE/derived queries).
 */
public class GaussDBText implements GaussDBExpression {

    private final String text;

    public GaussDBText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        throw new AssertionError("GaussDBText");
    }
}
