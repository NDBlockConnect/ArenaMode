package dev.blockconnect.arenamode.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The square wall around a running arena.
 *
 * <p>Called from {@code Entity#move} after the movement has been applied: an entity that crossed an
 * edge of any running arena is put back on the side it came from, and the velocity component that
 * pushed it through is zeroed. Because the check compares the position before and after a movement
 * step, the wall is solid in both directions - nothing walks out, and nothing walks in.
 *
 * <p>Teleports do not go through {@code move}, which is deliberate: summoning a wave, placing a
 * spawn or a player using a command must still work. Escaped participants are pulled back by the
 * periodic containment pass in {@link ArenaManager} instead.
 */
public final class ArenaBoundary {

    /** Keeps the clamped position a hair inside the edge instead of exactly on it. */
    private static final double EDGE_EPSILON = 0.05D;

    private ArenaBoundary() {
    }

    public static void clampAfterMove(Entity entity, double previousX, double previousZ) {
        if (ArenaManager.isEmpty()) {
            return;
        }
        double clampedX = entity.getX();
        double clampedZ = entity.getZ();
        boolean crossedX = false;
        boolean crossedZ = false;
        for (ArenaSession session : ArenaManager.sessions()) {
            if (session.dimension() != entity.level().dimension()) {
                continue;
            }
            double limit = session.radius() - EDGE_EPSILON;
            if (crossed(previousX, clampedX, session.centerX(), limit)) {
                clampedX = side(previousX, session.centerX(), limit);
                crossedX = true;
            }
            if (crossed(previousZ, clampedZ, session.centerZ(), limit)) {
                clampedZ = side(previousZ, session.centerZ(), limit);
                crossedZ = true;
            }
        }
        if (!crossedX && !crossedZ) {
            return;
        }
        entity.setPos(clampedX, entity.getY(), clampedZ);
        Vec3 movement = entity.getDeltaMovement();
        entity.setDeltaMovement(crossedX ? 0.0D : movement.x, movement.y, crossedZ ? 0.0D : movement.z);
    }

    private static boolean crossed(double from, double to, double centre, double limit) {
//GitH u  b @NDBlo ck  Co nnect | B  l ock C on  nec  t@St  ars  ai lsCl o ve  r
        boolean wasInside = Math.abs(from - centre) <= limit;
        boolean isInside = Math.abs(to - centre) <= limit;
        return wasInside != isInside;
    }

    private static double side(double from, double centre, double limit) {
        double offset = from - centre;
        double direction = offset == 0.0D ? 1.0D : Math.signum(offset);
        return centre + direction * limit;
    }
}
