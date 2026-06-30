package pw.fusionmine.fusioncases.case_system;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationConfig;
import pw.fusionmine.fusioncases.database.DatabaseManager;
import pw.fusionmine.fusioncases.hologram.HologramBridge;
import pw.fusionmine.fusioncases.utility.ColorUtil;
import pw.fusionmine.fusioncases.utility.ItemUtil;

public class CaseManager {

    private final Map<String, CaseModel> cases = new HashMap<>();
    private final FusionCases plugin;
    private final Map<Location, String> caseBlocks = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerKeys = new HashMap<>();
    private final Map<String, List<HistoryEntry>> history = new HashMap<>();
    private final Map<Location, String> blockHolos = new HashMap<>();
    private final Set<String> disabledCases = new HashSet<>();

    private File casesFolder;
    private DatabaseManager db;

    public CaseManager(FusionCases plugin) {
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
        cfg.set("animation", "chests");

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
        this.history.putAll(this.db.loadAllHistory());
    }

    public List<HistoryEntry> getHistory(String caseName) {
        return this.history.getOrDefault(caseName.toLowerCase(), new ArrayList<>());
    }

    public void addHistoryEntry(String caseName, String username, String rewardName, String rewardMat) {
        List<HistoryEntry> list = this.history.computeIfAbsent(caseName.toLowerCase(), k -> new ArrayList());
        long ts = System.currentTimeMillis();
        list.add(0, new HistoryEntry(username, rewardName, rewardMat, new Timestamp(ts)));
        if (list.size() > 9) list.remove(list.size() - 1);

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            this.db.addHistory(caseName, username, rewardName, rewardMat, ts);
            this.db.trimHistory(caseName);
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

    public String formatPlaceholders(String text, Player p, CaseModel cm) {
        if (text == null) return null;
        int keys = getKeys(p.getUniqueId(), cm.getName());
        int allKeys = 0;
        for (CaseModel c : getCases()) allKeys += getKeys(p.getUniqueId(), c.getName());
        return ColorUtil.color(text
                .replace("%keys%", String.valueOf(keys))
                .replace("%all_keys%", String.valueOf(allKeys))
                .replace("%case_display_name%", cm.getDisplayName())
                .replace("%username%", p.getName()));
    }

    public String getActionAtSlot(CaseModel cm, int slot) {
        ConfigurationSection gui = cm.getGuiConfig();
        if (gui == null) return null;
        List<?> items = gui.getList("items");
        if (items == null) return null;

        for (Object obj : items) {
            if (!(obj instanceof Map))
                continue;
            Map<?, ?> map = (Map<?, ?>) obj;
            List<?> slots = (List) map.get("slots");
            if (slots != null && slots.contains(Integer.valueOf(slot))) return (String) map.get("action");
            Object single = map.get("slot");
            if (single instanceof Number && ((Number) single).intValue() == slot) return (String) map.get("action");
        }
        return null;
    }

    public void openGui(Player p, CaseModel cm, Location loc) {
        ConfigurationSection gui = cm.getGuiConfig();
        int rows = (gui != null) ? gui.getInt("rows", 6) : 6;
        String rawTitle = (gui != null) ? gui.getString("title", "&aКейс: %case_display_name%") : "&aКейс: %case_display_name%";
        String title = formatPlaceholders(rawTitle, p, cm);

        Inventory inv = Bukkit.createInventory(new CaseGuiHolder(cm, loc), rows * 9, title);

        if (gui != null) {
            List<?> itemsList = gui.getList("items");
            if (itemsList != null) {
                List<CaseModel> playerCases = new ArrayList<>();
                for (CaseModel c : getCases()) {
                    int amount = getKeys(p.getUniqueId(), c.getName());
                    for (int i = 0; i < amount; i++) {
                        playerCases.add(c);
                    }
                }

                playerCases.sort(Comparator.comparing(CaseModel::getName));
                int caseIndex = 0;

                for (Object obj : itemsList) {
                    if (!(obj instanceof Map))
                        continue;
                    Map<?, ?> map = (Map<?, ?>) obj;

                    List<Integer> slotsList = new ArrayList<>();
                    List<?> slots = (List) map.get("slots");
                    if (slots != null) {
                        for (Object so : slots) {
                            if (so instanceof Number) {
                                slotsList.add(Integer.valueOf(((Number) so).intValue()));
                                continue;
                            }
                            if (so != null) {
                                try {
                                    slotsList.add(Integer.valueOf(Integer.parseInt(so.toString().trim())));
                                } catch (NumberFormatException numberFormatException) {}
                            }
                        }
                    }

                    Object single = map.get("slot");
                    if (single instanceof Number) {
                        slotsList.add(Integer.valueOf(((Number) single).intValue()));
                    } else if (single != null) {
                        try {
                            slotsList.add(Integer.valueOf(Integer.parseInt(single.toString().trim())));
                        } catch (NumberFormatException numberFormatException) {}
                    }

                    String type = (String) map.get("type");
                    ItemStack stack = null;

                    if (type != null && type.equals("case")) {
                        for (Iterator<Integer> iterator = slotsList.iterator(); iterator.hasNext(); ) {
                            int sl = (iterator.next()).intValue();
                            if (sl >= 0 && sl < rows * 9 && caseIndex < playerCases.size()) {
                                CaseModel targetCase = playerCases.get(caseIndex);
                                ItemStack caseStack = createCaseItem(targetCase, p, cm, map);
                                inv.setItem(sl, caseStack);
                                caseIndex++;
                            }
                        }
                    } else if (type != null && type.startsWith("history-")) {
                        try {
                            int idx = Integer.parseInt(type.substring(8)) - 1;
                            List<HistoryEntry> hist = getHistory(cm.getName());
                            if (idx >= 0 && idx < hist.size()) {
                                HistoryEntry entry = hist.get(idx);
                                stack = ItemUtil.getItem(entry.getRewardMaterial(), Material.PAPER);
                                ItemMeta meta = stack.getItemMeta();
                                if (meta != null) {
                                    List<String> wonLore = new ArrayList<>();
                                    for(String s : plugin.getLangManager().getStringList("gui-history-won")) {
                                        s = s
                                        .replace("%time%", entry.getTime())
                                        .replace("%reward%", entry.getRewardDisplayName());
                                        wonLore.add(ColorUtil.color(s));
                                    }
                                    String playerColor = this.plugin.getLangManager().get("gui-history-player-color");
                                    meta.setDisplayName(ColorUtil.color(playerColor + entry.getUsername()));
                                    meta.setLore(wonLore);
                                    stack.setItemMeta(meta);
                                }
                            } else {
                                stack = new ItemStack(Material.BARRIER);
                                ItemMeta meta = stack.getItemMeta();
                                if (meta != null) {
                                    meta.setDisplayName(ColorUtil.color(this.plugin.getMsg("gui-history-empty", new String[0])));
                                    stack.setItemMeta(meta);
                                }
                            }
                        } catch (NumberFormatException numberFormatException) {}
                    } else {
                        String matStr = (String) map.get("material");
                        if (matStr != null) {
                            Material mat = Material.matchMaterial(matStr);
                            if (mat != null) {
                                stack = new ItemStack(mat);
                                ItemMeta meta = stack.getItemMeta();
                                if (meta != null) {
                                    String dn = (String) map.get("name");
                                    if (dn != null) meta.setDisplayName(formatPlaceholders(dn, p, cm));
                                    List<?> lore = (List) map.get("lore");
                                    if (lore != null) {
                                        List<String> colored = new ArrayList<>();
                                        for (Object line : lore)
                                            colored.add(formatPlaceholders(line.toString(), p, cm));
                                        meta.setLore(colored);
                                    }
                                    stack.setItemMeta(meta);
                                }
                            }
                        }
                    }

                    if (stack != null) {
                        for (Iterator<Integer> iterator = slotsList.iterator(); iterator.hasNext(); ) {
                            int sl = (iterator.next()).intValue();
                            if (sl >= 0 && sl < rows * 9) inv.setItem(sl, stack);
                        }

                    }
                }
            }
        }
        p.openInventory(inv);
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

    public FusionCases getPlugin() {
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

    private ItemStack createCaseItem(CaseModel targetCase, Player p, CaseModel openCase, Map<?, ?> guiItemMap) {
        String customMat = (guiItemMap != null) ? (String) guiItemMap.get("material") : null;
        String matStr = (customMat != null) ? customMat : targetCase.getMaterial();
        ItemStack stack = ItemUtil.getItem(matStr, Material.CHEST);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String customName = (guiItemMap != null) ? (String) guiItemMap.get("name") : null;
            if (customName != null) {
                meta.setDisplayName(formatPlaceholders(customName, p, targetCase));
            } else {
                meta.setDisplayName(formatPlaceholders(targetCase.getDisplayName(), p, targetCase));
            }
            List<?> customLore = (guiItemMap != null) ? (List) guiItemMap.get("lore") : null;
            if (customLore != null) {
                List<String> colored = new ArrayList<>();
                for (Object line : customLore) {
                    colored.add(formatPlaceholders(line.toString(), p, targetCase));
                }
                meta.setLore(colored);
            } else {
                List<String> lore = Collections.singletonList(ColorUtil.color(this.plugin.getMsg("gui-case-lore")
                        .replace("%keys%", String.valueOf(getKeys(p.getUniqueId(), targetCase.getName())))));
                meta.setLore(lore);
            }
            NamespacedKey key = new NamespacedKey(this.plugin, "case_name");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, targetCase.getName());
            stack.setItemMeta(meta);
        }
        return stack;
    }

}