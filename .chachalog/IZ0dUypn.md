---
# Allowed version bumps: patch, minor, major
tools: minor
---

Added wire packages analyzer (#304)

Adds a new tool to identify which active Jahia modules are wired at runtime to specific Java packages, useful to detect modules that still depend on packages scheduled for removal (deprecated packages).
