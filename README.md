# SQLancer PostgreSQL Branch

This branch keeps the SQLancer core and the PostgreSQL provider only. Non-PostgreSQL DBMS providers, tests, JDBC dependencies, and dedicated smoke scripts have been removed to keep the branch focused on PostgreSQL testing.

## Build

```bash
source ~/.zshrc
mvn -q -DskipTests compile
mvn -q package -DskipTests
```

## Run PostgreSQL

```bash
java -jar target/sqlancer-2.0.0.jar \
  --host localhost \
  --port 5432 \
  --username postgres \
  --password your_password \
  --num-tries 1 \
  --timeout-seconds 100 \
  postgres --oracle QUERY_PARTITIONING
```

## PostgreSQL Coverage

Supported PostgreSQL oracles include NoREC, PQS, TLP variants, CERT, DQP, DQE, EET, CODDTEST, FUZZER, and the combined `QUERY_PARTITIONING` oracle.

The PostgreSQL provider covers extended data types such as temporal types, JSON/JSONB, UUID, BYTEA, arrays, enum, range, inet, bit strings, and money. It also includes PostgreSQL-specific DDL/DML coverage such as partitioning, indexes, statistics, sequences, views, rules, functions, foreign key setup, and bombard mode.

Logs are written under `logs/postgres/` by default.
