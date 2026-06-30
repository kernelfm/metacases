package pw.fusionmine.fusioncases.animation.impl.soulwell;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.animation.api.CaseAnimation;
import pw.fusionmine.fusioncases.case_system.CaseModel;

@AllArgsConstructor
public class SoulWellAnimation implements CaseAnimation {

    private final FusionCases plugin;

    public String getName() {
        return "soulwell";
    }

    public AnimationSession createSession(Player player, Location location, CaseModel caseModel, ConfigurationSection animationConfig) {
        return new SoulWellSession(this.plugin, player, location, caseModel, animationConfig);
    }

}
