package sqlancer.mysql.ast;

import java.util.function.BinaryOperator;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.mysql.ast.MySQLBinaryArithmeticOperation.MySQLBinaryArithmeticOperator;

/**
 * Represents binary arithmetic operations (*, /) in MySQL.
 * Similar to PostgreSQL's PostgresBinaryArithmeticOperation.
 */
public class MySQLBinaryArithmeticOperation extends BinaryOperatorNode<MySQLExpression, MySQLBinaryArithmeticOperator>
        implements MySQLExpression {

    public enum MySQLBinaryArithmeticOperator implements Operator {

        MULTIPLICATION("*") {
            @Override
            public MySQLConstant apply(MySQLConstant left, MySQLConstant right) {
                return applyArithmeticOperation(left, right, (l, r) -> l * r);
            }
        },
        DIVISION("/") {
            @Override
            public MySQLConstant apply(MySQLConstant left, MySQLConstant right) {
                // Handle division by zero - MySQL returns NULL for division by zero in some contexts
                return applyArithmeticOperation(left, right, (l, r) -> {
                    if (r == 0) {
                        // Division by zero returns NULL in MySQL for integer division
                        return null;
                    }
                    return l / r;
                });
            }
        };

        private String textRepresentation;

        private static MySQLConstant applyArithmeticOperation(MySQLConstant left, MySQLConstant right,
                BinaryOperator<Long> op) {
            if (left.isNull() || right.isNull()) {
                return MySQLConstant.createNullConstant();
            } else {
                // Convert both to integers for calculation
                MySQLConstant leftInt = left.castAs(MySQLCastOperation.CastType.SIGNED);
                MySQLConstant rightInt = right.castAs(MySQLCastOperation.CastType.SIGNED);
                if (leftInt == null || rightInt == null) {
                    return MySQLConstant.createNullConstant();
                }
                long leftVal = leftInt.getInt();
                long rightVal = rightInt.getInt();
                Long value = op.apply(leftVal, rightVal);
                if (value == null) {
                    return MySQLConstant.createNullConstant();
                }
                return MySQLConstant.createIntConstant(value);
            }
        }

        MySQLBinaryArithmeticOperator(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        @Override
        public String getTextRepresentation() {
            return textRepresentation;
        }

        public abstract MySQLConstant apply(MySQLConstant left, MySQLConstant right);

        public static MySQLBinaryArithmeticOperator getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public MySQLBinaryArithmeticOperation(MySQLExpression left, MySQLExpression right,
            MySQLBinaryArithmeticOperator op) {
        super(left, right, op);
    }

    @Override
    public MySQLConstant getExpectedValue() {
        MySQLConstant leftExpected = getLeft().getExpectedValue();
        MySQLConstant rightExpected = getRight().getExpectedValue();
        if (leftExpected == null || rightExpected == null) {
            return null;
        }
        return getOp().apply(leftExpected, rightExpected);
    }

}