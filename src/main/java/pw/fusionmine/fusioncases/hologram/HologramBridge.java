package pw.fusionmine.fusioncases.hologram;

import java.util.List;
import java.util.logging.Logger;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
public class HologramBridge {

    private HologramProvider provider = null;

    public void init(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        if (Bukkit.getPluginManager().isPluginEnabled("FancyHolograms")) {
            provider = new FancyHologramsProvider(plugin);
            log.info("[HologramBridge] Using FancyHolograms.");
        } else {
            provider = null;
            log.warning("[HologramBridge] No hologram plugin found (FancyHolograms). Holograms disabled.");
        }
    }

    public boolean isAvailable() {
        return (provider != null);
    }

    public void create(String name, Location loc, List<String> lines) {
        if (provider == null)
            return;
        try {
            provider.create(name, loc, lines);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[HologramBridge] create '" + name + "': " + t.getMessage());
        }

    }

    public void createWithItem(String name, Location loc, List<String> textLines, String material) {
        if (provider == null)
            return;
        try {
            provider.createWithItem(name, loc, textLines, material);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[HologramBridge] createWithItem '" + name + "': " + t.getMessage());
        }

    }

    public void remove(String name) {
        if (provider == null)
            return;
        try {
            provider.remove(name);
        } catch (Throwable throwable) {
        }
    }

    public void update(String name, List<String> newLines) {
        if (provider == null)
            return;
        try {
            provider.update(name, newLines);
        } catch (Throwable throwable) {
        }
    }

}