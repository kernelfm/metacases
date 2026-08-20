package fun.metaproject.metaCases.hologram;
import java.util.List;
import org.bukkit.Location;
public interface HologramProvider {
  void create(String paramString, Location paramLocation, List<String> paramList);
  void createWithItem(String paramString1, Location paramLocation, List<String> paramList, String paramString2);
  void remove(String paramString);
  void update(String paramString, List<String> paramList);
}
