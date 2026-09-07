package ic2.core.geometry;

import static ic2.core.geometry.FaceSelection.Face.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FaceSelectionTest {
    @Test
    void centersAndCornersWorkOnEveryFace() {
        for (var face : FaceSelection.Face.values()) {
            assertEquals(face, FaceSelection.select(face, .5, .5, .5));
            assertEquals(face.opposite(), FaceSelection.select(face, .25, .25, .25));
            assertEquals(face.opposite(), FaceSelection.select(face, .75, .75, .75));
        }
    }

    @Test
    void edgesUseWorldAxesAndInclusiveQuarterBoundaries() {
        assertEquals(WEST, FaceSelection.select(UP, .25, 1, .5));
        assertEquals(NORTH, FaceSelection.select(DOWN, .5, 0, .25));
        assertEquals(UP, FaceSelection.select(NORTH, .5, .75, 0));
        assertEquals(EAST, FaceSelection.select(SOUTH, .75, .5, 1));
        assertEquals(SOUTH, FaceSelection.select(WEST, 0, .5, .75));
        assertEquals(DOWN, FaceSelection.select(EAST, 1, .25, .5));
        assertEquals(NORTH, FaceSelection.select(NORTH, .2501, .7499, 0));
    }
}
