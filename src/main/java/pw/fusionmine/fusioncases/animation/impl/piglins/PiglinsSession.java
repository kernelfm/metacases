package pw.fusionmine.fusioncases.animation.impl.piglins;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationConfig;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.case_system.CaseModel;
import pw.fusionmine.fusioncases.case_system.RewardModel;
import pw.fusionmine.fusioncases.hologram.HologramBridge;

class PiglinsSession extends AnimationSession {

    private final List<Piglin> piglins = new ArrayList();
    private BukkitTask placementTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int placeIdx = 0;
    private PiglinsSession.State state;
    private static final int[][] OFFSETS = new int[][]{{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};

    public PiglinsSession(FusionCases plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.PIGLINS), State.PLACING);
    }

    public void start() {
        this.placementTask = (new BukkitRunnable() {
            public void run() {
                if (PiglinsSession.this.placeIdx < PiglinsSession.OFFSETS.length) {
                    PiglinsSession.this.spawnPiglin(PiglinsSession.this.placeIdx++);
                } else {
                    PiglinsSession.this.onAllPlaced();
                }

            }
        }).runTaskTimer(this.plugin, 0L, (long)this.ac.placementSpeed);
    }

    private void spawnPiglin(int idx) {
        Location spawnLoc = this.caseLoc.clone().add((double)OFFSETS[idx][0] + 0.5D, this.ac.yOffset, (double)OFFSETS[idx][1] + 0.5D);
        Location center = this.caseLoc.clone().add(0.5D, 0.0D, 0.5D);
        Vector dir = center.toVector().subtract(spawnLoc.toVector());
        spawnLoc.setDirection(dir);
        Piglin pig = spawnLoc.getWorld().spawn(spawnLoc, Piglin.class);
        pig.setAI(false);
        pig.setAdult();
        pig.setImmuneToZombification(true);
        pig.setCanPickupItems(false);
        if (pig.getEquipment() != null) {
            pig.getEquipment().clear();
        }

        this.plugin.getAnimationManager().registerEntity(pig, this);
        this.piglins.add(pig);
        spawnLoc.getWorld().playSound(spawnLoc, this.ac.placementSound, this.ac.placementSoundVolume, this.ac.placementSoundPitch);
        spawnLoc.getWorld().spawnParticle(this.ac.placementParticle, spawnLoc.clone().add(0.0D, 0.5D, 0.0D), this.ac.placementParticleCount, 0.3D, 0.5D, 0.3D, (double)this.ac.placementParticleSpeed);
    }

    private void onAllPlaced() {
        this.placementTask.cancel();
        this.state = PiglinsSession.State.WAITING;
        String title = this.plugin.getMsg("piglin-title", new String[0]);
        String sub = this.plugin.getMsg("piglin-subtitle", new String[0]);
        this.player.sendTitle(title, sub, 10, 70, 20);
        this.player.playSound(this.player.getLocation(), this.ac.titleSound, this.ac.titleSoundVolume, this.ac.titleSoundPitch);
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (PiglinsSession.this.state == PiglinsSession.State.WAITING && !PiglinsSession.this.piglins.isEmpty()) {
                    PiglinsSession.this.reveal((Piglin)PiglinsSession.this.piglins.get(ThreadLocalRandom.current().nextInt(PiglinsSession.this.piglins.size())));
                }

            }
        }).runTaskLater(this.plugin, (long)this.ac.timeout * 20L);
    }

    public void handleInteract(Player clicker, Block b, Action action) {
        if (this.state == PiglinsSession.State.WAITING) {
            if (clicker.getUniqueId().equals(this.player.getUniqueId())) {
                if (action == Action.LEFT_CLICK_BLOCK) {
                    Piglin closest = null;
                    double minDist = Double.MAX_VALUE;
                    Iterator var7 = this.piglins.iterator();

                    while(var7.hasNext()) {
                        Piglin pig = (Piglin)var7.next();
                        double d = pig.getLocation().distance(b.getLocation());
                        if (d < minDist) {
                            minDist = d;
                            closest = pig;
                        }
                    }

                    if (closest != null && minDist <= 2.0D) {
                        this.reveal(closest);
                    }
                }
            }
        }
    }

    private void reveal(final Piglin pig) {
        this.state = PiglinsSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }

        pig.setHealth(0.0D);
        pig.getWorld().spawnParticle(this.ac.clickParticle, pig.getLocation().clone().add(0.0D, 0.5D, 0.0D), this.ac.clickParticleCount, 0.3D, 0.5D, 0.3D, (double)this.ac.clickParticleSpeed);
        RewardModel reward = this.caseModel.getRandomReward();
        if (reward != null) {
            this.plugin.getCaseManager().addHistoryEntry(this.caseModel.getName(), this.player.getName(), reward.getDisplayName(), reward.getMaterial());
            dispatchReward(player, reward);
            if (HologramBridge.isAvailable()) {
                String var10000 = String.valueOf(this.player.getUniqueId());
                String hn = "fusioncase_" + var10000 + "_" + System.currentTimeMillis();
                this.spawnWinHolo(hn, pig.getLocation().clone().add(0.0D, 0.5D, 0.0D), reward);
            }
        }

        this.revealOtherTask = (new BukkitRunnable() {
            public void run() {
                if (PiglinsSession.this.state != PiglinsSession.State.DONE) {
                    Iterator var1 = PiglinsSession.this.piglins.iterator();

                    while(var1.hasNext()) {
                        Piglin other = (Piglin)var1.next();
                        if (!other.getUniqueId().equals(pig.getUniqueId())) {
                            other.setHealth(0.0D);
                            RewardModel sim = PiglinsSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(PiglinsSession.this.player.getUniqueId());
                                String hn = "fusioncase_sim_" + var10000 + "_" + other.getLocation().getBlockX() + "_" + other.getLocation().getBlockZ() + "_" + System.currentTimeMillis();
                                PiglinsSession.this.spawnWinHolo(hn, other.getLocation().clone().add(0.0D, 0.5D, 0.0D), sim);
                            }
                        }
                    }

                }
            }
        }).runTaskLater(this.plugin, 35L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                PiglinsSession.this.stop();
            }
        }).runTaskLater(this.plugin, 100L);
    }

    public void stop() {
        if (this.state != PiglinsSession.State.DONE) {
            this.state = PiglinsSession.State.DONE;
            if (this.placementTask != null) {
                this.placementTask.cancel();
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
            var1 = this.piglins.iterator();

            while(var1.hasNext()) {
                Piglin pig = (Piglin)var1.next();
                this.plugin.getAnimationManager().unregisterEntity(pig);
                pig.remove();
            }

            this.piglins.clear();
            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }

}
