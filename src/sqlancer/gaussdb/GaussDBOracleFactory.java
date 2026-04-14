package sqlancer.gaussdb;

import sqlancer.OracleFactory;
import sqlancer.common.oracle.TLPWhereOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.gaussdb.gen.GaussDBExpressionGenerator;

public enum GaussDBOracleFactory implements OracleFactory<GaussDBGlobalState> {
    TLP_WHERE {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            GaussDBExpressionGenerator gen = new GaussDBExpressionGenerator(globalState);
            return new TLPWhereOracle<>(globalState, gen, GaussDBErrors.getExpressionErrors());
        }
    },
    QUERY_PARTITIONING {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            // Composite oracle entrypoint. Currently aliases to TLP_WHERE until additional GaussDB(M) generators/oracles are added.
            return TLP_WHERE.create(globalState);
        }
    },
    HAVING {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    AGGREGATE {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    PQS {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    CERT {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    DQP {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    DQE {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    },
    FUZZER {
        @Override
        public TestOracle<GaussDBGlobalState> create(GaussDBGlobalState globalState) throws Exception {
            return TLP_WHERE.create(globalState);
        }
    };
}

