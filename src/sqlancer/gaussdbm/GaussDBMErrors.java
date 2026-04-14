package sqlancer.gaussdbm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import sqlancer.common.query.ExpectedErrors;

public final class GaussDBMErrors {

    private GaussDBMErrors() {
    }

    private static List<String> expressionErrorStrings() {
        ArrayList<String> errors = new ArrayList<>();
        errors.add("syntax error");
        errors.add("invalid input syntax");
        errors.add("division by zero");
        errors.add("out of range");
        errors.add("BIGINT value is out of range");
        errors.add("Incorrect TIMESTAMP value");
        errors.add("Incorrect DATETIME value");
        errors.add("Incorrect DATE value");
        errors.add("Incorrect TIME value");
        errors.add("Data truncated");
        errors.add("Duplicate entry");
        return errors;
    }

    public static List<Pattern> getExpressionRegexErrors() {
        return new ArrayList<>();
    }

    public static void addExpressionErrors(ExpectedErrors errors) {
        errors.addAll(expressionErrorStrings());
        errors.addAllRegexes(getExpressionRegexErrors());
    }

    public static ExpectedErrors getExpressionErrors() {
        ExpectedErrors errors = new ExpectedErrors();
        addExpressionErrors(errors);
        return errors;
    }

    /**
     * Expected errors for HAVING and similar aggregate-context expressions (MySQL-compatible wording where applicable).
     */
    public static List<String> getExpressionHavingErrors() {
        ArrayList<String> errors = new ArrayList<>();
        errors.add("is not in GROUP BY clause");
        errors.add("contains nonaggregated column");
        errors.add("Unknown column");
        errors.add("which is not in GROUP BY");
        return errors;
    }

    public static void addExpressionHavingErrors(ExpectedErrors errors) {
        errors.addAll(getExpressionHavingErrors());
    }

    public static List<String> getInsertUpdateErrors() {
        ArrayList<String> errors = new ArrayList<>();
        errors.add("doesn't have a default value");
        errors.add("Data truncation");
        errors.add("Incorrect integer value");
        errors.add("Duplicate entry");
        errors.add("Data truncated for column");
        errors.add("cannot be null");
        errors.add("Incorrect decimal value");
        errors.add("The value specified for generated column");
        return errors;
    }

    public static void addInsertUpdateErrors(ExpectedErrors errors) {
        errors.addAll(getInsertUpdateErrors());
    }
}
