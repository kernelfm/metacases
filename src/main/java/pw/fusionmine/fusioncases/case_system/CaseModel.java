package pw.fusionmine.fusioncases.case_system;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.configuration.ConfigurationSection;

public class CaseModel {

    private final String name;
    private final String displayName;
    private final String material;
    private final String animationName;
    private final ConfigurationSection animationConfig;
    private final List<RewardModel> rewards;
    private final ConfigurationSection guiConfig;
    private final boolean holoEnabled;
    private final double holoYOffset;
    private final List<String> holoLines;

    public CaseModel(String name, String displayName, String material, String animationName, ConfigurationSection animationConfig, List<RewardModel> rewards, ConfigurationSection guiConfig, boolean holoEnabled, double holoYOffset, List<String> holoLines) {
        this.name = name;
        this.displayName = displayName;
        this.material = material;
        this.animationName = animationName;
        this.animationConfig = animationConfig;
        this.rewards = rewards;
        this.guiConfig = guiConfig;
        this.holoEnabled = holoEnabled;
        this.holoYOffset = holoYOffset;
        this.holoLines = holoLines;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getMaterial() {
        return this.material;
    }

    public String getAnimationName() {
        return this.animationName;
    }

    public ConfigurationSection getAnimationConfig() {
        return this.animationConfig;
    }

    public List<RewardModel> getRewards() {
        return this.rewards;
    }

    public ConfigurationSection getGuiConfig() {
        return this.guiConfig;
    }

    public boolean isHoloEnabled() {
        return this.holoEnabled;
    }

    public double getHoloYOffset() {
        return this.holoYOffset;
    }

    public List<String> getHoloLines() {
        return this.holoLines;
    }

    public RewardModel getRandomReward() {
        if (this.rewards.isEmpty()) return null;

        double total = 0.0D;
        for (RewardModel r : this.rewards) total += r.getChance();

        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double acc = 0.0D;
        for (RewardModel r : this.rewards) {
            acc += r.getChance();
            if (roll <= acc) return r;
        }
        return this.rewards.get(this.rewards.size() - 1);
    }

}