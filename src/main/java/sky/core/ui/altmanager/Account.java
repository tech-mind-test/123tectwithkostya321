package sky.core.ui.altmanager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import sky.core.utils.other.AsyncManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Account {
    public String accountName = "";
    public long dateAdded;
    public ResourceLocation skin;

    public Account(String accountName) {
        this(accountName, System.currentTimeMillis());
    }

    public Account(String accountName, long dateAdded) {
        this.accountName = accountName;
        this.dateAdded = dateAdded;

        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8));
        this.skin = DefaultPlayerSkin.getDefaultSkin(offline);
    }

    /** Loads skin asynchronously; call with delay between accounts to avoid Mojang 429. */
    public void loadSkinAsync() {
        UUID offline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8));
        AsyncManager.run(() -> {
            UUID resolved;
            try {
                resolved = resolveUUID(accountName);
            } catch (IOException ignored) {
                resolved = offline;
            }
            final UUID uuid = resolved;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.execute(() -> mc.getSkinManager().loadProfileTextures(new GameProfile(uuid, accountName), (type, loc, tex) -> {
                if (type == MinecraftProfileTexture.Type.SKIN) {
                    Account.this.skin = loc;
                }
            }, false));
        });
    }

    public static UUID resolveUUID(String name) throws IOException {
        try (InputStreamReader in = new InputStreamReader(
                new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openStream(),
                StandardCharsets.UTF_8
        )) {
            JsonObject json = new Gson().fromJson(in, JsonObject.class);
            if (json == null || !json.has("id")) {
                return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            }
            String raw = json.get("id").getAsString();
            String dashed = raw.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            return UUID.fromString(dashed);
        } catch (Throwable ignored) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }
    }
}
