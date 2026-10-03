package dev.blockconnect.arenamode.mixin;

import dev.blockconnect.arenamode.core.ArenaBoundary;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the arena wall to real movement.
 *
 * <p>The position before the movement step is stashed in two {@code @Unique} fields, and after the
 * step {@link ArenaBoundary} puts the entity back on the side of the edge it came from. Hooking
 * {@code Entity#move} rather than {@code setPos} is deliberate: spawning, summoning and teleporting
 * all set a position directly and must keep working, while every walking, flying and pushed entity
 * goes through this method.
 *
 * <p>Risk note: this patches a very hot method on the base {@code Entity} class. The work added per
 * call is two field writes plus a size check against each running arena (normally zero).
 */
@Mixin(Entity.class)
public abstract class EntityMoveMixin {

    @Unique
    private double arenamode$previousX;

    @Unique
    private double arenamode$previousZ;

    @Inject(method = "move", at = @At("HEAD"))
    private void arenamode$capturePosition(MoverType type, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        this.arenamode$previousX = self.getX();
        this.arenamode$previousZ = self.getZ();
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void arenamode$applyBoundary(MoverType type, Vec3 movement, CallbackInfo ci) {
        ArenaBoundary.clampAfterMove((Entity) (Object) this, this.arenamode$previousX, this.arenamode$previousZ);
    }
//G  i tHu  b @NDBlo ckCon ne  c t | B  lo  ck Con  ne  ct  @St  a  rsa il  s C lov  e r
}
