package fun.metaproject.metaCases;
import java.io.File;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import fun.metaproject.metaCases.animation.impl.bees.BeesAnimation;
import fun.metaproject.metaCases.animation.api.AnimationManager;
import fun.metaproject.metaCases.animation.api.CaseAnimation;
import fun.metaproject.metaCases.animation.impl.chests.ChestsAnimation;
import fun.metaproject.metaCases.animation.impl.piglins.PiglinsAnimation;
import fun.metaproject.metaCases.animation.impl.shulkers.ShulkersAnimation;
import fun.metaproject.metaCases.animation.impl.soulwell.SoulWellAnimation;
import fun.metaproject.metaCases.animation.impl.tnt.TntAnimation;
import fun.metaproject.metaCases.case_system.CaseGuiManager;
import fun.metaproject.metaCases.case_system.CaseManager;
import fun.metaproject.metaCases.command.CaseCommand;
import fun.metaproject.metaCases.hologram.HologramBridge;
import fun.metaproject.metaCases.listener.CaseListener;
import fun.metaproject.metaCases.utility.ColorUtil;
import fun.metaproject.metaCases.utility.LangManager;
@Getter
public final class MetaCasesFree extends JavaPlugin {
    private CaseManager caseManager;
    private CaseGuiManager caseGuiManager;
    private AnimationManager animationManager;
    public void onEnable() {
        saveDefaultConfig();
        HologramBridge.init(this);
        this.langManager = new LangManager(this);
        this.caseManager = new CaseManager(this);
        this.caseGuiManager = new CaseGuiManager(this.caseManager);
        this.animationManager = new AnimationManager(this);
        registerAnimationIfFileExists(this.animationManager, "chests", new ChestsAnimation(this));
        registerAnimationIfFileExists(this.animationManager, "piglins", new PiglinsAnimation(this));
        registerAnimationIfFileExists(this.animationManager, "shulkers", new ShulkersAnimation(this));
        registerAnimationIfFileExists(this.animationManager, "soulwell", new SoulWellAnimation(this));
        registerAnimationIfFileExists(this.animationManager, "tnt", new TntAnimation(this));
        registerAnimationIfFileExists(this.animationManager, "bees", new BeesAnimation(this));
        getServer().getPluginManager().registerEvents(new CaseListener(this), this);
        CaseCommand cmd = new CaseCommand(this);
        getCommand("metacases").setExecutor(cmd);
        getCommand("metacases").setTabCompleter(cmd);
        int pluginId = 33540;
        new Metrics(this, pluginId);
    }
    private LangManager langManager;
    public void onDisable() {
        if (this.animationManager != null) {
            this.animationManager.stopAll();
        }
        if (this.caseManager != null) {
            this.caseManager.removeAllBlockHolograms();
            if (this.caseManager.getDatabaseManager() != null) {
                this.caseManager.getDatabaseManager().close();
            }
        }
    }
    public String getMsg(String key, String... repls) {
        String msg = this.langManager.get(key);
        for (int i = 0; i < repls.length; i += 2) {
            if (i + 1 < repls.length) {
                msg = msg.replace(repls[i], repls[i + 1]);
            }
        }
        if (key.startsWith("keys-info-") || key.startsWith("my-keys-") || key
                .equals("help-header") || key.startsWith("help-item-") || key
                .startsWith("gui-")) {
            return ColorUtil.color(msg);
        }
        return ColorUtil.color(getConfig().getString("prefix", "&#FF8C00&lMetaCases Free &7» &f") + msg);
    }
    public String getPerm(String key) {
        return getConfig().getString("permissions." + key, "metacases." + key);
    }
    private void registerAnimationIfFileExists(AnimationManager manager, String name, CaseAnimation anim) {
        File animFile = new File(new File(getDataFolder(), "animations"), name.toLowerCase() + ".yml");
        if (animFile.exists()) {
            manager.registerAnimation(anim);
        }
    }
}
