package fun.metaproject.metaCases.case_system;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import fun.metaproject.metaCases.utility.ColorUtil;
import fun.metaproject.metaCases.utility.ItemUtil;
import java.util.*;
@AllArgsConstructor
public class CaseGuiManager {
    private CaseManager caseManager;
    public void openGui(Player p, CaseModel cm, Location loc) {
        ConfigurationSection gui = cm.getGuiConfig();
        int rows = (gui != null) ? gui.getInt("rows", 6) : 6;
        String rawTitle = (gui != null) ? gui.getString("title", "&aКейс: %case_display_name%") : "&aКейс: %case_display_name%";
        String title = formatPlaceholders(rawTitle, p, cm);
        Inventory inv = Bukkit.createInventory(new CaseGuiHolder(cm, loc), rows * 9, title);
        if (gui != null) {
            List<?> itemsList = gui.getList("items");
            if (itemsList != null) {
                List<CaseModel> playerCases = fetchPlayerCases(p);
                int caseIndex = 0;
                for (Object obj : itemsList) {
                    if (!(obj instanceof Map)) continue;
                    Map<?, ?> map = (Map<?, ?>) obj;
                    List<Integer> slotsList = extractSlotsFromMap(map);
                    String type = (String) map.get("type");
                    if (type != null && type.equals("case")) {
                        caseIndex = fillCaseItems(inv, slotsList, playerCases, caseIndex, rows, p, cm, map);
                        continue;
                    }
                    ItemStack stack = buildItemByType(type, map, p, cm);
                    if (stack != null) {
                        for (int sl : slotsList) {
                            if (sl >= 0 && sl < rows * 9) inv.setItem(sl, stack);
                        }
                    }
                }
            }
        }
        p.openInventory(inv);
    }
    private List<CaseModel> fetchPlayerCases(Player p) {
        List<CaseModel> playerCases = new ArrayList<>();
        for (CaseModel c : caseManager.getCases()) {
            int amount = caseManager.getKeys(p.getUniqueId(), c.getName());
            for (int i = 0; i < amount; i++) {
                playerCases.add(c);
            }
        }
        playerCases.sort(Comparator.comparing(CaseModel::getName));
        return playerCases;
    }
    private List<Integer> extractSlotsFromMap(Map<?, ?> map) {
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
        return slotsList;
    }
    private int fillCaseItems(Inventory inv, List<Integer> slotsList, List<CaseModel> playerCases, int caseIndex, int rows, Player p, CaseModel cm, Map<?, ?> map) {
        for (int sl : slotsList) {
            if (sl >= 0 && sl < rows * 9 && caseIndex < playerCases.size()) {
                CaseModel targetCase = playerCases.get(caseIndex);
                ItemStack caseStack = createCaseItem(targetCase, p, cm, map);
                inv.setItem(sl, caseStack);
                caseIndex++;
            }
        }
        return caseIndex;
    }
    private ItemStack buildItemByType(String type, Map<?, ?> map, Player p, CaseModel cm) {
        if (type != null && type.startsWith("history-")) {
            return buildHistoryItem(type);
        }
        return buildDecorativeItem(map, p, cm);
    }
    private ItemStack buildHistoryItem(String type) {
        try {
            int idx = Integer.parseInt(type.substring(8)) - 1;
            if (idx >= 0 && idx < caseManager.history.size()) {
                HistoryEntry entry = caseManager.history.get(idx);
                ItemStack stack = ItemUtil.getItem(entry.getRewardMaterial(), Material.PAPER);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    List<String> wonLore = new ArrayList<>();
                    for (String s : caseManager.plugin.getLangManager().getStringList("gui-history-won")) {
                        s = s.replace("%time%", entry.getTime())
                                .replace("%reward%", entry.getRewardDisplayName())
                                .replace("%case%", entry.getCaseName());
                        wonLore.add(ColorUtil.color(s));
                    }
                    String playerColor = caseManager.plugin.getLangManager().get("gui-history-player-color");
                    meta.setDisplayName(ColorUtil.color(playerColor + entry.getUsername()));
                    meta.setLore(wonLore);
                    stack.setItemMeta(meta);
                }
                return stack;
            } else {
                ItemStack stack = new ItemStack(Material.BARRIER);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtil.color(caseManager.plugin.getMsg("gui-history-empty")));
                    stack.setItemMeta(meta);
                }
                return stack;
            }
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }
    private ItemStack buildDecorativeItem(Map<?, ?> map, Player p, CaseModel cm) {
        String matStr = (String) map.get("material");
        if (matStr != null) {
            Material mat = Material.matchMaterial(matStr);
            if (mat != null) {
                ItemStack stack = new ItemStack(mat);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    String dn = (String) map.get("name");
                    if (dn != null) meta.setDisplayName(formatPlaceholders(dn, p, cm));
                    List<?> lore = (List) map.get("lore");
                    if (lore != null) {
                        List<String> colored = new ArrayList<>();
                        for (Object line : lore) {
                            colored.add(formatPlaceholders(line.toString(), p, cm));
                        }
                        meta.setLore(colored);
                    }
                    stack.setItemMeta(meta);
                }
                return stack;
            }
        }
        return null;
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
                List<String> lore = Collections.singletonList(ColorUtil.color(caseManager.plugin.getMsg("gui-case-lore")
                        .replace("%keys%", String.valueOf(caseManager.getKeys(p.getUniqueId(), targetCase.getName())))));
                meta.setLore(lore);
            }
            NamespacedKey key = new NamespacedKey(caseManager.plugin, "case_name");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, targetCase.getName());
            stack.setItemMeta(meta);
        }
        return stack;
    }
    public String formatPlaceholders(String text, Player p, CaseModel cm) {
        if (text == null) return null;
        int keys = caseManager.getKeys(p.getUniqueId(), cm.getName());
        int allKeys = 0;
        for (CaseModel c : caseManager.getCases()) allKeys += caseManager.getKeys(p.getUniqueId(), c.getName());
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
}
