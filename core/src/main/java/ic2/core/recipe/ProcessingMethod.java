package ic2.core.recipe;

/** Recipe families are independent of the machine selecting them. IDs match existing datapacks. */
public enum ProcessingMethod {
    MACERATOR("macerator"),
    EXTRACTOR("extractor"),
    COMPRESSOR("compressor"),
    METAL_FORMER_EXTRUDING("metal_former_extruding"),
    METAL_FORMER_ROLLING("metal_former_rolling"),
    METAL_FORMER_CUTTING("metal_former_cutting"),
    BLOCK_CUTTER("block_cutter");
    private final String id;

    ProcessingMethod(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
