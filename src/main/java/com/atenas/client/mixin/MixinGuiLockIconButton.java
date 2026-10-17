package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.gui.AtenasButton;
import com.atenas.client.gui.Theme;
import com.atenas.client.modules.ButtonsModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;

/**
 * El candado de la dificultad (Opciones, un jugador). Vanilla lo dibuja con la textura gris de widgets.png; aqui se pinta con el
 * estilo Atenas. Abierto = se puede cambiar la dificultad (el boton esta activo); cerrado = bloqueada (el boton esta desactivado).
 */
@Mixin(GuiLockIconButton.class)
public abstract class MixinGuiLockIconButton extends GuiButton {
    @Unique private float atenas$glow;

    /** Solo para que compile (un mixin que extiende GuiButton necesita constructor); Mixin nunca lo llama. */
    protected MixinGuiLockIconButton(int id, int x, int y) { super(id, x, y, 20, 20, ""); }

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void atenas$draw(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        ButtonsModule m = ModuleManager.find(ButtonsModule.class);
        if (m == null || !m.applies()) return;
        ci.cancel();
        if (!visible) return;
        hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
        atenas$glow = AtenasButton.paint(xPosition, yPosition, width, height, "", enabled, hovered, atenas$glow);
        AtenasButton.padlock(xPosition, yPosition, width, height, !enabled, !enabled ? Theme.OFF : hovered ? Theme.CYAN : Theme.TEXT);
    }
}
