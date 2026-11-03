# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

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
