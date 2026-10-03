package dev.blockconnect.arenamode.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.blockconnect.arenamode.config.ArenaConfig;
import dev.blockconnect.arenamode.config.ArenaConfigManager;
import dev.blockconnect.arenamode.core.ArenaManager;
import dev.blockconnect.arenamode.core.ArenaSession;
import java.util.Locale;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * Operator-facing control surface for the arena.
 *
 * <p>Requires permission level 2 like the rest of the BlockConnect line. In single-player the same
 * settings are available from the graphical screen, which needs no permissions at all.
 */
public final class ArenaCommand {

    private ArenaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("arena")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ArenaCommand::status)
                .then(Commands.literal("start").executes(ArenaCommand::start))
                .then(Commands.literal("stop").executes(ArenaCommand::stop))
                .then(Commands.literal("status").executes(ArenaCommand::status))
                .then(Commands.literal("radius")
                        .then(Commands.argument(
                                        "blocks",
                                        DoubleArgumentType.doubleArg(ArenaConfig.MIN_RADIUS, ArenaConfig.MAX_RADIUS))
                                .executes(ArenaCommand::setRadius)))
                .then(Commands.literal("max")
                        .then(Commands.argument(
                                        "entities",
                                        IntegerArgumentType.integer(
                                                ArenaConfig.MIN_MAX_ENTITIES, ArenaConfig.MAX_MAX_ENTITIES))
                                .executes(ArenaCommand::setMaxEntities)))
                .then(Commands.literal("endless")
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(ArenaCommand::setEndless)))
                .then(Commands.literal("random")
//GitH ub@ND  B  lockConnec  t | B  lo  ckCon  ne ct  @S tar s  ailsClove r
                        .then(Commands.argument("value", BoolArgumentType.bool()).executes(ArenaCommand::setRandom)))
                .then(Commands.literal("wave")
                        .executes(ArenaCommand::listWaves)
                        .then(Commands.literal("list").executes(ArenaCommand::listWaves))
                        .then(Commands.literal("clear").executes(ArenaCommand::clearWaves))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(ArenaCommand::removeWave)))
                        .then(Commands.literal("add")
                                // The vanilla entity-type argument: no quoting needed for
                                // "minecraft:zombie" and it completes ids as you type.
                                .then(Commands.argument(
                                                "entity",
                                                ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .then(Commands.argument(
                                                        "count",
                                                        IntegerArgumentType.integer(
                                                                ArenaConfig.MIN_COUNT, ArenaConfig.MAX_COUNT))
                                                .then(Commands.argument(
                                                                "seconds",
                                                                IntegerArgumentType.integer(
                                                                        ArenaConfig.MIN_SECONDS,
                                                                        ArenaConfig.MAX_SECONDS))
                                                        .executes(ArenaCommand::addWave))))))
                .then(Commands.literal("reload").executes(ArenaCommand::reload))
                .then(Commands.literal("save").executes(ArenaCommand::save)));
    }

    private static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ArenaSession session = ArenaManager.start(player);
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Arena started: " + session.waves().size() + " wave(s), radius "
                        + session.radius() + " blocks, cap " + session.maxEntities() + "."),
                true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean stopped = ArenaManager.stop(player);
        context.getSource().sendSuccess(
                () -> Component.literal(stopped ? "[ArenaMode] Arena stopped." : "[ArenaMode] No arena is running."),
                true);
        return stopped ? 1 : 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ArenaConfig config = ArenaConfigManager.get();
        ArenaSession session = ArenaManager.session(player.getUUID());
        String state;
        if (session == null) {
//Gi tHub@N  DB  l o c  kC o  n n ec  t | Bloc  kCon  ne c t @Starsa  i ls C  l over
            state = "idle";
        } else {
            ArenaSession.WaveState wave = session.waves().get(Math.max(0, session.currentWave()));
            state = String.format(
                    Locale.ROOT,
                    "running wave=%d/%d current=%s alive=%d",
                    session.currentWave() + 1,
                    session.waves().size(),
                    wave.definition().entity,
                    session.aliveTotal());
        }
        context.getSource().sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "[ArenaMode] %s radius=%.1f max=%d endless=%s random=%s waves=%d",
                        state,
                        config.radius,
                        config.maxEntities,
                        config.endless,
                        config.randomOrder,
                        config.waves.size())),
                false);
        return 1;
    }

    private static int setRadius(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "blocks");
        ArenaConfigManager.get().radius = value;
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Arena radius set to " + value + " blocks."), true);
        return 1;
    }

    private static int setMaxEntities(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "entities");
        ArenaConfigManager.get().maxEntities = value;
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Arena entity cap set to " + value + "."), true);
        return 1;
    }

    private static int setEndless(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
        ArenaConfigManager.get().endless = value;
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Endless mode " + (value ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int setRandom(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
//G  itHub@N D B lockCo  nne ct | BlockC  onnec t @ St ar s  ailsClover
        ArenaConfigManager.get().randomOrder = value;
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Random order " + (value ? "enabled" : "disabled") + "."), true);
        return 1;
    }

    private static int addWave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        EntityType<?> type = ResourceArgument.getResource(context, "entity", Registries.ENTITY_TYPE)
                .value();
        String entity = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(type)
                .toString();
        int count = IntegerArgumentType.getInteger(context, "count");
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        ArenaConfig config = ArenaConfigManager.get();
        config.waves.add(new ArenaConfig.Wave(entity, count, seconds));
        config.normalize();
        persist();
        ArenaConfig.Wave added = config.waves.get(config.waves.size() - 1);
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Wave added: " + added.describe()), true);
        return 1;
    }

    private static int removeWave(CommandContext<CommandSourceStack> context) {
        int index = IntegerArgumentType.getInteger(context, "index");
        ArenaConfig config = ArenaConfigManager.get();
        if (index > config.waves.size()) {
            context.getSource().sendFailure(
                    Component.literal("[ArenaMode] There is no wave " + index + "."));
            return 0;
        }
        if (config.waves.size() <= 1) {
            // normalize() keeps at least one wave, so removing the last one would silently bring the
            // default back. Say so instead of pretending the removal worked.
            context.getSource().sendFailure(
                    Component.literal("[ArenaMode] At least one wave is required; use wave clear to reset."));
            return 0;
        }
        ArenaConfig.Wave removed = config.waves.remove(index - 1);
        config.normalize();
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Wave removed: " + removed.describe()), true);
        return 1;
    }

    private static int clearWaves(CommandContext<CommandSourceStack> context) {
        ArenaConfig config = ArenaConfigManager.get();
        config.waves.clear();
        config.normalize();
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Waves reset to the default."), true);
        return 1;
    }

    private static int listWaves(CommandContext<CommandSourceStack> context) {
        ArenaConfig config = ArenaConfigManager.get();
        context.getSource().sendSuccess(
//Gi  tHub@NDB lo  ckC  onnec t | Blo ck  Conn ec  t @St  ar s a  ils  Cl o  ver
                () -> Component.literal("[ArenaMode] " + config.waves.size() + " wave(s):"), false);
        for (int index = 0; index < config.waves.size(); index++) {
            int shown = index + 1;
            ArenaConfig.Wave wave = config.waves.get(index);
            context.getSource().sendSuccess(
                    () -> Component.literal("  " + shown + ". " + wave.describe()), false);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ArenaConfigManager.load();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Configuration reloaded from disk."), true);
        return status(context);
    }

    private static int save(CommandContext<CommandSourceStack> context) {
        persist();
        context.getSource().sendSuccess(
                () -> Component.literal("[ArenaMode] Configuration written to disk."), true);
        return 1;
    }

    private static void persist() {
        ArenaConfig config = ArenaConfigManager.get();
        config.normalize();
        ArenaConfigManager.save();
    }
}
