package pw.fusionmine.fusioncases.utility;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

@UtilityClass
public class ItemUtil {

    public ItemStack getItem(String material, Material defaultMaterial) {
        ItemStack item;
        if (material.startsWith("BASE64-")) {
            item = ItemUtil.getSkull(material);
        } else {
            Material mat = Material.matchMaterial(material);
            if (mat == null) mat = defaultMaterial;
            item = new ItemStack(mat);
        }
        return item;
    }

    public ItemStack getSkull(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) return skull;
        if (base64.startsWith("BASE64-")) base64 = base64.substring(7);

        try {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
            profile.setProperty(new ProfileProperty("textures", base64));

            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(profile);
                skull.setItemMeta(meta);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skull;
    }

}
