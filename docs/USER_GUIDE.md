# SQLancer PostgreSQL User Guide

## Basic Usage

```bash
java -jar target/sqlancer-2.0.0.jar \
  --host localhost \
  --port 5432 \
  --username postgres \
  --password your_password \
  postgres --oracle QUERY_PARTITIONING
```

## Oracles

| Oracle | Purpose |
|--------|---------|
| `NOREC` | Optimization correctness |
| `TLP_WHERE`, `HAVING`, `AGGREGATE`, `DISTINCT`, `GROUP_BY` | Ternary logic partitioning variants |
| `QUERY_PARTITIONING` | Combined TLP-style oracle |
| `PQS` | Pivoted query synthesis |
| `CERT` | Cardinality estimation |
| `DQP` | Differential query plans |
| `DQE` | SELECT/UPDATE/DELETE equivalence |
| `EET` | Equivalent expression transformation |
| `CODDTEST` | Constant-driven optimization testing |
| `FUZZER` | Random SQL generation |

## PostgreSQL Options

```bash
--pg-tables 3                     # Number of tables created per database
--pg-table-columns 10             # Number of columns in CREATE TABLE
--pg-generate-sql-num 3           # Mutation-stage SQL count budget
--pg-generate-rows-per-insert 5   # Maximum rows in one INSERT ... VALUES
--pg-index-model 0                # 0-6 index generation model
--test-foreign-keys true          # Prepare FK groups before mutation
--bombard true                    # Run PostgreSQL concurrent stress mode
--bombard-workers 8               # Worker count for bombard mode
```

## PQS/CERT Smoke

PQS and CERT need non-empty tables. A compact smoke command:

```bash
java -jar target/sqlancer-2.0.0.jar \
  --host localhost \
  --port 5432 \
  --username postgres \
  --password your_password \
  --num-threads 4 \
  --timeout-seconds 60 \
  postgres --pg-tables 1 \
  --pg-generate-sql-num 10 \
  --pg-generate-rows-per-insert 5 \
  --oracle PQS
```

## Data Types

The PostgreSQL branch generates and reads common PostgreSQL data types, including integer, boolean, text/varchar/char, decimal/float/real, money, bit strings, inet, date/time/timestamp/timestamptz/timetz/interval, JSON/JSONB, UUID, BYTEA, arrays, enum, and range types.

## Logs

Logs are written under `logs/postgres/` by default. Use `--log-each-select true` when a reproducible SELECT trace is needed.
