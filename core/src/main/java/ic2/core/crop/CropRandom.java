package ic2.core.crop;

/** Minimal random source so crop rules stay free of Minecraft types. */
public interface CropRandom {
    int nextInt(int bound);
}
