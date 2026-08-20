package fun.metaproject.metaCases.case_system;
import lombok.Getter;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
@Getter
public class HistoryEntry {
    private final String caseName;
    private final String username;
    private final String rewardDisplayName;
    private final String rewardMaterial;
    private final String time;
    public HistoryEntry(String caseName, String username, String rewardDisplayName, String rewardMaterial, Timestamp timestamp) {
        this.caseName = caseName;
        this.username = username;
        this.rewardDisplayName = rewardDisplayName;
        this.rewardMaterial = rewardMaterial;
        this.time = formatTimestamp(timestamp);
    }
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "Никогда";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return timestamp.toLocalDateTime().format(formatter);
    }
}
