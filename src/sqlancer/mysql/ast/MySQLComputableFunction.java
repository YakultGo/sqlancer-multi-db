package sqlancer.mysql.ast;

import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import sqlancer.IgnoreMeException;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.ast.MySQLCastOperation.CastType;

public class MySQLComputableFunction implements MySQLExpression {

    private final MySQLFunction func;
    private final MySQLExpression[] args;

    public MySQLComputableFunction(MySQLFunction func, MySQLExpression... args) {
        this.func = func;
        this.args = args.clone();
    }

    public MySQLFunction getFunction() {
        return func;
    }

    public MySQLExpression[] getArguments() {
        return args.clone();
    }

    public enum MySQLFunction {

        // ABS(1, "ABS") {
        // @Override
        // public MySQLConstant apply(MySQLConstant[] args, MySQLExpression[] origArgs) {
        // if (args[0].isNull()) {
        // return MySQLConstant.createNullConstant();
        // }
        // MySQLConstant intVal = args[0].castAs(CastType.SIGNED);
        // return MySQLConstant.createIntConstant(Math.abs(intVal.getInt()));
        // }
        // },
        /**
         * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/bit-functions.html#function_bit-count">Bit Functions
         *      and Operators</a>
         */
        BIT_COUNT(1, "BIT_COUNT") {

            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                } else {
                    long val = arg.castAs(CastType.SIGNED).getInt();
                    return MySQLConstant.createIntConstant(Long.bitCount(val));
                }
            }

        },
        // BENCHMARK(2, "BENCHMARK") {
        //
        // @Override
        // public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression[] args) {
        // if (evaluatedArgs[0].isNull()) {
        // return MySQLConstant.createNullConstant();
        // }
        // if (evaluatedArgs[0].castAs(CastType.SIGNED).getInt() < 0) {
        // return MySQLConstant.createNullConstant();
        // }
        // if (Math.abs(evaluatedArgs[0].castAs(CastType.SIGNED).getInt()) > 10) {
        // throw new IgnoreMeException();
        // }
        // return MySQLConstant.createIntConstant(0);
        // }
        //
        // },
        COALESCE(2, "COALESCE") {

            @Override
            public MySQLConstant apply(MySQLConstant[] args, MySQLExpression... origArgs) {
                MySQLConstant result = MySQLConstant.createNullConstant();
                for (MySQLConstant arg : args) {
                    if (!arg.isNull()) {
                        result = MySQLConstant.createStringConstant(arg.castAsString());
                        break;
                    }
                }
                return castToMostGeneralType(result, origArgs);
            }

            @Override
            public boolean isVariadic() {
                return true;
            }

        },
        /**
         * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/control-flow-functions.html#function_if">Flow Control
         *      Functions</a>
         */
        IF(3, "IF") {

            @Override
            public MySQLConstant apply(MySQLConstant[] args, MySQLExpression... origArgs) {
                MySQLConstant cond = args[0];
                MySQLConstant left = args[1];
                MySQLConstant right = args[2];
                MySQLConstant result;
                if (cond.isNull() || !cond.asBooleanNotNull()) {
                    result = right;
                } else {
                    result = left;
                }
                return castToMostGeneralType(result, new MySQLExpression[] { origArgs[1], origArgs[2] });

            }

        },
        /**
         * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/control-flow-functions.html#function_ifnull">IFNULL</a>
         */
        IFNULL(2, "IFNULL") {

            @Override
            public MySQLConstant apply(MySQLConstant[] args, MySQLExpression... origArgs) {
                MySQLConstant result;
                if (args[0].isNull()) {
                    result = args[1];
                } else {
                    result = args[0];
                }
                return castToMostGeneralType(result, origArgs);
            }

        },
        LEAST(2, "LEAST", true) {

            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return aggregate(evaluatedArgs, (min, cur) -> cur.isLessThan(min).asBooleanNotNull() ? cur : min);
            }

        },
        GREATEST(2, "GREATEST", true) {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return aggregate(evaluatedArgs, (max, cur) -> cur.isLessThan(max).asBooleanNotNull() ? max : cur);
            }
        };

        private String functionName;
        final int nrArgs;
        private final boolean variadic;

        private static MySQLConstant aggregate(MySQLConstant[] evaluatedArgs, BinaryOperator<MySQLConstant> op) {
            boolean containsNull = Stream.of(evaluatedArgs).anyMatch(arg -> arg.isNull());
            if (containsNull) {
                return MySQLConstant.createNullConstant();
            }
            MySQLConstant least = evaluatedArgs[1];
            for (MySQLConstant arg : evaluatedArgs) {
                MySQLConstant left = castToMostGeneralType(least, evaluatedArgs);
                MySQLConstant right = castToMostGeneralType(arg, evaluatedArgs);
                least = op.apply(right, left);
            }
            return castToMostGeneralType(least, evaluatedArgs);
        }

        MySQLFunction(int nrArgs, String functionName) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = false;
        }

        MySQLFunction(int nrArgs, String functionName, boolean variadic) {
            this.nrArgs = nrArgs;
            this.functionName = functionName;
            this.variadic = variadic;
        }

        /**
         * Gets the number of arguments if the function is non-variadic. If the function is variadic, the minimum number
         * of arguments is returned.
         *
         * @return the number of arguments
         */
        public int getNrArgs() {
            return nrArgs;
        }

        public abstract MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args);

        public static MySQLFunction getRandomFunction() {
            return Randomly.fromOptions(values());
        }

        @Override
        public String toString() {
            return functionName;
        }

        public boolean isVariadic() {
            return variadic;
        }

        public String getName() {
            return functionName;
        }
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant[] constants = new MySQLConstant[args.length];
        for (int i = 0; i < constants.length; i++) {
            constants[i] = args[i].getExpectedValue();
            if (constants[i] == null) {
                return null;
            }
        }
        return func.apply(constants, args);
    }

    public static MySQLConstant castToMostGeneralType(MySQLConstant cons, MySQLExpression... typeExpressions) {
        if (cons.isNull()) {
            return cons;
        }
        MySQLDataType type = getMostGeneralType(typeExpressions);
        switch (type) {
        case INT:
            if (cons.isInt()) {
                return cons;
            } else {
                return MySQLConstant.createIntConstant(cons.castAs(CastType.SIGNED).getInt());
            }
        case VARCHAR:
            return MySQLConstant.createStringConstant(cons.castAsString());
        case DATE:
            if (cons.getType() == MySQLDataType.DATE && cons instanceof MySQLConstant.MySQLDateConstant) {
                return cons;
            }
            return parseDate(cons.castAsString());
        case TIME:
            if (cons.getType() == MySQLDataType.TIME && cons instanceof MySQLConstant.MySQLTimeConstant) {
                return cons;
            }
            return parseTime(cons.castAsString());
        case DATETIME:
            if (cons.getType() == MySQLDataType.DATETIME && cons instanceof MySQLConstant.MySQLDateTimeConstant) {
                return cons;
            }
            return parseDateTime(cons.castAsString());
        case TIMESTAMP:
            if (cons.getType() == MySQLDataType.TIMESTAMP && cons instanceof MySQLConstant.MySQLTimestampConstant) {
                return cons;
            }
            return parseTimestamp(cons.castAsString());
        case YEAR:
            if (cons.getType() == MySQLDataType.YEAR && cons instanceof MySQLConstant.MySQLYearConstant) {
                return cons;
            }
            return parseYear(cons.castAsString());
        default:
            throw new IgnoreMeException();
        }
    }

    private static MySQLConstant parseDate(String s) {
        try {
            if (s == null) {
                throw new IgnoreMeException();
            }
            String[] parts = s.split("-");
            if (parts.length != 3) {
                throw new IgnoreMeException();
            }
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                throw new IgnoreMeException();
            }
            return new MySQLConstant.MySQLDateConstant(year, month, day);
        } catch (NumberFormatException e) {
            throw new IgnoreMeException();
        }
    }

    private static MySQLConstant parseTime(String s) {
        try {
            if (s == null) {
                throw new IgnoreMeException();
            }
            String base = s;
            int fsp = 0;
            int fraction = 0;
            int dot = s.indexOf('.');
            if (dot != -1) {
                base = s.substring(0, dot);
                String fracStr = s.substring(dot + 1);
                if (fracStr.isEmpty() || fracStr.length() > 6) {
                    throw new IgnoreMeException();
                }
                fsp = fracStr.length();
                fraction = Integer.parseInt(fracStr);
            }
            String[] parts = base.split(":");
            if (parts.length != 3) {
                throw new IgnoreMeException();
            }
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = Integer.parseInt(parts[2]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                throw new IgnoreMeException();
            }
            if (fsp > 0) {
                return new MySQLConstant.MySQLTimeConstant(hour, minute, second, fraction, fsp);
            }
            return new MySQLConstant.MySQLTimeConstant(hour, minute, second);
        } catch (RuntimeException e) {
            if (e instanceof IgnoreMeException) {
                throw e;
            }
            throw new IgnoreMeException();
        }
    }

    private static MySQLConstant parseDateTime(String s) {
        return parseDateTimeLike(s, true);
    }

    private static MySQLConstant parseTimestamp(String s) {
        return parseDateTimeLike(s, false);
    }

    private static MySQLConstant parseDateTimeLike(String s, boolean isDateTime) {
        try {
            if (s == null) {
                throw new IgnoreMeException();
            }
            String[] dtParts = s.split(" ");
            if (dtParts.length != 2) {
                throw new IgnoreMeException();
            }
            String datePart = dtParts[0];
            String timePart = dtParts[1];
            String[] dateParts = datePart.split("-");
            if (dateParts.length != 3) {
                throw new IgnoreMeException();
            }
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int day = Integer.parseInt(dateParts[2]);
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                throw new IgnoreMeException();
            }
            String timeBase = timePart;
            int fsp = 0;
            int fraction = 0;
            int dot = timePart.indexOf('.');
            if (dot != -1) {
                timeBase = timePart.substring(0, dot);
                String fracStr = timePart.substring(dot + 1);
                if (fracStr.isEmpty() || fracStr.length() > 6) {
                    throw new IgnoreMeException();
                }
                fsp = fracStr.length();
                fraction = Integer.parseInt(fracStr);
            }
            String[] t = timeBase.split(":");
            if (t.length != 3) {
                throw new IgnoreMeException();
            }
            int hour = Integer.parseInt(t[0]);
            int minute = Integer.parseInt(t[1]);
            int second = Integer.parseInt(t[2]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                throw new IgnoreMeException();
            }

            if (fsp > 0) {
                if (isDateTime) {
                    return new MySQLConstant.MySQLDateTimeConstant(year, month, day, hour, minute, second, fraction,
                            fsp);
                } else {
                    return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second, fraction,
                            fsp);
                }
            }
            if (isDateTime) {
                return new MySQLConstant.MySQLDateTimeConstant(year, month, day, hour, minute, second);
            } else {
                return new MySQLConstant.MySQLTimestampConstant(year, month, day, hour, minute, second);
            }
        } catch (RuntimeException e) {
            if (e instanceof IgnoreMeException) {
                throw e;
            }
            throw new IgnoreMeException();
        }
    }

    private static MySQLConstant parseYear(String s) {
        try {
            if (s == null) {
                throw new IgnoreMeException();
            }
            int year = Integer.parseInt(s.trim());
            return new MySQLConstant.MySQLYearConstant(year);
        } catch (NumberFormatException e) {
            throw new IgnoreMeException();
        }
    }

    public static MySQLDataType getMostGeneralType(MySQLExpression... expressions) {
        MySQLDataType type = null;
        for (MySQLExpression expr : expressions) {
            MySQLDataType exprType;
            if (expr instanceof MySQLColumnReference) {
                exprType = ((MySQLColumnReference) expr).getColumn().getType();
            } else {
                exprType = expr.getExpectedValue().getType();
            }
            if (type == null) {
                type = exprType;
            } else if (exprType == MySQLDataType.VARCHAR) {
                type = MySQLDataType.VARCHAR;
            }

        }
        return type;
    }

}
