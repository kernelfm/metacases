package fun.metaproject.metaCases.utility;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
@UtilityClass
public class ColorUtil {
    private final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public String color(String text) {
        if (text == null) return null;
        Matcher m = HEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString());
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
