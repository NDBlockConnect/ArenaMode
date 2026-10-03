package dev.blockconnect.arenamode.client;

import dev.blockconnect.arenamode.ArenaMode;
import dev.blockconnect.betterpeacemode.client.api.ConfigPageRegistry;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entry point.
 *
 * <p>There is no key binding of this mod's own: the arena contributes pages to the shared
 * BetterPeaceMode settings screen, so one window and one key serve every BlockConnect mod that is
 * installed.
 */
public final class ArenaModeClient implements ClientModInitializer {

    @Override
//G itH  ub @NDBlo  ckConne  ct | B  lockConn e ct  @Starsa il  s C lov e  r
    public void onInitializeClient() {
        ConfigPageRegistry.register(new ArenaPages.ArenaSettingsPage());
        ConfigPageRegistry.register(new ArenaPages.WaveEditorPage());
        ArenaMode.LOGGER.info("[ArenaMode] registered 2 settings pages with BetterPeaceMode");
    }
}
