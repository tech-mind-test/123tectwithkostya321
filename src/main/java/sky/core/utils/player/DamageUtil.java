package sky.core.utils.player;

import sky.core.events.EventDamage;
import sky.core.events.EventPacket;
import sky.core.utils.Wrapper;
import sky.core.utils.math.TimeUtil;
import lombok.Getter;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.potion.Effects;

@Getter
public class DamageUtil implements Wrapper {
    private final TimeUtil timeTracker = new TimeUtil();
    private boolean normalDamage;
    private boolean fallDamage;
    private boolean explosionDamage;
    private boolean arrowDamage;
    private boolean pearlDamage;
    private boolean blockDamage;

    public void onPacketEvent(EventPacket eventPacket) {
        if (mc.player != null && mc.world != null) {
            boolean isDamage = fallDamage || arrowDamage || explosionDamage || pearlDamage || blockDamage;
            if (!isBadEffects()) {
                if (eventPacket.getPacket() instanceof SExplosionPacket) {
                    explosionDamage = true;
                }

                if (!isDamage) {
                    IPacket<?> packet = eventPacket.getPacket();
                    if (packet instanceof SEntityStatusPacket statusPacket) {
                        if (statusPacket.getOpCode() == 2 && statusPacket.getEntity(mc.world) == mc.player) {
                            normalDamage = true;
                        }
                    }
                } else if (mc.player.hurtTime > 0) {
                    normalDamage = false;
                    reset();
                }

            }
        }
    }

    public boolean time(long time) {
        if (normalDamage) {
            if (timeTracker.hasTimeElapsed(time)) {
                normalDamage = false;
                timeTracker.reset();
                return true;
            }
        } else {
            timeTracker.reset();
        }

        return false;
    }

    public void processDamage(EventDamage damageEvent) {
        switch (damageEvent.getDamageType()) {
            case FALL -> fallDamage = true;
            case ARROW -> arrowDamage = true;
            case ENDER_PEARL -> pearlDamage = true;
            case BLOCK -> blockDamage = true;
        }

        normalDamage = false;
    }

    public void resetDamages() {
        normalDamage = false;
        reset();
        timeTracker.reset();
    }

    private void reset() {
        fallDamage = false;
        explosionDamage = false;
        arrowDamage = false;
        pearlDamage = false;
        blockDamage = false;
    }

    private boolean isBadEffects() {
        if (mc.player == null) {
            return false;
        } else {
            return mc.player.isPotionActive(Effects.POISON) || mc.player.isPotionActive(Effects.WITHER) || mc.player.isPotionActive(Effects.INSTANT_DAMAGE);
        }
    }
}
