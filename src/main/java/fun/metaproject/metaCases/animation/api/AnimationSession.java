package fun.metaproject.metaCases.animation.api;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.case_system.CaseModel;
import fun.metaproject.metaCases.case_system.RewardModel;
import fun.metaproject.metaCases.hologram.HologramBridge;
import fun.metaproject.metaCases.utility.ColorUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@AllArgsConstructor
public abstract class AnimationSession {
    public final MetaCasesFree plugin;
    public final Player player;
    public final Location caseLoc;
    public final CaseModel caseModel;
    public final AnimationConfig ac;
    public State state;
    public final List<String> holoNames = new ArrayList();
    public abstract void start();
    public abstract void handleInteract(Player player, Block block, Action action);
    public abstract void stop();
    public void dispatchReward(RewardModel reward) {
        for (String cmd : reward.getCommands()) {
            String s = cmd
                    .replace("%username%", player.getName())
                    .replace("%group%", reward.getId()
            );
            if (s.startsWith("[command] ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.substring(10));
            } else if (s.startsWith("[broadcast] ")) {
                Bukkit.broadcastMessage(ColorUtil.color(s.substring(12)));
            } else if (s.startsWith("[message] ")) {
                player.sendMessage(ColorUtil.color(s.substring(10)));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
            }
        }
    }
    public void spawnWinHolo(String name, Location loc, RewardModel reward) {
        HologramBridge.createWithItem(name, loc, Arrays.asList(reward.getDisplayName().replace('&', '§')), reward.getMaterial());
        this.holoNames.add(name);
    }
    public enum State {
        PLACING,
        WAITING,
        REVEALING,
        DONE;
    }
}
