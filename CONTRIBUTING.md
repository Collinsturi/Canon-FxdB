# Contributing to fxDb

Thanks for taking a look. This is a small, young project, so the process is deliberately light.

## Getting set up

No Maven install needed - use the wrapper:

```
./mvnw.cmd test     # Windows
./mvnw test         # if you're ever on another OS
```

To actually run the app, see the [README](README.md#running-it) - specifically, run
`com.fxdb.ui.Launcher`, not `com.fxdb.ui.MainApp`, if you're launching from an IDE.

## Before opening a PR

- `./mvnw.cmd test` passes locally. CI will run the same thing, but catching it yourself first
  saves a round trip.
- If you added a source adapter, mapping behavior, or anything else with real logic, add a
  test for it. Look at `src/test/java/com/fxdb` for the existing shape - most tests run
  against real SQLite temp files or small fixtures rather than mocks.
- If you changed something user-visible in the JavaFX UI, mention in the PR description that
  you ran the app and clicked through the affected screens - automated tests don't cover the
  UI layer itself.
- Keep commits focused and the message explaining *why*, not just *what* the diff shows -
  skim `git log` for the style this repo already uses.

## Reporting a bug / requesting a feature

Open an issue using the templates - they ask for the minimum needed to act on it (repro steps
for a bug, the use case for a feature). Rough notes are fine; a clean write-up isn't required.

## Project layout, if you're orienting yourself

See the top of [README.md](README.md) - briefly: `core` is the database-agnostic interfaces,
`db`/`file` are the JDBC and flat-file adapters, `mapping` is auto-match and profile
persistence, `transfer` is the engine, `ui` is the JavaFX wizard.

## License

By contributing, you agree your contribution is licensed under this project's
[MIT License](LICENSE).
