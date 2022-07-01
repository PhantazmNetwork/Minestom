package net.minestom.server.collision;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RayUtilsTest {
    @Test
    void manyHeightIntersections() {
        Pos rayStart = new Pos(12.812719819964117, 100.0, 16.498964891258396, 89.95482F, 0F);
        Vec rayDirection = new Vec(0.273, -0.0784, 0.0);
        BoundingBox collidableStatic = new BoundingBox(1, 1, 1);
        Vec staticCollidableOffset = new Vec(13.0, 99.0, 16.0);

        for(double y = 1; y < 10; y += 0.001D) {
            BoundingBox moving = new BoundingBox(0.6, y, 0.6);
            assertTrue(RayUtils.BoundingBoxIntersectionCheck(moving, rayStart, rayDirection, collidableStatic,
                    staticCollidableOffset), moving.toString());
        }
    }
}
