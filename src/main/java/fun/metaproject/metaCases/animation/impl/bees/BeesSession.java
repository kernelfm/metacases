package fun.metaproject.metaCases.animation.impl.bees;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.animation.api.AnimationConfig;
import fun.metaproject.metaCases.animation.api.AnimationSession;
import fun.metaproject.metaCases.case_system.CaseModel;
import fun.metaproject.metaCases.case_system.RewardModel;
import fun.metaproject.metaCases.hologram.HologramBridge;
class BeesSession extends AnimationSession implements Listener {
    private final List<Bee> bees = new ArrayList();
    private BukkitTask flightTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int ticks = 0;
    public BeesSession(MetaCasesFree plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.BEES), State.PLACING);
    }
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        for(int i = 0; i < 8; ++i) {
            Location spawnLoc = this.caseLoc.clone().add(0.5D, 0.5D, 0.5D);
            Bee bee = (Bee)spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BEE);
            bee.setAI(false);
            bee.setGravity(false);
            bee.setRemoveWhenFarAway(false);
            this.bees.add(bee);
            this.plugin.getAnimationManager().registerEntity(bee, this);
        }
        this.flightTask = (new BukkitRunnable() {
            public void run() {
                if (BeesSession.this.state != BeesSession.State.DONE) {
                    ++BeesSession.this.ticks;
                    double currentRadius;
                    double currentHeight;
                    if (BeesSession.this.state == BeesSession.State.PLACING) {
                        if (BeesSession.this.ticks <= 20) {
                            currentRadius = (double)BeesSession.this.ticks / 20.0D * 2.5D;
                            currentHeight = 0.5D + (double)BeesSession.this.ticks / 20.0D * 1.7000000000000002D;
                        } else {
                            BeesSession.this.state = BeesSession.State.WAITING;
                            BeesSession.this.onAllPlaced();
                            currentRadius = 2.5D;
                            currentHeight = 2.2D;
                        }
                    } else {
                        if (BeesSession.this.state != BeesSession.State.WAITING) {
                            return;
                        }
                        currentRadius = 2.5D;
                        currentHeight = 2.2D;
                    }
                    double baseAngle = (double)BeesSession.this.ticks * 0.05D;
                    for(int i = 0; i < 8; ++i) {
                        double angle = baseAngle + (double)i * 0.7853981633974483D;
                        double x = BeesSession.this.caseLoc.getX() + 0.5D + currentRadius * Math.cos(angle);
                        double z = BeesSession.this.caseLoc.getZ() + 0.5D + currentRadius * Math.sin(angle);
                        double y = BeesSession.this.caseLoc.getY() + currentHeight + BeesSession.this.ac.yOffset;
                        Location loc = new Location(BeesSession.this.caseLoc.getWorld(), x, y, z);
                        double nextAngle = angle + 0.05D;
                        double nx = BeesSession.this.caseLoc.getX() + 0.5D + currentRadius * Math.cos(nextAngle);
                        double nz = BeesSession.this.caseLoc.getZ() + 0.5D + currentRadius * Math.sin(nextAngle);
                        double dx = nx - x;
                        double dz = nz - z;
                        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                        loc.setYaw(yaw);
                        loc.setPitch(0.0F);
                        Bee bee = (Bee)BeesSession.this.bees.get(i);
                        if (bee.isValid()) {
                            bee.teleport(loc);
                            if (BeesSession.this.ticks % 5 == 0 && BeesSession.this.state == BeesSession.State.PLACING) {
                                loc.getWorld().spawnParticle(BeesSession.this.ac.placementParticle, loc, BeesSession.this.ac.placementParticleCount, 0.1D, 0.1D, 0.1D, (double)BeesSession.this.ac.placementParticleSpeed);
                                loc.getWorld().playSound(loc, BeesSession.this.ac.placementSound, 0.3F, 1.2F);
                            }
                        }
                    }
                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);
    }
    public void stop() {
        if (this.state != BeesSession.State.DONE) {
            this.state = BeesSession.State.DONE;
            HandlerList.unregisterAll(this);
            if (this.flightTask != null) {
                this.flightTask.cancel();
            }
            if (this.timeoutTask != null) {
                this.timeoutTask.cancel();
            }
            if (this.cleanupTask != null) {
                this.cleanupTask.cancel();
            }
            if (this.revealOtherTask != null) {
                this.revealOtherTask.cancel();
            }
            Iterator var1 = this.holoNames.iterator();
            while(var1.hasNext()) {
                String name = (String)var1.next();
                HologramBridge.remove(name);
            }
            this.holoNames.clear();
            var1 = this.bees.iterator();
            while(var1.hasNext()) {
                Bee bee = (Bee)var1.next();
                this.plugin.getAnimationManager().unregisterEntity(bee);
                if (bee.isValid()) {
                    bee.remove();
                }
            }
            this.bees.clear();
            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }
    private void onAllPlaced() {
        String title = this.plugin.getMsg("chest-title", new String[0]);
        String sub = this.plugin.getMsg("chest-subtitle", new String[0]);
        player.sendTitle(title, sub, 10, 70, 20);
        player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        int timeout = this.ac.timeout;
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (BeesSession.this.state == BeesSession.State.WAITING) {
                    Bee rand = null;
                    Iterator var2 = BeesSession.this.bees.iterator();
                    while(var2.hasNext()) {
                        Bee bee = (Bee)var2.next();
                        if (bee.isValid()) {
                            rand = bee;
                            break;
                        }
                    }
                    if (rand != null) {
                        BeesSession.this.reveal(rand);
                    } else {
                        BeesSession.this.stop();
                    }
                }
            }
        }).runTaskLater(this.plugin, (long)timeout * 20L);
    }
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (this.state == BeesSession.State.WAITING) {
            if (e.getDamager().getUniqueId().equals(this.player.getUniqueId())) {
                if (this.bees.contains(e.getEntity())) {
                    e.setCancelled(true);
                    this.reveal((Bee)e.getEntity());
                }
            }
        }
    }
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (this.bees.contains(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }
    public void handleInteract(Player clicker, Block b, Action action) {}
    private void reveal(final Bee clickedBee) {
        this.state = BeesSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }
        if (this.flightTask != null) {
            this.flightTask.cancel();
        }
        Iterator var2 = this.bees.iterator();
        while(var2.hasNext()) {
            Bee bee = (Bee)var2.next();
            if (bee.isValid()) {
                Location loc = bee.getLocation();
                double dx = this.caseLoc.getX() + 0.5D - loc.getX();
                double dz = this.caseLoc.getZ() + 0.5D - loc.getZ();
                float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                loc.setYaw(yaw);
                loc.setPitch(0.0F);
                bee.teleport(loc);
            }
        }
        Location clickedLoc = clickedBee.getLocation();
        clickedBee.remove();
        clickedLoc.getWorld().playSound(clickedLoc, this.ac.clickSound, this.ac.clickSoundVolume, this.ac.clickSoundPitch);
        clickedLoc.getWorld().spawnParticle(this.ac.clickParticle, clickedLoc, this.ac.clickParticleCount, 0.2D, 0.2D, 0.2D, this.ac.clickParticleSpeed);
        RewardModel reward = this.caseModel.getRandomReward();
        if (reward != null) {
            this.plugin.getCaseManager().addHistoryEntry(this.caseModel.getDisplayName(), this.player.getName(), reward.getDisplayName(), reward.getMaterial());
            dispatchReward(reward);
            if (HologramBridge.isAvailable()) {
                String var10000 = String.valueOf(this.player.getUniqueId());
                String hn = "metacase_" + var10000 + "_" + System.currentTimeMillis();
                this.spawnWinHolo(hn, clickedLoc.clone().add(0.0D, 0.5D, 0.0D), reward);
            }
        }
        this.revealOtherTask = (new BukkitRunnable() {
            public void run() {
                if (BeesSession.this.state != BeesSession.State.DONE) {
                    Iterator var1 = BeesSession.this.bees.iterator();
                    while(var1.hasNext()) {
                        Bee bee = (Bee)var1.next();
                        if (!bee.equals(clickedBee) && bee.isValid()) {
                            Location loc = bee.getLocation();
                            bee.remove();
                            loc.getWorld().playSound(loc, BeesSession.this.ac.revealSound, BeesSession.this.ac.revealSoundVolume, BeesSession.this.ac.revealSoundPitch);
                            loc.getWorld().spawnParticle(BeesSession.this.ac.revealParticle, loc, BeesSession.this.ac.revealParticleCount, 0.2D, 0.2D, 0.2D, (double)BeesSession.this.ac.revealParticleSpeed);
                            RewardModel sim = BeesSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(BeesSession.this.player.getUniqueId());
                                String hn = "metacase_sim_" + var10000 + "_" + loc.getBlockX() + "_" + loc.getBlockZ() + "_" + System.currentTimeMillis();
                                BeesSession.this.spawnWinHolo(hn, loc.clone().add(0.0D, 0.5D, 0.0D), sim);
                            }
                        }
                    }
                }
            }
        }).runTaskLater(this.plugin, 30L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                BeesSession.this.stop();
            }
        }).runTaskLater(this.plugin, 100L);
    }
}
