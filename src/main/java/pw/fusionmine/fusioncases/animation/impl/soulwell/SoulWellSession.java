package pw.fusionmine.fusioncases.animation.impl.soulwell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationConfig;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.case_system.CaseModel;
import pw.fusionmine.fusioncases.case_system.RewardModel;
import pw.fusionmine.fusioncases.hologram.HologramBridge;

class SoulWellSession extends AnimationSession {

    private final List<ArmorStand> stands = new ArrayList();
    private BukkitTask animationTask;
    private BukkitTask timeoutTask;
    private BukkitTask cleanupTask;
    private BukkitTask revealOtherTask;
    private int ticks = 0;

    public SoulWellSession(FusionCases plugin, Player p, Location caseLoc, CaseModel caseModel, ConfigurationSection config) {
        super(plugin, p, caseLoc, caseModel, new AnimationConfig(plugin, config, AnimationConfig.Type.SOULWELL), State.WAITING);
    }

    public void start() {
        Location center = this.caseLoc.clone().add(0.5D, this.ac.yOffset, 0.5D);

        for(int i = 0; i < 4; ++i) {
            double angle = (double)i * 2.0D * 3.141592653589793D / 4.0D;
            Location spawnLoc = center.clone().add(1.5D * Math.cos(angle), 1.0D, 1.5D * Math.sin(angle));
            ArmorStand stand = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class);
            stand.setInvisible(true);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setAI(false);
            stand.setMarker(false);
            if (stand.getEquipment() != null) {
                stand.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
            }

            this.plugin.getAnimationManager().registerEntity(stand, this);
            this.stands.add(stand);
            spawnLoc.getWorld().playSound(spawnLoc, this.ac.placementSound, this.ac.placementSoundVolume, this.ac.placementSoundPitch);
        }

