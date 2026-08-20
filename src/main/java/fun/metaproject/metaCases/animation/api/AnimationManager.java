package fun.metaproject.metaCases.animation.api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.case_system.CaseModel;
public class AnimationManager {
    private final MetaCasesFree plugin;
    private final Map<String, CaseAnimation> animations = new HashMap();
    private final Map<UUID, AnimationSession> playerSessions = new HashMap();
    private final Map<Location, AnimationSession> locationSessions = new HashMap();
    private final Map<Location, AnimationSession> blockSessions = new HashMap();
    private final Map<UUID, AnimationSession> entitySessions = new HashMap();
    public AnimationManager(MetaCasesFree plugin) {
        this.plugin = plugin;
    }
    public void registerAnimation(CaseAnimation anim) {
        this.animations.put(anim.getName().toLowerCase(), anim);
    }
    public boolean startAnimation(Player p, Location loc, CaseModel caseModel) {
        if (this.playerSessions.containsKey(p.getUniqueId())) {
            return false;
        } else if (this.locationSessions.containsKey(loc)) {
            return false;
        } else {
            String animName = caseModel.getAnimationName().toLowerCase();
            CaseAnimation anim;
            if (animName.equals("random")) {
                List<CaseAnimation> pool = new ArrayList();
                Iterator var7 = this.animations.values().iterator();
                while(var7.hasNext()) {
                    CaseAnimation a = (CaseAnimation)var7.next();
                    if (!a.getName().equalsIgnoreCase("random")) {
                        pool.add(a);
                    }
                }
                if (pool.isEmpty()) {
                    this.plugin.getLogger().warning("No animations available for random selection!");
                    return false;
                }
                anim = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            } else {
                anim = this.animations.get(animName);
            }
            if (anim == null) {
                this.plugin.getLogger().warning("Animation not found: " + caseModel.getAnimationName());
                return false;
            } else {
                AnimationSession session = anim.createSession(p, loc, caseModel, caseModel.getAnimationConfig());
                this.playerSessions.put(p.getUniqueId(), session);
                this.locationSessions.put(loc, session);
                this.plugin.getCaseManager().removeBlockHologram(loc);
                session.start();
                return true;
            }
        }
    }
    public void endSession(Player p, Location loc) {
        this.playerSessions.remove(p.getUniqueId());
        this.locationSessions.remove(loc);
        String caseName = this.plugin.getCaseManager().getCaseAtBlock(loc);
        if (caseName != null) {
            CaseModel cm = this.plugin.getCaseManager().getCase(caseName);
            if (cm != null && cm.isHoloEnabled()) {
                this.plugin.getCaseManager().spawnBlockHologram(loc, cm);
            }
        }
    }
    public void stopSession(Player p) {
        AnimationSession s = this.playerSessions.get(p.getUniqueId());
        if (s != null) {
            s.stop();
        }
    }
    public void stopAll() {
        Iterator var1 = this.playerSessions.values().iterator();
        while(var1.hasNext()) {
            AnimationSession s = (AnimationSession)var1.next();
            s.stop();
        }
        this.playerSessions.clear();
        this.locationSessions.clear();
        this.blockSessions.clear();
        this.entitySessions.clear();
    }
    public void registerBlock(Location loc, AnimationSession session) {
        this.blockSessions.put(loc, session);
    }
    public void unregisterBlock(Location loc) {
        this.blockSessions.remove(loc);
    }
    public AnimationSession getSessionByBlock(Location loc) {
        return (AnimationSession)this.blockSessions.get(loc);
    }
    public AnimationSession getSessionByPlayer(UUID uuid) {
        return (AnimationSession)this.playerSessions.get(uuid);
    }
    public boolean isAnimationBlock(Location loc) {
        return this.blockSessions.containsKey(loc);
    }
    public boolean isLocationActive(Location loc) {
        return this.locationSessions.containsKey(loc);
    }
    public void registerEntity(Entity e, AnimationSession session) {
        this.entitySessions.put(e.getUniqueId(), session);
    }
    public void unregisterEntity(Entity e) {
        this.entitySessions.remove(e.getUniqueId());
    }
    public AnimationSession getSessionByEntity(Entity e) {
        return this.entitySessions.get(e.getUniqueId());
    }
    public boolean isAnimationEntity(Entity e) {
        return this.entitySessions.containsKey(e.getUniqueId());
    }
}
