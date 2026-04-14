package sqlancer.gaussdbm.ast;

import java.util.List;

public class GaussDBAggregate implements GaussDBExpression {

    public enum GaussDBAggregateFunction {
        COUNT("COUNT", null, false), COUNT_DISTINCT("COUNT", "DISTINCT", true),
        SUM("SUM", null, false), SUM_DISTINCT("SUM", "DISTINCT", false),
        MIN("MIN", null, false), MIN_DISTINCT("MIN", "DISTINCT", false),
        MAX("MAX", null, false), MAX_DISTINCT("MAX", "DISTINCT", false);

        private final String name;
        private final String option;
        private final boolean variadic;

        GaussDBAggregateFunction(String name, String option, boolean variadic) {
            this.name = name;
            this.option = option;
            this.variadic = variadic;
        }

        public String getName() {
            return name;
        }

        public String getOption() {
            return option;
        }

        public boolean isVariadic() {
            return variadic;
        }
    }

    private final List<GaussDBExpression> exprs;
    private final GaussDBAggregateFunction func;

    public GaussDBAggregate(List<GaussDBExpression> exprs, GaussDBAggregateFunction func) {
        this.exprs = exprs;
        this.func = func;
    }

    public List<GaussDBExpression> getExprs() {
        return exprs;
    }

    public GaussDBAggregateFunction getFunc() {
        return func;
    }
}
