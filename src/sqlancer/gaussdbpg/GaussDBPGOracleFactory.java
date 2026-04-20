package sqlancer.gaussdbpg;

import java.util.ArrayList;
import java.util.List;

import sqlancer.OracleFactory;
import sqlancer.common.oracle.CompositeTestOracle;
import sqlancer.common.oracle.NoRECOracle;
import sqlancer.common.oracle.TLPWhereOracle;
import sqlancer.common.oracle.TestOracle;
import sqlancer.common.query.ExpectedErrors;
import sqlancer.gaussdbpg.gen.GaussDBPGExpressionGenerator;
import sqlancer.gaussdbpg.oracle.GaussDBPGFuzzer;
import sqlancer.gaussdbpg.oracle.GaussDBPGPivotedQuerySynthesisOracle;
import sqlancer.gaussdbpg.oracle.ext.GaussDBPGTLPDistinctOracle;
import sqlancer.gaussdbpg.oracle.ext.GaussDBPGTLPGroupByOracle;
import sqlancer.gaussdbpg.oracle.tlp.GaussDBPGTLPAggregateOracle;
import sqlancer.gaussdbpg.oracle.tlp.GaussDBPGTLPHavingOracle;

public enum GaussDBPGOracleFactory implements OracleFactory<GaussDBPGGlobalState> {

    TLP_WHERE {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            GaussDBPGExpressionGenerator gen = new GaussDBPGExpressionGenerator(globalState);
            ExpectedErrors expectedErrors = ExpectedErrors.newErrors()
                    .with(GaussDBPGErrors.getExpressionErrorStrings()).build();
            return new TLPWhereOracle<>(globalState, gen, expectedErrors);
        }
    },

    NOREC {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            GaussDBPGExpressionGenerator gen = new GaussDBPGExpressionGenerator(globalState);
            ExpectedErrors errors = ExpectedErrors.newErrors()
                    .with(GaussDBPGErrors.getExpressionErrorStrings()).build();
            return new NoRECOracle<>(globalState, gen, errors);
        }
    },

    QUERY_PARTITIONING {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            List<TestOracle<GaussDBPGGlobalState>> oracles = new ArrayList<>();
            oracles.add(TLP_WHERE.create(globalState));
            oracles.add(HAVING.create(globalState));
            oracles.add(AGGREGATE.create(globalState));
            return new CompositeTestOracle<>(oracles, globalState);
        }
    },

    HAVING {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGTLPHavingOracle(globalState);
        }
    },

    AGGREGATE {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGTLPAggregateOracle(globalState);
        }
    },

    DISTINCT {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGTLPDistinctOracle(globalState);
        }
    },

    GROUP_BY {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGTLPGroupByOracle(globalState);
        }
    },

    PQS {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGPivotedQuerySynthesisOracle(globalState);
        }

        @Override
        public boolean requiresAllTablesToContainRows() {
            return true;
        }
    },

    CERT {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            // Placeholder - returns TLP_WHERE for now
            return TLP_WHERE.create(globalState);
        }
    },

    DQP {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            // Placeholder - returns TLP_WHERE for now
            return TLP_WHERE.create(globalState);
        }
    },

    DQE {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            // Placeholder - returns TLP_WHERE for now
            return TLP_WHERE.create(globalState);
        }
    },

    FUZZER {
        @Override
        public TestOracle<GaussDBPGGlobalState> create(GaussDBPGGlobalState globalState) throws Exception {
            return new GaussDBPGFuzzer(globalState);
        }
    }
}