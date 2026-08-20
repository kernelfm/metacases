package fun.metaproject.metaCases.case_system;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.animation.api.AnimationConfig;
import fun.metaproject.metaCases.database.DatabaseManager;
import fun.metaproject.metaCases.hologram.HologramBridge;
import fun.metaproject.metaCases.utility.ColorUtil;
public class CaseManager {
    private final Map<String, CaseModel> cases = new HashMap<>();
    public final MetaCasesFree plugin;
    private final Map<Location, String> caseBlocks = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerKeys = new HashMap<>();
    public List<HistoryEntry> history = new ArrayList<>();
    private final Map<Location, String> blockHolos = new HashMap<>();
    private final Set<String> disabledCases = new HashSet<>();
    private File casesFolder;
    private DatabaseManager db;
    public CaseManager(MetaCasesFree plugin) {
        this.plugin = plugin;
        this.db = new DatabaseManager(plugin);
        initDirs();
        loadCases();
        loadBlocks();
        loadKeys();
        loadHistory();
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnAllBlockHolograms, 20L);
    }
    private void initDirs() {
        this.plugin.getDataFolder().mkdirs();
        this.casesFolder = new File(this.plugin.getDataFolder(), "cases");
        if (!this.casesFolder.exists()) {
            this.casesFolder.mkdirs();
            createDefaultCase();
        }
        AnimationConfig.initializeDefaultConfigs(this.plugin);
    }
    private void createDefaultCase() {
        File f = new File(this.casesFolder, "default.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("display-name", "&aDefault Case");
        cfg.set("material", "BASE64-eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzczYjM1NWM5MWJjYzQyMjA4OWQwM2Y1NDY5ODdlMWE3MWVlNzc1OWY0YzdkNTdlZDU5ODgyY2U4YmM2MTM3NCJ9fX0=");
        cfg.set("animation", "random");
        ConfigurationSection rewards = cfg.createSection("rewards");
        ConfigurationSection r1 = rewards.createSection("diamond");
        r1.set("display-name", "&bDiamond");
        r1.set("material", "DIAMOND");
        r1.set("chance", Double.valueOf(20.0D));
        r1.set("commands", Arrays.asList(new String[]{"[command] give %username% diamond 3", "[message] &a[v] &fYou won &bDiamonds&f!"}));
        ConfigurationSection r2 = rewards.createSection("gold");
        r2.set("display-name", "&6Gold Ingot");
        r2.set("material", "GOLD_INGOT");
        r2.set("chance", Double.valueOf(50.0D));
        r2.set("commands", Arrays.asList(new String[]{"[command] give %username% gold_ingot 5", "[broadcast] &6&lWin! &e%username% &fwon &6Gold Ingots &ffrom the default case!"}));
        ConfigurationSection r3 = rewards.createSection("coal");
        r3.set("display-name", "&8Coal");
        r3.set("material", "COAL");
        r3.set("chance", Double.valueOf(30.0D));
        r3.set("commands", Arrays.asList(new String[]{"[command] give %username% coal 10", "[message] &a[v] &fYou won &8Coal&f!"}));
        ConfigurationSection gui = cfg.createSection("gui");
        gui.set("title", "&aCase: %case_display_name%");
        gui.set("rows", Integer.valueOf(6));
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> glass = new HashMap<>();
        glass.put("material", "GRAY_STAINED_GLASS_PANE");
        glass.put("name", " ");
        glass.put("slots", Arrays.asList(new Integer[]{Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6), Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9), Integer.valueOf(17), Integer.valueOf(18), Integer.valueOf(26), Integer.valueOf(27), Integer.valueOf(35), Integer.valueOf(36), Integer.valueOf(44)}));
        items.add(glass);
        Map<String, Object> startBtn = new HashMap<>();
        startBtn.put("material", "TRIPWIRE_HOOK");
        startBtn.put("name", "&aOpen Case");
        startBtn.put("lore", Arrays.asList(new String[]{"&6Click to open the case!", "&fYour keys: %keys%", "", "&eBuy on the website: https://kernelfm.github.io/"}));
        startBtn.put("slots", Collections.singletonList(Integer.valueOf(22)));
        startBtn.put("action", "start");
        items.add(startBtn);
        for (int i = 1; i <= 9; i++) {
            Map<String, Object> histItem = new HashMap<>();
            histItem.put("type", "history-" + i);
            histItem.put("slots", Collections.singletonList(Integer.valueOf(44 + i)));
            items.add(histItem);
        }
        gui.set("items", items);
        ConfigurationSection holo = cfg.createSection("hologram");
        holo.set("enabled", Boolean.valueOf(true));
        holo.set("y-offset", Double.valueOf(2.2D));
        holo.set("lines", Arrays.asList(new String[]{"&#FF8C00&lDefault Case", "&7Left/Right click to open"}));
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadCases() {
        this.cases.clear();
        this.disabledCases.clear();
        File[] files = this.casesFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null)
            return;
        for (File f : files) {
            String name = f.getName().replace(".yml", "").toLowerCase();
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String displayName = cfg.getString("display-name", name);
            String material = cfg.getString("material", "CHEST");
            String animName = cfg.getString("animation", "chests").toLowerCase();
            ConfigurationSection animCfg = cfg.getConfigurationSection("animation-settings");
            ConfigurationSection guiCfg = cfg.getConfigurationSection("gui");
            ConfigurationSection holoSec = cfg.getConfigurationSection("hologram");
            boolean holoEnabled = (holoSec != null && holoSec.getBoolean("enabled", false));
            double holoOffset = (holoSec != null) ? holoSec.getDouble("y-offset", 2.0D) : 2.0D;
            List<String> holoLines = (holoSec != null) ? holoSec.getStringList("lines") : new ArrayList<>();
            List<RewardModel> rewardList = new ArrayList<>();
            ConfigurationSection rewardsSec = cfg.getConfigurationSection("rewards");
            if (rewardsSec != null) {
                for (String key : rewardsSec.getKeys(false)) {
                    ConfigurationSection rs = rewardsSec.getConfigurationSection(key);
                    if (rs == null)
                        continue;
                    rewardList.add(new RewardModel(key, rs
                            .getString("display-name", key), rs
                            .getString("material", "STONE"), rs
                            .getDouble("chance", 1.0D), rs
                            .getStringList("commands")));
                }
            }
            this.cases.put(name, new CaseModel(name, displayName, material, animName, animCfg, rewardList, guiCfg, holoEnabled, holoOffset, holoLines));
        }
    }
    public void loadBlocks() {
        this.caseBlocks.clear();
        for (Map.Entry<String, String> e : this.db.loadAllBlocks().entrySet()) {
            Location loc = deserializeLoc(e.getKey());
            if (loc != null) this.caseBlocks.put(loc, e.getValue());
        }
    }
    public void loadKeys() {
        this.playerKeys.clear();
        this.playerKeys.putAll(this.db.loadAllKeys());
    }
    public void loadHistory() {
        this.history.clear();
        this.history = this.db.loadAllHistory();
    }
    public void addHistoryEntry(String caseName, String username, String rewardName, String rewardMat) {
        long ts = System.currentTimeMillis();
        history.add(0, new HistoryEntry(caseName, username, rewardName, rewardMat, new Timestamp(ts)));
        if (history.size() > 9) history.remove(history.size() - 1);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.db.addHistory(caseName, username, rewardName, rewardMat, ts);
            this.db.trimHistory();
        });
    }
    public CaseModel getCase(String name) {
        return this.cases.get(name.toLowerCase());
    }
    public Collection<CaseModel> getCases() {
        return this.cases.values();
    }
    public String getCaseAtBlock(Location loc) {
        return this.caseBlocks.get(loc);
    }
    public void registerBlock(Location loc, String caseName) {
        this.caseBlocks.put(loc, caseName.toLowerCase());
        String locStr = serializeLoc(loc);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.db.saveBlock(locStr, caseName));
        CaseModel cm = getCase(caseName);
        if (cm != null && cm.isHoloEnabled()) spawnBlockHologram(loc, cm);
    }
    public void unregisterBlock(Location loc) {
        this.caseBlocks.remove(loc);
        String locStr = serializeLoc(loc);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.db.deleteBlock(locStr));
        removeBlockHologram(loc);
    }
    public int getKeys(UUID uuid, String caseName) {
        Map<String, Integer> map = this.playerKeys.get(uuid);
        return (map == null) ? 0 : (map.getOrDefault(caseName.toLowerCase(), Integer.valueOf(0))).intValue();
    }
    public void setKeys(UUID uuid, String caseName, int amount) {
        Map<String, Integer> map = this.playerKeys.computeIfAbsent(uuid, k -> new HashMap<>());
        int val = Math.max(0, amount);
        map.put(caseName.toLowerCase(), val);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.db.saveKey(uuid, caseName, val));
    }
    public void addKeys(UUID uuid, String caseName, int amount) {
        setKeys(uuid, caseName, getKeys(uuid, caseName) + amount);
    }
    public boolean takeKey(UUID uuid, String caseName) {
        int cur = getKeys(uuid, caseName);
        if (cur <= 0) return false;
        setKeys(uuid, caseName, cur - 1);
        return true;
    }
    public DatabaseManager getDatabaseManager() {
        return this.db;
    }
    public void spawnAllBlockHolograms() {
        if (!HologramBridge.isAvailable())
            return;
        for (Map.Entry<Location, String> e : this.caseBlocks.entrySet()) {
            CaseModel cm = getCase(e.getValue());
            if (cm != null && cm.isHoloEnabled()) spawnBlockHologram(e.getKey(), cm);
        }
    }
    public void spawnBlockHologram(Location loc, CaseModel cm) {
        if (!HologramBridge.isAvailable())
            return;
        String name = "caseblock_" + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        Location holoLoc = loc.clone().add(0.5D, cm.getHoloYOffset(), 0.5D);
        List<String> colored = new ArrayList<>();
        for (String line : cm.getHoloLines()) colored.add(ColorUtil.color(line));
        try {
            HologramBridge.remove(name);
            HologramBridge.create(name, holoLoc, colored);
            this.blockHolos.put(loc, name);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public void removeBlockHologram(Location loc) {
        String name = this.blockHolos.remove(loc);
        if (name != null && HologramBridge.isAvailable()) {
            HologramBridge.remove(name);
        }
    }
    public void removeAllBlockHolograms() {
        if (!HologramBridge.isAvailable())
            return;
        for (String name : this.blockHolos.values()) {
            HologramBridge.remove(name);
        }
        this.blockHolos.clear();
    }
    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY();
    }
    private Location deserializeLoc(String str) {
        String[] parts = str.split(",");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public MetaCasesFree getPlugin() {
        return this.plugin;
    }
    public boolean isCaseDisabled(String caseName) {
        return this.disabledCases.contains(caseName.toLowerCase());
    }
    public void disableCase(String caseName) {
        this.disabledCases.add(caseName.toLowerCase());
    }
    public void enableCase(String caseName) {
        this.disabledCases.remove(caseName.toLowerCase());
    }
}
