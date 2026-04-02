---
date: 2026-04-01
topic: project-cleanup
---

# Project Cleanup/Improvements

## What We're Building

Maintenance pass: remove dead code/dependencies, upgrade all
dependency/plugin/Gradle versions, and reorganize `libs.versions.toml`
with consistent grouping and sorting.

## Why This Approach

Requirements are explicit — no design decisions needed. Straightforward
cleanup executed in a single pass with test verification after each
change category.

## Key Decisions

- **Order of operations**: cleanup first (unused code/deps), then
  version upgrades, then toml reorganization — reduces noise in diffs
- **Version strategy**: upgrade to latest stable releases; skip
  alpha/beta/RC unless already used
- **toml organization**: group by domain (kotlin, compose, ktor,
  database, testing, etc.), alphabetical within groups

## Open Questions

- None — requirements are clear

## Next Steps

→ `/workflows:plan` for implementation details
