package com.atenas.client;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import com.atenas.client.core.Config;
import com.atenas.client.core.ModuleManager;
import com.atenas.client.core.Stats;
import com.atenas.client.cosmetics.CosmeticSync;
import com.atenas.client.cosmetics.Cosmetics;
import com.atenas.client.cosmetics.PetRender;
import com.atenas.client.social.SocialEvents;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = AtenasClient.MODID, name = AtenasClient.NAME, version = AtenasClient.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class AtenasClient {

    public static final String MODID = "atenasclient";
    public static final String NAME = "Atenas Client";
    public static final String VERSION = "1.0.4";

    /** true si OptiFine esta cargado (se detecta por un campo que solo OptiFine agrega a GameSettings). */
    public static boolean hasOptiFine() {
        try {
            net.minecraft.client.settings.GameSettings.class.getField("ofFastRender");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static KeyBinding keyMenu;
    public static KeyBinding keyPerspective;
    public static KeyBinding keyZoom;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        keyMenu = new KeyBinding("Abrir men\u00fa Atenas", Keyboard.KEY_RSHIFT, NAME);
        ClientRegistry.registerKeyBinding(keyMenu);
        keyPerspective = new KeyBinding("Perspective (mirar alrededor)", Keyboard.KEY_LMENU, NAME);
        ClientRegistry.registerKeyBinding(keyPerspective);
        keyZoom = new KeyBinding("Zoom", Keyboard.KEY_C, NAME);
        ClientRegistry.registerKeyBinding(keyZoom);

        ModuleManager.init();
        Config.load();
        CosmeticSync.ensureToken();
        Config.save();

        register(new ClientEvents());
        register(ModuleManager.INSTANCE);
        register(new Stats());
        register(new CosmeticSync());
        register(new PetRender());
        register(new SocialEvents());
        register(new com.atenas.client.social.Badge());
        register(new com.atenas.client.modules.PerfEvents());

        Display.setTitle(NAME + " " + VERSION + " | Minecraft 1.8.9");
        setWindowIcon();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Cosmetics.registerLayers();
    }

    /** Pone el logo de Atenas (el mismo del launcher) como icono de la ventana. */
    private static void setWindowIcon() {
        try {
            java.io.InputStream in = AtenasClient.class.getResourceAsStream("/assets/atenasclient/textures/gui/icon128.png");
            if (in == null) return;
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
            in.close();
            int[] sizes = {16, 32, 128};
            java.nio.ByteBuffer[] icons = new java.nio.ByteBuffer[sizes.length];
            for (int k = 0; k < sizes.length; k++) {
                int sz = sizes[k];
                java.awt.image.BufferedImage sc = new java.awt.image.BufferedImage(sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = sc.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img, 0, 0, sz, sz, null);
                g.dispose();
                int[] px = sc.getRGB(0, 0, sz, sz, null, 0, sz);
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(sz * sz * 4);
                for (int p : px) {
                    bb.put((byte) (p >> 16 & 255)).put((byte) (p >> 8 & 255)).put((byte) (p & 255)).put((byte) (p >> 24 & 255));
                }
                bb.flip();
                icons[k] = bb;
            }
            Display.setIcon(icons);
        } catch (Exception ignored) { /* el icono es opcional */ }
    }

    /** En 1.8.9 los eventos de tick/teclado van por el bus de FML y el resto por el de Forge. */
    private static void register(Object handler) {
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }
}
