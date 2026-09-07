# Development tests

This source set is loaded only by `:neoforge:runGameTestServer`. Its `ic2_tests` mod, test functions, datapack and structure are excluded from the production JAR.

`empty.nbt` is a deterministic gzip-compressed compound containing `size: [1, 1, 1]`, empty `entities` and `blocks` lists, and one `palette` entry `{Name: "minecraft:air"}`. No game world or player data is included.

The registration test checks the actual item registry and stack construction after bootstrap. The core test catches missing runtime module wiring. Neither test claims to validate client rendering or unported machines.
