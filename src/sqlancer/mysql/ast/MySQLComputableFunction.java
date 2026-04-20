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
        },
        // ========== 数学函数 ==========
        ABS(1, "ABS") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                long val = arg.castAs(CastType.SIGNED).getInt();
                return MySQLConstant.createIntConstant(Math.abs(val));
            }
        },
        CEIL(1, "CEIL") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                // 对于整数类型，CEIL 返回原值
                if (arg.isInt()) {
                    return arg;
                }
                // 对于字符串，尝试转换为数值后向上取整
                try {
                    String str = arg.castAsString();
                    double val = Double.parseDouble(str);
                    return MySQLConstant.createIntConstant((long) Math.ceil(val));
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            }
        },
        FLOOR(1, "FLOOR") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                if (arg.isInt()) {
                    return arg;
                }
                try {
                    String str = arg.castAsString();
                    double val = Double.parseDouble(str);
                    return MySQLConstant.createIntConstant((long) Math.floor(val));
                } catch (NumberFormatException e) {
                    throw new IgnoreMeException();
                }
            }
        },
        ROUND(1, "ROUND") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                long val = arg.castAs(CastType.SIGNED).getInt();
                return MySQLConstant.createIntConstant(val);
            }
        },
        MOD(2, "MOD") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg1 = evaluatedArgs[0];
                MySQLConstant arg2 = evaluatedArgs[1];
                if (arg1.isNull() || arg2.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                long val1 = arg1.castAs(CastType.SIGNED).getInt();
                long val2 = arg2.castAs(CastType.SIGNED).getInt();
                if (val2 == 0) {
                    return MySQLConstant.createNullConstant();  // 除数为 0 返回 NULL
                }
                return MySQLConstant.createIntConstant(val1 % val2);
            }
        },
        SIGN(1, "SIGN") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                long val = arg.castAs(CastType.SIGNED).getInt();
                if (val < 0) return MySQLConstant.createIntConstant(-1);
                if (val == 0) return MySQLConstant.createIntConstant(0);
                return MySQLConstant.createIntConstant(1);
            }
        },
        // ========== 字符串函数 ==========
        CONCAT(2, "CONCAT", true) {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                StringBuilder sb = new StringBuilder();
                for (MySQLConstant arg : evaluatedArgs) {
                    if (arg.isNull()) {
                        return MySQLConstant.createNullConstant();  // CONCAT 中任意 NULL 返回 NULL
                    }
                    sb.append(arg.castAsString());
                }
                return MySQLConstant.createStringConstant(sb.toString());
            }
        },
        LENGTH(1, "LENGTH") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                String str = arg.castAsString();
                return MySQLConstant.createIntConstant(str.length());
            }
        },
        UPPER(1, "UPPER") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                return MySQLConstant.createStringConstant(arg.castAsString().toUpperCase());
            }
        },
        LOWER(1, "LOWER") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                return MySQLConstant.createStringConstant(arg.castAsString().toLowerCase());
            }
        },
        TRIM(1, "TRIM") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                return MySQLConstant.createStringConstant(arg.castAsString().trim());
            }
        },
        LEFT(2, "LEFT") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant strArg = evaluatedArgs[0];
                MySQLConstant lenArg = evaluatedArgs[1];
                if (strArg.isNull() || lenArg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                String str = strArg.castAsString();
                int len = (int) lenArg.castAs(CastType.SIGNED).getInt();
                if (len < 0) {
                    return MySQLConstant.createStringConstant("");
                }
                if (len > str.length()) {
                    len = str.length();
                }
                return MySQLConstant.createStringConstant(str.substring(0, len));
            }
        },
        RIGHT(2, "RIGHT") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant strArg = evaluatedArgs[0];
                MySQLConstant lenArg = evaluatedArgs[1];
                if (strArg.isNull() || lenArg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                String str = strArg.castAsString();
                int len = (int) lenArg.castAs(CastType.SIGNED).getInt();
                if (len < 0) {
                    return MySQLConstant.createStringConstant("");
                }
                if (len > str.length()) {
                    len = str.length();
                }
                return MySQLConstant.createStringConstant(str.substring(str.length() - len));
            }
        },
        // ========== JSON 函数 ==========
        JSON_TYPE(1, "JSON_TYPE") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                // 简化实现：返回 JSON 值的类型
                String json = arg.castAsString();
                if (json.startsWith("{")) return MySQLConstant.createStringConstant("OBJECT");
                if (json.startsWith("[")) return MySQLConstant.createStringConstant("ARRAY");
                if (json.equals("null")) return MySQLConstant.createStringConstant("NULL");
                if (json.equals("true") || json.equals("false")) return MySQLConstant.createStringConstant("BOOLEAN");
                try {
                    Double.parseDouble(json);
                    return MySQLConstant.createStringConstant("INTEGER");
                } catch (NumberFormatException e) {
                    return MySQLConstant.createStringConstant("STRING");
                }
            }
        },
        JSON_VALID(1, "JSON_VALID") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                MySQLConstant arg = evaluatedArgs[0];
                if (arg.isNull()) {
                    return MySQLConstant.createNullConstant();
                }
                // 简化实现：假设生成的 JSON 都是有效的
                return MySQLConstant.createIntConstant(1);
            }
        },
        // ========== 时间函数（返回 null，因为依赖当前时间） ==========
        NOW(0, "NOW") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;  // 无法计算期望值
            }
        },
        CURDATE(0, "CURDATE") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;  // 无法计算期望值
            }
        },
        CURTIME(0, "CURTIME") {
            @Override
            public MySQLConstant apply(MySQLConstant[] evaluatedArgs, MySQLExpression... args) {
                return null;  // 无法计算期望值
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
