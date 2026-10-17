package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.ChatModule;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;

/** Chat sin fondo, con animacion suave e historial infinito (ajustes del mod "Chat"). Si el mod esta apagado, el chat es el de siempre. */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void atenas$slideStart(int updateCounter, CallbackInfo ci) {
        GlStateManager.pushMatrix(); // siempre: el popMatrix de RETURN tiene que cuadrar
        ChatModule m = ModuleManager.find(ChatModule.class);
        if (m != null && m.isEnabled()) {
            float off = m.slideOffset(((GuiNewChat) (Object) this).getChatScale());
            if (off > 0f) GlStateManager.translate(0f, off, 0f);
        }
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void atenas$slideEnd(int updateCounter, CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    /** El primer drawRect de drawChat es el fondo de cada linea (los siguientes son la barrita de scroll: esos se dejan). */
    @Redirect(method = "drawChat", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"))
    private void atenas$lineBackground(int left, int top, int right, int bottom, int color) {
        ChatModule m = ModuleManager.find(ChatModule.class);
        if (m != null && m.isEnabled() && m.hidesBackground()) return;
        Gui.drawRect(left, top, right, bottom, color);
    }

    /**
     * setChatLine borra los mensajes de mas de 100 (tanto las lineas partidas como los mensajes originales). Con "Chat infinito"
     * el limite pasa a ser enorme, asi que se puede subir con la rueda hasta el primer mensaje de la sesion.
     */
    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int atenas$historyLimit(int vanilla) {
        ChatModule m = ModuleManager.find(ChatModule.class);
        return m != null && m.isEnabled() && m.keepsAllMessages() ? Integer.MAX_VALUE : vanilla;
    }
}
