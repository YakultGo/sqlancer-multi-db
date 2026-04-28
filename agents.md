# SQLancer PostgreSQL Branch Guide

## Overview

- This branch keeps SQLancer core plus the PostgreSQL provider only.
- Non-PostgreSQL DBMS providers, tests, scripts, and JDBC dependencies have been removed.
- Main work areas are PostgreSQL generation, oracles, schema/type support, logging, and reproducibility.

## Build

```bash
source ~/.zshrc
mvn -q -DskipTests compile
mvn -q package -DskipTests
```

## Run

```bash
java -jar target/sqlancer-2.0.0.jar \
  --host localhost \
  --port 5432 \
  --username postgres \
  --password password \
  --num-tries 1 \
  --timeout-seconds 100 \
  --log-each-select true \
  --num-threads 2 \
  postgres --oracle QUERY_PARTITIONING
```

## Logs

- Logs are written under `logs/postgres/` by default.
- Assertion mismatches with logged comparison output usually indicate a potential PostgreSQL logic bug.
- Tool crashes should be reduced with the generated reproduction logs before diagnosis.
