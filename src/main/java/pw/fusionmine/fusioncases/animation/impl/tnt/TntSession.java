package pw.fusionmine.fusioncases.animation.impl.tnt;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationConfig;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.case_system.CaseModel;
import pw.fusionmine.fusioncases.case_system.RewardModel;
import pw.fusionmine.fusioncases.hologram.HologramBridge;

class TntSession extends AnimationSession {

    private final Map<Location, BlockState> savedBlocks = new HashMap();
    private final List<Location> tntLocs = new ArrayList();
    private BukkitTask placementTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int placeIdx = 0;
    private int landedCount = 0;
    private final List<BlockDisplay> activeDisplays = new ArrayList();
    private TntSession.State state;
    private static final int[][] OFFSETS = new int[][]{{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};

    public TntSession(FusionCases plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.TNT), State.PLACING);
    }

    public void start() {
        int speed = this.ac.placementSpeed;
        this.placementTask = (new BukkitRunnable() {
            public void run() {
                if (TntSession.this.placeIdx < TntSession.OFFSETS.length) {
                    TntSession.this.launchTnt(TntSession.this.placeIdx++);
                } else {
                    this.cancel();
                }

            }
        }).runTaskTimer(this.plugin, 0L, (long)speed);
    }

    public void stop() {
        if (this.state != TntSession.State.DONE) {
            this.state = TntSession.State.DONE;
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

            Iterator var1 = this.activeDisplays.iterator();

            while(var1.hasNext()) {
                BlockDisplay bd = (BlockDisplay)var1.next();
                if (bd.isValid()) {
                    bd.remove();
                }
            }

            this.activeDisplays.clear();
            var1 = this.holoNames.iterator();

            while(var1.hasNext()) {
                String name = (String)var1.next();
                HologramBridge.remove(name);
            }

            this.holoNames.clear();
            var1 = this.savedBlocks.entrySet().iterator();

            while(var1.hasNext()) {
                Entry<Location, BlockState> e = (Entry)var1.next();
                this.plugin.getAnimationManager().unregisterBlock((Location)e.getKey());
                ((BlockState)e.getValue()).update(true, false);
            }

            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }

    private void launchTnt(int idx) {
        final Location start = this.caseLoc.clone().add(0.0D, 1.0D + this.ac.yOffset, 0.0D);
        final Location end = this.caseLoc.clone().add((double)OFFSETS[idx][0], this.ac.yOffset, (double)OFFSETS[idx][1]);
        final Location targetBlockLoc = this.caseLoc.clone().add((double)OFFSETS[idx][0], this.ac.yOffset, (double)OFFSETS[idx][1]);
        final BlockDisplay bd = (BlockDisplay)start.getWorld().spawn(start, BlockDisplay.class);
        bd.setBlock(Bukkit.createBlockData(Material.TNT));
        this.activeDisplays.add(bd);
        final int flightTicks = 15;
        (new BukkitRunnable() {
            int t = 0;

            public void run() {
                if (TntSession.this.state == TntSession.State.DONE) {
                    if (bd.isValid()) {
                        bd.remove();
                    }

                    this.cancel();
                } else if (this.t >= flightTicks) {
                    if (bd.isValid()) {
                        bd.remove();
                    }

                    TntSession.this.activeDisplays.remove(bd);
                    TntSession.this.placeTnt(targetBlockLoc);
                    this.cancel();
                } else {
                    double alpha = (double)this.t / (double)flightTicks;
                    double x = start.getX() + alpha * (end.getX() - start.getX());
                    double z = start.getZ() + alpha * (end.getZ() - start.getZ());
                    double y = start.getY() + alpha * (end.getY() - start.getY()) + 8.0D * alpha * (1.0D - alpha);
                    Location loc = new Location(start.getWorld(), x, y, z);
                    bd.teleport(loc);
                    ++this.t;
                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);
    }

    private void placeTnt(Location loc) {
        if (this.state != TntSession.State.DONE) {
            if (!this.savedBlocks.containsKey(loc)) {
                this.savedBlocks.put(loc, loc.getBlock().getState());
            }

            Block b = loc.getBlock();
            b.setType(Material.TNT);
            this.plugin.getAnimationManager().registerBlock(loc, this);
            this.tntLocs.add(loc);
            loc.getWorld().playSound(loc, this.ac.placementSound, this.ac.placementSoundVolume, this.ac.placementSoundPitch);
            loc.getWorld().playSound(loc, this.ac.placementPlingSound, this.ac.placementPlingSoundVolume, this.ac.placementPlingSoundPitch);
            loc.getWorld().spawnParticle(this.ac.placementParticle, loc.clone().add(0.5D, 0.5D, 0.5D), this.ac.placementParticleCount, 0.3D, 0.3D, 0.3D, (double)this.ac.placementParticleSpeed);
            ++this.landedCount;
            if (this.landedCount == OFFSETS.length) {
                this.onAllPlaced();
            }

        }
    }

    private void onAllPlaced() {
        this.placementTask.cancel();
        this.state = TntSession.State.WAITING;
        String title = this.plugin.getMsg("chest-title", new String[0]);
        String sub = this.plugin.getMsg("chest-subtitle", new String[0]);
        this.player.sendTitle(title, sub, 10, 70, 20);
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        int timeout = this.ac.timeout;
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (TntSession.this.state == TntSession.State.WAITING) {
                    Location rand = (Location)TntSession.this.tntLocs.get(ThreadLocalRandom.current().nextInt(TntSession.this.tntLocs.size()));
                    TntSession.this.reveal(rand.getBlock());
                }

            }
        }).runTaskLater(this.plugin, (long)timeout * 20L);
    }

    public void handleInteract(Player clicker, Block b, Action action) {
        if (this.state == TntSession.State.WAITING) {
            if (clicker.getUniqueId().equals(this.player.getUniqueId())) {
                if (action == Action.LEFT_CLICK_BLOCK) {
                    if (this.tntLocs.contains(b.getLocation())) {
                        this.reveal(b);
                    }
                }
            }
        }
    }

    private void reveal(Block b) {
        this.state = TntSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }

        b.setType(Material.AIR);
        b.getWorld().playSound(b.getLocation(), this.ac.revealSound, this.ac.revealSoundVolume, this.ac.revealSoundPitch);
        b.getWorld().spawnParticle(this.ac.revealParticle, b.getLocation().clone().add(0.5D, 0.5D, 0.5D), this.ac.revealParticleCount, 0.3D, 0.3D, 0.3D, this.ac.revealParticleSpeed);
        RewardModel reward = this.caseModel.getRandomReward();
        if (reward != null) {
            this.plugin.getCaseManager().addHistoryEntry(this.caseModel.getName(), this.player.getName(), reward.getDisplayName(), reward.getMaterial());
            dispatchReward(reward);
            if (HologramBridge.isAvailable()) {
                String var10000 = String.valueOf(this.player.getUniqueId());
                String hn = "fusioncase_" + var10000 + "_" + System.currentTimeMillis();
                this.spawnWinHolo(hn, b.getLocation().clone().add(0.5D, 1.5D, 0.5D), reward);
            }
        }

        final Location clickedLoc = b.getLocation();
        this.revealOtherTask = (new BukkitRunnable() {
            public void run() {
                if (TntSession.this.state != TntSession.State.DONE) {
                    Iterator var1 = TntSession.this.tntLocs.iterator();

                    while(var1.hasNext()) {
                        Location loc = (Location)var1.next();
                        if (!loc.equals(clickedLoc)) {
                            Block ob = loc.getBlock();
                            ob.setType(Material.AIR);
                            loc.getWorld().playSound(loc, TntSession.this.ac.revealSound, TntSession.this.ac.revealSoundVolume, TntSession.this.ac.revealSoundPitch);
                            loc.getWorld().spawnParticle(TntSession.this.ac.revealParticle, loc.clone().add(0.5D, 0.5D, 0.5D), TntSession.this.ac.revealParticleCount, 0.2D, 0.2D, 0.2D, (double)TntSession.this.ac.revealParticleSpeed);
                            RewardModel sim = TntSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(TntSession.this.player.getUniqueId());
                                String hn = "fusioncase_sim_" + var10000 + "_" + loc.getBlockX() + "_" + loc.getBlockZ() + "_" + System.currentTimeMillis();
                                TntSession.this.spawnWinHolo(hn, loc.clone().add(0.5D, 1.5D, 0.5D), sim);
                            }
                        }
                    }

                }
            }
        }).runTaskLater(this.plugin, 30L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                TntSession.this.stop();
            }
        }).runTaskLater(this.plugin, 100L);
    }

}
