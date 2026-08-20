package fun.metaproject.metaCases.command;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.case_system.CaseManager;
import fun.metaproject.metaCases.case_system.CaseModel;
public class CaseCommand implements CommandExecutor, TabCompleter {
    private final MetaCasesFree plugin;
    private final CaseManager caseManager;
    public CaseCommand(MetaCasesFree plugin) {
        this.plugin = plugin;
        this.caseManager = plugin.getCaseManager();
    }
    private void msg(CommandSender sender, String key, String... repls) {
        sender.sendMessage(this.plugin.getMsg(key, repls));
    }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "keys":
                if (!(sender instanceof Player)) {
                    msg(sender, "only-players");
                    return true;
                }
                if (!sender.hasPermission(this.plugin.getPerm("use"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                showKeys((Player) sender);
                return true;
            case "set":
                if (!(sender instanceof Player)) {
                    msg(sender, "only-players");
                    return true;
                }
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg(sender, "set-usage");
                    return true;
                }
                setCase((Player) sender, args[1]);
                return true;
            case "delete":
            case "remove":
                if (!(sender instanceof Player)) {
                    msg(sender, "only-players");
                    return true;
                }
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                deleteCase((Player) sender, (args.length >= 2) ? args[1] : null);
                return true;
            case "givekey":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 4) {
                    msg(sender, "givekey-usage");
                    return true;
                }
                giveKey(sender, args[1], args[2], args[3]);
                return true;
            case "takekeys":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 4) {
                    msg(sender, "takekeys-usage");
                    return true;
                }
                takeKeys(sender, args[1], args[2], args[3]);
                return true;
            case "infoplayer":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg(sender, "infoplayer-usage");
                    return true;
                }
                infoPlayer(sender, args[1]);
                return true;
            case "giveall":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    msg(sender, "giveall-usage");
                    return true;
                }
                giveAllKeys(sender, args[1], args[2]);
                return true;
            case "on":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg(sender, "on-usage");
                    return true;
                }
                enableCase(sender, args[1]);
                return true;
            case "off":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg(sender, "off-usage");
                    return true;
                }
                disableCase(sender, args[1]);
                return true;
            case "reload":
                if (!sender.hasPermission(this.plugin.getPerm("admin"))) {
                    msg(sender, "no-permission");
                    return true;
                }
                reloadPlugin(sender);
                return true;
        }
        sendHelp(sender);
        return true;
    }
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.plugin.getMsg("help-header"));
        if (sender.hasPermission(this.plugin.getPerm("use"))) {
            sender.sendMessage(this.plugin.getMsg("help-item-keys"));
        }
        if (sender.hasPermission(this.plugin.getPerm("admin"))) {
            sender.sendMessage(this.plugin.getMsg("help-item-set"));
            sender.sendMessage(this.plugin.getMsg("help-item-delete"));
            sender.sendMessage(this.plugin.getMsg("help-item-givekey"));
            sender.sendMessage(this.plugin.getMsg("help-item-takekeys"));
            sender.sendMessage(this.plugin.getMsg("help-item-infoplayer"));
            sender.sendMessage(this.plugin.getMsg("help-item-giveall"));
            sender.sendMessage(this.plugin.getMsg("help-item-on"));
            sender.sendMessage(this.plugin.getMsg("help-item-off"));
            sender.sendMessage(this.plugin.getMsg("help-item-reload"));
        }
    }
    private void showKeys(Player p) {
        p.sendMessage(this.plugin.getMsg("my-keys-header"));
        boolean hasKeys = false;
        for (CaseModel caseModel : this.caseManager.getCases()) {
            int amount = this.caseManager.getKeys(p.getUniqueId(), caseModel.getName());
            if (amount > 0) {
                p.sendMessage(this.plugin.getMsg("my-keys-item", new String[]{"%case%", caseModel.getDisplayName(), "%amount%", String.valueOf(amount)}));
                hasKeys = true;
            }
        }
        if (!hasKeys) p.sendMessage(this.plugin.getMsg("my-keys-empty"));
    }
    private void setCase(Player p, String caseName) {
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(p, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        Block block = p.getTargetBlockExact(5);
        if (block == null || block.getType().isAir()) {
            msg(p, "look-at-block");
            return;
        }
        this.caseManager.registerBlock(block.getLocation(), caseModel.getName());
        msg(p, "set-success", new String[]{"%coords%", "" + block.getX() + ", " + block.getX() + ", " + block.getY(), "%case%", caseModel.getDisplayName()});
    }
    private void deleteCase(Player p, String caseName) {
        Block block = p.getTargetBlockExact(5);
        if (block == null || block.getType().isAir()) {
            msg((CommandSender) p, "look-at-block");
            return;
        }
        String caseAtBlock = this.caseManager.getCaseAtBlock(block.getLocation());
        if (caseAtBlock == null) {
            msg((CommandSender) p, "delete-not-registered");
            return;
        }
        if (caseName != null && !caseAtBlock.equalsIgnoreCase(caseName)) {
            msg(p, "delete-mismatch", new String[]{"%case%", caseAtBlock, "%target%", caseName});
            return;
        }
        this.caseManager.unregisterBlock(block.getLocation());
        msg((CommandSender) p, "delete-success");
    }
    private void giveKey(CommandSender sender, String playerName, String caseName, String amountStr) {
        int amount;
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(sender, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                msg(sender, "amount-positive");
                return;
            }
        } catch (NumberFormatException e) {
            msg(sender, "invalid-amount");
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        this.caseManager.addKeys(offlinePlayer.getUniqueId(), caseModel.getName(), amount);
        msg(sender, "givekey-success", new String[]{"%player%", playerName, "%amount%", String.valueOf(amount), "%case%", caseModel.getDisplayName()});
    }
    private void takeKeys(CommandSender sender, String targetName, String caseName, String amountStr) {
        int amount;
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(sender, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                msg(sender, "amount-positive");
                return;
            }
        } catch (NumberFormatException e) {
            msg(sender, "invalid-amount");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            msg(sender, "player-not-found");
            return;
        }
        int current = this.caseManager.getKeys(target.getUniqueId(), caseModel.getName());
        int newAmount = Math.max(0, current - amount);
        this.caseManager.setKeys(target.getUniqueId(), caseModel.getName(), newAmount);
        msg(sender, "takekey-success", new String[]{"%player%", target.getName(), "%amount%", String.valueOf(current - newAmount), "%case%", caseModel.getDisplayName()});
    }
    private void infoPlayer(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            msg(sender, "player-not-found");
            return;
        }
        sender.sendMessage(this.plugin.getMsg("keys-info-header", new String[]{"%player%", target.getName()}));
        boolean hasKeys = false;
        for (CaseModel caseModel : this.caseManager.getCases()) {
            int amount = this.caseManager.getKeys(target.getUniqueId(), caseModel.getName());
            if (amount > 0) {
                sender.sendMessage(this.plugin.getMsg("keys-info-item", new String[]{"%case%", caseModel.getDisplayName(), "%amount%", String.valueOf(amount)}));
                hasKeys = true;
            }
        }
        if (!hasKeys)
            sender.sendMessage(this.plugin.getMsg("keys-info-empty"));
    }
    private void giveAllKeys(CommandSender sender, String caseName, String amountStr) {
        int amount;
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(sender, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        try {
            amount = Integer.parseInt(amountStr);
            if (amount <= 0) {
                msg(sender, "amount-positive");
                return;
            }
        } catch (NumberFormatException e) {
            msg(sender, "invalid-amount");
            return;
        }
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.caseManager.addKeys(p.getUniqueId(), caseModel.getName(), amount);
            msg((CommandSender) p, "giveall-broadcast", new String[]{"%amount%", String.valueOf(amount), "%case%", caseModel.getDisplayName()});
            count++;
        }
        msg(sender, "giveall-success", new String[]{"%amount%", String.valueOf(amount), "%case%", caseModel.getDisplayName(), "%count%", String.valueOf(count)});
    }
    private void enableCase(CommandSender sender, String caseName) {
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(sender, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        if (!this.caseManager.isCaseDisabled(caseModel.getName())) {
            msg(sender, "on-already", new String[]{"%case%", caseModel.getDisplayName()});
            return;
        }
        this.caseManager.enableCase(caseModel.getName());
        msg(sender, "on-success", new String[]{"%case%", caseModel.getDisplayName()});
    }
    private void disableCase(CommandSender sender, String caseName) {
        CaseModel caseModel = this.caseManager.getCase(caseName);
        if (caseModel == null) {
            msg(sender, "case-config-not-found", new String[]{"%case%", caseName});
            return;
        }
        if (this.caseManager.isCaseDisabled(caseModel.getName())) {
            msg(sender, "off-already", new String[]{"%case%", caseModel.getDisplayName()});
            return;
        }
        this.caseManager.disableCase(caseModel.getName());
        msg(sender, "off-success", new String[]{"%case%", caseModel.getDisplayName()});
    }
    private void reloadPlugin(CommandSender sender) {
        this.plugin.reloadConfig();
        this.plugin.getLangManager().load();
        this.plugin.getAnimationManager().stopAll();
        this.caseManager.removeAllBlockHolograms();
        this.caseManager.loadCases();
        this.caseManager.loadBlocks();
        this.caseManager.loadKeys();
        this.caseManager.loadHistory();
        this.caseManager.spawnAllBlockHolograms();
        msg(sender, "reload-success");
    }
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (sender.hasPermission(this.plugin.getPerm("use"))) {
                list.add("keys");
            }
            if (sender.hasPermission(this.plugin.getPerm("admin"))) {
                list.addAll(Arrays.asList(new String[]{"set", "delete", "remove", "givekey", "takekeys", "infoplayer", "giveall", "on", "off", "reload"}));
            }
            return (List<String>) StringUtil.copyPartialMatches(args[0], list, new ArrayList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") && sender.hasPermission(this.plugin.getPerm("admin"))) {
                List<String> cases = new ArrayList<>();
                for (CaseModel caseModel : this.caseManager.getCases()) {
                    cases.add(caseModel.getName());
                }
                return (List<String>) StringUtil.copyPartialMatches(args[1], cases, new ArrayList());
            }
            if (sub.equals("delete") && sender.hasPermission(this.plugin.getPerm("admin"))) {
                List<String> cases = new ArrayList<>();
                for (CaseModel caseModel : this.caseManager.getCases()) {
                    cases.add(caseModel.getName());
                }
                return (List<String>) StringUtil.copyPartialMatches(args[1], cases, new ArrayList());
            }
            if (((sub.equals("givekey") || sub.equals("takekeys") || sub.equals("infoplayer")) && sender.hasPermission(this.plugin.getPerm("admin")))) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return (List<String>) StringUtil.copyPartialMatches(args[1], players, new ArrayList());
            }
            if ((sub.equals("giveall") || sub.equals("on") || sub.equals("off")) && sender.hasPermission(this.plugin.getPerm("admin"))) {
                List<String> cases = new ArrayList<>();
                for (CaseModel caseModel : this.caseManager.getCases()) {
                    cases.add(caseModel.getName());
                }
                return (List<String>) StringUtil.copyPartialMatches(args[1], cases, new ArrayList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("givekey") || sub.equals("takekeys")) && sender.hasPermission(this.plugin.getPerm("admin"))) {
                List<String> cases = new ArrayList<>();
                for (CaseModel caseModel : this.caseManager.getCases()) {
                    cases.add(caseModel.getName());
                }
                return (List<String>) StringUtil.copyPartialMatches(args[2], cases, new ArrayList());
            }
        }
        return Collections.emptyList();
    }
}
