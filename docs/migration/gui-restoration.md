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

## Validation

Run from the repository root:

```bash
./gradlew build --console=plain
git diff --check
```

The shared layouts and representative real-client checks remove the broad GUI
regression. Individual machines may still have small cosmetic differences that
can be corrected from their legacy screen without changing machine behavior.
