# Forge migration reference

`forge-1.20.1` preserves the recovered source and build configuration from commit `5d292712bf792ca67a7e1e519216dd7c694cbac9` / tag `2.10.39-ex120-cannerfix1`.

This directory is intentionally excluded from the NeoForge build. Do not edit or reformat it: use it to characterize old behavior and port a bounded feature into `core` or `neoforge`. Update the migration ledger with the new implementation and its tests.

For a runnable historical build, use the `forge/1.20.1` branch with its original Gradle wrapper and Java 17 configuration. These build files are reference material, not a standalone nested project.
