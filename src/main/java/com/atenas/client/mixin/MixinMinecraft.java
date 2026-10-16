package com.atenas.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.atenas.client.core.ModuleManager;
import com.atenas.client.modules.OldAnimationsModule;

import net.minecraft.block.material.Material;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

/**
 * Opcional: "Picar el bloque de verdad" mientras usas un objeto (1.7). En 1.8 sendClickBlockToController no hace nada
 * si estas usando un objeto. El balanceo del brazo NO se hace aqui (lo hace OldAnimationsModule.onTick); este mixin
 * solo actua si el jugador activo el ajuste de picar de verdad, y si no, el juego sigue vanilla.
 */
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow private int leftClickCounter;
    @Shadow public MovingObjectPosition objectMouseOver;
    @Shadow public PlayerControllerMP playerController;
    @Shadow public EffectRenderer effectRenderer;
    @Shadow public WorldClient theWorld;
    @Shadow public EntityPlayerSP thePlayer;

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    private void atenas$blockHit(boolean leftClick, CallbackInfo ci) {
        if (!leftClick || thePlayer == null || !thePlayer.isUsingItem()) return;
        OldAnimationsModule m = ModuleManager.find(OldAnimationsModule.class);
        if (m == null || !m.isEnabled() || !m.breaksBlocks()) return;
        ItemStack held = thePlayer.getHeldItem();
        if (!m.wants(OldAnimationsModule.kindOf(held))) return;

        ci.cancel();
        if (leftClickCounter > 0) return;
        if (objectMouseOver == null || objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        BlockPos pos = objectMouseOver.getBlockPos();
        if (theWorld.getBlockState(pos).getBlock().getMaterial() == Material.air) return;
        if (playerController.onPlayerDamageBlock(pos, objectMouseOver.sideHit)) {
            effectRenderer.addBlockHitEffects(pos, objectMouseOver.sideHit);
            thePlayer.swingItem();
        }
    }

    /**
     * Causa del bug "si pulso primero el clic izquierdo no puedo bloquear con el derecho": en 1.8.9, rightClickMouse
     * no hace nada mientras PlayerControllerMP.getIsHittingBlock() sea true (y lo es mientras mantienes el izquierdo
     * sobre un bloque). En 1.7 si se podia. Aqui, con un objeto "animable" en la mano, decimos que no estas picando.
     */
    @Redirect(method = "rightClickMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;getIsHittingBlock()Z"))
    private boolean atenas$useWhileHitting(PlayerControllerMP pc) {
        boolean hitting = pc.getIsHittingBlock();
        if (!hitting || thePlayer == null) return hitting;
        OldAnimationsModule m = ModuleManager.find(OldAnimationsModule.class);
        if (m == null || !m.isEnabled() || !m.allowsUseWhileHitting()) return hitting;
        return m.wants(OldAnimationsModule.kindOf(thePlayer.getHeldItem())) ? false : hitting;
    }
}
