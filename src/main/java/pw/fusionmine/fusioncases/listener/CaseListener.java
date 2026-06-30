package pw.fusionmine.fusioncases.listener;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pw.fusionmine.fusioncases.FusionCases;
import pw.fusionmine.fusioncases.animation.api.AnimationManager;
import pw.fusionmine.fusioncases.animation.api.AnimationSession;
import pw.fusionmine.fusioncases.case_system.CaseGuiHolder;
import pw.fusionmine.fusioncases.case_system.CaseManager;
import pw.fusionmine.fusioncases.case_system.CaseModel;

public class CaseListener implements Listener {

    private final FusionCases plugin;

    public CaseListener(FusionCases plugin) {
        this.plugin = plugin;
        this.caseManager = plugin.getCaseManager();
        this.animationManager = plugin.getAnimationManager();
    }

    private final CaseManager caseManager;
    private final AnimationManager animationManager;

    private void msg(Player p, String key, String... repls) {
        p.sendMessage(this.plugin.getMsg(key, repls));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND)
            return;
        Block b = e.getClickedBlock();
        if (b == null)
            return;
        Location loc = b.getLocation();
        Player p = e.getPlayer();

        AnimationSession blockSession = this.animationManager.getSessionByBlock(loc);
        if (blockSession != null) {
            e.setCancelled(true);
            blockSession.handleInteract(p, b, e.getAction());
            return;
        }
        String caseName = this.caseManager.getCaseAtBlock(loc);
        if (caseName != null) {
            e.setCancelled(true);
            CaseModel caseModel = this.caseManager.getCase(caseName);
            if (caseModel == null) {
                msg(p, "case-not-configured", new String[0]);
                return;
            }
            if (this.caseManager.isCaseDisabled(caseName)) {
                msg(p, "case-disabled", new String[0]);
                return;
            }
            if (this.animationManager.getSessionByPlayer(p.getUniqueId()) != null) {
                msg(p, "already-opening", new String[0]);
                return;
            }
            if (this.animationManager.isLocationActive(loc)) {
                msg(p, "already-opened-by-other", new String[0]);
                return;
            }
            this.caseManager.openGui(p, caseModel, loc);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof CaseGuiHolder) {
            CaseGuiHolder holder = (CaseGuiHolder) inventoryHolder;
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player)) {
                return;
            }
            Player p = (Player) e.getWhoClicked();
            CaseModel caseModel = holder.getCaseModel();
            Location loc = holder.getCaseLocation();
            int slot = e.getRawSlot();

            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta() && e.getClick().isLeftClick()) {
                ItemMeta meta = clickedItem.getItemMeta();
                NamespacedKey key = new NamespacedKey(this.caseManager.getPlugin(), "case_name");
                if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    String clickedCaseName = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    CaseModel targetCaseModel = this.caseManager.getCase(clickedCaseName);
                    if (targetCaseModel != null) {
                        p.closeInventory();

                        if (this.caseManager.isCaseDisabled(targetCaseModel.getName())) {
                            msg(p, "case-disabled", new String[0]);
                            return;
                        }

                        if (this.animationManager.getSessionByPlayer(p.getUniqueId()) != null) {
                            msg(p, "already-opening", new String[0]);
                            return;
                        }

                        if (this.animationManager.isLocationActive(loc)) {
                            msg(p, "already-opened-by-other", new String[0]);
                            return;
                        }

                        int keys = this.caseManager.getKeys(p.getUniqueId(), targetCaseModel.getName());
                        if (keys <= 0) {
                            msg(p, "no-keys");
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
                            return;
                        }
                        if (this.caseManager.takeKey(p.getUniqueId(), targetCaseModel.getName())) {
                            boolean started = this.animationManager.startAnimation(p, loc, targetCaseModel);
                            if (!started) {
                                this.caseManager.addKeys(p.getUniqueId(), targetCaseModel.getName(), 1);
                                msg(p, "animation-start-failed", new String[0]);
                            }
                        } else {
                            msg(p, "no-keys", new String[0]);
                        }

                        return;
                    }
                }
            }
            String action = this.caseManager.getActionAtSlot(caseModel, slot);
            if (action != null && action.equalsIgnoreCase("start")) {
                p.closeInventory();
                if (this.caseManager.isCaseDisabled(caseModel.getName())) {
                    msg(p, "case-disabled", new String[0]);
                    return;
                }
                if (this.animationManager.getSessionByPlayer(p.getUniqueId()) != null) {
                    msg(p, "already-opening", new String[0]);
                    return;
                }
                if (this.animationManager.isLocationActive(loc)) {
                    msg(p, "already-opened-by-other", new String[0]);
                    return;
                }
                int keys = this.caseManager.getKeys(p.getUniqueId(), caseModel.getName());
                if (keys <= 0) {
                    msg(p, "no-keys", new String[0]);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
                    return;
                }
                if (this.caseManager.takeKey(p.getUniqueId(), caseModel.getName())) {
                    boolean started = this.animationManager.startAnimation(p, loc, caseModel);
                    if (!started) {
                        this.caseManager.addKeys(p.getUniqueId(), caseModel.getName(), 1);
                        msg(p, "animation-start-failed", new String[0]);
                    }
                } else {
                    msg(p, "no-keys", new String[0]);
                }
            }
        }

    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof CaseGuiHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Location loc = b.getLocation();
        if (this.animationManager.isAnimationBlock(loc)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        this.animationManager.stopSession(e.getPlayer());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (this.animationManager.isAnimationEntity(e.getEntity())) {
            e.setCancelled(true);
            Entity entity = e.getDamager();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                AnimationSession session = this.animationManager.getSessionByEntity(e.getEntity());
                if (session != null) {
                    session.handleInteract(player, e.getEntity().getLocation().getBlock(), Action.LEFT_CLICK_BLOCK);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent) {
            return;
        }
        if (this.animationManager.isAnimationEntity(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND)
            return;
        if (this.animationManager.isAnimationEntity(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (this.animationManager.isAnimationEntity(e.getRightClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (this.animationManager.isAnimationEntity(e.getEntity())) {
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

}