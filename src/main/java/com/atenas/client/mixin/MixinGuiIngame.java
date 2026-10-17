package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.ScoreboardModule;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;

/** Scoreboard sin fondo y sin los numeros rojos (modulo "Scoreboard"). */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    @Redirect(method = "renderScoreboard", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiIngame;drawRect(IIIII)V"))
    private void atenas$scoreBg(int l, int t, int r, int b, int color) {
        ScoreboardModule m = ModuleManager.find(ScoreboardModule.class);
        if (m != null && m.hidesBackground()) return;
        Gui.drawRect(l, t, r, b, color);
    }

    @Redirect(method = "renderScoreboard", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int atenas$scoreText(FontRenderer fr, String text, int x, int y, int color) {
        ScoreboardModule m = ModuleManager.find(ScoreboardModule.class);
        if (m != null && m.hidesNumbers() && text != null && text.matches("\u00a7c\\d+")) return x;
        return fr.drawString(text, x, y, color);
    }
}
