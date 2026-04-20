package sqlancer.gaussdba.ast;

public enum GaussDBADataType {
    NUMBER, VARCHAR2, DATE, TIMESTAMP, CLOB, BLOB;

    public boolean isNumeric() {
        return this == NUMBER;
    }

    public boolean isString() {
        return this == VARCHAR2 || this == CLOB;
    }

    public boolean isTemporal() {
        return this == DATE || this == TIMESTAMP;
    }
}