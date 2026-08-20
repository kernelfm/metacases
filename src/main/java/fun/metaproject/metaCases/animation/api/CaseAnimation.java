package fun.metaproject.metaCases.animation.api;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import fun.metaproject.metaCases.case_system.CaseModel;
public interface CaseAnimation {
  String getName();
  AnimationSession createSession(Player paramPlayer, Location paramLocation, CaseModel paramCaseModel, ConfigurationSection paramConfigurationSection);
}
