package ic2.core.machine;

/** Common extraction contract for stored work and work produced continuously each world tick. */
public interface WorkSupply {
    int available(long tick, int bandwidth);

    int extract(long tick, int bandwidth, int maximum);
}
