package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.GuiOverlayDebug;

/**
 * F3: la primera linea decia "Minecraft 1.8.9 (1.8.9/fml,forge)". Aqui se cambia SOLO lo que se muestra en pantalla por la marca del
 * cliente (como hace Lunar Client con "lunarclient:..."). No toca lo que el juego le dice al servidor.
 * Para que diga "vanilla" en vez de la marca, cambia BRAND.
 */
@Mixin(GuiOverlayDebug.class)
public abstract class MixinGuiOverlayDebug {
    @Unique private static final String BRAND = "atenasclient";

    @Redirect(method = {"call", "func_175238_c"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/ClientBrandRetriever;getClientModName()Ljava/lang/String;"))
    private String atenas$brand() { return BRAND; }
}
