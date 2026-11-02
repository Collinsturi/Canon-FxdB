# fxDb

A JavaFX desktop app for migrating data between databases and flat files, regardless of
what either side actually is - PostgreSQL, MySQL, SQL Server, Oracle, SQLite, CSV, JSON or
Excel can all be a source or a target. Pick a source, pick a target, review the auto-matched
column mapping, run it, get a report. See `src/main/java/com/fxdb` for the package layout
(`core` = the database-agnostic interfaces, `db`/`file` = the adapters, `mapping` = auto-match
and profile persistence, `transfer` = the engine, `ui` = the JavaFX wizard).

## Running it

- From an IDE: run `com.fxdb.ui.Launcher`, **not** `com.fxdb.ui.MainApp` directly. JavaFX
  refuses to start when the main class is itself an `Application` subclass launched via the
  plain classpath (which is what IntelliJ's Run button does) - `Launcher` is a thin,
  non-`Application` indirection that gets past that check.
- From Maven: `mvn javafx:run` (unaffected by the above; the plugin launches its own way).
  No Maven installed? Use `./mvnw` (`mvnw.cmd` on Windows) instead - it downloads the pinned
  Maven version on first use, so there's nothing to install first.

## Testing

`./mvnw test` - everything runs against SQLite temp files, H2 in-memory databases, and small
fixture files, so no external services are needed. Real-vendor testing against Postgres/
MySQL/SQL Server/Oracle is manual/exploratory (e.g. local Docker containers), not automated.
CI (`.github/workflows/ci.yml`) runs the same build on every push and pull request.

## Packaging a native Windows build

```
./build-installer.ps1                 # app-image: a folder with fxDb.exe, no extra tooling
./build-installer.ps1 -Type exe       # a real installer - needs the WiX Toolset (v3) on PATH
```

The script builds a shaded jar (`mvn package`, via `maven-shade-plugin`) with every
dependency merged in, then wraps it with `jpackage`. fxDb's dependencies aren't JPMS modules,
so this deliberately skips `jlink`'s custom-runtime-image path (which only accepts explicit
modules) in favor of jpackage's simpler classpath-app mode - jpackage still builds its own
bundled JRE internally, just using JDK+JavaFX modules rather than trying to modularize every
dependency.

## Adding a JDBC driver fxDb doesn't bundle

Oracle's `ojdbc` driver is deliberately not bundled - its Maven Central artifact carries an
OTN license that restricts redistribution, so shipping it in fxDb's installer isn't something
to do by default. Drop the driver jar into a `drivers/` folder next to the app (next to
`fxDb.exe` for a packaged build, or the repo root when running from an IDE/Maven) and it'll
be picked up automatically at startup via `ExternalDriverLoader` - no rebuild needed. The same
mechanism works for any other JDBC driver not already on the classpath.

## License

MIT - see [LICENSE](LICENSE).
