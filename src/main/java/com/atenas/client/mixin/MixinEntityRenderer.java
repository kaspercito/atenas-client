package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.PerspectiveModule;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/**
 * Perspective: mientras la camara esta suelta, orientCamera "ve" al jugador mirando hacia donde apunta la camara libre.
 * Solo durante ese metodo: antes y despues el jugador conserva su rotacion real (por eso no gira de verdad).
 */
@Mixin(net.minecraft.client.renderer.EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow private Minecraft mc;

    private boolean atenas$swapped;
    private float atenas$yaw, atenas$pitch, atenas$prevYaw, atenas$prevPitch;

    @Inject(method = "orientCamera", at = @At("HEAD"))
    private void atenas$camHead(float partialTicks, CallbackInfo ci) {
        atenas$swapped = false;
        PerspectiveModule m = ModuleManager.find(PerspectiveModule.class);
        if (m == null || !m.isEnabled() || !m.freelooking()) return;
        Entity e = mc.getRenderViewEntity();
        if (e == null) return;
        atenas$yaw = e.rotationYaw;
        atenas$pitch = e.rotationPitch;
        atenas$prevYaw = e.prevRotationYaw;
        atenas$prevPitch = e.prevRotationPitch;
        e.rotationYaw = e.prevRotationYaw = m.camYaw();
        e.rotationPitch = e.prevRotationPitch = m.camPitch();
        atenas$swapped = true;
    }

    @Inject(method = "orientCamera", at = @At("RETURN"))
    private void atenas$camReturn(float partialTicks, CallbackInfo ci) {
        if (!atenas$swapped) return;
        atenas$swapped = false;
        Entity e = mc.getRenderViewEntity();
        if (e == null) return;
        e.rotationYaw = atenas$yaw;
        e.rotationPitch = atenas$pitch;
        e.prevRotationYaw = atenas$prevYaw;
        e.prevRotationPitch = atenas$prevPitch;
    }
}
