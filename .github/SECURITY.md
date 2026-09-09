# Security Policy

## Supported versions

Security fixes are released for the latest `3.x` release. Older majors are not maintained.

| Version | Supported |
|---------|-----------|
| 3.x     | Yes       |
| < 3.0   | No        |

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

Report it through [GitHub's private vulnerability reporting](https://github.com/compress4j/compress4j/security/advisories/new).
If that is not available to you, email the maintainers listed in `build.gradle.kts`.

Include, as far as you can:

- the affected version,
- an archive or test case that reproduces the problem,
- what an attacker gains.

We aim to acknowledge a report within 5 working days, and to ship a fix or a mitigation plan within 30 days of
confirming it. You will be credited in the advisory unless you prefer otherwise.

## Handling untrusted archives

Compress4J extracts what an archive tells it to. When the archive comes from an untrusted source:

- **Path traversal** is rejected: entry paths are resolved canonically against the output directory, so `../` entries
  and writes through a symlink that points outside the output directory both fail.
- **Escaping symlinks** are allowed by default. Set
  `escapingSymlinkPolicy(EscapingSymlinkPolicy.DISALLOW)` to reject them outright, or `RELATIVIZE_ABSOLUTE` to rewrite
  absolute targets so they stay inside the output directory.
- **Decompression bombs** are not bounded by default. Set `maxEntries`, `maxEntrySize` and `maxTotalSize` on the
  extractor to cap what an archive may expand to; breaching a limit throws `ArchiveLimitExceededException` and cannot
  be suppressed by an error handler.

```java
try (var extractor = TarGzArchiveExtractor.builder(in)
        .escapingSymlinkPolicy(EscapingSymlinkPolicy.DISALLOW)
        .maxEntries(10_000)
        .maxEntrySize(100L * 1024 * 1024)
        .maxTotalSize(1024L * 1024 * 1024)
        .build()) {
    extractor.extract(outputDir);
}
```
