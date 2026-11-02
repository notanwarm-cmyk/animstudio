package com.example.animstudio.iris;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
/**
 * Soft Iris integration by reflection (no compile-time dependency, mod still loads without Iris).
 * Public API:  net.irisshaders.iris.api.v0.IrisApi  (isShaderPackInUse, getConfig().setShadersEnabledAndApply)
 * Pack choice: no public API exists, so we use Iris' internal  Iris.getIrisConfig().setShaderPackName() + Iris.reload().
 */
public final class IrisBridge {
    public static boolean present() { return FabricLoader.getInstance().isModLoaded("iris"); }

    public static boolean packInUse() {
        try { Object api = api(); return (boolean) api.getClass().getMethod("isShaderPackInUse").invoke(api); } catch (Throwable t) { return false; }
    }
    public static void setEnabled(boolean on) {
        try { Object api = api(); Object cfg = api.getClass().getMethod("getConfig").invoke(api);
              cfg.getClass().getMethod("setShadersEnabledAndApply", boolean.class).invoke(cfg, on); } catch (Throwable t) { t.printStackTrace(); }
    }
    public static void selectPack(String name) {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, name);
            cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, true);   // name may differ by Iris version
            try { cfg.getClass().getMethod("save").invoke(cfg); } catch (NoSuchMethodException ignored) {}
            Method reload = iris.getMethod("reload"); reload.invoke(null);
        } catch (Throwable t) { t.printStackTrace(); }
    }
    public static List<String> packs() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks");
        try (Stream<Path> s = Files.list(dir)) { return s.map(p -> p.getFileName().toString()).sorted().toList(); } catch (Exception e) { return List.of(); }
    }
    private static Object api() throws Exception {
        Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi"); return c.getMethod("getInstance").invoke(null);
    }
}
