package pw.fusionmine.fusioncases.animation.impl.chests;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.animation.api.CaseAnimation;
import pw.fusionmine.fusioncases.case_system.CaseModel;

@AllArgsConstructor
public class ChestsAnimation implements CaseAnimation {

    private final FusionCases plugin;

    public String getName() {
        return "chests";
    }

    public AnimationSession createSession(Player p, Location loc, CaseModel caseModel, ConfigurationSection cfg) {
        return new ChestsSession(this.plugin, p, loc, caseModel, cfg);
    }

}