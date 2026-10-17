package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.BadgeModule;
import com.atenas.client.modules.TabListModule;
import com.atenas.client.social.Badge;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;

/** TAB sin fondo (modulo "Lista TAB") y con el logo de Atenas junto a quienes usan el cliente o el mod (modulo "Logo Atenas"). */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;drawRect(IIIII)V"))
    private void atenas$rect(int l, int t, int r, int b, int color) {
        TabListModule m = ModuleManager.find(TabListModule.class);
        if (m != null) {
            if (color == Integer.MIN_VALUE && m.hidesBackground()) return;
            if (color == 553648127 && m.hidesCells()) return;
        }
        Gui.drawRect(l, t, r, b, color);
    }

    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int atenas$name(FontRenderer fr, String text, float x, float y, int color) {
        int out = fr.drawStringWithShadow(text, x, y, color);
        BadgeModule b = ModuleManager.find(BadgeModule.class);
        if (b != null && b.inTab() && Badge.textHasUser(text)) {
            Badge.drawIcon(x + fr.getStringWidth(text) + 3f, y - 0.5f, 9f);
        }
        return out;
    }
}
