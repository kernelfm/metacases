package pw.fusionmine.fusioncases;

import java.io.File;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import pw.fusionmine.fusioncases.animation.impl.bees.BeesAnimation;
import pw.fusionmine.fusioncases.animation.api.AnimationManager;
import pw.fusionmine.fusioncases.animation.api.CaseAnimation;
import pw.fusionmine.fusioncases.animation.impl.chests.ChestsAnimation;
import pw.fusionmine.fusioncases.animation.impl.piglins.PiglinsAnimation;
import pw.fusionmine.fusioncases.animation.impl.shulkers.ShulkersAnimation;
import pw.fusionmine.fusioncases.animation.impl.soulwell.SoulWellAnimation;
import pw.fusionmine.fusioncases.animation.impl.tnt.TntAnimation;
import pw.fusionmine.fusioncases.case_system.CaseGuiManager;
import pw.fusionmine.fusioncases.case_system.CaseManager;
import pw.fusionmine.fusioncases.command.CaseCommand;
import pw.fusionmine.fusioncases.hologram.HologramBridge;
import pw.fusionmine.fusioncases.listener.CaseListener;
import pw.fusionmine.fusioncases.utility.ColorUtil;
import pw.fusionmine.fusioncases.utility.LangManager;

@Getter
public final class FusionCases extends JavaPlugin {

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
        getCommand("fusioncases").setExecutor(cmd);
        getCommand("fusioncases").setTabCompleter(cmd);
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
        return ColorUtil.color(getConfig().getString("prefix") + msg);
    }

    public String getPerm(String key) {
        return getConfig().getString("permissions." + key, "fusioncases." + key);
    }

    private void registerAnimationIfFileExists(AnimationManager manager, String name, CaseAnimation anim) {
        File animFile = new File(new File(getDataFolder(), "animations"), name.toLowerCase() + ".yml");
        if (animFile.exists()) {
            manager.registerAnimation(anim);
        }
    }

}