        String title = this.plugin.getMsg("soulwell-title", new String[0]);
        String sub = this.plugin.getMsg("soulwell-subtitle", new String[0]);
        this.player.sendTitle(title, sub, 10, 70, 20);
        this.player.playSound(this.player.getLocation(), this.ac.titleSound, this.ac.titleSoundVolume, this.ac.titleSoundPitch);
        this.animationTask = (new BukkitRunnable() {
            public void run() {
                if (SoulWellSession.this.state == SoulWellSession.State.DONE) {
                    this.cancel();
                } else {
                    ++SoulWellSession.this.ticks;
                    Location cent = SoulWellSession.this.caseLoc.clone().add(0.5D, SoulWellSession.this.ac.yOffset, 0.5D);

                    for(int i = 0; i < SoulWellSession.this.stands.size(); ++i) {
                        ArmorStand stand = SoulWellSession.this.stands.get(i);
                        if (stand.isValid()) {
                            double angle = (double)SoulWellSession.this.ticks * 0.06D + (double)i * 2.0D * 3.141592653589793D / 4.0D;
                            double x = cent.getX() + 1.5D * Math.cos(angle);
                            double z = cent.getZ() + 1.5D * Math.sin(angle);
                            double y = cent.getY() + 0.6D + 0.25D * Math.sin((double)SoulWellSession.this.ticks * 0.12D + (double)i * 3.141592653589793D);
                            Location loc = new Location(cent.getWorld(), x, y, z, (float)Math.toDegrees(angle) + 90.0F, 0.0F);
                            stand.teleport(loc);
                            Location particleLoc = loc.clone().add(0.0D, 0.4D, 0.0D);
                            particleLoc.getWorld().spawnParticle(SoulWellSession.this.ac.ambientParticle1, particleLoc, SoulWellSession.this.ac.ambientParticle1Count, 0.02D, 0.02D, 0.02D, (double)SoulWellSession.this.ac.ambientParticle1Speed);
                            particleLoc.getWorld().spawnParticle(SoulWellSession.this.ac.ambientParticle2, particleLoc, SoulWellSession.this.ac.ambientParticle2Count, 0.02D, 0.02D, 0.02D, (double)SoulWellSession.this.ac.ambientParticle2Speed);
                        }
                    }

                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);
        this.timeoutTask = (new BukkitRunnable() {
            public void run() {
                if (SoulWellSession.this.state == SoulWellSession.State.WAITING && !SoulWellSession.this.stands.isEmpty()) {
                    SoulWellSession.this.reveal(SoulWellSession.this.stands.get(ThreadLocalRandom.current().nextInt(SoulWellSession.this.stands.size())));
                }

            }
        }).runTaskLater(this.plugin, (long)this.ac.timeout * 20L);
    }

    public void handleInteract(Player clicker, Block b, Action action) {
        if (this.state == SoulWellSession.State.WAITING) {
            if (clicker.getUniqueId().equals(this.player.getUniqueId())) {
                ArmorStand closest = null;
                double minDist = Double.MAX_VALUE;
                Iterator var7 = this.stands.iterator();

                while(var7.hasNext()) {
                    ArmorStand stand = (ArmorStand)var7.next();
                    double d = stand.getLocation().distance(b.getLocation());
                    if (d < minDist) {
                        minDist = d;
                        closest = stand;
                    }
                }

                if (closest != null && minDist <= 2.5D) {
                    this.reveal(closest);
                }
            }
        }
    }

    private void reveal(final ArmorStand clickedStand) {
        this.state = SoulWellSession.State.REVEALING;
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }

        if (this.animationTask != null) {
            this.animationTask.cancel();
        }

        Location loc = clickedStand.getLocation().clone().add(0.0D, 0.5D, 0.0D);
        loc.getWorld().strikeLightningEffect(loc);
        loc.getWorld().playSound(loc, this.ac.clickSound, this.ac.clickSoundVolume, this.ac.clickSoundPitch);
        loc.getWorld().spawnParticle(this.ac.clickParticle, loc, this.ac.clickParticleCount, 0.2D, 0.2D, 0.2D, (double)this.ac.clickParticleSpeed);
        RewardModel reward = this.caseModel.getRandomReward();
        if (reward != null) {
            this.plugin.getCaseManager().addHistoryEntry(this.caseModel.getDisplayName(), this.player.getName(), reward.getDisplayName(), reward.getMaterial());
            dispatchReward(reward);
            if (HologramBridge.isAvailable()) {
                String var10000 = String.valueOf(this.player.getUniqueId());
                String hn = "fusioncase_" + var10000 + "_" + System.currentTimeMillis();
                this.spawnWinHolo(hn, loc, reward);
            }
        }

        this.revealOtherTask = (new BukkitRunnable() {
            public void run() {
                if (SoulWellSession.this.state != SoulWellSession.State.DONE) {
                    Iterator var1 = SoulWellSession.this.stands.iterator();

                    while(var1.hasNext()) {
                        ArmorStand s = (ArmorStand)var1.next();
                        if (!s.equals(clickedStand)) {
                            Location sLoc = s.getLocation().clone().add(0.0D, 0.5D, 0.0D);
                            sLoc.getWorld().strikeLightningEffect(sLoc);
                            sLoc.getWorld().playSound(sLoc, SoulWellSession.this.ac.revealSound, SoulWellSession.this.ac.revealSoundVolume, SoulWellSession.this.ac.revealSoundPitch);
                            RewardModel sim = SoulWellSession.this.caseModel.getRandomReward();
                            if (sim != null && HologramBridge.isAvailable()) {
                                String var10000 = String.valueOf(SoulWellSession.this.player.getUniqueId());
                                String hn = "fusioncase_sim_" + var10000 + "_" + sLoc.getBlockX() + "_" + sLoc.getBlockZ() + "_" + System.currentTimeMillis();
                                SoulWellSession.this.spawnWinHolo(hn, sLoc, sim);
                            }
                        }
                    }

                }
            }
        }).runTaskLater(this.plugin, 30L);
        this.cleanupTask = (new BukkitRunnable() {
            public void run() {
                SoulWellSession.this.stop();
            }
        }).runTaskLater(this.plugin, 100L);
    }

    public void stop() {
        if (this.state != SoulWellSession.State.DONE) {
            this.state = SoulWellSession.State.DONE;
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
            var1 = this.stands.iterator();

            while(var1.hasNext()) {
                ArmorStand s = (ArmorStand)var1.next();
                this.plugin.getAnimationManager().unregisterEntity(s);
                s.remove();
            }

            this.stands.clear();
            this.plugin.getAnimationManager().endSession(this.player, this.caseLoc);
        }
    }

}
