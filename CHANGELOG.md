# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [1.7.0] - 2026-11-04

Target database creation and schema duplication.

### Added

- A "Create Database" action on the target connection panel - creates the database if it
  doesn't exist yet on PostgreSQL/MySQL/SQL Server (not supported for Oracle; SQLite already
  creates its file automatically), then copies every table's structure from the source
  database into it, skipping any that already exist there. Covers both "the target has no
  database yet" and "duplicate this database under a new name" in one action.

### Fixed

- SQL Server's admin connection (used only for the Create Database step) now carries the same
  encrypt/trustServerCertificate/authentication options as the real target connection, instead
  of silently dropping them and failing a TLS handshake against a self-signed certificate the
  real connection was already configured to trust.
- Generated `CREATE TABLE` DDL now uses vendor-correct type names instead of one global ANSI
  mapping - SQL Server has none of `BOOLEAN`/`DOUBLE PRECISION`/`BLOB` as column types, and its
  own `TIMESTAMP` means a row-version column, not a date/time value; Postgres has no `BLOB`;
  Oracle has no native boolean type before 23c.

## [1.6.0] - 2026-11-04

Query log panel polish: a clearer way to collapse it, and telling source apart from target
at a glance.

### Added

- A collapse arrow in the panel's top-right corner, sized and placed as an obvious button -
  a second way to close the open tab besides clicking it again.
- Each Query Log line now carries a colored tag - yellow for the source database, green for
  the target - so which side a statement ran against is obvious without reading the URL.

### Fixed

- The collapse arrow started out small and easy to miss, sitting inline in the tab row - moved
  to its own header bar above the content, bigger and clearly a button.

## [1.5.0] - 2026-11-04

An Errors tab alongside the query log.

### Added

- A second "Errors" tab in the bottom panel, fed by migration-run errors - automatically grabs
  focus the moment a new error shows up unless the user is already looking at it. Replaces the
  old Execution-screen-only error log with one visible from any wizard screen.

### Fixed

- The Query Log tab could become unreachable once it was pinned open during a run and focus
  moved to Errors - pinning now only blocks collapsing the active tab, not switching to it.

## [1.4.0] - 2026-11-04

Redesigned the query log panel to look and behave like browser tabs.

### Added

- The query log panel was rebuilt as a generic, reusable bottom tab-strip component - tabs
  cluster bottom-left, click one to open it, click the open one again to collapse. Built with
  reuse in mind, so later tabs (like Errors, below) are a one-line addition.

## [1.3.0] - 2026-11-04

Let the query log be shown or hidden depending on what you're doing.

### Added

- The query log stays collapsed by default on the earlier wizard screens (Connect/Tables/
  Mapping, which only ever touch schema metadata, not real queries) but can be opened on
  request; it's forced open once a migration actually starts running queries.

## [1.2.0] - 2026-11-04

The first cut of the query log: a persistent panel showing the real SQL this app runs.

### Added

- A persistent panel, visible across every wizard screen, logging the actual
  `SELECT`/batched-`INSERT`/`CREATE TABLE` statements fxDb runs against the source and target
  databases as they happen.

## [1.1.0] - 2026-11-04

Background work: keep the UI responsive, and a couple of small fixes.

### Added

- A shared background-task helper and busy overlay, so blocking JDBC/file round trips (Test
  Connection, schema loading, auto-match) run off the JavaFX thread instead of freezing the UI.
- A Contributor Covenant Code of Conduct.

### Fixed

- The wizard stepper's connector line between steps now renders as a solid dash instead of a
  near-invisible hairline.

## [1.0.0] - 2026-11-03

First tagged release. A one-shot, database-agnostic migration tool: pick a source, pick a
target, review the auto-matched column mapping, run it, get a report.

### Added

- Core database-agnostic data model and `DataSource`/`DataSink`/`SchemaProvider` contracts
  shared by every adapter.
- JDBC support for PostgreSQL, MySQL, SQL Server, Oracle, and SQLite, with per-vendor SQL
  dialects (identifier quoting, DDL generation) isolated behind `SqlDialect`.
- File support for CSV, JSON, and Excel as both source and target, with schema inference
  from headers/samples.
- Auto-match column mapping (Jaro-Winkler name similarity + type compatibility scoring),
  with manual override and JSON profile save/reload.
- A synchronous transfer engine with batched reads/writes, cancellation, and a configurable
  error policy (abort, skip, or retry-then-skip) per table.
- A 5-screen JavaFX wizard: connection setup, table selection, mapping grid, execution
  progress, and a summary report.
- A splash screen and a step-through onboarding tour on first launch, reopenable on demand.
- SQL Server-specific connection options: authentication type (SQL auth, Windows integrated,
  Azure AD password), encrypt and trust-server-certificate toggles, and a free-text escape
  hatch for anything else `mssql-jdbc` supports.
- Support for loading a JDBC driver fxDb doesn't bundle (e.g. Oracle's `ojdbc`, which can't be
  redistributed) from a `drivers/` folder at startup.
- Native Windows packaging via `build-installer.ps1` (jpackage, app-image or a WiX installer),
  plus CI (build/test on every push and PR) and a release workflow that builds and publishes
  the installer to a GitHub Release on a `v*.*.*` tag push.

### Fixed

- Connection, query, and batch-write failures now surface the actual driver-reported error
  (message, SQL state, and any chained exception) instead of a generic "failed to connect/
  query/write" message.
- `DatabaseMetaData` lookups now scope to the connection's own catalog instead of passing
  `null`, which on servers where a login can see sibling databases (common on SQL Server) was
  pulling in tables that didn't belong to the target database.
- Table listing now excludes each vendor's own system/catalog schemas (SQL Server's `sys` and
  `INFORMATION_SCHEMA`, MySQL's `information_schema`/`performance_schema`/`mysql`/`sys`,
  Postgres's `pg_catalog`/`information_schema`), so schema introspection isn't swamped with
  hundreds of system objects a migration would never target.
