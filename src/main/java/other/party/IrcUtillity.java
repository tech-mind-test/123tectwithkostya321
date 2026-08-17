package other.party;



import sky.core.SkyCore;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IrcUtillity implements Wrapper {

    public static IRCClient getClient() {
        try {
            if (SkyCore.getInstance().getIrcClient() != null) {
                return SkyCore.getInstance().getIrcClient();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean isIRCConnected() {
        IRCClient client = getClient();
        return client != null && client.isConnected();
    }

    public static String getSelfName() {
        try {
            String nativeName = "ABOBAPASTER";
            if (nativeName != null && !nativeName.isEmpty() && !nativeName.equals("Player")) {
                return nativeName;
            }
        } catch (Exception e) {
        }

        if (mc.player != null) {
            return mc.player.getName().getString();
        }
        return "Player";
    }

    public static List<String> getIRCUsers() {
        IRCClient client = getClient();
        if (client == null) return Collections.emptyList();
        return client.getOnlineUsers();
    }

    public static int getIRCUserCount() {
        return getIRCUsers().size();
    }

    public static boolean isUserOnIRC(String playerName) {
        if (playerName == null) return false;
        return getIRCUsers().contains(playerName);
    }

    public static boolean isInParty() {
        IRCClient client = getClient();
        return client != null && client.isInParty();
    }

    public static boolean isOwner() {
        IRCClient client = getClient();
        if (client == null) return false;
        return client.isPartyOwner(getSelfName());
    }

    public static String getOwner() {
        IRCClient client = getClient();
        if (client == null) return null;
        return client.getPartyOwner();
    }

    public static String getPartyName() {
        IRCClient client = getClient();
        if (client == null) return null;
        return client.getLastPartyName();
    }

    public static List<String> getAllMembers() {
        IRCClient client = getClient();
        if (client == null || !client.isInParty()) return Collections.emptyList();

        Set<String> members = new LinkedHashSet<>();
        members.add(getSelfName());

        ConcurrentHashMap<String, IRCClient.PartyPlayerData> positions = client.getPartyPositions();
        members.addAll(positions.keySet());

        return new ArrayList<>(members);
    }

    public static List<String> getOnlineMembers() {
        IRCClient client = getClient();
        if (client == null || !client.isInParty()) return Collections.emptyList();

        long now = System.currentTimeMillis();
        Set<String> online = new LinkedHashSet<>();
        online.add(getSelfName());

        for (Map.Entry<String, IRCClient.PartyPlayerData> entry : client.getPartyPositions().entrySet()) {
            if (now - entry.getValue().lastUpdate < 10000) {
                online.add(entry.getKey());
            }
        }
        return new ArrayList<>(online);
    }

    public static List<String> getOfflineMembers() {
        IRCClient client = getClient();
        if (client == null || !client.isInParty()) return Collections.emptyList();

        long now = System.currentTimeMillis();
        List<String> offline = new ArrayList<>();
        for (Map.Entry<String, IRCClient.PartyPlayerData> entry : client.getPartyPositions().entrySet()) {
            if (now - entry.getValue().lastUpdate >= 10000) {
                offline.add(entry.getKey());
            }
        }
        return offline;
    }

    public static int getMemberCount() {
        return getAllMembers().size();
    }

    public static int getOnlineMemberCount() {
        return getOnlineMembers().size();
    }

    public static boolean isMember(String playerName) {
        if (playerName == null) return false;
        return getAllMembers().contains(playerName);
    }

    public static boolean isMemberOnline(String playerName) {
        if (playerName == null) return false;
        if (playerName.equals(getSelfName())) return true;

        IRCClient client = getClient();
        if (client == null) return false;
        IRCClient.PartyPlayerData data = client.getPartyPositions().get(playerName);
        return data != null && (System.currentTimeMillis() - data.lastUpdate < 10000);
    }

    public static IRCClient.PartyPlayerData getPlayerData(String playerName) {
        IRCClient client = getClient();
        if (client == null || playerName == null) return null;
        return client.getPartyPositions().get(playerName);
    }

    public static ConcurrentHashMap<String, IRCClient.PartyPlayerData> getAllPositions() {
        IRCClient client = getClient();
        if (client == null) return new ConcurrentHashMap<>();
        return client.getPartyPositions();
    }

    public static double[] getPlayerCoords(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        if (data == null) return null;
        return new double[]{data.x, data.y, data.z};
    }

    public static double getPlayerX(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.x : Double.NaN;
    }

    public static double getPlayerY(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.y : Double.NaN;
    }

    public static double getPlayerZ(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.z : Double.NaN;
    }

    public static float getPlayerYaw(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.yaw : 0;
    }

    public static float getPlayerPitch(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.pitch : 0;
    }

    public static float getPlayerHealth(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.health : -1;
    }

    public static float getPlayerMaxHealth(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.maxHealth : -1;
    }

    public static float getPlayerHealthPercent(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        if (data == null || data.maxHealth <= 0) return -1;
        return data.health / data.maxHealth;
    }

    public static int getPlayerArmor(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.armor : -1;
    }

    public static String getPlayerServer(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.server : null;
    }

    public static String getPlayerDimension(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.dimension : null;
    }

    public static long getPlayerLastUpdate(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        return data != null ? data.lastUpdate : -1;
    }

    public static long getPlayerLastUpdateAgo(String playerName) {
        long lastUpdate = getPlayerLastUpdate(playerName);
        if (lastUpdate < 0) return -1;
        return (System.currentTimeMillis() - lastUpdate) / 1000;
    }

    public static double getDistanceTo(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        if (data == null) return -1;

        if (mc.player == null) return -1;

        double dx = mc.player.getPosX() - data.x;
        double dy = mc.player.getPosY() - data.y;
        double dz = mc.player.getPosZ() - data.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double getHorizontalDistanceTo(String playerName) {
        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        if (data == null) return -1;

        if (mc.player == null) return -1;

        double dx = mc.player.getPosX() - data.x;
        double dz = mc.player.getPosZ() - data.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static String getNearestPlayer() {
        IRCClient client = getClient();
        if (client == null) return null;

        String nearest = null;
        double minDist = Double.MAX_VALUE;

        for (String name : client.getPartyPositions().keySet()) {
            double dist = getDistanceTo(name);
            if (dist >= 0 && dist < minDist) {
                minDist = dist;
                nearest = name;
            }
        }
        return nearest;
    }

    public static String getFarthestPlayer() {
        IRCClient client = getClient();
        if (client == null) return null;

        String farthest = null;
        double maxDist = -1;

        for (String name : client.getPartyPositions().keySet()) {
            double dist = getDistanceTo(name);
            if (dist > maxDist) {
                maxDist = dist;
                farthest = name;
            }
        }
        return farthest;
    }

    public static List<String> getPlayersByDistance() {
        IRCClient client = getClient();
        if (client == null) return Collections.emptyList();

        return client.getPartyPositions().keySet().stream()
                .sorted(Comparator.comparingDouble(IrcUtillity::getDistanceTo))
                .collect(Collectors.toList());
    }

    public static List<String> getPlayersInRadius(double radius) {
        IRCClient client = getClient();
        if (client == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        for (String name : client.getPartyPositions().keySet()) {
            double dist = getDistanceTo(name);
            if (dist >= 0 && dist <= radius) {
                result.add(name);
            }
        }
        return result;
    }

    public static String getLowestHealthPlayer() {
        IRCClient client = getClient();
        if (client == null) return null;

        String lowest = null;
        float minHp = Float.MAX_VALUE;

        for (Map.Entry<String, IRCClient.PartyPlayerData> entry : client.getPartyPositions().entrySet()) {
            if (entry.getValue().health < minHp) {
                minHp = entry.getValue().health;
                lowest = entry.getKey();
            }
        }
        return lowest;
    }

    public static List<String> getPlayersOnServer(String server) {
        IRCClient client = getClient();
        if (client == null || server == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, IRCClient.PartyPlayerData> entry : client.getPartyPositions().entrySet()) {
            if (server.equalsIgnoreCase(entry.getValue().server)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static List<String> getPlayersInDimension(String dimension) {
        IRCClient client = getClient();
        if (client == null || dimension == null) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, IRCClient.PartyPlayerData> entry : client.getPartyPositions().entrySet()) {
            if (dimension.equalsIgnoreCase(entry.getValue().dimension)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static void requestPositions() {
        IRCClient client = getClient();
        if (client != null) client.requestPartyPositions();
    }

    public static void mutePlayer(String playerName, int seconds, String reason) {
        if (!isOwner()) {
            return;
        }
        if (playerName == null || playerName.isEmpty()) return;
        if (reason == null) reason = "Без причины";

        String cmd = ".party mute " + playerName + " " + seconds + " " + reason;
        sendIRCMessage(cmd);
    }

    public static void mutePlayerMinutes(String playerName, int minutes, String reason) {
        mutePlayer(playerName, minutes * 60, reason);
    }

    public static void mutePlayerHours(String playerName, int hours, String reason) {
        mutePlayer(playerName, hours * 3600, reason);
    }

    public static void mutePlayerPermanent(String playerName, String reason) {
        mutePlayer(playerName, 0, reason);
    }

    public static void unmutePlayer(String playerName) {
        if (!isOwner()) {
            return;
        }
        sendIRCMessage(".party unmute " + playerName);
    }

    public static boolean isMuted(String playerName) {
        IRCClient client = getClient();
        if (client == null || playerName == null) return false;
        return client.getMutedPlayers().containsKey(playerName);
    }

    public static ModerationInfo getMuteInfo(String playerName) {
        IRCClient client = getClient();
        if (client == null || playerName == null) return null;

        Map<String, Object> info = client.getMutedPlayers().get(playerName);
        if (info == null) return null;
        return ModerationInfo.fromMap(playerName, info);
    }

    public static long getMuteTimeLeft(String playerName) {
        ModerationInfo info = getMuteInfo(playerName);
        if (info == null) return -1;
        if (info.permanent) return 0;
        long left = (info.expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, left);
    }

    public static Map<String, ModerationInfo> getMutedPlayers() {
        IRCClient client = getClient();
        if (client == null) return Collections.emptyMap();

        Map<String, ModerationInfo> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : client.getMutedPlayers().entrySet()) {
            result.put(entry.getKey(), ModerationInfo.fromMap(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public static void banPlayer(String playerName, int seconds, String reason) {
        if (!isOwner()) {
            return;
        }
        if (playerName == null || playerName.isEmpty()) return;
        if (reason == null) reason = "Без причины";

        String cmd = ".party ban " + playerName + " " + seconds + " " + reason;
        sendIRCMessage(cmd);
    }

    public static void banPlayerMinutes(String playerName, int minutes, String reason) {
        banPlayer(playerName, minutes * 60, reason);
    }

    public static void banPlayerHours(String playerName, int hours, String reason) {
        banPlayer(playerName, hours * 3600, reason);
    }

    public static void banPlayerDays(String playerName, int days, String reason) {
        banPlayer(playerName, days * 86400, reason);
    }

    public static void banPlayerPermanent(String playerName, String reason) {
        banPlayer(playerName, 0, reason);
    }

    public static void unbanPlayer(String playerName) {
        if (!isOwner()) {
            return;
        }
        sendIRCMessage(".party unban " + playerName);
    }

    public static boolean isBanned(String playerName) {
        IRCClient client = getClient();
        if (client == null || playerName == null) return false;
        return client.getBannedPlayers().containsKey(playerName);
    }

    public static ModerationInfo getBanInfo(String playerName) {
        IRCClient client = getClient();
        if (client == null || playerName == null) return null;

        Map<String, Object> info = client.getBannedPlayers().get(playerName);
        if (info == null) return null;
        return ModerationInfo.fromMap(playerName, info);
    }

    public static long getBanTimeLeft(String playerName) {
        ModerationInfo info = getBanInfo(playerName);
        if (info == null) return -1;
        if (info.permanent) return 0;
        long left = (info.expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, left);
    }

    public static Map<String, ModerationInfo> getBannedPlayers() {
        IRCClient client = getClient();
        if (client == null) return Collections.emptyMap();

        Map<String, ModerationInfo> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : client.getBannedPlayers().entrySet()) {
            result.put(entry.getKey(), ModerationInfo.fromMap(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public static ConcurrentHashMap<String, String> getStaffList() {
        IRCClient client = getClient();
        if (client == null) return new ConcurrentHashMap<>();
        return client.getPartyStaffList();
    }

    public static boolean isStaff(String playerName) {
        return getStaffList().containsKey(playerName);
    }

    public static String getStaffStatus(String playerName) {
        return getStaffList().get(playerName);
    }

    public static void sendStaffUpdate(Map<String, String> staffData) {
        IRCClient client = getClient();
        if (client != null) client.sendStaffListUpdate(staffData);
    }

    public static void sendIRCMessage(String message) {
        IRCClient client = getClient();
        if (client == null || message == null || message.isEmpty()) return;
        client.sendMessage(message);
    }

    public static void sendPartyChat(String message) {
        if (message == null || message.isEmpty()) return;
        sendIRCMessage("!" + message);
    }

    public static void createParty(String name) {
        sendIRCMessage(".party create " + name);
    }

    public static void createParty(String name, String password) {
        sendIRCMessage(".party create " + name + " " + password);
    }

    public static void joinParty(String name) {
        sendIRCMessage(".party join " + name);
    }

    public static void joinParty(String name, String password) {
        sendIRCMessage(".party join " + name + " " + password);
    }

    public static void leaveParty() {
        sendIRCMessage(".party leave");
    }

    public static void kickPlayer(String playerName) {
        sendIRCMessage(".party kick " + playerName);
    }

    public static void invitePlayer(String playerName) {
        sendIRCMessage(".party invite " + playerName);
    }

    public static void disbandParty() {
        sendIRCMessage(".party disband");
    }

    public static void requestPartyList() {
        sendIRCMessage(".party list");
    }

    private static void chat(String msg) {
        ChatUtil.addText(msg, "Party");
    }

    public static void printIRCUsers() {
        if (!isIRCConnected()) {
            return;
        }

        List<String> users = getIRCUsers();
        chat("[IRC] Подключенные пользователи (" + users.size() + "):");
        if (users.isEmpty()) {
            chat("Список пуст (отправьте сообщение чтобы обновить)");
            return;
        }
        for (String name : users) {
            String selfMark = name.equals(getSelfName()) ? " (ты)" : "";
            chat("  ● " + name + selfMark);
        }
    }


    public static void printPartyMembers() {
        if (!isInParty()) {
            chat("[Party] Не в party");
            return;
        }

        List<String> online = getOnlineMembers();
        List<String> offline = getOfflineMembers();

        chat("[Party] Участники (" + getMemberCount() + "):");

        for (String name : online) {
            double dist = getDistanceTo(name);
            float hp = getPlayerHealth(name);
            String server = getPlayerServer(name);

            String distStr = dist >= 0 ? String.format("%.0f", dist) + "m" : "?";
            String hpStr = hp >= 0 ? String.format("%.1f", hp) + "hp" : "?";
            String srvStr = (server != null && !server.isEmpty()) ? server : "?";

            String ownerMark = name.equals(getOwner()) ? " §6★" : "";
            String selfMark = name.equals(getSelfName()) ? " §e(ты)" : "";
            String mutedMark = isMuted(name) ? " §c[MUTED]" : "";
            String bannedMark = isBanned(name) ? " §4[BANNED]" : "";

            chat("  §a● §f" + name + ownerMark + selfMark + mutedMark + bannedMark
                    + " §7[" + distStr + " | " + hpStr + " | " + srvStr + "]");
        }

        for (String name : offline) {
            String ownerMark = name.equals(getOwner()) ? " §6★" : "";
            String mutedMark = isMuted(name) ? " §c[MUTED]" : "";
            String bannedMark = isBanned(name) ? " §4[BANNED]" : "";
            chat("  §c● §7" + name + ownerMark + mutedMark + bannedMark);
        }
    }

    public static void printPlayerInfo(String playerName) {
        if (playerName == null) {
            chat("§7[Party] §cУкажи ник!");
            return;
        }

        IRCClient.PartyPlayerData data = getPlayerData(playerName);
        if (data == null) {
            chat("§7[Party] §cИгрок §f" + playerName + " §cне найден");
            return;
        }

        chat("§7[Party] §b═══ " + playerName + " ═══");
        chat("§7  Позиция: §f" + String.format("%.1f, %.1f, %.1f", data.x, data.y, data.z));
        chat("§7  Поворот: §fYaw=" + String.format("%.1f", data.yaw) + " Pitch=" + String.format("%.1f", data.pitch));
        chat("§7  Здоровье: §c" + String.format("%.1f/%.1f", data.health, data.maxHealth)
                + " §7(" + String.format("%.0f%%", getPlayerHealthPercent(playerName) * 100) + ")");
        chat("§7  Броня: §9" + data.armor);
        chat("§7  Сервер: §e" + (data.server.isEmpty() ? "?" : data.server));
        chat("§7  Измерение: §d" + data.dimension);

        double dist = getDistanceTo(playerName);
        if (dist >= 0) {
            chat("§7  Расстояние: §f" + String.format("%.1f", dist) + "m");
        }

        chat("§7  Обновлено: §f" + getPlayerLastUpdateAgo(playerName) + " сек назад");
        chat("§7  Онлайн: " + (isMemberOnline(playerName) ? "§aДа" : "§cНет"));

        if (isMuted(playerName)) {
            ModerationInfo mute = getMuteInfo(playerName);
            chat("§7  Мут: §c" + (mute.permanent ? "ПЕРМАНЕНТНЫЙ" : getMuteTimeLeft(playerName) + " сек")
                    + " §7(" + mute.reason + ")");
        }
        if (isBanned(playerName)) {
            ModerationInfo ban = getBanInfo(playerName);
            chat("§7  Бан: §4" + (ban.permanent ? "ПЕРМАНЕНТНЫЙ" : getBanTimeLeft(playerName) + " сек")
                    + " §7(" + ban.reason + ")");
        }
    }

    public static void printPartyInfo() {
        if (!isIRCConnected()) {
            chat("§7[Party] §cIRC не подключен");
            return;
        }
        if (!isInParty()) {
            chat("§7[Party] §cНе в party");
            return;
        }

        String partyName = getPartyName();
        String owner = getOwner();

        chat("§7[Party] §6═══════════════════════════");
        chat("§7[Party] §bParty: §f" + (partyName != null ? partyName : "???"));
        chat("§7[Party] §7Владелец: §a" + (owner != null ? owner : "???"));
        chat("§7[Party] §7Участники: §f" + getOnlineMemberCount() + "/" + getMemberCount() + " онлайн");
        chat("§7[Party] §6═══════════════════════════");

        printPartyMembers();

        Map<String, ModerationInfo> muted = getMutedPlayers();
        Map<String, ModerationInfo> banned = getBannedPlayers();

        if (!muted.isEmpty()) {
            chat("§7[Party] §c── Муты ──");
            for (Map.Entry<String, ModerationInfo> entry : muted.entrySet()) {
                ModerationInfo info = entry.getValue();
                String time = info.permanent ? "ПЕРМАНЕНТНЫЙ" : getMuteTimeLeft(entry.getKey()) + "с";
                chat("§7  §c" + entry.getKey() + " §7— " + time + " §7(" + info.reason + ")");
            }
        }

        if (!banned.isEmpty()) {
            chat("§7[Party] §4── Баны ──");
            for (Map.Entry<String, ModerationInfo> entry : banned.entrySet()) {
                ModerationInfo info = entry.getValue();
                String time = info.permanent ? "ПЕРМАНЕНТНЫЙ" : getBanTimeLeft(entry.getKey()) + "с";
                chat("§7  §4" + entry.getKey() + " §7— " + time + " §7(" + info.reason + ")");
            }
        }
    }

    public static void printMutedPlayers() {
        Map<String, ModerationInfo> muted = getMutedPlayers();
        if (muted.isEmpty()) {
            chat("§7[Party] §fНет замученных игроков");
            return;
        }

        chat("§7[Party] §cЗамученные (" + muted.size() + "):");
        for (Map.Entry<String, ModerationInfo> entry : muted.entrySet()) {
            ModerationInfo info = entry.getValue();
            String time = info.permanent ? "§4ПЕРМАНЕНТНЫЙ" : "§e" + formatTime(getMuteTimeLeft(entry.getKey()));
            chat("  §c● §f" + entry.getKey() + " §7— " + time + " §7| " + info.reason
                    + " §7| Выдал: §f" + info.issuedBy);
        }
    }

    public static void printBannedPlayers() {
        Map<String, ModerationInfo> banned = getBannedPlayers();
        if (banned.isEmpty()) {
            chat("§7[Party] §fНет забаненных игроков");
            return;
        }

        chat("§7[Party] §4Забаненные (" + banned.size() + "):");
        for (Map.Entry<String, ModerationInfo> entry : banned.entrySet()) {
            ModerationInfo info = entry.getValue();
            String time = info.permanent ? "§4ПЕРМАНЕНТНЫЙ" : "§e" + formatTime(getBanTimeLeft(entry.getKey()));
            chat("  §4● §f" + entry.getKey() + " §7— " + time + " §7| " + info.reason
                    + " §7| Выдал: §f" + info.issuedBy);
        }
    }

    public static void printNearbyPlayers(double radius) {
        List<String> nearby = getPlayersInRadius(radius);
        if (nearby.isEmpty()) {
            chat("§7[Party] §fНет игроков в радиусе " + (int) radius + "m");
            return;
        }

        chat("§7[Party] §fИгроки в радиусе " + (int) radius + "m (" + nearby.size() + "):");
        for (String name : nearby) {
            double dist = getDistanceTo(name);
            float hp = getPlayerHealth(name);
            chat("  §a● §f" + name + " §7— §e" + String.format("%.1f", dist) + "m"
                    + (hp >= 0 ? " §c" + String.format("%.1f", hp) + "hp" : ""));
        }
    }

    public static void printAllInfo() {
        chat("§7IRC подключение: " + (isIRCConnected() ? "§aДа" : "§cНет"));
        chat("§7Наш ник: §b" + getSelfName());

        printIRCUsers();

        if (isInParty()) {
            printPartyInfo();
        } else {
            chat("§7[Party] §cНе в party");
        }
    }

    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0с";
        if (seconds < 60) return seconds + "с";
        if (seconds < 3600) return (seconds / 60) + "м " + (seconds % 60) + "с";
        if (seconds < 86400) return (seconds / 3600) + "ч " + ((seconds % 3600) / 60) + "м";
        return (seconds / 86400) + "д " + ((seconds % 86400) / 3600) + "ч";
    }

    public static String getPartyStatusShort() {
        if (!isIRCConnected()) return "§cOffline";
        if (!isInParty()) return "§7No party";
        return "§a" + getOnlineMemberCount() + "/" + getMemberCount();
    }

    public static String getPartyHUDString() {
        if (!isIRCConnected()) return "IRC: §cOffline";
        if (!isInParty()) return "IRC: §aOnline §7| No party";

        String name = getPartyName() != null ? getPartyName() : "???";
        return "§b" + name + " §7| " + getPartyStatusShort();
    }

    public static class ModerationInfo {
        public final String playerName;
        public final String reason;
        public final String issuedBy;
        public final long expireTime;
        public final long issuedAt;
        public final boolean permanent;

        public ModerationInfo(String playerName, String reason, String issuedBy,
                              long expireTime, long issuedAt, boolean permanent) {
            this.playerName = playerName;
            this.reason = reason;
            this.issuedBy = issuedBy;
            this.expireTime = expireTime;
            this.issuedAt = issuedAt;
            this.permanent = permanent;
        }

        public static ModerationInfo fromMap(String playerName, Map<String, Object> map) {
            String reason = map.getOrDefault("reason", "Без причины").toString();
            String issuedBy = map.getOrDefault("issuedBy", "???").toString();
            long expireTime = 0;
            long issuedAt = 0;
            boolean permanent = false;

            try {
                Object exp = map.get("expireTime");
                if (exp != null) expireTime = Long.parseLong(exp.toString());
            } catch (Exception e) { }

            try {
                Object iss = map.get("issuedAt");
                if (iss != null) issuedAt = Long.parseLong(iss.toString());
            } catch (Exception e) { }

            try {
                Object perm = map.get("permanent");
                if (perm != null) permanent = Boolean.parseBoolean(perm.toString());
            } catch (Exception e) { }

            return new ModerationInfo(playerName, reason, issuedBy, expireTime, issuedAt, permanent);
        }

        @Override
        public String toString() {
            String time = permanent ? "PERMANENT" : ((expireTime - System.currentTimeMillis()) / 1000) + "s left";
            return playerName + " | " + reason + " | by " + issuedBy + " | " + time;
        }
    }
}