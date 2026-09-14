# Legacy machine GUI restoration

## Goal

Restore the NeoForge machine screens to the IC2 1.20.1 layout while keeping the
current menus and machine behavior. The visible regressions were shared across
most machines: generic gray backgrounds, widened menus, misplaced slots, and a
large universal energy bar that did not match the legacy GUI.

## Implementation

- Copied all 77 legacy GUI resources into the NeoForge resource set. The source
  and destination directories compare byte-for-byte with `diff -qr`.
- Added `LegacyMachineGui` as the shared mapping for machine backgrounds, energy
  gauges, progress gauges, heat/fuel gauges, and legacy atlas regions.
- Restored the legacy menu dimensions and per-machine player inventory and slot
  coordinates in `MachineKind` and `MachineMenu`.
- Replaced the universal energy/progress rendering in `MachineScreen` with the
  matching legacy gauge for each machine family.
- Restored machine-specific tanks, gauges, labels, and compact controls in the
  specialized screen classes. This includes storage devices, charge pads,
  furnaces and processors, canning and fluid machines, nuclear machines,
  generators, miners, crop machines, replicator/scanner/pattern storage, and
  kinetic/heat machines.
- Added a dedicated pump screen and restored framed and unframed fluid rendering
  through `FluidTankDisplay`.

The legacy texture remains the source of the visual slot frame. The live slot is
positioned on the same coordinate by the menu, so rendering and interaction use
one layout definition per machine.

## Real-client checks

The checks below opened each block with an empty hand in a local world. James
performed the visual checks so the primary agent did not spend the image-viewing
budget. These checks cover successful opening and visual alignment; they do not
claim that every slot was exercised with an item.

| Machine | Acceptance result | Evidence |
| --- | --- | --- |
| MFE | 176 x 196 legacy layout; horizontal energy gauge; storage text and gauge do not overlap | `images/gui-restored-mfe.png` |
| Iron Furnace | 176 x 166 legacy layout; input, fuel, output, progress, and inventory art align | `images/gui-restored-iron-furnace.png` |
| Canning Machine | 176 x 184 legacy layout; both tanks and mode control align; no Inventory-label overlap | `images/gui-restored-canner.png` |
| Replicator | 176 x 184 legacy layout; compact controls do not clip; UU-Matter tank is at `(27, 30)` | `images/gui-restored-replicator.png` |

The corresponding pre-restoration MFE, Iron Furnace, and Canning Machine images
are retained as `images/gui-baseline-*.png` for comparison.

The client log contained only the already-known missing narrator/flite and X11
cursor messages during these checks; no machine-screen exception was observed.

## Control and text pass

The follow-up audit found that several specialized screens drew modern vanilla
buttons over icons already present in the legacy texture. `MachineScreen` now
provides transparent legacy control regions with click, hover, enable and
tooltip handling. Canner, advanced miner, pattern storage, replicator, UU
scanner and fluid controls use these regions, so their original artwork remains
the only visible icon layer.

`ContainerScreenBase.drawFittedText` constrains translated and live values to
the legacy label region and appends an ellipsis when necessary. Machine screens
also suppress the automatic vanilla Inventory label because the old `Ic2Gui`
base did not draw one; Trade-O-Mat and Energy-O-Mat add it explicitly at their
original coordinate. This removes the observed Advanced Miner, Pattern Storage
and UU Scanner overlap without moving their slots.

Additional exact restorations in this pass include:

- legacy tanks and controls for fermenter, fluid distributor/regulator and the
  complete 220-pixel steam-generator control panel;
- legacy two-column Energy-O-Mat keypad and storage/transformer tool icons;
- pattern-storage and replicator item previews, scanner progress overlay and
  replicator status region;
- the five-row weighted distributor priority matrix, with server-authoritative
  row moves and a regression test for insert, reorder, remove and front-face
  rejection.

The first follow-up client pass rechecked the four screens that had visible
overlap reports. Advanced Miner, Pattern Storage, UU Scanner and Energy-O-Mat
all opened with their labels, icons and slots separated. The before/after
captures for the first three are retained as `images/gui-overlap-*.png` and
`images/gui-overlap-after-*.png`; Energy-O-Mat was verified interactively
without adding another screenshot.

Opening Advanced Miner during this pass also exposed a live menu contract bug:
the menu bound four upgrade slots while its block entity allocated only three.
The block entity now derives its inventory size from `MachineKind`, and a live
menu audit reads every bound slot so this failure cannot silently return.

## Validation

Run from the repository root:

```bash
./gradlew build --console=plain
git diff --check
```

The control/text pass completed 578 required GameTests in both IC2 and GT
energy modes. The runs used an isolated copy with unrelated local tag edits
reset to `HEAD`; the working tree's modified `toolbox_tools.json` otherwise
causes the pre-existing toolbox/dynamite admission test to fail.

The shared layouts and representative real-client checks remove the broad GUI
regression. Individual machines may still have small cosmetic differences that
can be corrected from their legacy screen without changing machine behavior.
