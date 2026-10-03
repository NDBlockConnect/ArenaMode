package dev.blockconnect.arenamode;

import dev.blockconnect.arenamode.command.ArenaCommand;
import dev.blockconnect.arenamode.config.ArenaConfig;
import dev.blockconnect.arenamode.config.ArenaConfigManager;
import dev.blockconnect.arenamode.core.ArenaManager;
import dev.blockconnect.arenamode.item.ArenaItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point.
 *
 * <p>The arena runs on the logical server, so everything here is registered from the common entry
 * point; the client half only contributes pages to the shared BetterPeaceMode settings screen.
 */
public final class ArenaMode implements ModInitializer {

    public static final String MOD_ID = "arenamode";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ArenaConfigManager.load();
        ArenaItems.register();
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ArenaCommand.register(dispatcher, registryAccess));
        ServerTickEvents.END_SERVER_TICK.register(ArenaManager::tick);
        ArenaConfig config = ArenaConfigManager.get();
        LOGGER.info(
                "[ArenaMode] loaded, waves={} radius={} maxEntities={} endless={} random={}",
                config.waves.size(),
                config.radius,
//GitHub@NDBl  ockCo  n  n ect | B lock  C o  n nect@  Sta r sail s C lo  ver
                config.maxEntities,
                config.endless,
                config.randomOrder);
    }
}
