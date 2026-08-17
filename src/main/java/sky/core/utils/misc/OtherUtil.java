package sky.core.utils.misc;


import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import mods.viaversion.vialoadingbase.ViaLoadingBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector2f;

import java.util.Locale;

import static sky.core.utils.Wrapper.mc;
import static sky.core.utils.Wrapper.mw;

public class OtherUtil {

//
//    private static final Faker FAKER = new Faker();
//
//    public static String generateRandomNickname() {
//        String s = sanitizeUsername(FAKER.name().username());
//        if (s.length() < 3) s += FAKER.number().digits(3);
//        return s.length() > 16 ? s.substring(0, 16) : s;
//    }
//
//    private static String sanitizeUsername(String input) {
//        if (input == null) return "Player" + FAKER.number().digits(3);
//        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
//        normalized = normalized.replaceAll("\\p{M}+", "");
//        String cleaned = normalized.replaceAll("[^A-Za-z0-9_]", "");
//        if (cleaned.isEmpty()) cleaned = "Player";
//        if (cleaned.length() >= 1) {
//            String first = cleaned.substring(0, 1).toUpperCase(Locale.ROOT);
//            String rest = cleaned.substring(1);
//            cleaned = first + rest;
//        }
//        return cleaned;
//    }

    public static Vector2f getMouse(int mouseX, int mouseY) {
        return GuiUtils.getMouse(mouseX, mouseY);
    }

    public static String getPlayerCoordinates() {
        return PlayerUtils.getPlayerCoordinates();
    }

    public static String calculateBPS() {
        return PlayerUtils.calculateBPS();
    }

    public static int calculatePing() {
        return PlayerUtils.calculatePing();
    }

    public static boolean ViaCheck(ProtocolVersion protocolVersion) {
        if (!ViaLoadingBase.getInstance().getTargetVersion().newerThanOrEqualTo(protocolVersion)) return false;
        for (UserConnection conn : Via.getManager().getConnectionManager().getConnections()) {
            if (conn == null) return false;
            if (conn.getProtocolInfo().getUsername().equalsIgnoreCase(mc.session.getProfile().getName())) return true;
        }
        return false;
    }

    public static class GuiUtils {
        public static Vector2f getMouse(float mouseX, float mouseY) {
            double scale = mw.getGuiScaleFactor() / 2;
            return new Vector2f((float) (mouseX * scale), (float) (mouseY * scale));
        }
    }

    public static class PlayerUtils {
        private static int cachedPing = 0;
        private static long lastUpdateTime = 0;

        public static String getPlayerCoordinates() {
            PlayerEntity player = mc.player;
            if (player != null) {
                int posX = (int) player.getPosX();
                int posY = (int) player.getPosY();
                int posZ = (int) player.getPosZ();
                return String.format("%d %d %d", posX, posY, posZ);
            }
            return "";
        }

        public static String calculateBPS() {
            PlayerEntity player = mc.player;
            double deltaX = player.getPosX() - player.prevPosX;
            double deltaZ = player.getPosZ() - player.prevPosZ;
            double deltaY = player.getPosY() - player.prevPosY;

            double distance = Math.hypot(deltaX, deltaZ);
            distance = Math.hypot(distance, deltaY);

            return String.format(Locale.US, "%.1f", distance * 20.0D);
        }

        public static int calculatePing() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 100) {
                if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUniqueID()) != null) {
                    cachedPing = mc.getConnection().getPlayerInfo(mc.player.getUniqueID()).getResponseTime();
                } else {
                    cachedPing = 0;
                }
                lastUpdateTime = currentTime;
            }
            return cachedPing;
        }
    }
}