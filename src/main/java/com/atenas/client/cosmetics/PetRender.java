package com.atenas.client.cosmetics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Mascotas cosmeticas. Cada jugador con una mascota elegida (y a menos de {@link Cosmetics#MAX_DIST_SQ} de ti) tiene un
 * {@link PetBrain} que se mueve una vez por tick; se dibuja despues de las entidades del mundo, con luz del lugar donde esta.
 * Solo cliente: la mascota no existe para el servidor ni choca con nada.
 */
public class PetRender {

    private static final class Pet {
        final PetBrain brain;
        Cosmetics.Item item;
        boolean seen;
        Pet(long seed) { brain = new PetBrain(seed); }
    }

    private final Map<String, Pet> pets = new HashMap<String, Pet>();
    private final Map<String, PetModel> models = new HashMap<String, PetModel>();

    /** El suelo para la mascota: la cara de arriba del bloque solido mas alto que este a "fromY" o mas abajo (hasta 8 bloques). */
    private static final PetBrain.Ground GROUND = new PetBrain.Ground() {
        @Override public double ground(double x, double z, double fromY) {
            World w = Minecraft.getMinecraft().theWorld;
            if (w == null) return Double.NaN;
            int bx = MathHelper.floor_double(x), bz = MathHelper.floor_double(z);
            int top = MathHelper.floor_double(fromY);
            for (int y = top; y >= top - 8 && y >= 0; y--) {
                BlockPos pos = new BlockPos(bx, y, bz);
                IBlockState st = w.getBlockState(pos);
                Block b = st.getBlock();
                AxisAlignedBB bb = b.getCollisionBoundingBox(w, pos, st);
                if (bb != null) return bb.maxY;
            }
            return Double.NaN;
        }
    };

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) { pets.clear(); return; }
        for (Pet p : pets.values()) p.seen = false;
        for (EntityPlayer ep : mc.theWorld.playerEntities) {
            if (!(ep instanceof AbstractClientPlayer)) continue;
            AbstractClientPlayer p = (AbstractClientPlayer) ep;
            String name = p.getName();
            if (name == null) continue;
            Cosmetics.Item item = Cosmetics.of(p, Cosmetics.Type.PET);
            if (item == null || !Cosmetics.inRange(p)) continue;
            Pet pet = pets.get(name);
            if (pet == null) {
                pet = new Pet(name.hashCode());
                pets.put(name, pet);
            }
            pet.seen = true;
            pet.item = item;
            pet.brain.tick(p.posX, p.posY, p.posZ, p.rotationYaw, p.isSneaking(), GROUND);
        }
        // quien ya no esta (se fue, se quito la mascota o se alejo) pierde su mascota; al volver aparece junto a el
        for (Iterator<Pet> it = pets.values().iterator(); it.hasNext();) if (!it.next().seen) it.remove();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (pets.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        RenderManager rm = mc.getRenderManager();
        float pt = e.partialTicks;

        RenderHelper.enableStandardItemLighting();
        mc.entityRenderer.enableLightmap();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        for (Pet pet : pets.values()) {
            if (pet.item == null) continue;
            PetBrain b = pet.brain;
            double x = b.ix(pt), y = b.iy(pt), z = b.iz(pt);
            int light = mc.theWorld.getCombinedLight(new BlockPos(x, y + 0.5, z), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
            GlStateManager.color(1f, 1f, 1f, 1f);

            String kind = PetSpec.kindFor(pet.item.id).name;
            PetModel model = models.get(kind);
            if (model == null) {
                model = new PetModel(PetSpec.kindFor(pet.item.id));
                models.put(kind, model);
            }
            mc.getTextureManager().bindTexture(pet.item.texture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ);
            GlStateManager.rotate(180f - b.iyaw(pt), 0f, 1f, 0f);
            GlStateManager.scale(-PetSpec.SCALE, -PetSpec.SCALE, PetSpec.SCALE);
            GlStateManager.translate(0f, -1.501f, 0f); // en el modelo los pies estan en y = 24 (1.5 bloques)
            model.render(b, pt, 0.0625f);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
    }
}
