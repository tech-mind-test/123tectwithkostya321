package sky.core.utils.misc;

import sky.core.utils.Wrapper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SUpdateBossInfoPacket;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ServerUtil implements Wrapper {

    public static boolean isPvP() {
        return ServerUtil.BossInfoHandler.isPvP();
    }

    public static float getHealth(LivingEntity entity) {
        Iterator<Map.Entry<ScoreObjective, Score>> iterator;
        if (entity instanceof PlayerEntity p) {
            if (mc.world != null && (iterator = mc.world.getScoreboard().getObjectivesForEntity(p.getGameProfile().getName()).entrySet().iterator()).hasNext()) {
                Map.Entry<ScoreObjective, Score> entry = iterator.next();
                Score score = entry.getValue();
                return score.getScorePoints();
            }
        }
        return (int) (entity.getHealth() + entity.getAbsorptionAmount());
    }

    public static boolean isConnectedToServer(String ip) {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null && mc.getCurrentServerData().serverIP.contains(ip);
    }

    public static class BossInfoHandler {
        private static boolean pvpMode = false;
        private static UUID uuid = null;

        public static void updateBossInfo(SUpdateBossInfoPacket packet) {
            if (packet.getOperation() == SUpdateBossInfoPacket.Operation.ADD) {
                String name = StringUtils.stripControlCodes(packet.getName().getString()).toLowerCase();
                if (name.contains("pvp") || name.contains("у вас осталось")) {
                    pvpMode = true;
                    uuid = packet.getUniqueId();
                }
            } else if (packet.getOperation() == SUpdateBossInfoPacket.Operation.REMOVE) {
                if (packet.getUniqueId().equals(uuid)) {
                    pvpMode = false;
                    uuid = null;
                }
            }
        }

        public static boolean isPvP() {
            return pvpMode;
        }
    }
}