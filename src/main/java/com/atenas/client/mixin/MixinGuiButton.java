package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.gui.AtenasButton;
import com.atenas.client.modules.ButtonsModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionButton;

/**
 * Dibuja los botones vanilla con el estilo Atenas SIN cambiarlos por otra clase: las pantallas de Minecraft guardan referencias a
 * sus botones (activar/desactivar "Unirse al servidor", cambiar el texto...) y cambiarlos de objeto las rompe. Solo se tocan los
 * GuiButton y GuiOptionButton normales; los deslizadores y los botones con dibujo propio (idioma, OptiFine...) quedan como estan.
 */
@Mixin(GuiButton.class)
public abstract class MixinGuiButton {
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public String displayString;
    @Shadow public boolean enabled;
    @Shadow public boolean visible;
    @Shadow protected int width;
    @Shadow protected int height;
    @Shadow protected boolean hovered;

    @Unique private float atenas$glow;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void atenas$draw(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        Class<?> c = ((Object) this).getClass();
        if (c != GuiButton.class && c != GuiOptionButton.class) return;
        ButtonsModule m = ModuleManager.find(ButtonsModule.class);
        if (m == null || !m.applies()) return;
        ci.cancel();
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        atenas$glow = AtenasButton.paint(xPosition, yPosition, width, height, displayString, enabled, hovered, atenas$glow);
    }
}
