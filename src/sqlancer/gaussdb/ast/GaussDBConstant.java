package sqlancer.gaussdb.ast;

import sqlancer.Randomly;

public abstract class GaussDBConstant implements GaussDBExpression {

    public abstract String getTextRepresentation();

    public static GaussDBConstant createNullConstant() {
        return new GaussDBNullConstant();
    }

    public static GaussDBConstant createIntConstant(long val) {
        return new GaussDBIntConstant(val);
    }

    public static GaussDBConstant createStringConstant(String val) {
        return new GaussDBStringConstant(val);
    }

    public static GaussDBConstant createBooleanConstant(boolean val) {
        return new GaussDBBooleanConstant(val);
    }

    public static GaussDBConstant createRandomConstant() {
        switch (Randomly.fromOptions(0, 1, 2)) {
        case 0:
            return createIntConstant(Randomly.getNotCachedInteger(-100, 100));
        case 1:
            return createStringConstant(new Randomly().getString().substring(0, Math.min(10, new Randomly().getString().length())));
        case 2:
            return createNullConstant();
        default:
            throw new AssertionError();
        }
    }

    @Override
    public GaussDBConstant getExpectedValue() {
        return this;
    }

    public static final class GaussDBNullConstant extends GaussDBConstant {
        @Override
        public String getTextRepresentation() {
            return "NULL";
        }
    }

    public static final class GaussDBIntConstant extends GaussDBConstant {
        private final long val;

        GaussDBIntConstant(long val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return String.valueOf(val);
        }
    }

    public static final class GaussDBStringConstant extends GaussDBConstant {
        private final String val;

        GaussDBStringConstant(String val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            String escaped = val.replace("\\", "\\\\").replace("'", "''");
            return "'" + escaped + "'";
        }
    }

    public static final class GaussDBBooleanConstant extends GaussDBConstant {
        private final boolean val;

        GaussDBBooleanConstant(boolean val) {
            this.val = val;
        }

        @Override
        public String getTextRepresentation() {
            return val ? "TRUE" : "FALSE";
        }
    }
}

