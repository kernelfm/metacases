package pw.fusionmine.fusioncases.animation.impl.shulkers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
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

class ShulkersSession extends AnimationSession {

    private final List<Shulker> shulkers = new ArrayList();
    private BukkitTask placementTask;
    private BukkitTask animationTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int placeIdx = 0;
    private ShulkersSession.State state;
    private static final int[][] OFFSETS = new int[][]{{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};

    public ShulkersSession(FusionCases plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.SHULKERS), State.PLACING);
    }

    public void start() {
        int speed = this.ac.placementSpeed;
        this.placementTask = (new BukkitRunnable() {
            public void run() {
                if (ShulkersSession.this.placeIdx < 8) {
                    ShulkersSession.this.spawnShulker(ShulkersSession.this.placeIdx++);
                } else {
                    ShulkersSession.this.onAllPlaced();
                }

            }
        }).runTaskTimer(this.plugin, 0L, (long)speed);
    }

    public void stop() {
        if (this.state != ShulkersSession.State.DONE) {
            this.state = ShulkersSession.State.DONE;
            if (this.placementTask != null) {
                this.placementTask.cancel();
            }

            if (this.animationTask != null) {
                this.animationTask.cancel();
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
            var1 = this.shulkers.iterator();

            while(var1.hasNext()) {
                Shulker s = (Shulker)var1.next();
                this.plugin.getAnimationManager().unregisterEntity(s);
                s.remove();
            }

            this.shulkers.clear();
            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }

    private void spawnShulker(int idx) {
        Location spawnLoc = this.caseLoc.clone().add((double)OFFSETS[idx][0] + 0.5D, this.ac.yOffset, (double)OFFSETS[idx][1] + 0.5D);
        Location center = this.caseLoc.clone().add(0.5D, 0.0D, 0.5D);
        Vector dir = center.toVector().subtract(spawnLoc.toVector());
        spawnLoc.setDirection(dir);
        Shulker shulker = (Shulker)spawnLoc.getWorld().spawn(spawnLoc, Shulker.class);
        shulker.setAI(false);
        shulker.setCanPickupItems(false);
        DyeColor[] colors = DyeColor.values();
        shulker.setColor(colors[ThreadLocalRandom.current().nextInt(colors.length)]);
        this.plugin.getAnimationManager().registerEntity(shulker, this);
        this.shulkers.add(shulker);
        spawnLoc.getWorld().playSound(spawnLoc, this.ac.placementSound, this.ac.placementSoundVolume, this.ac.placementSoundPitch);
        spawnLoc.getWorld().spawnParticle(this.ac.placementParticle, spawnLoc.clone().add(0.0D, 0.5D, 0.0D), this.ac.placementParticleCount, 0.2D, 0.2D, 0.2D, (double)this.ac.placementParticleSpeed);
    }

    private void onAllPlaced() {
        this.placementTask.cancel();
        this.state = ShulkersSession.State.WAITING;
        String title = this.plugin.getMsg("shulker-title", new String[0]);
        String sub = this.plugin.getMsg("shulker-subtitle", new String[0]);
        this.player.sendTitle(title, sub, 10, 70, 20);
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        this.animationTask = (new BukkitRunnable() {
            private int tick = 0;

            public void run() {
                if (ShulkersSession.this.state != ShulkersSession.State.WAITING) {
                    this.cancel();
                } else {
                    ++this.tick;

                    for(int i = 0; i < ShulkersSession.this.shulkers.size(); ++i) {
                        double phase = (double)this.tick * 0.08D + (double)i * 2.0D * 3.141592653589793D / 8.0D;
                        float peek = (float)Math.max(0.0D, Math.sin(phase));
                        ((Shulker)ShulkersSession.this.shulkers.get(i)).setPeek(peek);
                    }

                }
            }
        }).runTaskTimer(this.plugin, 0L, 2L);
        int timeout = this.ac.timeout;
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (ShulkersSession.this.state == ShulkersSession.State.WAITING && !ShulkersSession.this.shulkers.isEmpty()) {
                    ShulkersSession.this.reveal((Shulker)ShulkersSession.this.shulkers.get(ThreadLocalRandom.current().nextInt(ShulkersSession.this.shulkers.size())));
                }

            }
        }).runTaskLater(this.plugin, (long)timeout * 20L);
    }

    public void handleInteract(Player clicker, Block b, Action action) {
        if (this.state == ShulkersSession.State.WAITING) {
            if (clicker.getUniqueId().equals(this.player.getUniqueId())) {
                Shulker closest = null;
                double minDist = Double.MAX_VALUE;
                Iterator var7 = this.shulkers.iterator();

                while(var7.hasNext()) {
                    Shulker s = (Shulker)var7.next();
                    double d = s.getLocation().distance(b.getLocation());
                    if (d < minDist) {
                        minDist = d;
                        closest = s;
                    }
                }

                if (closest != null && minDist <= 2.0D) {
                    this.reveal(closest);
                }
            }
        }
    }

    private void reveal(final Shulker clickedShulker) {
        this.state = ShulkersSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }

        if (this.animationTask != null) {
            this.animationTask.cancel();
        }

        Iterator var2 = this.shulkers.iterator();

        while(var2.hasNext()) {
            Shulker s = (Shulker)var2.next();
            s.setPeek(0.0F);
        }

        Location shulkerLoc = clickedShulker.getLocation();
        shulkerLoc.getWorld().playSound(shulkerLoc, this.ac.shootSound, this.ac.shootSoundVolume, this.ac.shootSoundPitch);
        final ShulkerBullet bullet = (ShulkerBullet)shulkerLoc.getWorld().spawn(shulkerLoc.clone().add(0.0D, 1.0D, 0.0D), ShulkerBullet.class);
        bullet.setGravity(false);
        bullet.setVelocity(new Vector(0.0D, 0.25D, 0.0D));
        (new BukkitRunnable() {
            private int count = 0;

            public void run() {
                if (this.count >= 12) {
                    this.cancel();
                    bullet.remove();
                    ShulkersSession.this.explodeBullet(bullet.getLocation(), clickedShulker);
                } else {
                    ++this.count;
                    bullet.getLocation().getWorld().spawnParticle(ShulkersSession.this.ac.bulletParticle, bullet.getLocation(), 2, 0.05D, 0.05D, 0.05D, (double)ShulkersSession.this.ac.bulletParticleSpeed);
                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);
    }

    private void explodeBullet(Location loc, final Shulker clickedShulker) {
        loc.getWorld().playSound(loc, this.ac.explodeSound, this.ac.explodeSoundVolume, this.ac.explodeSoundPitch);
        loc.getWorld().spawnParticle(this.ac.explodeParticle, loc, this.ac.explodeParticleCount, 0.5D, 0.5D, 0.5D, this.ac.explodeParticleSpeed);
        RewardModel reward = this.caseModel.getRandomReward();
        if (reward != null) {
            this.plugin.getCaseManager().addHistoryEntry(this.caseModel.getName(), this.player.getName(), reward.getDisplayName(), reward.getMaterial());
            dispatchReward(player, reward);
            if (HologramBridge.isAvailable()) {
                String var10000 = String.valueOf(this.player.getUniqueId());
                String hn = "fusioncase_" + var10000 + "_" + System.currentTimeMillis();
                this.spawnWinHolo(hn, loc.clone().add(0.0D, 0.5D, 0.0D), reward);
            }
        }

        this.revealOtherTask = (new BukkitRunnable() {
            public void run() {
                if (ShulkersSession.this.state != ShulkersSession.State.DONE) {
                    Iterator var1 = ShulkersSession.this.shulkers.iterator();

                    while(var1.hasNext()) {
                        Shulker s = (Shulker)var1.next();
                        if (s.getEntityId() != clickedShulker.getEntityId()) {
                            s.setPeek(1.0F);
                            Location sLoc = s.getLocation();
                            sLoc.getWorld().spawnParticle(ShulkersSession.this.ac.revealParticle, sLoc.clone().add(0.0D, 0.5D, 0.0D), ShulkersSession.this.ac.revealParticleCount, 0.2D, 0.2D, 0.2D, (double)ShulkersSession.this.ac.revealParticleSpeed);
                            sLoc.getWorld().playSound(sLoc, ShulkersSession.this.ac.revealSound, ShulkersSession.this.ac.revealSoundVolume, ShulkersSession.this.ac.revealSoundPitch);
                            RewardModel sim = ShulkersSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(ShulkersSession.this.player.getUniqueId());
                                String hn = "fusioncase_sim_" + var10000 + "_" + sLoc.getBlockX() + "_" + sLoc.getBlockZ() + "_" + System.currentTimeMillis();
                                ShulkersSession.this.spawnWinHolo(hn, sLoc.clone().add(0.0D, 1.5D, 0.0D), sim);
                            }
                        }
                    }

                }
            }
        }).runTaskLater(this.plugin, 10L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                ShulkersSession.this.stop();
            }
        }).runTaskLater(this.plugin, 90L);
    }

}
