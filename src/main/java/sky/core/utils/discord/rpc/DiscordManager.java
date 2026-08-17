package sky.core.utils.discord.rpc;

import com.adl.nativeprotect.User;
import lombok.Getter;
import com.adl.nativeprotect.Native;
import sky.core.utils.discord.rpc.utils.DiscordEventHandlers;
import sky.core.utils.discord.rpc.utils.DiscordRPC;
import sky.core.utils.discord.rpc.utils.DiscordRichPresence;
import sky.core.utils.discord.rpc.utils.RPCButton;

@Getter
public class DiscordManager {

    private DiscordDaemonThread discordDaemonThread;
    private long applicationId;
    private boolean running;

    private String image;
    private String telegram;
    private String site;

    @Native
    private void initDefaults() {
        discordDaemonThread = new DiscordDaemonThread();
        applicationId = 1458796229814259806L;
        running = true;
        image = "skycore";
        telegram = "https://t.me/skycoredll";
        site = "https://skycoredlc.ru";
    }

    @Native
    public void init() {
        initDefaults();
        DiscordRichPresence.Builder builder = new DiscordRichPresence.Builder();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
        DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(applicationId), handlers, true, "");
        builder.setStartTimestamp(System.currentTimeMillis() / 1000L);
        String user = User.getInstance().profile("username");
        String uid = User.getInstance().profile("uid");
        builder.setDetails("User: " + user);
        builder.setState("UID: " + uid);
        builder.setLargeImage(image, "https://skycoredlc.ru");
        builder.setButtons(
                RPCButton.create("News", telegram),
                RPCButton.create("Website", site)
        );
        DiscordRPC.INSTANCE.Discord_UpdatePresence(builder.build());
        discordDaemonThread.start();
    }

    @Native
    public DiscordManager start() {
        try {
            init();
        } catch (Throwable ignored) {
            running = false;
        }
        return this;
    }

    @Native
    public void stopRPC() {
        try {
            running = false;
            DiscordRPC.INSTANCE.Discord_Shutdown();
            if (discordDaemonThread != null && discordDaemonThread.isAlive()) {
                discordDaemonThread.interrupt();
            }
        } catch (Throwable ignored) {
        }
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");
            try {
                while (running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15_000L);
                }
            } catch (Throwable ignored) {
                running = false;
            }
        }
    }
}
