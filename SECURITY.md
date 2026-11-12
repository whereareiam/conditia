# Security Policy

Conditia decides whether a member or a player may do something, so a flaw that lets a check pass when its requirements do not hold matters, for example when a broken type, an unbound place or a removed scope allows instead of denying.

## Supported versions

Security fixes go into the next release built from `dev`. Only the latest release receives them; update to it before reporting.

## Reporting a vulnerability

Report vulnerabilities privately through [GitHub's private vulnerability reporting](https://github.com/whereareiam/conditia/security/advisories/new). Do not open a public issue, pull request, or Discord message with the details.

Include:

- the Conditia version and the host it runs in
- the requirements and the situation that pass when they should not
- steps to reproduce

You receive an answer on the report. The fix is released before the details are published, and the release notes name it as a security fix.
