package other.party;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;



import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class IRCClient {
    private static final String SERVER_URL = "wss://irc.dimasikfree.fun";
    private static final String ENCRYPTION_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final ConcurrentHashMap<String, Map<String, Object>> mutedPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, Object>> bannedPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PartyPlayerData> allPositions = new ConcurrentHashMap<>();
    private final List<String> onlineUsers = Collections.synchronizedList(new ArrayList<>());

    private WebSocketClient client;
    private String username;
    private String clientId;
    private String serverKey;
    private byte[] encryptionKey;
    private final Gson gson = new Gson();
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ITextComponent> componentQueue = new ConcurrentLinkedQueue<>();
    private boolean connected = false;
    private boolean authenticated = false;

    private final ConcurrentHashMap<String, String> partyStaffList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PlayerFullData> playerDataCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PartyPlayerData> partyPositions = new ConcurrentHashMap<>();

    private long lastPositionSend = 0;
    private static final long POSITION_SEND_INTERVAL = 0;

    private String partyOwner = null;
    private boolean inParty = false;
    private String lastPartyName = null;

    private boolean isAdmin = false;
    private final List<String> adminList = Collections.synchronizedList(new ArrayList<>());

    public IRCClient(String username) {
        this.username = username;
        this.encryptionKey = hexToBytes(ENCRYPTION_KEY);
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public List<String> getAdminList() {
        return new ArrayList<>(adminList);
    }

    public boolean isInParty() {
        return inParty;
    }

    public String getPartyOwner() {
        return partyOwner;
    }

    public String getLastPartyName() {
        return lastPartyName;
    }

    public boolean isPartyOwner(String playerName) {
        return inParty && partyOwner != null && partyOwner.equals(playerName);
    }

    public Queue<String> getMessageQueue() {
        return messageQueue;
    }

    public Queue<ITextComponent> getComponentQueue() {
        return componentQueue;
    }

    public boolean isConnected() {
        return connected && authenticated;
    }

    public ConcurrentHashMap<String, String> getPartyStaffList() {
        return partyStaffList;
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers);
    }

    public ConcurrentHashMap<String, PartyPlayerData> getAllPositions() {
        long now = System.currentTimeMillis();
        allPositions.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 15000);
        return allPositions;
    }

    public PartyPlayerData getAnyPlayerData(String playerName) {
        if (playerName == null) return null;
        PartyPlayerData data = partyPositions.get(playerName);
        if (data != null) return data;
        return allPositions.get(playerName);
    }

    public ConcurrentHashMap<String, Map<String, Object>> getMutedPlayers() {
        long now = System.currentTimeMillis();
        mutedPlayers.entrySet().removeIf(entry -> {
            Object perm = entry.getValue().get("permanent");
            if (perm != null && Boolean.parseBoolean(perm.toString())) return false;
            Object exp = entry.getValue().get("expireTime");
            if (exp == null) return true;
            try { return Long.parseLong(exp.toString()) < now; } catch (Exception e) { return true; }
        });
        return mutedPlayers;
    }

    public ConcurrentHashMap<String, Map<String, Object>> getBannedPlayers() {
        long now = System.currentTimeMillis();
        bannedPlayers.entrySet().removeIf(entry -> {
            Object perm = entry.getValue().get("permanent");
            if (perm != null && Boolean.parseBoolean(perm.toString())) return false;
            Object exp = entry.getValue().get("expireTime");
            if (exp == null) return true;
            try { return Long.parseLong(exp.toString()) < now; } catch (Exception e) { return true; }
        });
        return bannedPlayers;
    }

    public ConcurrentHashMap<String, PartyPlayerData> getPartyPositions() {
        long now = System.currentTimeMillis();
        partyPositions.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 15000);
        return partyPositions;
    }

    public PlayerFullData getCachedPlayerData(String playerName) {
        return playerDataCache.get(playerName);
    }

    public ConcurrentHashMap<String, PlayerFullData> getPlayerDataCache() {
        return playerDataCache;
    }

    public void updateUsername(String newUsername) {
        if (newUsername == null || newUsername.isEmpty()) return;
        if (newUsername.equals(this.username)) return;

        String oldUsername = this.username;
        this.username = newUsername;

        if (connected) {
            reconnect();
        }
    }

    public void reconnect() {
        authenticated = false;
        connected = false;
        partyPositions.clear();
        partyStaffList.clear();
        partyOwner = null;
        inParty = false;

        if (client != null) {
            try {
                client.closeBlocking();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        connect();
    }

    public void connect() {
        try {
            client = new WebSocketClient(new URI(SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected = true;
                }

                @Override
                public void onMessage(String encryptedMessage) {
                    handleMessage(encryptedMessage);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    authenticated = false;
                    partyPositions.clear();
                    partyStaffList.clear();
                    partyOwner = null;
                    inParty = false;
                }

                @Override
                public void onError(Exception ex) {
                }
            };

            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, null, new SecureRandom());
                client.setSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                    sslContext.init(null, null, new SecureRandom());
                    client.setSocketFactory(sslContext.getSocketFactory());
                } catch (Exception e2) {
                }
            }

            client.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null) client.close();
    }
    public void sendMessage(String message) {
        if (!connected || !authenticated) return;

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("type", "message");
        msgData.put("message", message);
        msgData.put("messageId", generateMessageId());
        msgData.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(msgData);
    }

    public void sendPartyCommand(String command, String arg) {
        if (!connected || !authenticated) return;

        Map<String, Object> data = new HashMap<>();
        data.put("type", "party_command");
        data.put("command", command);
        data.put("arg", arg != null ? arg : "");
        data.put("gameName", getGameName());
        data.put("messageId", generateMessageId());
        data.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(data);
    }

    public void sendStaffListUpdate(Map<String, String> staffData) {
        if (!connected || !authenticated) return;

        Map<String, Object> data = new HashMap<>();
        data.put("type", "staff_sync");
        data.put("staff", staffData);
        data.put("messageId", generateMessageId());
        data.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(data);
    }

    public void sendModerationAction(String action, String target, int seconds, String reason) {
        if (!connected || !authenticated) return;

        Map<String, Object> data = new HashMap<>();
        data.put("type", "moderation_action");
        data.put("action", action);
        data.put("target", target);
        data.put("seconds", seconds);
        data.put("reason", reason != null ? reason : "Без причины");
        data.put("messageId", generateMessageId());
        data.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(data);
    }

    public void requestModerationData() {
        if (!connected || !authenticated) return;

        Map<String, Object> req = new HashMap<>();
        req.put("type", "request_moderation");
        req.put("messageId", generateMessageId());
        req.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(req);
    }

    public void sendPositionUpdate(double x, double y, double z,
                                   float yaw, float pitch,
                                   float health, float maxHealth,
                                   int armor, String server, String dimension) {
        if (!connected || !authenticated) return;

        long now = System.currentTimeMillis();
        if (now - lastPositionSend < POSITION_SEND_INTERVAL) return;
        lastPositionSend = now;

        Map<String, Object> posData = new HashMap<>();
        posData.put("type", "position_update");
        posData.put("gameName", getGameName());
        posData.put("x", String.valueOf(x));
        posData.put("y", String.valueOf(y));
        posData.put("z", String.valueOf(z));
        posData.put("yaw", String.valueOf(yaw));
        posData.put("pitch", String.valueOf(pitch));
        posData.put("health", String.valueOf(health));
        posData.put("maxHealth", String.valueOf(maxHealth));
        posData.put("armor", String.valueOf(armor));
        posData.put("server", server != null ? server : "");
        posData.put("dimension", dimension != null ? dimension : "overworld");
        posData.put("messageId", generateMessageId());
        posData.put("timestamp", now);

        sendSecureMessage(posData);
    }

    public void requestPartyPositions() {
        if (!connected || !authenticated) return;

        Map<String, Object> reqData = new HashMap<>();
        reqData.put("type", "position_request");
        reqData.put("messageId", generateMessageId());
        reqData.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(reqData);
    }

    public void requestPlayerData(String playerName) {
        if (!connected || !authenticated || playerName == null) return;

        Map<String, Object> req = new HashMap<>();
        req.put("type", "request_player_data");
        req.put("target", playerName);
        req.put("messageId", generateMessageId());
        req.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(req);
    }

    public void requestAllPlayersData() {
        if (!connected || !authenticated) return;

        Map<String, Object> req = new HashMap<>();
        req.put("type", "request_all_data");
        req.put("messageId", generateMessageId());
        req.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(req);
    }

    private void handleMessage(String encryptedMessage) {
        try {
            String decrypted = decrypt(encryptedMessage);
            if (decrypted == null) return;

            JsonObject json = gson.fromJson(decrypted, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "handshake":
                    clientId = json.get("clientId").getAsString();
                    serverKey = json.get("serverKey").getAsString();
                    authenticate();
                    break;

                case "auth_success":
                    isAdmin = json.has("isAdmin") && json.get("isAdmin").getAsBoolean();
                    break;

                case "message":
                    String msgUsername = json.get("username").getAsString();
                    String message = json.get("message").getAsString();
                    break;

                case "system":
                    String sysMessage = json.get("message").getAsString();
                    break;

                case "party_response":
                    handlePartyResponse(json);
                    break;

                case "party_event":
                    handlePartyEvent(json);
                    break;

                case "party_invite":
                    handlePartyInvite(json);
                    break;

                case "party_position":
                    handlePartyPosition(json);
                    break;

                case "party_positions":
                    handlePartyPositions(json);
                    break;

                case "all_positions":
                    handleAllPositions(json);
                    break;

                case "mute_update":
                    handleMuteUpdate(json);
                    break;

                case "ban_update":
                    handleBanUpdate(json);
                    break;

                case "unmute":
                    handleUnmute(json);
                    break;

                case "unban":
                    handleUnban(json);
                    break;

                case "mute_list":
                    handleMuteList(json);
                    break;

                case "ban_list":
                    handleBanList(json);
                    break;

                case "user_list":
                    handleUserList(json);
                    break;

                case "admin_status":
                    isAdmin = json.has("isAdmin") && json.get("isAdmin").getAsBoolean();
                    if (json.has("admins") && json.get("admins").isJsonArray()) {
                        adminList.clear();
                        for (JsonElement el : json.getAsJsonArray("admins")) {
                            adminList.add(el.getAsString());
                        }
                    }
                    break;

                case "staff_sync":
                    handleStaffSync(json);
                    break;

                case "request_your_data":
                    handleDataRequest(json);
                    break;

                case "player_data_result":
                    handlePlayerDataResult(json);
                    break;

                case "all_players_data":
                    handleAllPlayersData(json);
                    break;


                case "moderation_response":
                    handleModerationResponse(json);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePartyResponse(JsonObject json) {
        try {
            String command = json.has("command") ? json.get("command").getAsString() : "";
            boolean success = json.has("success") && json.get("success").getAsBoolean();

            if (!success) {
                String error = json.has("error") ? json.get("error").getAsString() : "unknown";
                addComponentToChat(PartyFormatter.formatPartyError(error));
                return;
            }

            switch (command) {
                case "create": {
                    String code = json.has("code") ? json.get("code").getAsString() : "???";
                    inParty = true;
                    partyOwner = this.username;
                    addComponentToChat(PartyFormatter.formatPartyCreated(code));
                    break;
                }

                case "joincode":
                case "join": {
                    int count = json.has("memberCount") ? json.get("memberCount").getAsInt() : 0;
                    inParty = true;
                    addComponentToChat(PartyFormatter.formatMemberJoined(this.username, count));
                    break;
                }

                case "leave": {
                    inParty = false;
                    partyOwner = null;
                    partyPositions.clear();
                    partyStaffList.clear();
                    addComponentToChat(PartyFormatter.formatYouLeft());
                    break;
                }

                case "kick": {
                    String target = json.has("targetIrcName") ? json.get("targetIrcName").getAsString() : "???";
                    addComponentToChat(PartyFormatter.formatPlayerKicked(target));
                    break;
                }

                case "invite": {
                    String target = json.has("targetIrcName") ? json.get("targetIrcName").getAsString() : "???";
                    addComponentToChat(PartyFormatter.formatPlayerInvited(target));
                    break;
                }

                case "disband": {
                    inParty = false;
                    partyOwner = null;
                    partyPositions.clear();
                    partyStaffList.clear();
                    addComponentToChat(PartyFormatter.formatYouDisbanded());
                    break;
                }

                case "dismiss": {
                    addComponentToChat(PartyFormatter.formatYouDismissed());
                    break;
                }

                case "info": {
                    if (json.has("party") && !json.get("party").isJsonNull()) {
                        JsonObject party = json.getAsJsonObject("party");
                        String code = party.has("code") ? party.get("code").getAsString() : "???";
                        String leader = party.has("leader") ? party.get("leader").getAsString() : "???";

                        addComponentToChat(PartyFormatter.formatPartyInfoHeader(code));

                        if (party.has("members") && party.get("members").isJsonArray()) {
                            JsonArray members = party.getAsJsonArray("members");
                            for (JsonElement el : members) {
                                JsonObject member = el.getAsJsonObject();
                                String ircName = member.has("ircName") ? member.get("ircName").getAsString() : "???";
                                String gameName = member.has("gameName") ? member.get("gameName").getAsString() : "???";

                                if (ircName.equals(leader)) {
                                    addComponentToChat(PartyFormatter.formatPartyInfoLeader(ircName, gameName));
                                } else {
                                    addComponentToChat(PartyFormatter.formatPartyInfoMember(ircName, gameName));
                                }
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error handling party_response: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePartyEvent(JsonObject json) {
        try {
            String event = json.has("event") ? json.get("event").getAsString() : "";

            switch (event) {
                case "member_joined": {
                    String ircName = json.has("ircName") ? json.get("ircName").getAsString() : "???";
                    int count = json.has("memberCount") ? json.get("memberCount").getAsInt() : 0;
                    addComponentToChat(PartyFormatter.formatMemberJoined(ircName, count));
                    break;
                }

                case "member_left": {
                    String ircName = json.has("ircName") ? json.get("ircName").getAsString() : "???";
                    String newLeader = json.has("newLeader") && !json.get("newLeader").isJsonNull()
                            ? json.get("newLeader").getAsString() : null;

                    partyPositions.remove(ircName);
                    addComponentToChat(PartyFormatter.formatMemberLeft(ircName));

                    if (newLeader != null) {
                        if (newLeader.equals(this.username)) {
                            partyOwner = this.username;
                            addComponentToChat(PartyFormatter.formatYouAreLeader());
                        } else {
                            partyOwner = newLeader;
                            addComponentToChat(PartyFormatter.formatNewLeader(newLeader));
                        }
                    }
                    break;
                }

                case "member_kicked": {
                    String ircName = json.has("ircName") ? json.get("ircName").getAsString() : "???";
                    partyPositions.remove(ircName);
                    addComponentToChat(PartyFormatter.formatPlayerKicked(ircName));
                    break;
                }

                case "kicked": {
                    inParty = false;
                    partyOwner = null;
                    partyPositions.clear();
                    partyStaffList.clear();
                    addComponentToChat(PartyFormatter.formatYouKicked());
                    break;
                }

                case "disbanded": {
                    String leaderName = json.has("leaderIrcName") ? json.get("leaderIrcName").getAsString() : "???";
                    inParty = false;
                    partyOwner = null;
                    partyPositions.clear();
                    partyStaffList.clear();
                    addComponentToChat(PartyFormatter.formatDisbanded(leaderName));
                    break;
                }

                case "invite_dismissed": {
                    String ircName = json.has("ircName") ? json.get("ircName").getAsString() : "???";
                    addComponentToChat(PartyFormatter.formatInviteDismissed(ircName));
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error handling party_event: " + e.getMessage());
        }
    }

    private void handlePartyInvite(JsonObject json) {
        try {
            String from = json.has("fromIrcName") ? json.get("fromIrcName").getAsString() : "???";
            addComponentToChat(PartyFormatter.formatInviteReceived(from));
        } catch (Exception e) {
            System.err.println("[IRC] Error handling party_invite: " + e.getMessage());
        }
    }

    /**
     * Отправляет ITextComponent в чат через main thread
     */
    private void addComponentToChat(ITextComponent component) {
        try {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.sendMessage(component, mc.player.getUniqueID());
                }
            });
        } catch (Exception e) {
            componentQueue.add(component);
        }
    }

    private void handlePartyPosition(JsonObject json) {
        try {
            String playerName = json.get("username").getAsString();

            PartyPlayerData existing = partyPositions.get(playerName);
            PartyPlayerData data;

            if (existing != null) {
                data = existing;
                data.prevX = data.x;
                data.prevY = data.y;
                data.prevZ = data.z;
                data.prevYaw = data.yaw;
                data.prevPitch = data.pitch;
                data.hasPrev = true;
            } else {
                data = new PartyPlayerData();
            }

            data.username = playerName;
            data.x = json.get("x").getAsDouble();
            data.y = json.get("y").getAsDouble();
            data.z = json.get("z").getAsDouble();
            data.yaw = json.has("yaw") ? json.get("yaw").getAsFloat() : 0;
            data.pitch = json.has("pitch") ? json.get("pitch").getAsFloat() : 0;
            data.health = json.has("health") ? json.get("health").getAsFloat() : 20;
            data.maxHealth = json.has("maxHealth") ? json.get("maxHealth").getAsFloat() : 20;
            data.armor = json.has("armor") ? json.get("armor").getAsInt() : 0;
            data.server = json.has("server") ? json.get("server").getAsString() : "";
            data.dimension = json.has("dimension") ? json.get("dimension").getAsString() : "overworld";
            data.lastUpdate = System.currentTimeMillis();

            partyPositions.put(playerName, data);
        } catch (Exception e) {
            System.err.println("[IRC] Error parsing party_position: " + e.getMessage());
        }
    }

    private void handlePartyPositions(JsonObject json) {
        try {
            if (!json.has("positions") || json.get("positions").isJsonNull()) return;
            JsonObject positions = json.getAsJsonObject("positions");
            if (positions == null) return;

            for (Map.Entry<String, JsonElement> entry : positions.entrySet()) {
                String key = entry.getKey();
                JsonObject posJson = entry.getValue().getAsJsonObject();

                PartyPlayerData data = partyPositions.getOrDefault(key, new PartyPlayerData());

                if (data.lastUpdate > 0) {
                    data.prevX = data.x;
                    data.prevY = data.y;
                    data.prevZ = data.z;
                    data.hasPrev = true;
                }

                data.username = key;
                data.x = posJson.get("x").getAsDouble();
                data.y = posJson.get("y").getAsDouble();
                data.z = posJson.get("z").getAsDouble();
                data.yaw = posJson.has("yaw") ? posJson.get("yaw").getAsFloat() : 0;
                data.pitch = posJson.has("pitch") ? posJson.get("pitch").getAsFloat() : 0;
                data.health = posJson.has("health") ? posJson.get("health").getAsFloat() : 20;
                data.maxHealth = posJson.has("maxHealth") ? posJson.get("maxHealth").getAsFloat() : 20;
                data.armor = posJson.has("armor") ? posJson.get("armor").getAsInt() : 0;
                data.server = posJson.has("server") ? posJson.get("server").getAsString() : "";
                data.dimension = posJson.has("dimension") ? posJson.get("dimension").getAsString() : "overworld";
                data.lastUpdate = System.currentTimeMillis();

                partyPositions.put(key, data);
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error parsing party positions: " + e.getMessage());
        }
    }

    private void handleAllPositions(JsonObject json) {
        try {
            if (!json.has("positions") || json.get("positions").isJsonNull()) return;
            JsonObject positions = json.getAsJsonObject("positions");
            if (positions == null) return;

            for (Map.Entry<String, JsonElement> entry : positions.entrySet()) {
                String key = entry.getKey();
                JsonObject posJson = entry.getValue().getAsJsonObject();

                PartyPlayerData data = allPositions.getOrDefault(key, new PartyPlayerData());

                if (data.lastUpdate > 0) {
                    data.prevX = data.x;
                    data.prevY = data.y;
                    data.prevZ = data.z;
                    data.hasPrev = true;
                }

                data.username = key;
                data.x = posJson.get("x").getAsDouble();
                data.y = posJson.get("y").getAsDouble();
                data.z = posJson.get("z").getAsDouble();
                data.yaw = posJson.has("yaw") ? posJson.get("yaw").getAsFloat() : 0;
                data.pitch = posJson.has("pitch") ? posJson.get("pitch").getAsFloat() : 0;
                data.health = posJson.has("health") ? posJson.get("health").getAsFloat() : 20;
                data.maxHealth = posJson.has("maxHealth") ? posJson.get("maxHealth").getAsFloat() : 20;
                data.armor = posJson.has("armor") ? posJson.get("armor").getAsInt() : 0;
                data.server = posJson.has("server") ? posJson.get("server").getAsString() : "";
                data.dimension = posJson.has("dimension") ? posJson.get("dimension").getAsString() : "overworld";
                data.lastUpdate = System.currentTimeMillis();

                allPositions.put(key, data);
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error all_positions: " + e.getMessage());
        }
    }


    private void handleModerationResponse(JsonObject json) {
        try {
            boolean success = json.has("success") && json.get("success").getAsBoolean();
            String message = json.has("message") ? json.get("message").getAsString() : "";
            String action = json.has("action") ? json.get("action").getAsString() : "";
            String target = json.has("target") ? json.get("target").getAsString() : "";

            if (success) {
                messageQueue.add(PartyFormatter.formatSuccess(message));
            } else {
                messageQueue.add(PartyFormatter.formatError(message));
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error handling moderation_response: " + e.getMessage());
        }
    }

    private void handleMuteUpdate(JsonObject json) {
        try {
            String playerName = json.get("playerName").getAsString();
            Map<String, Object> info = new HashMap<>();
            info.put("reason", json.has("reason") ? json.get("reason").getAsString() : "Без причины");
            info.put("issuedBy", json.has("issuedBy") ? json.get("issuedBy").getAsString() : "???");
            info.put("expireTime", json.has("expireTime") ? json.get("expireTime").getAsLong() : 0L);
            info.put("issuedAt", json.has("issuedAt") ? json.get("issuedAt").getAsLong() : System.currentTimeMillis());
            info.put("permanent", json.has("permanent") && json.get("permanent").getAsBoolean());
            mutedPlayers.put(playerName, info);
        } catch (Exception e) {
            System.err.println("[IRC] Error handling mute_update: " + e.getMessage());
        }
    }

    private void handleBanUpdate(JsonObject json) {
        try {
            String playerName = json.get("playerName").getAsString();
            Map<String, Object> info = new HashMap<>();
            info.put("reason", json.has("reason") ? json.get("reason").getAsString() : "Без причины");
            info.put("issuedBy", json.has("issuedBy") ? json.get("issuedBy").getAsString() : "???");
            info.put("expireTime", json.has("expireTime") ? json.get("expireTime").getAsLong() : 0L);
            info.put("issuedAt", json.has("issuedAt") ? json.get("issuedAt").getAsLong() : System.currentTimeMillis());
            info.put("permanent", json.has("permanent") && json.get("permanent").getAsBoolean());
            bannedPlayers.put(playerName, info);
        } catch (Exception e) {
            System.err.println("[IRC] Error handling ban_update: " + e.getMessage());
        }
    }

    private void handleUnmute(JsonObject json) {
        try {
            String playerName = json.get("playerName").getAsString();
            mutedPlayers.remove(playerName);
        } catch (Exception e) {
            System.err.println("[IRC] Error handling unmute: " + e.getMessage());
        }
    }

    private void handleUnban(JsonObject json) {
        try {
            String playerName = json.get("playerName").getAsString();
            bannedPlayers.remove(playerName);
        } catch (Exception e) {
            System.err.println("[IRC] Error handling unban: " + e.getMessage());
        }
    }

    private void handleMuteList(JsonObject json) {
        try {
            mutedPlayers.clear();
            if (!json.has("mutes") || json.get("mutes").isJsonNull()) return;
            JsonObject mutes = json.getAsJsonObject("mutes");
            for (Map.Entry<String, JsonElement> entry : mutes.entrySet()) {
                JsonObject muteJson = entry.getValue().getAsJsonObject();
                Map<String, Object> info = new HashMap<>();
                info.put("reason", muteJson.has("reason") ? muteJson.get("reason").getAsString() : "Без причины");
                info.put("issuedBy", muteJson.has("issuedBy") ? muteJson.get("issuedBy").getAsString() : "???");
                info.put("expireTime", muteJson.has("expireTime") ? muteJson.get("expireTime").getAsLong() : 0L);
                info.put("issuedAt", muteJson.has("issuedAt") ? muteJson.get("issuedAt").getAsLong() : 0L);
                info.put("permanent", muteJson.has("permanent") && muteJson.get("permanent").getAsBoolean());
                mutedPlayers.put(entry.getKey(), info);
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error handling mute_list: " + e.getMessage());
        }
    }

    private void handleBanList(JsonObject json) {
        try {
            bannedPlayers.clear();
            if (!json.has("bans") || json.get("bans").isJsonNull()) return;
            JsonObject bans = json.getAsJsonObject("bans");
            for (Map.Entry<String, JsonElement> entry : bans.entrySet()) {
                JsonObject banJson = entry.getValue().getAsJsonObject();
                Map<String, Object> info = new HashMap<>();
                info.put("reason", banJson.has("reason") ? banJson.get("reason").getAsString() : "Без причины");
                info.put("issuedBy", banJson.has("issuedBy") ? banJson.get("issuedBy").getAsString() : "???");
                info.put("expireTime", banJson.has("expireTime") ? banJson.get("expireTime").getAsLong() : 0L);
                info.put("issuedAt", banJson.has("issuedAt") ? banJson.get("issuedAt").getAsLong() : 0L);
                info.put("permanent", banJson.has("permanent") && banJson.get("permanent").getAsBoolean());
                bannedPlayers.put(entry.getKey(), info);
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error handling ban_list: " + e.getMessage());
        }
    }

    private void handleUserList(JsonObject json) {
        try {
            onlineUsers.clear();
            if (json.has("users") && !json.get("users").isJsonNull()) {
                JsonArray users = json.getAsJsonArray("users");
                for (JsonElement el : users) {
                    if (el.isJsonObject()) {
                        JsonObject userObj = el.getAsJsonObject();
                        String ircName = userObj.has("ircName") ? userObj.get("ircName").getAsString() : "";
                        if (!ircName.isEmpty()) {
                            onlineUsers.add(ircName);
                        }
                    } else if (el.isJsonPrimitive()) {
                        onlineUsers.add(el.getAsString());
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private void handleStaffSync(JsonObject json) {
        try {
            if (!json.has("staff") || json.get("staff").isJsonNull()) return;

            JsonObject staff = json.getAsJsonObject("staff");
            partyStaffList.clear();
            for (Map.Entry<String, JsonElement> entry : staff.entrySet()) {
                partyStaffList.put(entry.getKey(), entry.getValue().getAsString());
            }
        } catch (Exception e) {
            System.err.println("[IRC] staff_sync error: " + e.getMessage());
        }
    }

    private void handleDataRequest(JsonObject json) {
        try {
            String requestId = json.has("requestId") ? json.get("requestId").getAsString() : "";

            Map<String, Object> response = new HashMap<>();
            response.put("type", "player_data_response");
            response.put("requestId", requestId);
            response.put("messageId", generateMessageId());
            response.put("timestamp", System.currentTimeMillis());

            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    response.put("x", String.valueOf(mc.player.getPosX()));
                    response.put("y", String.valueOf(mc.player.getPosY()));
                    response.put("z", String.valueOf(mc.player.getPosZ()));
                    response.put("yaw", String.valueOf(mc.player.rotationYaw));
                    response.put("pitch", String.valueOf(mc.player.rotationPitch));
                    response.put("health", String.valueOf(mc.player.getHealth()));
                    response.put("maxHealth", String.valueOf(mc.player.getMaxHealth()));
                    response.put("armor", String.valueOf(mc.player.getTotalArmorValue()));
                    response.put("dimension", mc.player.world.getDimensionKey().getLocation().getPath());

                    if (mc.getCurrentServerData() != null) {
                        response.put("server", mc.getCurrentServerData().serverIP);
                    } else {
                        response.put("server", "singleplayer");
                    }

                    if (!mc.player.getHeldItemMainhand().isEmpty()) {
                        response.put("heldItem", mc.player.getHeldItemMainhand().getDisplayName().getString());
                    }

                    if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUniqueID()) != null) {
                        response.put("ping", String.valueOf(mc.getConnection().getPlayerInfo(mc.player.getUniqueID()).getResponseTime()));
                    }
                }
            } catch (Exception e) {
            }

            sendSecureMessage(response);
        } catch (Exception e) {
            System.err.println("[IRC] Error handling data request: " + e.getMessage());
        }
    }

    private void handlePlayerDataResult(JsonObject json) {
        try {
            String target = json.get("target").getAsString();

            PlayerFullData data = new PlayerFullData();
            data.username = target;
            data.online = json.has("online") && json.get("online").getAsBoolean();
            data.hasData = json.has("hasData") && json.get("hasData").getAsBoolean();
            data.isAdmin = json.has("isAdmin") && json.get("isAdmin").getAsBoolean();
            data.isMuted = json.has("isMuted") && json.get("isMuted").getAsBoolean();
            data.isBanned = json.has("isBanned") && json.get("isBanned").getAsBoolean();
            data.inParty = json.has("inParty") && json.get("inParty").getAsBoolean();
            data.partyName = json.has("partyName") && !json.get("partyName").isJsonNull()
                    ? json.get("partyName").getAsString() : null;

            if (json.has("muteInfo") && !json.get("muteInfo").isJsonNull()) {
                JsonObject mi = json.getAsJsonObject("muteInfo");
                data.muteReason = mi.has("reason") ? mi.get("reason").getAsString() : "";
                data.muteIssuedBy = mi.has("issuedBy") ? mi.get("issuedBy").getAsString() : "";
                data.muteExpire = mi.has("expireTime") ? mi.get("expireTime").getAsLong() : 0;
                data.mutePermanent = mi.has("permanent") && mi.get("permanent").getAsBoolean();
            }

            if (json.has("banInfo") && !json.get("banInfo").isJsonNull()) {
                JsonObject bi = json.getAsJsonObject("banInfo");
                data.banReason = bi.has("reason") ? bi.get("reason").getAsString() : "";
                data.banIssuedBy = bi.has("issuedBy") ? bi.get("issuedBy").getAsString() : "";
                data.banExpire = bi.has("expireTime") ? bi.get("expireTime").getAsLong() : 0;
                data.banPermanent = bi.has("permanent") && bi.get("permanent").getAsBoolean();
            }

            JsonObject posSource = null;
            if (json.has("gameData") && !json.get("gameData").isJsonNull()) {
                posSource = json.getAsJsonObject("gameData");
            } else if (json.has("position") && !json.get("position").isJsonNull()) {
                posSource = json.getAsJsonObject("position");
            }

            if (posSource != null) {
                data.x = posSource.has("x") ? posSource.get("x").getAsDouble() : 0;
                data.y = posSource.has("y") ? posSource.get("y").getAsDouble() : 0;
                data.z = posSource.has("z") ? posSource.get("z").getAsDouble() : 0;
                data.yaw = posSource.has("yaw") ? posSource.get("yaw").getAsFloat() : 0;
                data.pitch = posSource.has("pitch") ? posSource.get("pitch").getAsFloat() : 0;
                data.health = posSource.has("health") ? posSource.get("health").getAsFloat() : 20;
                data.maxHealth = posSource.has("maxHealth") ? posSource.get("maxHealth").getAsFloat() : 20;
                data.armor = posSource.has("armor") ? posSource.get("armor").getAsInt() : 0;
                data.server = posSource.has("server") ? posSource.get("server").getAsString() : "";
                data.dimension = posSource.has("dimension") ? posSource.get("dimension").getAsString() : "";
                data.heldItem = posSource.has("heldItem") ? posSource.get("heldItem").getAsString() : "";
                data.gamemode = posSource.has("gamemode") ? posSource.get("gamemode").getAsString() : "";
                data.ping = posSource.has("ping") ? posSource.get("ping").getAsInt() : 0;
                data.hasData = true;
            }

            data.lastUpdate = System.currentTimeMillis();
            playerDataCache.put(target, data);
        } catch (Exception e) {
            System.err.println("[IRC] Error player_data_result: " + e.getMessage());
        }
    }

    private void handleAllPlayersData(JsonObject json) {
        try {
            if (!json.has("players") || json.get("players").isJsonNull()) return;
            JsonObject players = json.getAsJsonObject("players");

            for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
                String name = entry.getKey();
                JsonObject pJson = entry.getValue().getAsJsonObject();

                PlayerFullData data = playerDataCache.getOrDefault(name, new PlayerFullData());
                data.username = name;
                data.online = pJson.has("online") && pJson.get("online").getAsBoolean();
                data.isAdmin = pJson.has("isAdmin") && pJson.get("isAdmin").getAsBoolean();
                data.isMuted = pJson.has("isMuted") && pJson.get("isMuted").getAsBoolean();
                data.isBanned = pJson.has("isBanned") && pJson.get("isBanned").getAsBoolean();
                data.inParty = pJson.has("inParty") && pJson.get("inParty").getAsBoolean();
                data.partyName = pJson.has("partyName") && !pJson.get("partyName").isJsonNull()
                        ? pJson.get("partyName").getAsString() : null;

                if (pJson.has("position") && !pJson.get("position").isJsonNull()) {
                    JsonObject pos = pJson.getAsJsonObject("position");
                    data.x = pos.has("x") ? pos.get("x").getAsDouble() : 0;
                    data.y = pos.has("y") ? pos.get("y").getAsDouble() : 0;
                    data.z = pos.has("z") ? pos.get("z").getAsDouble() : 0;
                    data.health = pos.has("health") ? pos.get("health").getAsFloat() : 20;
                    data.maxHealth = pos.has("maxHealth") ? pos.get("maxHealth").getAsFloat() : 20;
                    data.armor = pos.has("armor") ? pos.get("armor").getAsInt() : 0;
                    data.server = pos.has("server") ? pos.get("server").getAsString() : "";
                    data.dimension = pos.has("dimension") ? pos.get("dimension").getAsString() : "";
                    data.hasData = true;
                }

                data.lastUpdate = System.currentTimeMillis();
                playerDataCache.put(name, data);
            }
        } catch (Exception e) {
            System.err.println("[IRC] Error all_players_data: " + e.getMessage());
        }
    }

    private void authenticate() {
        Map<String, Object> authData = new HashMap<>();
        authData.put("type", "auth");
        authData.put("username", username);
        authData.put("gameName", getGameName());
        authData.put("messageId", generateMessageId());
        authData.put("timestamp", System.currentTimeMillis());

        sendSecureMessage(authData);
        authenticated = true;

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (Exception e) { return; }
            if (connected && authenticated) {
                Map<String, Object> req = new HashMap<>();
                req.put("type", "request_moderation");
                req.put("messageId", generateMessageId());
                req.put("timestamp", System.currentTimeMillis());
                sendSecureMessage(req);
            }
        }).start();
    }
    private String getGameName() {
        try {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                String playerName = mc.player.getName().getString();
                if (playerName != null && !playerName.isEmpty()) {
                    return playerName;
                }
            }
        } catch (Exception e) {
        }
        return username;
    }

    private void sendSecureMessage(Map<String, Object> data) {
        try {
            String signature = generateSignature(data);

            Map<String, Object> envelope = new HashMap<>();
            envelope.put("data", data);
            envelope.put("signature", signature);

            String json = gson.toJson(envelope);
            String encrypted = encrypt(json);

            client.send(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String encrypt(String plaintext) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[16];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(encryptionKey, "AES"),
                    new IvParameterSpec(iv));

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(iv) + ":" + bytesToHex(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decrypt(String encryptedData) {
        try {
            String[] parts = encryptedData.split(":");
            byte[] iv = hexToBytes(parts[0]);
            byte[] encrypted = hexToBytes(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(encryptionKey, "AES"),
                    new IvParameterSpec(iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateSignature(Map<String, Object> data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(serverKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return bytesToHex(mac.doFinal(gson.toJson(data).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    private String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static class PlayerFullData {
        public String username;
        public boolean online;
        public boolean hasData;
        public boolean isAdmin;
        public boolean isMuted;
        public boolean isBanned;
        public boolean inParty;
        public String partyName;
        public String muteReason;
        public String banReason;
        public String muteIssuedBy;
        public String banIssuedBy;
        public long muteExpire;
        public long banExpire;
        public boolean mutePermanent;
        public boolean banPermanent;

        public double x, y, z;
        public float yaw, pitch;
        public float health, maxHealth;
        public int armor;
        public String server;
        public String dimension;
        public long lastUpdate;

        public String heldItem;
        public String gamemode;
        public int ping;
    }

    public static class PartyPlayerData {
        public String username;
        public double x, y, z;
        public float yaw, pitch;
        public float health, maxHealth;
        public int armor;
        public String server;
        public String dimension;
        public long lastUpdate;

        public double prevX, prevY, prevZ;
        public float prevYaw, prevPitch;
        public boolean hasPrev = false;

        public void updatePrev() {
            prevX = x;
            prevY = y;
            prevZ = z;
            prevYaw = yaw;
            prevPitch = pitch;
            hasPrev = true;
        }

        public double getInterpolatedX(float partialTicks) {
            if (!hasPrev) return x;
            return prevX + (x - prevX) * partialTicks;
        }

        public double getInterpolatedY(float partialTicks) {
            if (!hasPrev) return y;
            return prevY + (y - prevY) * partialTicks;
        }

        public double getInterpolatedZ(float partialTicks) {
            if (!hasPrev) return z;
            return prevZ + (z - prevZ) * partialTicks;
        }
    }
}