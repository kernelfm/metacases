package fun.metaproject.metaCases.animation.impl.tnt;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.animation.api.AnimationSession;
import fun.metaproject.metaCases.animation.api.CaseAnimation;
import fun.metaproject.metaCases.case_system.CaseModel;
@AllArgsConstructor
public class TntAnimation implements CaseAnimation {
    private final MetaCasesFree plugin;
    public String getName() {
        return "tnt";
    }
    public AnimationSession createSession(Player p, Location loc, CaseModel caseModel, ConfigurationSection cfg) {
        return new TntSession(this.plugin, p, loc, caseModel, cfg);
    }
}
