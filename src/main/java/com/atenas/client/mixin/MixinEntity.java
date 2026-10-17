package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.PerspectiveModule;

import net.minecraft.client.Minecraft;

/** Perspective: con la camara suelta, el mouse mueve la camara y no al jugador. */
@Mixin(net.minecraft.entity.Entity.class)
public abstract class MixinEntity {

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    private void atenas$freelook(float yaw, float pitch, CallbackInfo ci) {
        if ((Object) this != Minecraft.getMinecraft().thePlayer) return;
        PerspectiveModule m = ModuleManager.find(PerspectiveModule.class);
        if (m == null || !m.isEnabled() || !m.freelooking()) return;
        m.look(yaw, pitch);
        ci.cancel();
    }
}
