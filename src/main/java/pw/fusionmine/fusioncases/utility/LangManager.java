package pw.fusionmine.fusioncases.utility;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import pw.fusionmine.fusioncases.FusionCases;

public class LangManager {

    private final FusionCases plugin;
    private YamlConfiguration lang;
    private String locale;

    public LangManager(FusionCases plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.locale = this.plugin.getConfig().getString("locale", "en").toLowerCase();
        String fileName = "lang_" + this.locale + ".yml";

        if (this.plugin.getResource(fileName) == null) {
            this.plugin.getLogger().warning("Language file '" + fileName + "' not found, falling back to 'en'.");
            this.locale = "en";
            fileName = "lang_en.yml";
        }

        File file = new File(this.plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            this.plugin.saveResource(fileName, false);
        }

        this.lang = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = this.plugin.getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.lang.setDefaults(defaults);
        }
    }

    public String get(String key) {
        String val = this.lang.getString("messages." + key);
        if (val == null) val = this.lang.getString("messages." + key, "?" + key + "?");
        return (val != null) ? val : ("?" + key + "?");
    }

}