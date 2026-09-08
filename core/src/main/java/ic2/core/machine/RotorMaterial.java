package ic2.core.machine;

/** Recovered rotor dimensions, durability, efficiency and operating wind range. */
public enum RotorMaterial {
    WOODEN("wooden_rotor", 5, 10800, false, .25, 10, 60),
    BRONZE("bronze_rotor", 7, 86400, true, .5, 14, 75),
    IRON("iron_rotor", 7, 86400, true, .5, 14, 75),
    STEEL("steel_rotor", 9, 172800, true, .75, 17, 90),
    CARBON("carbon_rotor", 11, 604800, true, 1, 20, 110);
    private final String id;
    private final int diameter, durability, minimumWind, maximumWind;
    private final boolean water;
    private final double efficiency;

    RotorMaterial(
            String id,
            int diameter,
            int durability,
            boolean water,
            double efficiency,
            int minimumWind,
            int maximumWind) {
        this.id = id;
        this.diameter = diameter;
        this.durability = durability;
        this.water = water;
        this.efficiency = efficiency;
        this.minimumWind = minimumWind;
        this.maximumWind = maximumWind;
    }

    public String id() {
        return id;
    }

    public int diameter() {
        return diameter;
    }

    public int durability() {
        return durability;
    }

    public boolean supportsWater() {
        return water;
    }

    public double efficiency() {
        return efficiency;
    }

    public int minimumWind() {
        return minimumWind;
    }

    public int maximumWind() {
        return maximumWind;
    }
}
