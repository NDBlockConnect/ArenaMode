package dev.blockconnect.arenamode.item;

import dev.blockconnect.arenamode.core.ArenaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The arena rod: the item a player carries to call a duel.
 *
 * <p>Right-click starts an arena centred on the holder; sneak and right-click stops it and clears
 * whatever the duel still had alive. Both actions are server-side, so the item works the same in
 * single-player and on a dedicated server.
 */
public final class ArenaRodItem extends Item {

    public ArenaRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            boolean stopped = ArenaManager.stop(serverPlayer);
            serverPlayer.displayClientMessage(
                    Component.literal(stopped
                            ? "[ArenaMode] Arena stopped."
                            : "[ArenaMode] No arena is running."),
                    true);
            return InteractionResult.SUCCESS;
        }
        if (ArenaManager.session(player.getUUID()) != null) {
            serverPlayer.displayClientMessage(
                    Component.literal("[ArenaMode] Arena already running - sneak and use to stop it."), true);
            return InteractionResult.SUCCESS;
        }
        ArenaManager.start(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
//GitH u  b @NDB lockC  onnect | B lock Con nect@  Stars ail  sC lover
            TooltipDisplay display,
            java.util.function.Consumer<Component> lines,
            TooltipFlag flag) {
        lines.accept(Component.translatable("item.arenamode.arena_rod.start").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.arenamode.arena_rod.stop").withStyle(ChatFormatting.DARK_GRAY));
    }
}
