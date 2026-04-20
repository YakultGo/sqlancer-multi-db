package sqlancer.gaussdbpg.ast;

public enum GaussDBPGDataType {
    INT, BOOLEAN, TEXT, DECIMAL, FLOAT, REAL, DATE, TIME, TIMESTAMP, TIMESTAMPTZ, INTERVAL;

    public boolean isNumeric() {
        return this == INT || this == DECIMAL || this == FLOAT || this == REAL;
    }
}