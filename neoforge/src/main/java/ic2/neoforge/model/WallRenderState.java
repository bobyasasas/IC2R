package ic2.neoforge.model;

import ic2.neoforge.machine.ObscuredWallBlockEntity;

import net.minecraft.world.item.DyeColor;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Immutable snapshot of an obscured wall: wall color plus up to six sampled face references. Safe
 * to share with meshing threads; identity of the reference states is intentional because block
 * states are interned globally.
 */
public record WallRenderState(DyeColor color, ObscuredWallBlockEntity.FaceData[] faces) {
    public static WallRenderState of(DyeColor color, ObscuredWallBlockEntity.FaceData[] faces) {
        return new WallRenderState(color, faces == null ? null : faces.clone());
    }

    public ObscuredWallBlockEntity.FaceData face(int index) {
        return this.faces == null ? null : this.faces[index];
    }

    public boolean isObscured() {
        if (this.faces == null) return false;
        for (ObscuredWallBlockEntity.FaceData data : this.faces) {
            if (data != null) return true;
        }
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof WallRenderState other)) return false;
        return other.color == this.color && sameFaces(other.faces, this.faces);
    }

    /** Face data arrays compare by value; record equals would compare the array references. */
    private static boolean sameFaces(
            ObscuredWallBlockEntity.FaceData @Nullable [] a,
            ObscuredWallBlockEntity.FaceData @Nullable [] b) {
        if (a == b) return true;
        if (a == null || b == null || a.length != b.length) return false;
        for (int index = 0; index < a.length; index++) {
            ObscuredWallBlockEntity.FaceData x = a[index];
            ObscuredWallBlockEntity.FaceData y = b[index];
            if (x == y) continue;
            if (x == null || y == null) return false;
            if (x.referenceState() != y.referenceState()
                    || x.referenceSide() != y.referenceSide()
                    || !Arrays.equals(x.colorMultipliers(), y.colorMultipliers())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * this.color.hashCode() + Arrays.deepHashCode(this.faces);
    }
}
