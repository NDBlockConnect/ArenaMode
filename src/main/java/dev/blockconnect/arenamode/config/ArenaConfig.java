package dev.blockconnect.arenamode.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain, serialisation-friendly arena settings.
 *
 * <p>Kept free of JSON annotations so the loader layer owns Gson and the rest of the mod stays
 * loader-agnostic.
 */
public final class ArenaConfig {

    /** Hard limits; the GUI sliders use the same range so the file cannot drift past them. */
    public static final double MIN_RADIUS = 4.0D;
    public static final double MAX_RADIUS = 128.0D;
    public static final int MIN_MAX_ENTITIES = 1;
    public static final int MAX_MAX_ENTITIES = 512;
    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 64;
    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 3600;

    /** Waves, in rotation order. Each one is an entity id, a count and a duration. */
    public List<Wave> waves = new ArrayList<>();

    /** Half-size of the square arena, in blocks, measured from the centre to each edge. */
    public double radius = 16.0D;

    /** Ceiling on how many arena entities may be alive at once. */
    public int maxEntities = 48;

    /** Keep rotating after the last wave instead of finishing the duel. */
    public boolean endless = false;

    /** Shuffle the rotation order every cycle, so every wave appears once per cycle. */
    public boolean randomOrder = false;

    /** Draw the arena edge as a particle outline for the player who owns the duel. */
    public boolean boundaryParticles = true;

    public ArenaConfig() {
        this.waves.add(new Wave("minecraft:zombie", 5, 180));
    }

    public void normalize() {
        if (this.waves == null) {
            this.waves = new ArrayList<>();
        }
        this.waves.removeIf(java.util.Objects::isNull);
        if (this.waves.isEmpty()) {
            this.waves.add(new Wave("minecraft:zombie", 5, 180));
        }
        for (Wave wave : this.waves) {
            wave.normalize();
        }
        this.radius = clamp(this.radius, MIN_RADIUS, MAX_RADIUS);
        this.maxEntities = (int) clamp(this.maxEntities, MIN_MAX_ENTITIES, MAX_MAX_ENTITIES);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
//G  i  tH  ub@  NDBlockConne ct | Bl oc  kC o nne ct @S  ta rsail sClov  er
    }

    /** One configured wave. */
    public static final class Wave {

        /** Entity id, e.g. {@code minecraft:zombie}. */
        public String entity = "minecraft:zombie";

        /** How many to summon each time this wave comes around. */
        public int count = 5;

        /** How long the wave lasts before the rotation moves on, in seconds. */
        public int seconds = 180;

        public Wave() {
        }

        public Wave(String entity, int count, int seconds) {
            this.entity = entity;
            this.count = count;
            this.seconds = seconds;
        }

        public Wave copy() {
            return new Wave(this.entity, this.count, this.seconds);
        }

        public void normalize() {
            if (this.entity == null || this.entity.isBlank()) {
                this.entity = "minecraft:zombie";
            }
            this.entity = this.entity.trim();
            if (this.entity.indexOf(':') < 0) {
                this.entity = "minecraft:" + this.entity;
            }
            this.count = (int) clamp(this.count, MIN_COUNT, MAX_COUNT);
            this.seconds = (int) clamp(this.seconds, MIN_SECONDS, MAX_SECONDS);
        }

        /** Short human-readable form used by the command status output. */
        public String describe() {
            return this.entity + " x" + this.count + " for " + this.seconds + "s";
        }
    }
}
