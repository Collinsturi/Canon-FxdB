# Security Policy

## Reporting a vulnerability

Please report security issues privately rather than opening a public issue -
use GitHub's [private vulnerability reporting](../../security/advisories/new)
for this repository. If that isn't available, email the maintainer
(see the GitHub profile linked from the commit history) with details and
reproduction steps.

You should get an acknowledgement within a few days. There's no bug bounty -
this is a small open-source project - but reports are taken seriously and
credited in the fix unless you ask otherwise.

## Scope and known limitations

fxDb is a desktop migration tool that runs locally and talks directly to
whatever database or file you point it at. A few things worth knowing before
reporting (or worrying about) a finding:

- **Mapping profiles are not a credential store.** `MigrationProfile` JSON
  files deliberately exclude passwords when saved - if you're relying on them
  being safe to commit or share, double check what got written.
- **Connections use whatever driver you supply.** Drivers dropped into the
  `drivers/` folder (e.g. a self-sourced Oracle `ojdbc` jar) are loaded and
  trusted like any other dependency - only add drivers from sources you trust.
- **No network listener.** fxDb doesn't open any ports or accept remote
  connections; its attack surface is local (the files/DBs you feed it and the
  drivers on your classpath).

## Supported versions

There isn't a formal LTS policy yet - fixes land on `master` and the latest
release. If you're on an older build, update before filing a report tied to a
version-specific bug.
