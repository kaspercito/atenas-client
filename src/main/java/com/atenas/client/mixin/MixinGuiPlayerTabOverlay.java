package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.BadgeModule;
import com.atenas.client.modules.TabListModule;
import com.atenas.client.social.Badge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;

/**
 * TAB sin fondo (modulo "Lista TAB") y con el logo de Atenas AL PRINCIPIO de la fila de quienes usan el cliente o el mod:
 * a la izquierda de la cabeza si el TAB muestra cabezas, y si no, a la izquierda del nombre.
 *
 * Como: a esos jugadores se les antepone al nombre un hueco de 3 espacios (asi la fila se ensancha y el logo tiene sitio sin
 * pisar la cabeza ni las barras de ping), la cabeza se corre unos pixeles a la derecha y el logo se dibuja en el hueco del principio.
 */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {

    /** Hueco que se antepone al nombre (3 espacios = 12 px) para que quepa el logo. */
    @Unique private static final String ATENAS_PAD = "   ";
    @Unique private static final float ATENAS_ICON = 8f;
    /** Cuanto se corre la cabeza hacia la derecha para dejar sitio al logo. */
    @Unique private static final int ATENAS_HEAD_SHIFT = 10;

    /** El jugador cuya fila se esta dibujando es usuario de Atenas (lo fija getPlayerName, lo consume el dibujo del nombre). */
    @Unique private boolean atenas$mark;
    /** La cabeza de esa fila ya se corrio (si el redirect de la cabeza no encajo, el logo se pone junto al nombre). */
    @Unique private boolean atenas$headShifted;

    @Inject(method = "renderPlayerlist", at = @At("HEAD"))
    private void atenas$reset(int width, net.minecraft.scoreboard.Scoreboard sb, net.minecraft.scoreboard.ScoreObjective obj, CallbackInfo ci) {
        atenas$mark = false;
        atenas$headShifted = false;
    }

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void atenas$tabName(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        atenas$mark = false;
        atenas$headShifted = false;
        BadgeModule b = ModuleManager.find(BadgeModule.class);
        if (b == null || !b.inTab() || info == null || info.getGameProfile() == null) return;
        String name = info.getGameProfile().getName();
        if (!Badge.isUser(name)) return;
        atenas$mark = true;
        cir.setReturnValue(ATENAS_PAD + cir.getReturnValue());
    }

    /** Las cabezas (y la capa de sombrero encima) se corren a la derecha en las filas con logo. */
    @Redirect(method = "renderPlayerlist", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawScaledCustomSizeModalRect(IIFFIIIIFF)V"))
    private void atenas$head(int x, int y, float u, float v, int uw, int vh, int w, int h, float tw, float th) {
        if (atenas$mark) {
            x += ATENAS_HEAD_SHIFT;
            atenas$headShifted = true;
        }
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uw, vh, w, h, tw, th);
    }

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
        boolean mark = atenas$mark;
        boolean shifted = atenas$headShifted;
        atenas$mark = false;
        atenas$headShifted = false;
        int out = fr.drawStringWithShadow(text, x, y, color);
        if (mark) {
            Minecraft mc = Minecraft.getMinecraft();
            // misma condicion que usa el juego para dibujar las cabezas
            boolean heads = mc.isIntegratedServerRunning()
                    || (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager().getIsencrypted());
            // con cabeza: el nombre empieza 9 px despues del borde de la fila, y el logo va en el borde (a la izquierda de la cabeza)
            float ix = (heads && shifted) ? x - 9f : x;
            Badge.drawIcon(ix, y, ATENAS_ICON);
        }
        return out;
    }
}
