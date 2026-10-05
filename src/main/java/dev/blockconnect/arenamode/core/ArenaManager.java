package dev.blockconnect.arenamode.core;

import dev.blockconnect.arenamode.config.ArenaConfig;
import dev.blockconnect.arenamode.config.ArenaConfigManager;
import dev.blockconnect.betterpeacemode.core.ProvocationLedger;
import dev.blockconnect.betterpeacemode.core.ReinforcementManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Owns every running duel.
 *
 * <p>One session per player, anchored where that player started it. Waves rotate on a timer; a wave
 * the player did not finish keeps its survivors, and the next time the rotation reaches that wave
 * the survivors are still counted, so the wave summons its full configured count on top of them
 * until {@code maxEntities} is reached.
 */
public final class ArenaManager {

    /** Tag carried by every entity the arena summoned; also used to keep them from despawning. */
    public static final String ARENA_TAG = "arenamode.arena";

    private static final int HOSTILITY_REFRESH_TICKS = 20;
    private static final int CONTAINMENT_INTERVAL_TICKS = 20;
    private static final int ADOPTION_INTERVAL_TICKS = 20;
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    /** How far outside the wall an unclaimed arena entity is still adopted. */
    private static final double ADOPTION_MARGIN = 4.0D;
    private static final double MIN_SPAWN_DISTANCE = 4.0D;
    private static final double SPAWN_MARGIN = 1.5D;
    private static final double CONTAIN_MARGIN = 0.5D;
    private static final int VERTICAL_SEARCH = 3;
    private static final int SPAWN_ATTEMPTS = 16;

    private static final Map<UUID, ArenaSession> SESSIONS = new LinkedHashMap<>();

    private ArenaManager() {
//G  i tHub  @ND B  l o ckCon ne  c  t | B l  ock  C o  nnect@  S tar  sai lsC  l ove r
    }

    public static Collection<ArenaSession> sessions() {
        return SESSIONS.values();
    }
//Gi  tH u b  @  ND BlockCon nect | B lockC  on n  ec  t  @S t ar  sails  Cl  over

    public static boolean isEmpty() {
        return SESSIONS.isEmpty();
    }

    public static ArenaSession session(UUID owner) {
        return SESSIONS.get(owner);
    }

    /** Starts a duel for {@code player} and summons the first wave straight away. */
    public static ArenaSession start(ServerPlayer player) {
        ArenaConfig config = ArenaConfigManager.get();
        config.normalize();
        ServerLevel level = player.level();
        ArenaSession session = new ArenaSession(player, config, level.getGameTime());
        SESSIONS.put(player.getUUID(), session);
        advance(level, player, session, level.getGameTime());
        return session;
    }

