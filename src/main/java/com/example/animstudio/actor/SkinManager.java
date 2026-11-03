package com.example.animstudio.actor;
import com.example.animstudio.data.ActorData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
public final class SkinManager {
    public static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("animstudio/skins"); }

    /** 64x64 ARGB. Never throws: falls back to a plain generated skin so the studio can never crash on a bad file. */
    public static int[] loadPixels(ActorData d) {
        try {
            BufferedImage img;
            if (d.skinFile != null && Files.exists(dir().resolve(d.skinFile))) img = ImageIO.read(dir().resolve(d.skinFile).toFile());
            else try (InputStream in = Minecraft.getInstance().getResourceManager().open(Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png"))) { img = ImageIO.read(in); }
            return convert(img);
        } catch (Throwable t) { return fallback(); }
    }
    static int[] convert(BufferedImage img) {
        int[] p = new int[64 * 64];
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) p[y * 64 + x] = (x < img.getWidth() && y < img.getHeight()) ? img.getRGB(x, y) : 0;
        if (img.getHeight() == 32) { mirror(p, 0, 16, 16, 48); mirror(p, 40, 16, 32, 48); }   // legacy skin: derive left leg/arm
        return p;
    }
    private static void mirror(int[] p, int sx, int sy, int dx, int dy) {
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) p[(dy + y) * 64 + dx + x] = p[(sy + y) * 64 + sx + 15 - x];
    }
    static int[] fallback() {
        int[] p = new int[64 * 64]; java.util.Arrays.fill(p, 0xFFC8A07A);
        for (int y = 0; y < 8; y++) for (int x = 8; x < 24; x++) p[y * 64 + x] = 0xFF4A3322;   // hair on top of head
        for (int y = 16; y < 32; y++) for (int x = 16; x < 40; x++) p[y * 64 + x] = 0xFF3A86C8; // shirt
        return p;
    }
    public static String importSkin(Path src) throws Exception {
        Files.createDirectories(dir()); Path dst = dir().resolve(src.getFileName().toString());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING); return dst.getFileName().toString();
    }
    public static List<String> list() {
        try (Stream<Path> s = Files.list(dir())) { return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png")).sorted().toList(); }
        catch (Exception e) { return List.of(); }
    }
}
