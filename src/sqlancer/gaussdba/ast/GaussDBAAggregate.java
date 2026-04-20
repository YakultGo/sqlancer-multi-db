package sqlancer.gaussdba.ast;

import java.util.List;

import sqlancer.Randomly;

public class GaussDBAAggregate implements GaussDBAExpression {

    public enum GaussDBAAggregateFunction {
        COUNT, SUM, AVG, MIN, MAX;

        @Override
        public String toString() {
            return super.toString();
        }

        public static GaussDBAAggregateFunction getRandom() {
            return Randomly.fromOptions(values());
        }
    }

    private final List<GaussDBAExpression> args;
    private final GaussDBAAggregateFunction func;

    public GaussDBAAggregate(List<GaussDBAExpression> args, GaussDBAAggregateFunction func) {
        this.args = args;
        this.func = func;
    }

    public List<GaussDBAExpression> getArgs() {
        return args;
    }

    public GaussDBAAggregateFunction getFunc() {
        return func;
    }

    @Override
    public GaussDBAConstant getExpectedValue() {
        // 聚合函数的期望值不能在表达式级别计算
        return null;
    }

    @Override
    public GaussDBADataType getExpressionType() {
        switch (func) {
        case COUNT:
            return GaussDBADataType.NUMBER;
        case SUM:
        case AVG:
            return GaussDBADataType.NUMBER;
        case MIN:
        case MAX:
            if (!args.isEmpty()) {
                return args.get(0).getExpressionType();
            }
            return GaussDBADataType.NUMBER;
        default:
            throw new AssertionError(func);
        }
    }

    public static GaussDBAAggregate create(List<GaussDBAExpression> args, GaussDBAAggregateFunction func) {
        return new GaussDBAAggregate(args, func);
    }
}