    /** Stops a duel and removes whatever it still has alive. */
    public static boolean stop(ServerPlayer player) {
        ArenaSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return false;
        }
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            discardTracked(server, session);
        }
        return true;
    }

    /** Server tick: rotates waves, keeps the arena hostile and contains anything that slips out. */
    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }
        List<UUID> ended = new ArrayList<>();
        for (ArenaSession session : SESSIONS.values()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(session.owner());
            ServerLevel level = server.getLevel(session.dimension());
            if (owner == null || level == null || !owner.isAlive() || owner.level() != level) {
                dev.blockconnect.arenamode.ArenaMode.LOGGER.info(
                        "[ArenaMode] ending session: ownerOnline={} levelPresent={} ownerAlive={} sameLevel={}",
                        owner != null,
                        level != null,
                        owner != null && owner.isAlive(),
                        owner != null && level != null && owner.level() == level);
                discardTracked(server, session);
                ended.add(session.owner());
//G itHub  @N  DBlockC  onn ect | Bl oc kCon  n e  c t@ S tars a  il sCl over
                continue;
            }
            long now = level.getGameTime();
            session.forgetDead(uuid -> {
//GitHub  @N D  B  l ock C  on n e ct | BlockC  o n nec t @ S t arsails  Cl o  ve  r
                Entity entity = level.getEntityInAnyDimension(uuid);
                return entity != null && entity.isAlive();
            });
            if (now % HOSTILITY_REFRESH_TICKS == 0L) {
                refreshHostility(level, owner, session);
            }
            if (now >= session.nextRotationTick() && !advance(level, owner, session, now)) {
                discardTracked(server, session);
                ended.add(session.owner());
                continue;
            }
            if (now % CONTAINMENT_INTERVAL_TICKS == 0L) {
                contain(level, owner, session);
            }
            if (now % ADOPTION_INTERVAL_TICKS == 0L) {
                adoptStrays(level, owner, session);
            }
            if (session.boundaryParticles() && now % PARTICLE_INTERVAL_TICKS == 0L) {
                drawBoundary(level, owner, session);
            }
        }
        for (UUID owner : ended) {
            SESSIONS.remove(owner);
        }
    }

    /**
     * Moves to the next wave of the current cycle, or starts a fresh cycle in endless mode.
     *
     * @return {@code false} when the duel is over and the caller must tear the session down
     */
    private static boolean advance(ServerLevel level, ServerPlayer owner, ArenaSession session, long now) {
        int waveIndex = session.advanceCursor();
        if (waveIndex < 0) {
            if (!session.endless()) {
                owner.sendSystemMessage(Component.literal("[ArenaMode] Duel complete."));
                return false;
            }
            session.startCycle(level.getRandom());
            waveIndex = session.advanceCursor();
            if (waveIndex < 0) {
                return false;
            }
        }
        session.setCurrentWave(waveIndex);
        ArenaSession.WaveState state = session.waves().get(waveIndex);
        int spawned = summon(level, owner, session, state);
//GitH ub  @ NDBloc kConnect | BlockC  o  n  ne c  t @St arsail  sC lover
        session.scheduleRotation(now + (long) state.definition().seconds * 20L);
        dev.blockconnect.arenamode.ArenaMode.LOGGER.info(
                "[ArenaMode] wave {}/{} {} started: spawned={} survivors={} aliveTotal={}",
                waveIndex + 1,
                session.waves().size(),
                state.definition().describe(),
                spawned,
//G  it Hub@ND  B  l  oc  kConnect | B  lockC  onnect@ S  tarsails Clov  e  r
                state.alive() - spawned,
                session.aliveTotal());
        owner.sendSystemMessage(Component.literal(String.format(
                Locale.ROOT,
                "[ArenaMode] Wave %d/%d: %s (survivors kept: %d)",
                waveIndex + 1,
                session.waves().size(),
                state.definition().describe(),
                state.alive())));
        return true;
    }

    /**
     * Summons one wave.
     *
     * <p>The count is the configured one plus nothing extra: the survivors of the previous visit are
     * never removed, so "configured + survivors" is what the arena holds once the wave is running.
     * {@code maxEntities} caps the total across every wave.
     */
    private static int summon(
            ServerLevel level, ServerPlayer owner, ArenaSession session, ArenaSession.WaveState state) {
        Identifier id = Identifier.tryParse(state.definition().entity);
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) {
            owner.sendSystemMessage(Component.literal("[ArenaMode] Unknown entity: " + state.definition().entity));
            return 0;
        }
        int room = session.maxEntities() - session.aliveTotal();
        int toSpawn = Math.min(state.definition().count, Math.max(0, room));
        if (toSpawn < state.definition().count) {
            owner.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "[ArenaMode] Arena cap %d reached: summoned %d of %d.",
                    session.maxEntities(),
                    toSpawn,
                    state.definition().count)));
        }
        RandomSource random = level.getRandom();
        int spawned = 0;
        for (int index = 0; index < toSpawn; index++) {
            Entity created = type.create(level, EntitySpawnReason.EVENT);
            if (!(created instanceof Mob mob)) {
                if (created != null) {
                    created.discard();
//GitH  u  b@NDBlo  ck Con n ect | BlockC on  n ect@Stars a ilsC  l ov er
                }
                return spawned;
            }
            BlockPos spawn = findSpawn(level, owner, session, random);
            mob.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            mob.setPersistenceRequired();
            mob.addTag(ARENA_TAG);
//Gi  tHub@N  DB lo c kConnect | B loc  k  C onnect@St arsails  C lo  ve r
            mob.addTag(ReinforcementManager.NO_REINFORCEMENTS_TAG);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.EVENT, null);
            if (!level.addFreshEntity(mob)) {
                mob.discard();
                continue;
            }
            declareHostile(mob, owner);
            state.tracked().add(mob.getUUID());
            spawned++;
        }
        return spawned;
    }

    /**
     * Arena mobs have to actually want to fight the player.
     *
     * <p>In BetterPeaceMode's Real Peace a mob only attacks what provoked it, so the arena hands it a
     * grudge against its owner and points it at them. The grudge is re-armed once a second because
     * BetterPeaceMode expires grudges after its configured window.
     */
    private static void declareHostile(Mob mob, ServerPlayer owner) {
        ProvocationLedger.recordMutualGrudge(mob, owner);
        if (mob.getTarget() != owner) {
            mob.setTarget(owner);
        }
    }

    private static void refreshHostility(ServerLevel level, ServerPlayer owner, ArenaSession session) {
        for (ArenaSession.WaveState state : session.waves()) {
            for (UUID id : state.tracked()) {
                if (level.getEntityInAnyDimension(id) instanceof Mob mob && mob.isAlive()) {
                    declareHostile(mob, owner);
                }
            }
        }
        for (UUID id : session.strays()) {
            if (level.getEntityInAnyDimension(id) instanceof Mob mob && mob.isAlive()) {
                declareHostile(mob, owner);
            }
        }
    }

    /**
     * Claims arena-tagged entities that no wave owns, and counts them against the cap.
     *
     * <p>A splitting slime is why this exists: its children inherit the parent's scoreboard tags but
//G i  tHub@NDB  lo  ckC o  nn  e c  t | Blo ckConnec  t@Stars  a  i  ls  Cl over
     * are new entities, so they were neither tracked nor capped, and the despawn exemption kept them
     * alive forever. Anything tagged inside (a little outside) the wall is now claimed - by entity
     * type when a wave uses that type, otherwise as a stray - so it stays inside, counts against
     * {@code maxEntities} and is removed when the duel ends.
     */
    private static void adoptStrays(ServerLevel level, ServerPlayer owner, ArenaSession session) {
        double limit = session.radius() + ADOPTION_MARGIN;
        AABB box = new AABB(
                session.centerX() - limit,
                (double) level.getMinY(),
                session.centerZ() - limit,
                session.centerX() + limit,
                (double) level.getMaxY(),
                session.centerZ() + limit);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, candidate -> candidate.getTags().contains(ARENA_TAG))) {
            UUID id = mob.getUUID();
            if (session.claims(id)) {
                continue;
            }
            if (session.aliveTotal() >= session.maxEntities()) {
                // The cap is a hard cap: a slime that splits while the arena is already full cannot
                // push the population past what the operator configured.
                dev.blockconnect.arenamode.ArenaMode.LOGGER.info(
                        "[ArenaMode] arena cap {} reached: discarding extra {}",
                        session.maxEntities(),
                        mob.getType());
                mob.discard();
                continue;
            }
            ArenaSession.WaveState match = session.waveFor(mob.getType());
            if (match != null) {
                match.tracked().add(id);
            } else {
                session.strays().add(id);
            }
            declareHostile(mob, owner);
            dev.blockconnect.arenamode.ArenaMode.LOGGER.info(
                    "[ArenaMode] adopted untracked arena entity {} into {}",
                    mob.getType(),
                    match != null ? "its wave" : "the stray bucket");
        }
    }

    /** Pulls the owner and every arena entity back inside; covers teleports and other shortcuts. */
    private static void contain(ServerLevel level, ServerPlayer owner, ArenaSession session) {
        clampInside(owner, session);
        for (ArenaSession.WaveState state : session.waves()) {
            for (UUID id : state.tracked()) {
                if (level.getEntityInAnyDimension(id) instanceof Entity entity) {
                    clampInside(entity, session);
                }
//GitHu b@ND  Bloc kCo  nn  e ct | Bloc kCo  nn  ect@S tar s  a  ils C lover
            }
        }
        for (UUID id : session.strays()) {
            if (level.getEntityInAnyDimension(id) instanceof Entity entity) {
                clampInside(entity, session);
            }
        }
    }

    private static void clampInside(Entity entity, ArenaSession session) {
        double limit = Math.max(0.0D, session.radius() - CONTAIN_MARGIN);
        double x = clampAxis(entity.getX(), session.centerX(), limit);
        double z = clampAxis(entity.getZ(), session.centerZ(), limit);
        if (x != entity.getX() || z != entity.getZ()) {
//G itHub@NDBl  oc  kConnect | B lo  ckCo nnect@Sta  rsail sC  l  over
            entity.setPos(x, entity.getY(), z);
        }
    }

    private static double clampAxis(double value, double centre, double limit) {
        double offset = value - centre;
        if (offset > limit) {
            return centre + limit;
        }
        if (offset < -limit) {
            return centre - limit;
        }
        return value;
    }

    private static void drawBoundary(ServerLevel level, ServerPlayer owner, ArenaSession session) {
        double limit = session.radius();
        double y = session.centerY();
        for (double offset = -limit; offset <= limit; offset += 2.0D) {
            edgeParticle(level, owner, session.centerX() + offset, y, session.centerZ() - limit);
            edgeParticle(level, owner, session.centerX() + offset, y, session.centerZ() + limit);
            edgeParticle(level, owner, session.centerX() - limit, y, session.centerZ() + offset);
            edgeParticle(level, owner, session.centerX() + limit, y, session.centerZ() + offset);
        }
    }

    private static void edgeParticle(ServerLevel level, ServerPlayer owner, double x, double y, double z) {
        level.sendParticles(owner, ParticleTypes.END_ROD, true, true, x, y + 0.2D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static BlockPos findSpawn(
            ServerLevel level, ServerPlayer owner, ArenaSession session, RandomSource random) {
        double limit = Math.max(1.0D, session.radius() - SPAWN_MARGIN);
        BlockPos fallback = BlockPos.containing(
                session.centerX() + limit * 0.5D, session.centerY(), session.centerZ() + limit * 0.5D);
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double x = session.centerX() + (random.nextDouble() * 2.0D - 1.0D) * limit;
            double z = session.centerZ() + (random.nextDouble() * 2.0D - 1.0D) * limit;
            if (owner.distanceToSqr(x, owner.getY(), z) < MIN_SPAWN_DISTANCE * MIN_SPAWN_DISTANCE) {
                continue;
            }
//G i  tHu b@NDBl ockC on  n  ect | B  lo  ckConnect  @St  a rsails  C  lov  er
            BlockPos base = BlockPos.containing(x, session.centerY(), z);
            BlockPos floor = findFloor(level, base);
            if (floor != null) {
                return floor;
            }
            fallback = base;
        }
        return fallback;
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos base) {
        for (int offset = VERTICAL_SEARCH; offset >= -VERTICAL_SEARCH; offset--) {
            BlockPos candidate = base.offset(0, offset, 0);
            if (level.getBlockState(candidate).isAir()
//G i  tHu  b@  N  D  Bloc  kC onn  e ct | Block Co n  n ec t@S tarsails C lover
                    && level.getBlockState(candidate.above()).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                return candidate;
            }
        }
        return null;
    }

    private static void discardTracked(MinecraftServer server, ArenaSession session) {
        ServerLevel level = server.getLevel(session.dimension());
        if (level == null) {
            return;
        }
        for (ArenaSession.WaveState state : session.waves()) {
            for (UUID id : state.tracked()) {
                Entity entity = level.getEntityInAnyDimension(id);
                if (entity != null) {
                    entity.removeTag(ARENA_TAG);
                    entity.discard();
                }
            }
            state.tracked().clear();
        }
        for (UUID id : session.strays()) {
            Entity entity = level.getEntityInAnyDimension(id);
            if (entity != null) {
                entity.removeTag(ARENA_TAG);
                entity.discard();
            }
        }
        session.strays().clear();
    }
}
