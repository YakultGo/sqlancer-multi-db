# Release Notes

## PostgreSQL Branch Cleanup | 2026-04-28

- Removed non-PostgreSQL DBMS provider packages from `src/sqlancer`.
- Removed non-PostgreSQL tests and smoke scripts.
- Reduced provider registration to PostgreSQL only.
- Trimmed Maven dependencies to PostgreSQL/core dependencies and made Jackson explicit for PostgreSQL query-plan parsing.
- Replaced README and user guide content with PostgreSQL-only usage.

## v0.1.82 | 2026-04-24

- Enhanced PostgreSQL foreign key coverage with a setup phase that prepares referenced columns, FK columns, UNIQUE constraints, FOREIGN KEY constraints, and stable seed values before mutation statements.
- Added FK topology coverage for star references, chains, self-references, deferred two-table cycles, composite keys, and compatible existing columns.
- Added `--test-foreign-keys=true|false`.
- Routed FK setup INSERT/UPDATE values through stable value pools or NULL.
- Verified with `mvn -q -DskipTests compile` and `mvn -q package -DskipTests`.

## v0.1.81 | 2026-04-24

- Extended PostgreSQL DDL/DML random generation with additional DROP, ALTER, MERGE, COPY, type, function, rule, index, and expected-error handling.

## v0.1.80 and Earlier

- Historical mixed-DB release notes were removed from this PostgreSQL-only branch.
