package sqlancer.postgres.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.postgres.PostgresSchema.PostgresDataType;

/**
 * Represents JSON containment and existence operators in PostgreSQL.
 *
 * These operators check JSON containment and key existence (JSONB only):
 * - @>: Contains (left contains right)
 * - <@: Is contained by (left is contained in right)
 * - ?: Key/string exists in JSON object/array
 * - ?|: Any of these keys/strings exist
 * - ?&: All of these keys/strings exist
 *
 * Usage examples:
 * - '{"a":1, "b":2}'::jsonb @> '{"a":1}'::jsonb returns true
 * - '{"a":1}'::jsonb <@ '{"a":1, "b":2}'::jsonb returns true
 * - '{"a":1, "b":2}'::jsonb ? 'a' returns true
 * - '{"a":1, "b":2}'::jsonb ?| array['a','c'] returns true
 * - '{"a":1, "b":2}'::jsonb ?& array['a','b'] returns true
 */
public class PostgresJsonContainOperation
        extends BinaryOperatorNode<PostgresExpression, PostgresJsonContainOperation.JsonContainOperator>
        implements PostgresExpression {

    public enum JsonContainOperator implements Operator {
        // Contains (jsonb only) - left contains top-level keys/values of right
        CONTAINS("@>"),
        // Is contained by (jsonb only) - left is contained in right
        IS_CONTAINED("<@"),
        // Key/string exists (jsonb only) - checks if key exists in object or string matches array element
        KEY_EXISTS("?"),
        // Any key/string exists (jsonb only) - checks if any of the keys exist
        ANY_KEY_EXISTS("?|"),
        // All keys/strings exist (jsonb only) - checks if all keys exist
        ALL_KEYS_EXIST("?&");

        private final String text;

        JsonContainOperator(String text) {
            this.text = text;
        }

        @Override
        public String getTextRepresentation() {
            return text;
        }

        public static JsonContainOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        /**
         * Returns true if this operator requires array input for the right operand.
         * ?| and ?& operators require a text array on the right side.
         */
        public boolean requiresArrayInput() {
            return this == ANY_KEY_EXISTS || this == ALL_KEYS_EXIST;
        }

        /**
         * Returns true if this operator works with scalar/text input on the right.
         * ?, @>, <@ can work with jsonb or text on the right.
         */
        public boolean allowsScalarInput() {
            return this == KEY_EXISTS || this == CONTAINS || this == IS_CONTAINED;
        }
    }

    public PostgresJsonContainOperation(JsonContainOperator op, PostgresExpression left, PostgresExpression right) {
        super(left, right, op);
    }

    @Override
    public PostgresDataType getExpressionType() {
        return PostgresDataType.BOOLEAN;
    }

    @Override
    public PostgresConstant getExpectedValue() {
        // Cannot compute expected value for JSON containment operations without executing the query
        return null;
    }
}