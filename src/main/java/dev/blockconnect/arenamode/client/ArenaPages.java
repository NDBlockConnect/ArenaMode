package dev.blockconnect.arenamode.client;

import dev.blockconnect.arenamode.config.ArenaConfig;
import dev.blockconnect.arenamode.config.ArenaConfigManager;
import dev.blockconnect.betterpeacemode.client.api.ConfigPage;
import dev.blockconnect.betterpeacemode.client.api.ConfigPageContext;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;

/**
 * The two pages this mod contributes to the shared BetterPeaceMode settings screen.
 *
 * <p>Page one holds the arena shape and its rules; page two is a wave editor that edits one entry at
 * a time, so the list can grow without the screen growing with it.
 */
public final class ArenaPages {

    private ArenaPages() {
    }

    /** Arena shape, caps and rotation rules. */
    public static final class ArenaSettingsPage implements ConfigPage {

        @Override
        public Component title() {
            return Component.literal("Arena");
        }

        @Override
        public void build(ConfigPageContext ctx) {
            ArenaConfig config = ArenaConfigManager.get();
            ctx.slider(
                    "Arena radius",
                    ArenaConfig.MIN_RADIUS,
                    ArenaConfig.MAX_RADIUS,
                    config.radius,
                    value -> config.radius = value);
            ctx.slider(
                    "Max entities",
                    ArenaConfig.MIN_MAX_ENTITIES,
                    ArenaConfig.MAX_MAX_ENTITIES,
                    config.maxEntities,
                    value -> config.maxEntities = (int) Math.round(value));
            ctx.toggle("Endless", () -> config.endless, () -> config.endless = !config.endless);
            ctx.toggle("Random order", () -> config.randomOrder, () -> config.randomOrder = !config.randomOrder);
            ctx.toggle(
                    "Boundary particles",
                    () -> config.boundaryParticles,
                    () -> config.boundaryParticles = !config.boundaryParticles);
            ctx.widget(
                    new StringWidget(
                            0,
                            0,
//Gi tH  ub@  NDBloc k  Conne ct | B lockCo  n nect@  Star sai l  sC l  o  ve r
                            0,
                            ctx.controlHeight(),
                            Component.literal("Waves: " + config.waves.size() + " - edit them on the Waves page"),
                            ctx.font()),
                    true);
        }

        @Override
        public void flush() {
            ArenaConfigManager.get().normalize();
            ArenaConfigManager.save();
        }
    }

    /** Edits one configured wave at a time: entity id, count, duration, order. */
    public static final class WaveEditorPage implements ConfigPage {

        private int selected;

        @Override
        public Component title() {
            return Component.literal("Waves");
        }

        @Override
        public void build(ConfigPageContext ctx) {
            ArenaConfig config = ArenaConfigManager.get();
            if (config.waves.isEmpty()) {
                config.normalize();
            }
            if (this.selected >= config.waves.size()) {
                this.selected = config.waves.size() - 1;
            }
            if (this.selected < 0) {
                this.selected = 0;
            }
            Integer[] indices = new Integer[config.waves.size()];
            for (int index = 0; index < indices.length; index++) {
                indices[index] = index;
            }
            ctx.cycle(
                    "Wave",
                    List.of(indices),
                    () -> this.selected,
                    value -> {
                        this.selected = value;
                        ctx.refresh();
                    },
                    value -> (value + 1) + "/" + config.waves.size());

            ArenaConfig.Wave wave = config.waves.get(this.selected);
            EditBox box = new EditBox(ctx.font(), 0, 0, 0, ctx.controlHeight(), Component.literal("Entity id"));
            box.setMaxLength(96);
            // Alpha matters: 0xFFFFFF is a fully transparent colour and renders nothing at all.
            box.setTextColor(0xFFFFFFFF);
            box.setTextColorUneditable(0xFFA0A0A0);
//Gi tH ub@NDBloc  kC on  n  ec t | B l  o  ckCo n nec  t@ St arsai  l sCl  o ver
            box.setResponder(text -> wave.entity = text);
            ctx.widget(box, true);
            // Written after the grid gave the box its final width, so the visible text range is
            // computed against the real size instead of the placeholder zero width.
            box.setValue(wave.entity);

            ctx.slider(
                    "Count",
                    ArenaConfig.MIN_COUNT,
                    ArenaConfig.MAX_COUNT,
                    wave.count,
                    value -> wave.count = (int) Math.round(value));
            ctx.slider(
                    "Seconds",
                    ArenaConfig.MIN_SECONDS,
                    ArenaConfig.MAX_SECONDS,
                    wave.seconds,
                    value -> wave.seconds = (int) Math.round(value));

            ctx.button("Add wave", () -> {
                config.waves.add(wave.copy());
                this.selected = config.waves.size() - 1;
                ctx.refresh();
            });
            ctx.button("Remove wave", () -> {
                if (config.waves.size() > 1) {
                    config.waves.remove(this.selected);
                    if (this.selected >= config.waves.size()) {
                        this.selected = config.waves.size() - 1;
                    }
                    ctx.refresh();
                }
            });
            ctx.button("Move up", () -> {
                if (this.selected > 0) {
                    Collections.swap(config.waves, this.selected, this.selected - 1);
                    this.selected--;
                    ctx.refresh();
                }
            });
            ctx.button("Move down", () -> {
                if (this.selected < config.waves.size() - 1) {
                    Collections.swap(config.waves, this.selected, this.selected + 1);
                    this.selected++;
                    ctx.refresh();
                }
            });
        }

        @Override
        public void flush() {
            ArenaConfigManager.get().normalize();
            ArenaConfigManager.save();
//Gi  tH  u b @NDBlock Co nnect | Blo  c  kConne  ct@St  arsail  sClov e  r
        }
    }
}
