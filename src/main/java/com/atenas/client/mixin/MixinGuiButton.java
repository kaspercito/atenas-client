package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.gui.AtenasButton;
import com.atenas.client.gui.ButtonKind;
import com.atenas.client.gui.SliderValue;
import com.atenas.client.modules.ButtonsModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Dibuja los botones vanilla con el estilo Atenas SIN cambiarlos por otra clase: las pantallas de Minecraft guardan referencias a
 * sus botones (activar/desactivar "Unirse al servidor", cambiar el texto...) y cambiarlos de objeto las rompe.
 *
 * Entran GuiButton y TODAS sus subclases que no sobrescriben drawButton (los botones anonimos de Opciones como "Super Secret
 * Settings", los de OptiFine...). Los que si lo sobrescriben (idioma, candado...) no pasan por aqui. Los deslizadores (los que
 * sobrescriben mouseDragged) solo se tocan si se puede leer su valor (GuiOptionSlider, ver MixinGuiOptionSlider); el resto queda vanilla.
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

    @Shadow protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Unique private float atenas$glow;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void atenas$draw(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        Object self = this;
        boolean slider = self instanceof SliderValue;
        if (!slider && ButtonKind.draggable(self.getClass())) return;
        ButtonsModule m = ModuleManager.find(ButtonsModule.class);
        if (m == null || !m.applies()) return;
        ci.cancel();
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        if (slider) mouseDragged(mc, mouseX, mouseY); // vanilla lo llama al final de drawButton: es lo que mueve el deslizador
        atenas$glow = AtenasButton.paint(xPosition, yPosition, width, height, displayString, enabled, hovered, atenas$glow);
        if (slider) AtenasButton.sliderHandle(xPosition, yPosition, width, height, ((SliderValue) self).atenas$sliderValue(), enabled);
    }
}
