package sqlancer.postgres.ast;

import sqlancer.Randomly;
import sqlancer.common.ast.BinaryOperatorNode;
import sqlancer.common.ast.BinaryOperatorNode.Operator;
import sqlancer.postgres.PostgresSchema.PostgresDataType;

/**
 * Represents JSON extraction operators in PostgreSQL.
 *
 * These operators extract values from JSON/JSONB data:
 * - ->: Get JSON object field by key (returns json/jsonb)
 * - ->>: Get JSON object field as text (returns text)
 * - #>: Get JSON value at specified path (returns json/jsonb)
 * - #>>: Get JSON value at specified path as text (returns text)
 *
 * Usage examples:
 * - '{"a": 1}'::jsonb->'a' returns 1 (as jsonb number)
 * - '{"a": 1}'::jsonb->>'a' returns '1' (as text)
 * - '{"a": {"b": 1}}'::jsonb#>'{a,b}' returns 1 (as jsonb number)
 * - '{"a": {"b": 1}}'::jsonb#>>'{a,b}' returns '1' (as text)
 */
public class PostgresBinaryJsonOperation
        extends BinaryOperatorNode<PostgresExpression, PostgresBinaryJsonOperation.JsonOperator>
        implements PostgresExpression {

    public enum JsonOperator implements Operator {
        // Get JSON object field by key (returns json/jsonb)
        GET_FIELD("->"),
        // Get JSON object field as text (returns text)
        GET_FIELD_TEXT("->>"),
        // Get JSON value at path (returns json/jsonb)
        GET_PATH("#>"),
        // Get JSON value at path as text (returns text)
        GET_PATH_TEXT("#>>");

        private final String text;

        JsonOperator(String text) {
            this.text = text;
        }

        @Override
        public String getTextRepresentation() {
            return text;
        }

        public static JsonOperator getRandom() {
            return Randomly.fromOptions(values());
        }

        /**
         * Returns true if this operator returns text (->>, #>>).
         */
        public boolean returnsText() {
            return this == GET_FIELD_TEXT || this == GET_PATH_TEXT;
        }

        /**
         * Returns true if this operator uses a path (#>, #>>).
         */
        public boolean usesPath() {
            return this == GET_PATH || this == GET_PATH_TEXT;
        }
    }

    private final PostgresDataType returnType;

    public PostgresBinaryJsonOperation(JsonOperator op, PostgresExpression left, PostgresExpression right) {
        super(left, right, op);
        // Determine return type based on operator
        // ->> and #>> return text, others return JSONB (could also be JSON based on input)
        this.returnType = op.returnsText() ? PostgresDataType.TEXT : PostgresDataType.JSONB;
    }

    @Override
    public PostgresDataType getExpressionType() {
        return returnType;
    }

    @Override
    public PostgresConstant getExpectedValue() {
        // Cannot compute expected value for JSON operations without executing the query
        return null;
    }
}