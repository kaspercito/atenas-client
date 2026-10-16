package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.OldAnimationsModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

/**
 * Animaciones 1.7 en primera persona.
 *
 * En 1.8.9, mientras usas un objeto (bloquear, comer/beber, arco) ItemRenderer llama a
 * transformFirstPersonItem(equipo, 0): el golpe no se ve. En 1.7 se pasaba el balanceo real.
 * Aqui cambiamos ese segundo parametro justo al entrar en transformFirstPersonItem, sin depender de
 * "ordinales" del bytecode (asi tambien aguanta mejor si hay otros mods como OptiFine tocando la clase).
 * Si algo no engancha, el juego sigue normal (defaultRequire = 0).
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Unique private float atenas$partial;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void atenas$capture(float partialTicks, CallbackInfo ci) {
        atenas$partial = partialTicks;
        OldAnimationsModule.mixinLoaded = true;
    }

    @ModifyVariable(method = "transformFirstPersonItem", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float atenas$swing(float swing) {
        if (swing != 0f) return swing;                       // rama normal (sin usar objeto): ya trae el balanceo
        OldAnimationsModule m = ModuleManager.find(OldAnimationsModule.class);
        if (m == null || !m.isEnabled()) return swing;
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null || p.getItemInUseCount() <= 0) return swing;
        ItemStack held = p.getHeldItem();
        if (!m.wants(OldAnimationsModule.kindOf(held))) return swing;
        return p.getSwingProgress(atenas$partial);
    }
}
