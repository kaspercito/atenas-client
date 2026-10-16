package com.atenas.client.modules;

import org.lwjgl.opengl.GL11;

import com.atenas.client.core.Module;
import com.atenas.client.core.Setting;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

/** Resalte del bloque que miras con color, grosor y relleno a tu gusto (en vez del contorno negro). */
public class BlockOverlayModule extends Module {
    private final Setting color, width, fill, fillAlpha;

    public BlockOverlayModule() {
        super("Resalte de bloque", "Cambia el contorno del bloque que miras: color, grosor y relleno.", Category.UTIL);
        color = addColor("Color", 8);
        width = addNum("Grosor", 2.0, 1.0, 6.0, 0.5);
        fill = addBool("Relleno", true);
        fillAlpha = addNum("Opacidad del relleno", 0.2, 0.05, 0.6, 0.05);
    }

    @Override
    public void onHighlight(DrawBlockHighlightEvent e) {
        MovingObjectPosition mop = e.target;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mc.theWorld == null) return;
        BlockPos pos = mop.getBlockPos();
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block.getMaterial() == Material.air || !mc.theWorld.getWorldBorder().contains(pos)) return;
        e.setCanceled(true);

        EntityPlayer p = e.player;
        float pt = e.partialTicks;
        double dx = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt;
        double dy = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt;
        double dz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt;
        block.setBlockBoundsBasedOnState(mc.theWorld, pos);
        AxisAlignedBB box = block.getSelectedBoundingBox(mc.theWorld, pos).expand(0.002, 0.002, 0.002).offset(-dx, -dy, -dz);

        int c = color.getColor();
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        if (fill.bool) drawFill(box, r / 255f, g / 255f, b / 255f, fillAlpha.getFloat());
        GL11.glLineWidth(width.getFloat());
        RenderGlobal.drawOutlinedBoundingBox(box, r, g, b, 255);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void quad(WorldRenderer w, double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4, float r, float g, float b, float a) {
        w.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        w.pos(x2, y2, z2).color(r, g, b, a).endVertex();
        w.pos(x3, y3, z3).color(r, g, b, a).endVertex();
        w.pos(x4, y4, z4).color(r, g, b, a).endVertex();
    }

    private static void drawFill(AxisAlignedBB q, float r, float g, float b, float a) {
        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(7, DefaultVertexFormats.POSITION_COLOR);
        quad(w, q.minX, q.minY, q.minZ, q.maxX, q.minY, q.minZ, q.maxX, q.minY, q.maxZ, q.minX, q.minY, q.maxZ, r, g, b, a); // abajo
        quad(w, q.minX, q.maxY, q.minZ, q.minX, q.maxY, q.maxZ, q.maxX, q.maxY, q.maxZ, q.maxX, q.maxY, q.minZ, r, g, b, a); // arriba
        quad(w, q.minX, q.minY, q.minZ, q.minX, q.maxY, q.minZ, q.maxX, q.maxY, q.minZ, q.maxX, q.minY, q.minZ, r, g, b, a); // norte
        quad(w, q.minX, q.minY, q.maxZ, q.maxX, q.minY, q.maxZ, q.maxX, q.maxY, q.maxZ, q.minX, q.maxY, q.maxZ, r, g, b, a); // sur
        quad(w, q.minX, q.minY, q.minZ, q.minX, q.minY, q.maxZ, q.minX, q.maxY, q.maxZ, q.minX, q.maxY, q.minZ, r, g, b, a); // oeste
        quad(w, q.maxX, q.minY, q.minZ, q.maxX, q.maxY, q.minZ, q.maxX, q.maxY, q.maxZ, q.maxX, q.minY, q.maxZ, r, g, b, a); // este
        t.draw();
    }
}
