package pw.fusionmine.fusioncases.case_system;

import java.util.List;

public class RewardModel {

    private final String id;
    private final String displayName;
    private final String material;
    private final double chance;
    private final List<String> commands;

    public RewardModel(String id, String displayName, String material, double chance, List<String> commands) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.chance = chance;
        this.commands = commands;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getMaterial() {
        return this.material;
    }

    public double getChance() {
        return this.chance;
    }

    public List<String> getCommands() {
        return this.commands;
    }

}