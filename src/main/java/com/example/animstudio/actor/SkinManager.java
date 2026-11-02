package com.example.animstudio.actor;
import com.example.animstudio.data.ActorData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
public final class SkinManager {
    public static Path dir() { return FabricLoader.getInstance().getGameDir().resolve("animstudio/skins"); }

    public record Skin(FaceSkin face, Identifier id, boolean slim) {
        /** NOTE: PlayerSkin's record shape changed in 1.21.9+ (ClientAsset). Adjust here if the compiler complains. */
        public PlayerSkin playerSkin() {
            ClientAsset.ResourceTexture tex = new ClientAsset.ResourceTexture(id, id);
            return new PlayerSkin(tex, null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, true);
        }
    }
    public static Skin load(ActorData d) {
        Minecraft mc = Minecraft.getInstance();
        try {
            BufferedImage img;
            if (d.skinFile != null && Files.exists(dir().resolve(d.skinFile))) img = ImageIO.read(dir().resolve(d.skinFile).toFile());
            else try (InputStream in = mc.getResourceManager().open(Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png"))) { img = ImageIO.read(in); }
            FaceSkin face = new FaceSkin(img);
            Identifier id = Identifier.fromNamespaceAndPath("animstudio", "skin/" + d.id.replace("-", ""));
            mc.getTextureManager().register(id, face.texture);
            return new Skin(face, id, d.slim);
        } catch (Exception e) { throw new RuntimeException("Skin load failed", e); }
    }
    /** Copy a dropped/chosen PNG into animstudio/skins. */
    public static String importSkin(Path src) throws Exception {
        Files.createDirectories(dir()); Path dst = dir().resolve(src.getFileName().toString());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING); return dst.getFileName().toString();
    }
    public static List<String> list() {
        try (Stream<Path> s = Files.list(dir())) { return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png")).sorted().toList(); }
        catch (Exception e) { return List.of(); }
    }
}
