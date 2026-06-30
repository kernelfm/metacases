package pw.fusionmine.fusioncases.animation.api;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class AnimationConfig {

    public final int placementSpeed;
    public final int timeout;
    public final int floorAnimationSpeed;
    public final Sound placementSound;
    public final float placementSoundPitch;
    public final float placementSoundVolume;
    public final Sound placementPlingSound;
    public final float placementPlingSoundPitch;
    public final float placementPlingSoundVolume;
    public final Particle placementParticle;
    public final int placementParticleCount;
    public final float placementParticleSpeed;
    public final Sound clickSound;
    public final float clickSoundPitch;
    public final float clickSoundVolume;
    public final Particle clickParticle;
    public final int clickParticleCount;
    public final float clickParticleSpeed;
    public final Sound revealSound;
    public final float revealSoundPitch;
    public final float revealSoundVolume;
    public final Particle revealParticle;
    public final int revealParticleCount;
    public final float revealParticleSpeed;
    public final Sound shootSound;
    public final float shootSoundPitch;
    public final float shootSoundVolume;
    public final Particle bulletParticle;
    public final float bulletParticleSpeed;
    public final Sound explodeSound;
    public final float explodeSoundPitch;
    public final float explodeSoundVolume;
    public final Particle explodeParticle;
    public final int explodeParticleCount;
    public final float explodeParticleSpeed;
    public final Sound titleSound;
    public final float titleSoundPitch;
    public final float titleSoundVolume;
    public final Particle ambientParticle1;
    public final int ambientParticle1Count;
    public final float ambientParticle1Speed;
    public final Particle ambientParticle2;
    public final int ambientParticle2Count;
    public final float ambientParticle2Speed;
    public final double yOffset;

    public static void initializeDefaultConfigs(JavaPlugin plugin) {
        File animDir = new File(plugin.getDataFolder(), "animations");
        if (!animDir.exists()) {
            animDir.mkdirs();
        }

        AnimationConfig.Type[] var2 = AnimationConfig.Type.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            AnimationConfig.Type type = var2[var4];
            String name = type.name().toLowerCase();
            File animFile = new File(animDir, name + ".yml");
            if (!animFile.exists()) {
                YamlConfiguration fileConfig = new YamlConfiguration();
                writeDefaults(fileConfig, type);

                try {
                    fileConfig.save(animFile);
                } catch (Exception var10) {
                    Logger var10000 = plugin.getLogger();
                    String var10001 = type.name();
                    var10000.severe("Could not save default animation config for " + var10001 + ": " + var10.getMessage());
                }
            }
        }

    }

    public AnimationConfig(JavaPlugin plugin, ConfigurationSection caseSection, AnimationConfig.Type type) {
        initializeDefaultConfigs(plugin);
        File animDir = new File(plugin.getDataFolder(), "animations");
        File animFile = new File(animDir, type.name().toLowerCase() + ".yml");
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(animFile);
        this.placementSpeed = Math.max(1, getInt(caseSection, fileConfig, "placement-speed", type == AnimationConfig.Type.CHESTS ? 5 : (type == AnimationConfig.Type.SHULKERS ? 6 : (type == AnimationConfig.Type.PIGLINS ? 5 : 5))));
        this.timeout = Math.max(1, getInt(caseSection, fileConfig, "timeout", 20));
        this.floorAnimationSpeed = Math.max(1, getInt(caseSection, fileConfig, "floor-animation-speed", 4));
        this.placementSound = sound(plugin, caseSection, fileConfig, "placement-sound", type == AnimationConfig.Type.CHESTS ? Sound.BLOCK_WOOD_PLACE : (type == AnimationConfig.Type.SHULKERS ? Sound.BLOCK_SHULKER_BOX_OPEN : (type == AnimationConfig.Type.PIGLINS ? Sound.ENTITY_PIGLIN_AMBIENT : (type == AnimationConfig.Type.SOULWELL ? Sound.ENTITY_WITHER_AMBIENT : Sound.BLOCK_WOOD_PLACE))));
        this.placementSoundPitch = (float)getDouble(caseSection, fileConfig, "placement-sound-pitch", type == AnimationConfig.Type.SOULWELL ? 1.5D : 1.0D);
        this.placementSoundVolume = (float)getDouble(caseSection, fileConfig, "placement-sound-volume", type == AnimationConfig.Type.SHULKERS ? 0.5D : (type == AnimationConfig.Type.SOULWELL ? 0.5D : 1.0D));
        this.placementPlingSound = sound(plugin, caseSection, fileConfig, "placement-pling-sound", Sound.BLOCK_NOTE_BLOCK_PLING);
        this.placementPlingSoundPitch = (float)getDouble(caseSection, fileConfig, "placement-pling-sound-pitch", 1.2D);
        this.placementPlingSoundVolume = (float)getDouble(caseSection, fileConfig, "placement-pling-sound-volume", 0.5D);
        this.placementParticle = particle(plugin, caseSection, fileConfig, "placement-particle", type == AnimationConfig.Type.CHESTS ? Particle.HAPPY_VILLAGER : (type == AnimationConfig.Type.SHULKERS ? Particle.CLOUD : (type == AnimationConfig.Type.PIGLINS ? Particle.HAPPY_VILLAGER : Particle.HAPPY_VILLAGER)));
        this.placementParticleCount = getInt(caseSection, fileConfig, "placement-particle-count", type == AnimationConfig.Type.CHESTS ? 10 : (type == AnimationConfig.Type.SHULKERS ? 5 : (type == AnimationConfig.Type.PIGLINS ? 10 : 10)));
        this.placementParticleSpeed = (float)getDouble(caseSection, fileConfig, "placement-particle-speed", type == AnimationConfig.Type.CHESTS ? 0.05D : (type == AnimationConfig.Type.SHULKERS ? 0.02D : (type == AnimationConfig.Type.PIGLINS ? 0.05D : 0.05D)));
        this.clickSound = sound(plugin, caseSection, fileConfig, "click-sound", type == AnimationConfig.Type.CHESTS ? Sound.BLOCK_CHEST_OPEN : (type == AnimationConfig.Type.SHULKERS ? Sound.ENTITY_SHULKER_SHOOT : (type == AnimationConfig.Type.PIGLINS ? Sound.ENTITY_PIGLIN_AMBIENT : (type == AnimationConfig.Type.SOULWELL ? Sound.ENTITY_WITHER_DEATH : Sound.BLOCK_LAVA_EXTINGUISH))));
        this.clickSoundPitch = (float)getDouble(caseSection, fileConfig, "click-sound-pitch", type == AnimationConfig.Type.SOULWELL ? 1.2D : 1.0D);
        this.clickSoundVolume = (float)getDouble(caseSection, fileConfig, "click-sound-volume", type == AnimationConfig.Type.SOULWELL ? 0.8D : 1.0D);
        this.clickParticle = particle(plugin, caseSection, fileConfig, "click-particle", type == AnimationConfig.Type.SOULWELL ? Particle.EXPLOSION : Particle.PORTAL);
        this.clickParticleCount = getInt(caseSection, fileConfig, "click-particle-count", type == AnimationConfig.Type.CHESTS ? 30 : (type == AnimationConfig.Type.PIGLINS ? 30 : (type == AnimationConfig.Type.SOULWELL ? 10 : 0)));        this.clickParticleSpeed = (float)getDouble(caseSection, fileConfig, "click-particle-speed", type == AnimationConfig.Type.SOULWELL ? 0.1D : 0.1D);
        this.revealSound = sound(plugin, caseSection, fileConfig, "reveal-sound", type == AnimationConfig.Type.CHESTS ? Sound.BLOCK_CHEST_OPEN : (type == AnimationConfig.Type.SHULKERS ? Sound.ENTITY_SHULKER_TELEPORT : (type == AnimationConfig.Type.SOULWELL ? Sound.ENTITY_WITHER_DEATH : Sound.BLOCK_CHEST_OPEN)));
        this.revealSoundPitch = (float)getDouble(caseSection, fileConfig, "reveal-sound-pitch", type == AnimationConfig.Type.SHULKERS ? 1.5D : (type == AnimationConfig.Type.SOULWELL ? 1.5D : 1.0D));
        this.revealSoundVolume = (float)getDouble(caseSection, fileConfig, "reveal-sound-volume", type == AnimationConfig.Type.CHESTS ? 0.5D : (type == AnimationConfig.Type.SHULKERS ? 0.5D : (type == AnimationConfig.Type.SOULWELL ? 0.4D : 1.0D)));
        this.revealParticle = particle(plugin, caseSection, fileConfig, "reveal-particle", Particle.PORTAL);
        this.revealParticleCount = getInt(caseSection, fileConfig, "reveal-particle-count", 10);
        this.revealParticleSpeed = (float)getDouble(caseSection, fileConfig, "reveal-particle-speed", type == AnimationConfig.Type.SHULKERS ? 0.1D : 0.05D);
        this.shootSound = sound(plugin, caseSection, fileConfig, "shoot-sound", Sound.ENTITY_SHULKER_SHOOT);
        this.shootSoundPitch = (float)getDouble(caseSection, fileConfig, "shoot-sound-pitch", 1.0D);
        this.shootSoundVolume = (float)getDouble(caseSection, fileConfig, "shoot-sound-volume", 1.0D);
        this.bulletParticle = particle(plugin, caseSection, fileConfig, "bullet-particle", Particle.END_ROD);
        this.bulletParticleSpeed = (float)getDouble(caseSection, fileConfig, "bullet-particle-speed", 0.01D);
        this.explodeSound = sound(plugin, caseSection, fileConfig, "explode-sound", Sound.ENTITY_FIREWORK_ROCKET_BLAST);
        this.explodeSoundPitch = (float)getDouble(caseSection, fileConfig, "explode-sound-pitch", 1.0D);
        this.explodeSoundVolume = (float)getDouble(caseSection, fileConfig, "explode-sound-volume", 1.2D);
        this.explodeParticle = particle(plugin, caseSection, fileConfig, "explode-particle", Particle.FIREWORK);
        this.explodeParticleCount = getInt(caseSection, fileConfig, "explode-particle-count", 40);
        this.explodeParticleSpeed = (float)getDouble(caseSection, fileConfig, "explode-particle-speed", 0.15D);
        this.titleSound = sound(plugin, caseSection, fileConfig, "title-sound", Sound.ENTITY_PLAYER_LEVELUP);
        this.titleSoundPitch = (float)getDouble(caseSection, fileConfig, "title-sound-pitch", type == AnimationConfig.Type.PIGLINS ? 1.5D : 1.2D);
        this.titleSoundVolume = (float)getDouble(caseSection, fileConfig, "title-sound-volume", 1.0D);
        this.ambientParticle1 = particle(plugin, caseSection, fileConfig, "ambient-particle-1", Particle.SOUL_FIRE_FLAME);
        this.ambientParticle1Count = getInt(caseSection, fileConfig, "ambient-particle-1-count", 1);
        this.ambientParticle1Speed = (float)getDouble(caseSection, fileConfig, "ambient-particle-1-speed", 0.01D);
        this.ambientParticle2 = particle(plugin, caseSection, fileConfig, "ambient-particle-2", Particle.SMOKE);
        this.ambientParticle2Count = getInt(caseSection, fileConfig, "ambient-particle-2-count", 1);
        this.ambientParticle2Speed = (float)getDouble(caseSection, fileConfig, "ambient-particle-2-speed", 0.01D);
        this.yOffset = getDouble(caseSection, fileConfig, "y-offset", 0.0D);
    }

    private static void writeDefaults(YamlConfiguration cfg, AnimationConfig.Type type) {
        cfg.set("y-offset", 0.0D);
        if (type == AnimationConfig.Type.CHESTS) {
            cfg.set("placement-speed", 5);
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "BLOCK_WOOD_PLACE");
            cfg.set("placement-sound-pitch", 1.0D);
            cfg.set("placement-sound-volume", 1.0D);
            cfg.set("placement-pling-sound", "BLOCK_NOTE_BLOCK_PLING");
            cfg.set("placement-pling-sound-pitch", 1.2D);
            cfg.set("placement-pling-sound-volume", 0.5D);
            cfg.set("placement-particle", "HAPPY_VILLAGER");
            cfg.set("placement-particle-count", 10);
            cfg.set("placement-particle-speed", 0.05D);
            cfg.set("click-sound", "BLOCK_CHEST_OPEN");
            cfg.set("click-sound-pitch", 1.0D);
            cfg.set("click-sound-volume", 1.0D);
            cfg.set("click-particle", "PORTAL");
            cfg.set("click-particle-count", 30);
            cfg.set("click-particle-speed", 0.1D);
            cfg.set("reveal-sound", "BLOCK_CHEST_OPEN");
            cfg.set("reveal-sound-pitch", 1.0D);
            cfg.set("reveal-sound-volume", 0.5D);
            cfg.set("reveal-particle", "PORTAL");
            cfg.set("reveal-particle-count", 10);
            cfg.set("reveal-particle-speed", 0.05D);
        } else if (type == AnimationConfig.Type.SHULKERS) {
            cfg.set("placement-speed", 6);
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "BLOCK_SHULKER_BOX_OPEN");
            cfg.set("placement-sound-pitch", 0.8D);
            cfg.set("placement-sound-volume", 0.5D);
            cfg.set("placement-particle", "CLOUD");
            cfg.set("placement-particle-count", 5);
            cfg.set("placement-particle-speed", 0.02D);
            cfg.set("shoot-sound", "ENTITY_SHULKER_SHOOT");
            cfg.set("shoot-sound-pitch", 1.0D);
            cfg.set("shoot-sound-volume", 1.0D);
            cfg.set("bullet-particle", "END_ROD");
            cfg.set("bullet-particle-speed", 0.01D);
            cfg.set("explode-sound", "ENTITY_FIREWORK_ROCKET_BLAST");
            cfg.set("explode-sound-pitch", 1.0D);
            cfg.set("explode-sound-volume", 1.2D);
            cfg.set("explode-particle", "FIREWORK");
            cfg.set("explode-particle-count", 40);
            cfg.set("explode-particle-speed", 0.15D);
            cfg.set("reveal-sound", "ENTITY_SHULKER_TELEPORT");
            cfg.set("reveal-sound-pitch", 1.5D);
            cfg.set("reveal-sound-volume", 0.5D);
            cfg.set("reveal-particle", "PORTAL");
            cfg.set("reveal-particle-count", 10);
            cfg.set("reveal-particle-speed", 0.1D);
        } else if (type == AnimationConfig.Type.PIGLINS) {
            cfg.set("placement-speed", 5);
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "ENTITY_PIGLIN_AMBIENT");
            cfg.set("placement-sound-pitch", 1.0D);
            cfg.set("placement-sound-volume", 1.0D);
            cfg.set("placement-particle", "HAPPY_VILLAGER");
            cfg.set("placement-particle-count", 10);
            cfg.set("placement-particle-speed", 0.05D);
            cfg.set("title-sound", "ENTITY_PLAYER_LEVELUP");
            cfg.set("title-sound-pitch", 1.5D);
            cfg.set("title-sound-volume", 1.0D);
            cfg.set("click-particle", "PORTAL");
            cfg.set("click-particle-count", 30);
            cfg.set("click-particle-speed", 0.1D);
        } else if (type == AnimationConfig.Type.SOULWELL) {
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "ENTITY_WITHER_AMBIENT");
            cfg.set("placement-sound-pitch", 1.5D);
            cfg.set("placement-sound-volume", 0.5D);
            cfg.set("title-sound", "ENTITY_PLAYER_LEVELUP");
            cfg.set("title-sound-pitch", 1.2D);
            cfg.set("title-sound-volume", 1.0D);
            cfg.set("ambient-particle-1", "SOUL_FIRE_FLAME");
            cfg.set("ambient-particle-1-count", 1);
            cfg.set("ambient-particle-1-speed", 0.01D);
            cfg.set("ambient-particle-2", "SMOKE");
            cfg.set("ambient-particle-2-count", 1);
            cfg.set("ambient-particle-2-speed", 0.01D);
            cfg.set("click-sound", "ENTITY_WITHER_DEATH");
            cfg.set("click-sound-pitch", 1.2D);
            cfg.set("click-sound-volume", 0.8D);
            cfg.set("click-particle", "EXPLOSION");
            cfg.set("click-particle-count", 10);
            cfg.set("click-particle-speed", 0.1D);
            cfg.set("reveal-sound", "ENTITY_WITHER_DEATH");
            cfg.set("reveal-sound-pitch", 1.5D);
            cfg.set("reveal-sound-volume", 0.4D);
        } else if (type == AnimationConfig.Type.TNT) {
            cfg.set("placement-speed", 5);
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "BLOCK_WOOD_PLACE");
            cfg.set("placement-sound-pitch", 1.0D);
            cfg.set("placement-sound-volume", 1.0D);
            cfg.set("placement-pling-sound", "BLOCK_NOTE_BLOCK_PLING");
            cfg.set("placement-pling-sound-pitch", 1.2D);
            cfg.set("placement-pling-sound-volume", 0.5D);
            cfg.set("placement-particle", "HAPPY_VILLAGER");
            cfg.set("placement-particle-count", 10);
            cfg.set("placement-particle-speed", 0.05D);
            cfg.set("click-sound", "ENTITY_TNT_PRIMED");
            cfg.set("click-sound-pitch", 1.0D);
            cfg.set("click-sound-volume", 1.0D);
            cfg.set("click-particle", "SMOKE");
            cfg.set("click-particle-count", 15);
            cfg.set("click-particle-speed", 0.05D);
            cfg.set("reveal-sound", "ENTITY_GENERIC_EXPLODE");
            cfg.set("reveal-sound-pitch", 1.0D);
            cfg.set("reveal-sound-volume", 1.0D);
            cfg.set("reveal-particle", "EXPLOSION");
            cfg.set("reveal-particle-count", 20);
            cfg.set("reveal-particle-speed", 0.1D);
        } else if (type == AnimationConfig.Type.BEES) {
            cfg.set("placement-speed", 5);
            cfg.set("timeout", 20);
            cfg.set("placement-sound", "ENTITY_BEE_LOOP");
            cfg.set("placement-sound-pitch", 1.0D);
            cfg.set("placement-sound-volume", 1.0D);
            cfg.set("placement-particle", "HAPPY_VILLAGER");
            cfg.set("placement-particle-count", 10);
            cfg.set("placement-particle-speed", 0.05D);
            cfg.set("click-sound", "ENTITY_BEE_STING");
            cfg.set("click-sound-pitch", 1.0D);
            cfg.set("click-sound-volume", 1.0D);
            cfg.set("click-particle", "PORTAL");
            cfg.set("click-particle-count", 20);
            cfg.set("click-particle-speed", 0.1D);
            cfg.set("reveal-sound", "ENTITY_BEE_DEATH");
            cfg.set("reveal-sound-pitch", 1.0D);
            cfg.set("reveal-sound-volume", 1.0D);
            cfg.set("reveal-particle", "PORTAL");
            cfg.set("reveal-particle-count", 15);
            cfg.set("reveal-particle-speed", 0.05D);
        }

    }

    private static int getInt(ConfigurationSection caseSec, ConfigurationSection fileSec, String key, int def) {
        if (caseSec != null && caseSec.isSet(key)) {
            return caseSec.getInt(key);
        } else {
            return fileSec != null && fileSec.isSet(key) ? fileSec.getInt(key) : def;
        }
    }

    private static double getDouble(ConfigurationSection caseSec, ConfigurationSection fileSec, String key, double def) {
        if (caseSec != null && caseSec.isSet(key)) {
            return caseSec.getDouble(key);
        } else {
            return fileSec != null && fileSec.isSet(key) ? fileSec.getDouble(key) : def;
        }
    }

    private static boolean getBool(ConfigurationSection caseSec, ConfigurationSection fileSec, String key, boolean def) {
        if (caseSec != null && caseSec.isSet(key)) {
            return caseSec.getBoolean(key);
        } else {
            return fileSec != null && fileSec.isSet(key) ? fileSec.getBoolean(key) : def;
        }
    }

    private static Sound sound(JavaPlugin plugin, ConfigurationSection caseSec, ConfigurationSection fileSec, String key, Sound def) {
        String raw = null;
        if (caseSec != null && caseSec.isSet(key)) {
            raw = caseSec.getString(key);
        }

        if ((raw == null || raw.trim().isEmpty()) && fileSec != null && fileSec.isSet(key)) {
            raw = fileSec.getString(key);
        }

        if (raw != null && !raw.trim().isEmpty()) {
            raw = raw.trim();

            try {
                NamespacedKey nk = raw.contains(":") ? NamespacedKey.fromString(raw.toLowerCase()) : NamespacedKey.minecraft(raw.toLowerCase().replace('_', '.').replaceAll("\\.+", "."));
                if (nk != null) {
                    Sound found = (Sound)Registry.SOUNDS.get(nk);
                    if (found != null) {
                        return found;
                    }
                }
            } catch (Exception var9) {
            }

            try {
                return Sound.valueOf(raw.toUpperCase().replace('-', '_').replace(' ', '_'));
            } catch (IllegalArgumentException var8) {
                plugin.getLogger().warning("Unknown sound '" + raw + "' for key '" + key + "' - using default.");
                return def;
            }
        } else {
            return def;
        }
    }

    private static Particle particle(JavaPlugin plugin, ConfigurationSection caseSec, ConfigurationSection fileSec, String key, Particle def) {
        String raw = null;
        if (caseSec != null && caseSec.isSet(key)) {
            raw = caseSec.getString(key);
        }

        if ((raw == null || raw.trim().isEmpty()) && fileSec != null && fileSec.isSet(key)) {
            raw = fileSec.getString(key);
        }

        if (raw != null && !raw.trim().isEmpty()) {
            raw = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');

            try {
                return Particle.valueOf(raw);
            } catch (IllegalArgumentException var7) {
                plugin.getLogger().warning("Unknown particle '" + raw + "' for key '" + key + "' - using default.");
                return def;
            }
        } else {
            return def;
        }
    }

    public static enum Type {
        CHESTS,
        SHULKERS,
        PIGLINS,
        SOULWELL,
        TNT,
        BEES;
    }

}
