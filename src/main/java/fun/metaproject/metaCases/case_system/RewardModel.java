package fun.metaproject.metaCases.case_system;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;
@AllArgsConstructor
@Getter
public class RewardModel {
    private final String id;
    private final String displayName;
    private final String material;
    private final double chance;
    private final List<String> commands;
}
