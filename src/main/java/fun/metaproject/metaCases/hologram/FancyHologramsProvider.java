package fun.metaproject.metaCases.hologram;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.ItemHologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3f;
import fun.metaproject.metaCases.utility.ColorUtil;
import fun.metaproject.metaCases.utility.ItemUtil;
public class FancyHologramsProvider implements HologramProvider {
    private Plugin plugin;
    private HologramManager hologramManager;
    public FancyHologramsProvider(Plugin plugin) {
        this.plugin = plugin;
        this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
    }
    public void create(String name, Location location, List<String> lines) {
        try {
            double configScale;
            boolean configShadow;
            removeSingle(name);
            TextHologramData data = new TextHologramData(name, location);
            List<String> coloredLines = lines.stream()
                    .map(line -> {
                        String legacyColored = ColorUtil.color(line);
                        return MiniMessage.miniMessage()
                                .serialize(LegacyComponentSerializer.legacySection().deserialize(legacyColored));
                    })
                    .collect(Collectors.toList());
            data.setText(coloredLines);
            data.setPersistent(false);
            data.setBillboard(Display.Billboard.VERTICAL);
            data.setBackground(Color.fromARGB(0, 0, 0, 0));
            if (name.startsWith("caseblock_")) {
                configScale = this.plugin.getConfig().getDouble("fancy-holograms.case.scale", 0.8D);
                configShadow = this.plugin.getConfig().getBoolean("fancy-holograms.case.shadow", true);
            } else {
                configScale = this.plugin.getConfig().getDouble("fancy-holograms.win.text-scale", 0.8D);
                configShadow = this.plugin.getConfig().getBoolean("fancy-holograms.win.text-shadow", true);
            }
            data.setTextShadow(configShadow);
            float sc = (float) configScale;
            data.setScale(new Vector3f(sc, sc, sc));
            Hologram hologram = hologramManager.create(data);
            hologramManager.addHologram(hologram);
            hologram.createHologram();
        } catch (Throwable e) {
            plugin.getLogger().log(Level.SEVERE, "[HologramBridge/Fancy] create '" + name + "' failed", e);
        }
    }
    public void createWithItem(String name, Location loc, List<String> textLines, String material) {
        create(name, loc, textLines);
        try {
            String itemName = name + "_item";
            removeSingle(itemName);
            ItemStack item = ItemUtil.getItem(material, Material.CHEST);
            double configItemScale = this.plugin.getConfig().getDouble("fancy-holograms.win.item-scale", 0.5D);
            double configItemYOffset = this.plugin.getConfig().getDouble("fancy-holograms.win.item-y-offset", -0.1D);
            Location itemLoc = loc.clone().add(0.0D, configItemYOffset, 0.0D);
            ItemHologramData itemData = new ItemHologramData(itemName, itemLoc);
            itemData.setItemStack(item);
            itemData.setPersistent(false);
            itemData.setBillboard(Display.Billboard.VERTICAL);
            float sc = (float) configItemScale;
            itemData.setScale(new Vector3f(sc, sc, sc));
            Hologram hologram = hologramManager.create(itemData);
            hologramManager.addHologram(hologram);
            hologram.createHologram();
        } catch (Throwable e) {
            plugin.getLogger().log(Level.SEVERE, "[HologramBridge/Fancy] createWithItem (item part) '" + name + "' failed", e);
        }
    }
    public void remove(String name) {
        removeSingle(name);
        removeSingle(name + "_item");
    }
    private void removeSingle(String name) {
        Hologram hologram = hologramManager.getHologram(name).orElse(null);
        if (hologram != null) {
            hologramManager.removeHologram(hologram);
            var viewers = hologram.getViewers()
                    .stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .toList();
            viewers.forEach(p -> hologram.hideHologram(p));
        }
    }
    public void update(String name, List<String> newLines) {
        Hologram hologram = hologramManager.getHologram(name).orElse(null);
        if (hologram != null && hologram.getData() instanceof TextHologramData textData) {
            textData.setText(newLines);
            hologram.forceUpdate();
        }
    }
}
