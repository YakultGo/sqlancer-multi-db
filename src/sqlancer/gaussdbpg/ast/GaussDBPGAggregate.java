package sqlancer.gaussdbpg.ast;

import java.util.List;

public class GaussDBPGAggregate implements GaussDBPGExpression {

    public enum GaussDBPGAggregateFunction {
        COUNT, SUM, AVG, MIN, MAX;

        public static GaussDBPGAggregateFunction getRandom() {
            return sqlancer.Randomly.fromOptions(values());
        }
    }

    private final List<GaussDBPGExpression> args;
    private final GaussDBPGAggregateFunction func;

    public GaussDBPGAggregate(List<GaussDBPGExpression> args, GaussDBPGAggregateFunction func) {
        this.args = args;
        this.func = func;
    }

    @Override
    public GaussDBPGConstant getExpectedValue() {
        // Aggregates do not have simple expected values for PQS
        return null;
    }

    @Override
    public GaussDBPGDataType getExpressionType() {
        switch (func) {
        case COUNT:
            return GaussDBPGDataType.INT;
        case SUM:
            if (!args.isEmpty()) {
                return args.get(0).getExpressionType();
            }
            return GaussDBPGDataType.INT;
        case AVG:
            return GaussDBPGDataType.FLOAT;
        case MIN:
        case MAX:
            if (!args.isEmpty()) {
                return args.get(0).getExpressionType();
            }
            return null;
        default:
            throw new AssertionError(func);
        }
    }

    public List<GaussDBPGExpression> getArgs() {
        return args;
    }

    public GaussDBPGAggregateFunction getFunc() {
        return func;
    }
}