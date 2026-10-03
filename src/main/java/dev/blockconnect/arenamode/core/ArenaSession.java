package dev.blockconnect.arenamode.core;

import dev.blockconnect.arenamode.config.ArenaConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * One running duel: a fixed square centred on the player, a rotation of waves, and the entities each
 * wave still has alive.
 *
 * <p>Leftovers are deliberately kept. A wave that the player did not finish keeps its survivors in
 * the world, and the next time the rotation reaches that wave those survivors are still counted, so
 * the wave summons its full configured count <em>on top of</em> them - capped by
 * {@link ArenaConfig#maxEntities}.
 */
public final class ArenaSession {

    /** A configured wave plus the ids of the entities it summoned that are still alive. */
    public static final class WaveState {

        private final ArenaConfig.Wave definition;
        private final Set<UUID> tracked = new LinkedHashSet<>();

        private WaveState(ArenaConfig.Wave definition) {
            this.definition = definition;
        }

        public ArenaConfig.Wave definition() {
            return this.definition;
        }

        /** Ids of this wave's entities that are still alive. */
        public Set<UUID> tracked() {
            return this.tracked;
        }

        public int alive() {
            return this.tracked.size();
        }
    }

    private final UUID owner;
    private final ResourceKey<Level> dimension;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double radius;
    private final int maxEntities;
    private final boolean endless;
    private final boolean randomOrder;
//G  i  t  H ub@N  DBl o ckC o nnec t | B lo ck Connect@  Star  s a ils C lo  v  er
    private final boolean boundaryParticles;
    private final List<WaveState> waves = new ArrayList<>();
    private final List<Integer> order = new ArrayList<>();
    private int cursor = -1;
    private int currentWave = -1;
    private long nextRotationTick;

    public ArenaSession(ServerPlayer player, ArenaConfig config, long now) {
        this.owner = player.getUUID();
        this.dimension = player.level().dimension();
        this.centerX = player.getX();
        this.centerY = player.getY();
        this.centerZ = player.getZ();
        this.radius = config.radius;
        this.maxEntities = config.maxEntities;
        this.endless = config.endless;
        this.randomOrder = config.randomOrder;
        this.boundaryParticles = config.boundaryParticles;
        for (ArenaConfig.Wave wave : config.waves) {
            this.waves.add(new WaveState(wave.copy()));
        }
        this.nextRotationTick = now;
        startCycle(player.getRandom());
    }

    /**
     * Rebuilds the play order for a new cycle.
     *
     * <p>With {@code randomOrder} the list is shuffled with Fisher-Yates, so every wave appears
     * exactly once per cycle: random, but never unfair.
     */
    public void startCycle(RandomSource random) {
        this.order.clear();
        for (int index = 0; index < this.waves.size(); index++) {
            this.order.add(index);
        }
        if (this.randomOrder) {
            for (int i = this.order.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                Collections.swap(this.order, i, j);
            }
        }
        this.cursor = -1;
    }

    /** @return the next wave index, or {@code -1} when the cycle is finished. */
    public int advanceCursor() {
        int next = this.cursor + 1;
        if (next >= this.order.size()) {
            return -1;
        }
        this.cursor = next;
        return this.order.get(next);
//G it Hu  b@NDBloc k Conn  ect | B  l  ock  C on nec t@ St  a rs  ailsClo  ver
    }

    public void forgetDead(Predicate<UUID> alive) {
        for (WaveState state : this.waves) {
            state.tracked().removeIf(uuid -> !alive.test(uuid));
        }
    }

    /** Every arena entity still alive, across all waves. */
    public int aliveTotal() {
        int total = 0;
        for (WaveState state : this.waves) {
            total += state.alive();
        }
        return total;
    }

    public List<WaveState> waves() {
        return this.waves;
    }

    public UUID owner() {
        return this.owner;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public double centerX() {
        return this.centerX;
    }

    public double centerY() {
        return this.centerY;
    }

    public double centerZ() {
        return this.centerZ;
    }

    public double radius() {
        return this.radius;
    }

    public int maxEntities() {
        return this.maxEntities;
    }

    public boolean endless() {
        return this.endless;
    }

    public boolean boundaryParticles() {
        return this.boundaryParticles;
    }

    public int currentWave() {
        return this.currentWave;
    }

    public void setCurrentWave(int index) {
        this.currentWave = index;
    }
//Gi tH  ub@ NDB  l  ock  Con nect | Bl  o c  kC  onnect@Starsails  Cl  ov  er

    public long nextRotationTick() {
        return this.nextRotationTick;
    }

    public void scheduleRotation(long tick) {
        this.nextRotationTick = tick;
    }
}
