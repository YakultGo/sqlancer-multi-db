package sqlancer.gaussdbpg;

import java.util.ArrayList;
import java.util.List;

import sqlancer.common.query.ExpectedErrors;

public final class GaussDBPGErrors {

    private GaussDBPGErrors() {
        // Utility class
    }

    public static ExpectedErrors getExpressionErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("syntax error");
        errors.add("invalid input syntax");
        errors.add("division by zero");
        errors.add("value out of range");
        errors.add("integer out of range");
        errors.add("numeric field overflow");
        errors.add("invalid value for date");
        errors.add("data exception - string data right truncation");
        errors.add("cannot cast type");
        errors.add("operator does not exist");
        errors.add("function does not exist");
        errors.add("column does not exist");
        errors.add("relation does not exist");
        errors.add("table does not exist");
        return errors;
    }

    public static ExpectedErrors getGroupingErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("must appear in the GROUP BY clause");
        errors.add("nonaggregated column");
        return errors;
    }

    public static ExpectedErrors getInsertUpdateErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("violates not-null constraint");
        errors.add("violates unique constraint");
        errors.add("duplicate key value");
        errors.add("null value in column");
        errors.add("value too long for type");
        return errors;
    }

    public static ExpectedErrors getFetchErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        errors.add("invalid input syntax");
        errors.add("division by zero");
        return errors;
    }

    public static List<String> getExpressionErrorStrings() {
        List<String> errors = new ArrayList<>();
        errors.add("syntax error");
        errors.add("invalid input syntax");
        errors.add("division by zero");
        errors.add("value out of range");
        errors.add("integer out of range");
        errors.add("numeric field overflow");
        errors.add("invalid value for date");
        errors.add("data exception - string data right truncation");
        return errors;
    }

    public static void addExpressionErrors(ExpectedErrors errors) {
        errors.addAll(getExpressionErrorStrings());
    }

    public static void addFetchErrors(ExpectedErrors errors) {
        errors.add("invalid input syntax");
        errors.add("division by zero");
    }
}