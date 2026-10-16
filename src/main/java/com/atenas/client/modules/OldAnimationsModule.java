package com.atenas.client.modules;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * TODAS las animaciones 1.7 en un solo mod:
 *  - balanceo del objeto al bloquear con la espada, comer/beber y tensar el arco (lo dibuja MixinItemRenderer);
 *  - el golpe se dispara aqui (onTick): un clic = un balanceo, y si mantienes el clic sobre un bloque el balanceo
 *    se repite sin parar, igual que en 1.7 (en 1.8 el clic se ignora mientras usas un objeto);
 *  - si Mixin no cargara, la espada igual se balancea al bloquear (aproximacion con transformaciones).
 */
public class OldAnimationsModule extends Module {
    public static final int EAT = 1, BLOCK = 2, BOW = 3;
    /** Lo pone el mixin al enganchar; si sigue en false, Mixin no cargo y se usa el modo de respaldo (solo espada). */
    public static volatile boolean mixinLoaded;

    private final Setting block, eat, bow, hold, breakBlocks, strength, useWhileHit, posOn, posX, posY, posZ, posSize;
    private boolean wasDown;

    public OldAnimationsModule() {
        super("Animaciones 1.7",
                "Todas las animaciones 1.7 juntas: balanceo al bloquear con la espada, comer/beber y usar el arco, y golpear bloques mientras usas el objeto.",
                Category.ANIM);
        block = addBool("Espada: bloquear + golpear", true);
        eat = addBool("Comer/beber: golpear mientras tanto", true);
        bow = addBool("Arco: golpear mientras tensas", true);
        hold = addBool("Mantener clic = balanceo continuo", true);
        breakBlocks = addBool("Picar el bloque de verdad (puede dar problemas con anticheat)", false);
        useWhileHit = addBool("Clic derecho aunque mantengas el izquierdo (bloquear/comer mientras picas)", true);
        strength = addNum("Intensidad (modo de respaldo)", 1.0, 0.3, 1.5, 0.1);
        // Posicion del objeto (antes era un mod aparte)
        posOn = addBool("Mover/cambiar tamaño del objeto en mano", false);
        posX = addNum("Objeto: horizontal", 0.0, -0.5, 0.5, 0.02);
        posY = addNum("Objeto: vertical", 0.0, -0.5, 0.5, 0.02);
        posZ = addNum("Objeto: distancia", 0.0, -0.5, 0.5, 0.02);
        posSize = addNum("Objeto: tamaño", 1.0, 0.4, 1.4, 0.05);
        startEnabled();
    }

    public boolean breaksBlocks() { return breakBlocks.bool; }

    /** true = el clic derecho se acepta aunque el izquierdo este picando un bloque (lo usa MixinMinecraft). */
    public boolean allowsUseWhileHitting() { return useWhileHit.bool; }

    public boolean wants(int kind) {
        switch (kind) {
            case EAT: return eat.bool;
            case BLOCK: return block.bool;
            case BOW: return bow.bool;
            default: return false;
        }
    }

    /** 0 = nada, 1 = comer/beber, 2 = bloquear, 3 = arco, segun el objeto de la mano. */
    public static int kindOf(ItemStack held) {
        if (held == null) return 0;
        EnumAction a = held.getItemUseAction();
        if (a == EnumAction.EAT || a == EnumAction.DRINK) return EAT;
        if (a == EnumAction.BLOCK) return BLOCK;
        if (a == EnumAction.BOW) return BOW;
        return 0;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null) return;
        boolean down = mc.currentScreen == null && mc.inGameHasFocus && mc.gameSettings.keyBindAttack.isKeyDown();
        boolean press = down && !wasDown;
        wasDown = down;
        if (!down || !mc.thePlayer.isUsingItem()) return;
        if (!wants(kindOf(mc.thePlayer.getHeldItem()))) return;
        boolean onBlock = mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        // 1.7: un clic siempre balancea; mantenerlo sobre un bloque sigue balanceando mientras "picas"
        if (press || (hold.bool && onBlock)) mc.thePlayer.swingItem();
    }

    @Override
    public void onDisable() { wasDown = false; }

    /** Respaldo si Mixin no cargo: la espada sigue balanceandose mientras bloqueas. */
    @Override
    public void onRenderHand(RenderHandEvent e) {
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        if (posOn.bool) {
            GlStateManager.translate(posX.getFloat(), posY.getFloat(), posZ.getFloat());
            float sc = posSize.getFloat();
            GlStateManager.scale(sc, sc, sc);
        }
        if (mixinLoaded || !block.bool) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemSword) || !mc.thePlayer.isBlocking()) return;
        float f = mc.thePlayer.getSwingProgress(e.partialTicks);
        if (f <= 0f) return;
        float k = strength.getFloat();
        float a = MathHelper.sin(MathHelper.sqrt_float(f) * (float) Math.PI);
        GlStateManager.translate(-a * 0.25f * k, MathHelper.sin(MathHelper.sqrt_float(f) * (float) Math.PI * 2f) * 0.12f * k,
                -MathHelper.sin(f * (float) Math.PI) * 0.15f * k);
        GlStateManager.rotate(a * -18f * k, 0f, 1f, 0f);
        GlStateManager.rotate(a * -45f * k, 1f, 0f, 0f);
    }
}
