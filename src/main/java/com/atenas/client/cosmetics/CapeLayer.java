package com.atenas.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.MathHelper;

public class CapeLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final CapeModel model = new CapeModel();

    public CapeLayer(RenderPlayer renderer) { this.renderer = renderer; }

    @Override
    public void doRenderLayer(AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float pt,
                              float age, float headYaw, float headPitch, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        if (p.isInvisible()) return;
        Cosmetics.Item item = Cosmetics.of(p, Cosmetics.Type.CAPE);
        if (item == null || !Cosmetics.inRange(p)) return;

        mc.getTextureManager().bindTexture(item.texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.pushMatrix();
        // un poco mas atras que la capa normal, para tapar la de Mojang si existe. Con alas puestas va por FUERA de ellas
        // (mas lejos del cuerpo): si no, las plumas la tapaban y casi no se veia.
        GlStateManager.translate(0f, 0f, Cosmetics.of(p, Cosmetics.Type.WINGS) != null ? 0.29f : 0.14f);

        double dx = p.prevChasingPosX + (p.chasingPosX - p.prevChasingPosX) * (double) pt
                - (p.prevPosX + (p.posX - p.prevPosX) * (double) pt);
        double dy = p.prevChasingPosY + (p.chasingPosY - p.prevChasingPosY) * (double) pt
                - (p.prevPosY + (p.posY - p.prevPosY) * (double) pt);
        double dz = p.prevChasingPosZ + (p.chasingPosZ - p.prevChasingPosZ) * (double) pt
                - (p.prevPosZ + (p.posZ - p.prevPosZ) * (double) pt);
        float yawOff = p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * pt;
        double sin = (double) MathHelper.sin(yawOff * (float) Math.PI / 180f);
        double cos = (double) (-MathHelper.cos(yawOff * (float) Math.PI / 180f));
        float lift = MathHelper.clamp_float((float) dy * 10f, -6f, 32f);
        float swing = (float) (dx * sin + dz * cos) * 100f;
        float sway = (float) (dx * cos - dz * sin) * 100f;
        if (swing < 0f) swing = 0f;
        float cam = p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * pt;
        lift += MathHelper.sin((p.prevDistanceWalkedModified
                + (p.distanceWalkedModified - p.prevDistanceWalkedModified) * pt) * 6f) * 32f * cam;
        if (p.isSneaking()) lift += 25f;

        // La inclinacion total es la de siempre, pero solo 35 % la gira el cuerpo entero: el resto lo doblan las tiras de tela.
        float tilt = swing / 2f + lift;
        float bend = (float) Math.toRadians(tilt * 0.65f);
        float amp = CapeWave.ampFor(swing); // quieta: respiro leve; corriendo: ondea fuerte
        GlStateManager.rotate(6f + tilt * 0.35f, 1f, 0f, 0f);
        GlStateManager.rotate(sway / 4f, 0f, 0f, 1f);
        GlStateManager.rotate(-sway / 2f, 0f, 1f, 0f);
        GlStateManager.rotate(180f, 0f, 1f, 0f);
        if (item.frames > 1) {
            // Capa animada: la textura es una tira vertical; se corre la UV al fotograma actual (matriz de textura).
            int f = Cosmetics.frameOf(item);
            GlStateManager.matrixMode(5890); // GL_TEXTURE
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, (float) f / item.frames, 0f);
            GlStateManager.scale(1f, 1f / item.frames, 1f);
            GlStateManager.matrixMode(5888); // GL_MODELVIEW
            model.renderChain(0.0625f, bend, amp, age, (float) Math.toRadians(sway / 4f));
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        } else {
            model.renderChain(0.0625f, bend, amp, age, (float) Math.toRadians(sway / 4f));
        }
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() { return false; }
}
