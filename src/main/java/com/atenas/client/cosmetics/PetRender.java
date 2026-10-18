package com.atenas.client.cosmetics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.block.material.Material;
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
        boolean seen, mine;
        String kind = "";
        Pet(long seed) { brain = new PetBrain(seed); }
    }

    /** Hombrera: una almohadilla de lana marron donde se posa la mascota (se dibuja solo cuando va en el hombro). */
    private static final class PadModel extends ModelBase {
        private final ModelRenderer box;
        PadModel() {
            textureWidth = 32; textureHeight = 16;
            box = new ModelRenderer(this, 0, 0);
            box.addBox(-3f, 0f, -3f, 6, 2, 6);
        }
        void render() { box.render(0.0625f); }
    }
    private static final ResourceLocation PAD_TEX = new ResourceLocation("textures/blocks/wool_colored_brown.png");
    private PadModel pad;

    private final PetBrain.Owner owner = new PetBrain.Owner();
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

        @Override public double water(double x, double z, double y) {
            World w = Minecraft.getMinecraft().theWorld;
            if (w == null) return Double.NaN;
            int bx = MathHelper.floor_double(x), bz = MathHelper.floor_double(z), by = MathHelper.floor_double(y);
            for (int dy = 0; dy >= -1; dy--) {
                int yy = by + dy;
                if (w.getBlockState(new BlockPos(bx, yy, bz)).getBlock().getMaterial() != Material.water) continue;
                for (int n = 0; n < 8; n++) {
                    if (w.getBlockState(new BlockPos(bx, yy + 1, bz)).getBlock().getMaterial() == Material.water) yy++; else break;
                }
                return yy + 0.89;
            }
            return Double.NaN;
        }
    };

    /** Misma condicion que usa Minecraft 1.8.9 para pausar (no hay metodo publico): mundo local + pantalla que pausa + sin abrir a LAN. En un servidor nunca se pausa. */
    private static boolean isPaused(Minecraft mc) {
        return mc.isSingleplayer() && mc.currentScreen != null && mc.currentScreen.doesGuiPauseGame()
                && mc.getIntegratedServer() != null && !mc.getIntegratedServer().getPublic();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) { pets.clear(); return; }
        if (isPaused(mc)) return;   // un solo jugador con el menu de pausa abierto: el mundo esta congelado, la mascota tambien
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
            PetSpec.Kind kd = PetSpec.kindFor(item.modelId());
            if (kd == null) continue;
            if (!kd.name.equals(pet.kind)) { pet.kind = kd.name; pet.brain.configure(kd.flier, kd.avoidWater, kd.hop); }
            pet.seen = true;
            pet.mine = p == mc.thePlayer;
            pet.item = item;
            owner.x = p.posX; owner.y = p.posY; owner.z = p.posZ;
            owner.yaw = p.rotationYaw; owner.bodyYaw = p.renderYawOffset; owner.pitch = p.rotationPitch;
            owner.sneaking = p.isSneaking(); owner.sprinting = p.isSprinting();
            owner.fallFlying = false; owner.flying = p.capabilities.isFlying; owner.swimming = false;
            owner.inWater = p.isInWater(); owner.riding = p.isRiding(); owner.climbing = p.isOnLadder(); owner.onGround = p.onGround;
            pet.brain.tick(owner, GROUND);
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
            // pegada a la camara (la tuya en primera persona, con la mascota en tu hombro) tapa la pantalla: no se dibuja
            double cdx = x - rm.viewerPosX, cdy = y + 0.2 - rm.viewerPosY, cdz = z - rm.viewerPosZ;
            // la tuya en primera persona se oculta si pasa cerca de la camara (estorba al girar y caminar); las demas, solo si casi la tocan
            double cut = pet.mine && mc.gameSettings.thirdPersonView == 0 ? 1.6 : 0.9;
            if (cdx * cdx + cdy * cdy + cdz * cdz < cut * cut) continue;
            int light = mc.theWorld.getCombinedLight(new BlockPos(x, y + 0.5, z), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
            GlStateManager.color(1f, 1f, 1f, 1f);

            PetSpec.Kind kd = PetSpec.kindFor(pet.item.modelId());
            if (kd == null) continue;
            PetModel model = models.get(kd.name);
            if (model == null) { model = new PetModel(kd); models.put(kd.name, model); }
            float sc = PetSpec.renderScale(kd, b, pt);
            TexFilter.prepare(pet.item.texture, -1);
            mc.getTextureManager().bindTexture(pet.item.texture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x - rm.viewerPosX, y - rm.viewerPosY, z - rm.viewerPosZ);
            GlStateManager.rotate(180f - b.iyaw(pt), 0f, 1f, 0f);
            float pitch = b.ipitch(pt), roll = b.iroll(pt);
            if (Math.abs(pitch) > 0.001f || Math.abs(roll) > 0.001f) {   // inclinada en el aire
                float cy = (kd.legH + kd.bodyH / 2f) / 16f * sc;
                GlStateManager.translate(0f, cy, 0f);
                GlStateManager.rotate((float) Math.toDegrees(roll), 0f, 0f, 1f);
                GlStateManager.rotate((float) -Math.toDegrees(pitch), 1f, 0f, 0f);
                GlStateManager.translate(0f, -cy, 0f);
            }
            float perch = b.iperch(pt);
            if (perch > 0.5f) {   // hombrera bajo las patas
                float padS = Math.min(sc * 1.6f, 0.9f) * Math.min(1f, (perch - 0.5f) * 2f);
                if (pad == null) pad = new PadModel();
                mc.getTextureManager().bindTexture(PAD_TEX);
                GlStateManager.pushMatrix();
                GlStateManager.scale(-padS, -padS, padS);
                pad.render();
                GlStateManager.popMatrix();
                mc.getTextureManager().bindTexture(pet.item.texture);
            }
            GlStateManager.scale(-sc, -sc, sc);
            GlStateManager.translate(0f, -1.501f, 0f); // en el modelo los pies estan en y = 24 (1.5 bloques)
            model.render(b, pt, 0.0625f);
            GlStateManager.popMatrix();
        }
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
    }

    // ------------------------------------------------------------------ vista previa del menu

    private static PetBrain previewBrain;
    private static String previewKey = "";
    private static long previewLast;
    private static final Map<String, PetModel> PREVIEW_MODELS = new HashMap<String, PetModel>();
    private static final PetBrain.Ground FLAT = new PetBrain.Ground() {
        @Override public double ground(double x, double z, double fromY) { return 0.0; }
    };

    /**
     * Dibuja la mascota elegida en la vista previa del menu, a un lado del jugador. La matriz ya debe estar en el marco del
     * jugador (pies en el origen, 1 unidad = 1 bloque). La mascota se queda quieta junto a el y hace sus gestos de reposo.
     */
    public static void renderPreview(Cosmetics.Item item) {
        if (item == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        PetSpec.Kind kd = PetSpec.kindFor(item.modelId());
        if (kd == null) return;
        if (previewBrain == null || !previewKey.equals(item.id)) {
            previewBrain = new PetBrain(item.id.hashCode());
            previewBrain.configure(kd.flier, kd.avoidWater, kd.hop);
            previewBrain.teleport(0.0, 0.0, 0.0, 0f);
            previewKey = item.id;
            previewLast = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        int ticks = (int) Math.min(5L, (now - previewLast) / 50L);
        if (ticks > 0) previewLast += ticks * 50L;
        for (int i = 0; i < ticks; i++) previewBrain.tick(0.0, 0.0, 0.0, 0f, false, FLAT);
        float pt = Math.min(1f, (now - previewLast) / 50f);

        PetModel model = PREVIEW_MODELS.get(kd.name);
        if (model == null) { model = new PetModel(kd); PREVIEW_MODELS.put(kd.name, model); }
        float sc = PetSpec.renderScale(kd, previewBrain, pt);
        TexFilter.prepare(item.texture, -1);
        mc.getTextureManager().bindTexture(item.texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.95f, kd.flier ? 0.55f + (float) Math.sin(now * 0.004) * 0.06f : 0f, 0f);   // a un lado del jugador
        GlStateManager.rotate(180f, 0f, 1f, 0f);       // mirando a la camara
        GlStateManager.scale(-sc, -sc, sc);
        GlStateManager.translate(0f, -1.501f, 0f);
        model.render(previewBrain, pt, 0.0625f);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }
}
