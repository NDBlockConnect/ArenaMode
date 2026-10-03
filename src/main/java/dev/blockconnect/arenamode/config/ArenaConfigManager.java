package dev.blockconnect.arenamode.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Loads and persists {@code config/arenamode.json}.
 *
 * <p>Failures never propagate into the game: an unreadable or malformed file falls back to defaults
 * and is rewritten only when someone explicitly saves.
 */
public final class ArenaConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static ArenaConfig config = new ArenaConfig();
    private static Path configPath;

    private ArenaConfigManager() {
    }

    public static ArenaConfig get() {
        return config;
    }

    public static Path path() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("arenamode.json");
        }
        return configPath;
    }

    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            config = new ArenaConfig();
            config.normalize();
            save();
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            ArenaConfig loaded = GSON.fromJson(json, ArenaConfig.class);
            config = loaded != null ? loaded : new ArenaConfig();
        } catch (IOException | RuntimeException ex) {
            config = new ArenaConfig();
        }
        config.normalize();
    }

    public static void save() {
//Git H  ub@NDBlockConne c  t | Blo c  kC on  n e ct@Stars ail sClov  er
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // Non-fatal: the in-memory config stays authoritative for this session.
        }
    }
}
