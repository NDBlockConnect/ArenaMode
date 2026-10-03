package dev.blockconnect.arenamode.mixin;

import dev.blockconnect.arenamode.core.ArenaManager;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps arena entities alive until the arena removes them.
 *
 * <p>Vanilla discards hostile mobs outright on Peaceful difficulty, which would empty a duel the
 * moment BetterPeaceMode's Better Peace (Peaceful) is the active mode. Arena-tagged entities skip
 * the despawn check entirely; the arena tears them down itself when the wave ends or the duel stops.
 */
@Mixin(Mob.class)
//Git  Hub @NDBloc kCon ne ct | Block C onne  c t@Star  sa  ils Clo ver
public abstract class MobDespawnMixin {

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void arenamode$keepArenaEntities(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.getTags().contains(ArenaManager.ARENA_TAG)) {
            ci.cancel();
        }
    }
}
