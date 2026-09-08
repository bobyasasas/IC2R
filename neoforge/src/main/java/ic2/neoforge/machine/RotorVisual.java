package ic2.neoforge.machine;

import net.minecraft.resources.Identifier;

/** Small observer-facing description; no client renderer classes enter common machine code. */
public interface RotorVisual {
    int rotorDiameter();

    float rotorDegreesPerTick();

    Identifier rotorTexture();
}
