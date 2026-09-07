package ic2.neoforge.fluid;

import net.neoforged.neoforge.fluids.FluidType;

/** Physical properties and appearance transcribed from the recovery baseline. */
public enum FluidDefinition {
    UU_MATTER("uu_matter", 3000, 3000, 0, 300, false, "uu_matter", "uu_matter", -12909261),
    CONSTRUCTION_FOAM(
            "construction_foam", 10000, 50000, 0, 300, false, "fluid_3", "hot_coolant", -199089630),
    COOLANT("coolant", 1000, 3000, 0, 300, false, "coolant", "coolant", -15443350),
    CREOSOTE("creosote", 10000, 50000, 0, 300, false, "fluid", "fluid", -331005940),
    HOT_COOLANT("hot_coolant", 1000, 3000, 0, 1200, false, "hot_coolant", "hot_coolant", -4904908),
    PAHOEHOE_LAVA("pahoehoe_lava", 50000, 250000, 10, 1200, false, "pahoehoe_lava", null, -8686484),
    BIOMASS("biomass", 1000, 3000, 0, 300, false, "fluid", "fluid", -1237485016),
    BIOGAS("biogas", 1000, 3000, 0, 300, true, "fluid_3", null, -188435879),
    DISTILLED_WATER(
            "distilled_water", 1000, 1000, 0, 300, false, "fluid_water", "fluid_water", -632331785),
    SUPERHEATED_STEAM("superheated_steam", -3000, 100, 0, 600, true, "fluid_3", null, -185797131),
    STEAM("steam", -800, 300, 0, 420, true, "fluid_3", null, -186852132),
    HOT_WATER("hot_water", 1000, 1000, 0, 350, false, "fluid_water", "fluid_water", -632884747),
    WEED_EX("weed_ex", 1000, 1000, 0, 300, false, "weed_ex", null, -16298220),
    AIR("air", -100, 500, 0, 300, true, "fluid_2", null, 1610481149),
    HYDROGEN("hydrogen", -100, 500, 0, 300, true, "fluid_2", null, -2034379563),
    OXYGEN("oxygen", 0, 500, 0, 300, true, "fluid_2", null, -2034581547),
    HEAVY_WATER("heavy_water", 1000, 1000, 0, 300, false, "fluid", "fluid", -45191196);
    private final String id, stillSprite, flowingSprite;
    private final int density, viscosity, luminosity, temperature, color;
    private final boolean gaseous;

    FluidDefinition(
            String id,
            int density,
            int viscosity,
            int luminosity,
            int temperature,
            boolean gaseous,
            String still,
            String flowing,
            int color) {
        this.id = id;
        this.density = density;
        this.viscosity = viscosity;
        this.luminosity = luminosity;
        this.temperature = temperature;
        this.gaseous = gaseous;
        this.color = color;
        stillSprite = "block/fluid/" + still + "_still";
        flowingSprite = flowing == null ? stillSprite : "block/fluid/" + flowing + "_flow";
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    public boolean gaseous() {
        return gaseous;
    }

    public String stillSprite() {
        return stillSprite;
    }

    public String flowingSprite() {
        return flowingSprite;
    }

    public FluidType.Properties properties() {
        return FluidType.Properties.create()
                .density(density)
                .viscosity(viscosity)
                .lightLevel(luminosity)
                .temperature(temperature)
                .descriptionId("fluid.ic2." + id);
    }
}
