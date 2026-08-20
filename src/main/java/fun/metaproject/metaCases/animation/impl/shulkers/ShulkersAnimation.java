package fun.metaproject.metaCases.animation.impl.shulkers;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.animation.api.AnimationSession;
import fun.metaproject.metaCases.animation.api.CaseAnimation;
import fun.metaproject.metaCases.case_system.CaseModel;
@AllArgsConstructor
public class ShulkersAnimation implements CaseAnimation {
    private final MetaCasesFree plugin;
    public String getName() {
        return "shulkers";
    }
    public AnimationSession createSession(Player player, Location location, CaseModel caseModel, ConfigurationSection animationConfig) {
        return new ShulkersSession(this.plugin, player, location, caseModel, animationConfig);
    }
}
