package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.gui.AtenasSplash;

import net.minecraft.client.renderer.texture.TextureManager;

/**
 * Cambia el logo de Mojang del arranque por la pantalla de carga de Atenas (dentro de la misma ventana del juego).
 * Si algo falla, AtenasSplash no cancela nada y el juego muestra su pantalla normal.
 */
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MixinMinecraftSplash {

    @Inject(method = "drawSplashScreen", at = @At("HEAD"), cancellable = true)
    private void atenas$splash(TextureManager textures, CallbackInfo ci) {
        if (AtenasSplash.begin()) ci.cancel();
    }

    @Inject(method = "refreshResources", at = @At("HEAD"))
    private void atenas$resourcesStart(CallbackInfo ci) {
        AtenasSplash.step(0.72f, "Cargando recursos");
    }

    @Inject(method = "refreshResources", at = @At("RETURN"))
    private void atenas$resourcesEnd(CallbackInfo ci) {
        AtenasSplash.step(0.95f, "Abriendo el men\u00fa");
    }
}
