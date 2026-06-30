package pw.fusionmine.fusioncases.animation.impl.chests;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lidded;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.ConfigurationSection;
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

class ChestsSession extends AnimationSession {

    private final Map<Location, BlockState> savedBlocks = new HashMap();
    private final List<Location> chestLocs = new ArrayList();
    private BukkitTask placementTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int placeIdx = 0;
    private ChestsSession.State state;
    private static final int[][] OFFSETS = new int[][]{{0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}};
    private static final BlockFace[] FACINGS;

    public ChestsSession(FusionCases plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.CHESTS), State.PLACING);
    }

    public void start() {
        int speed = this.ac.placementSpeed;
        this.placementTask = (new BukkitRunnable() {
            public void run() {
                if (ChestsSession.this.placeIdx < ChestsSession.OFFSETS.length) {
                    ChestsSession.this.placeChest(ChestsSession.this.placeIdx++);
                } else {
                    ChestsSession.this.onAllPlaced();
                }

            }
        }).runTaskTimer(this.plugin, 0L, (long)speed);
    }

    public void stop() {
        if (this.state != ChestsSession.State.DONE) {
            this.state = ChestsSession.State.DONE;
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
            var1 = this.savedBlocks.entrySet().iterator();

            while(var1.hasNext()) {
                Entry<Location, BlockState> e = (Entry)var1.next();
                this.plugin.getAnimationManager().unregisterBlock((Location)e.getKey());
                ((BlockState)e.getValue()).update(true, false);
            }

            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }

    private void placeChest(int idx) {
        Location loc = this.caseLoc.clone().add((double)OFFSETS[idx][0], this.ac.yOffset, (double)OFFSETS[idx][1]);
        if (!this.savedBlocks.containsKey(loc)) {
            this.savedBlocks.put(loc, loc.getBlock().getState());
        }

        Block b = loc.getBlock();
        b.setType(Material.CHEST);
        Chest data = (Chest)b.getBlockData();
        data.setFacing(FACINGS[idx]);
        b.setBlockData(data, false);
        this.plugin.getAnimationManager().registerBlock(loc, this);
        this.chestLocs.add(loc);
        loc.getWorld().playSound(loc, this.ac.placementSound, this.ac.placementSoundVolume, this.ac.placementSoundPitch);
        loc.getWorld().playSound(loc, this.ac.placementPlingSound, this.ac.placementPlingSoundVolume, this.ac.placementPlingSoundPitch);
        loc.getWorld().spawnParticle(this.ac.placementParticle, loc.clone().add(0.5D, 0.5D, 0.5D), this.ac.placementParticleCount, 0.3D, 0.3D, 0.3D, (double)this.ac.placementParticleSpeed);
    }

    private void onAllPlaced() {
        this.placementTask.cancel();
        this.state = ChestsSession.State.WAITING;
        String title = this.plugin.getMsg("chest-title", new String[0]);
        String sub = this.plugin.getMsg("chest-subtitle", new String[0]);
        this.player.sendTitle(title, sub, 10, 70, 20);
        this.player.playSound(this.player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        int timeout = this.ac.timeout;
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (ChestsSession.this.state == ChestsSession.State.WAITING) {
                    Location rand = ChestsSession.this.chestLocs.get(ThreadLocalRandom.current().nextInt(ChestsSession.this.chestLocs.size()));
                    ChestsSession.this.reveal(rand.getBlock());
                }

            }
        }).runTaskLater(this.plugin, (long)timeout * 20L);
    }

    public void handleInteract(Player clicker, Block b, Action action) {
        if (this.state == ChestsSession.State.WAITING) {
            if (clicker.getUniqueId().equals(this.player.getUniqueId())) {
                if (action == Action.LEFT_CLICK_BLOCK) {
                    if (this.chestLocs.contains(b.getLocation())) {
                        this.reveal(b);
                    }
                }
            }
        }
    }

    private void reveal(Block b) {
        this.state = ChestsSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }

        BlockState bs = b.getState();
        if (bs instanceof Lidded) {
            Lidded l = (Lidded)bs;
            l.open();
        }

        b.getWorld().playSound(b.getLocation(), this.ac.clickSound, this.ac.clickSoundVolume, this.ac.clickSoundPitch);
        b.getWorld().spawnParticle(this.ac.clickParticle, b.getLocation().clone().add(0.5D, 0.5D, 0.5D), this.ac.clickParticleCount, 0.3D, 0.3D, 0.3D, (double)this.ac.clickParticleSpeed);
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
                if (ChestsSession.this.state != ChestsSession.State.DONE) {
                    Iterator var1 = ChestsSession.this.chestLocs.iterator();

                    while(var1.hasNext()) {
                        Location loc = (Location)var1.next();
                        if (!loc.equals(clickedLoc)) {
                            Block ob = loc.getBlock();
                            BlockState obs = ob.getState();
                            if (obs instanceof Lidded) {
                                Lidded l = (Lidded)obs;
                                l.open();
                            }

                            loc.getWorld().playSound(loc, ChestsSession.this.ac.revealSound, ChestsSession.this.ac.revealSoundVolume, ChestsSession.this.ac.revealSoundPitch);
                            loc.getWorld().spawnParticle(ChestsSession.this.ac.revealParticle, loc.clone().add(0.5D, 0.5D, 0.5D), ChestsSession.this.ac.revealParticleCount, 0.2D, 0.2D, 0.2D, (double)ChestsSession.this.ac.revealParticleSpeed);
                            RewardModel sim = ChestsSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(ChestsSession.this.player.getUniqueId());
                                String hn = "fusioncase_sim_" + var10000 + "_" + loc.getBlockX() + "_" + loc.getBlockZ() + "_" + System.currentTimeMillis();
                                ChestsSession.this.spawnWinHolo(hn, loc.clone().add(0.5D, 1.5D, 0.5D), sim);
                            }
                        }
                    }

                }
            }
        }).runTaskLater(this.plugin, 30L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                ChestsSession.this.stop();
            }
        }).runTaskLater(this.plugin, 100L);
    }

    static {
        FACINGS = new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST, BlockFace.WEST, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST, BlockFace.EAST, BlockFace.EAST};
    }

}
