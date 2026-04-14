package sqlancer.gaussdb;

import sqlancer.common.query.ExpectedErrors;

public final class GaussDBErrors {

    private GaussDBErrors() {
    }

    public static ExpectedErrors getExpressionErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        // Keep intentionally small; extend based on observed GaussDB(M) error messages.
        errors.add("syntax error");
        errors.add("invalid input syntax");
        errors.add("division by zero");
        errors.add("out of range");
        return errors;
    }
}

