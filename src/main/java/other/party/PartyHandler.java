package other.party;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.events.EventSwapWorld;
import sky.core.events.EventUpdate;
import sky.core.utils.Wrapper;

public class PartyHandler implements Wrapper {
    private Thread positionThread;

    public IRCClient ircClient;
    private long lastPositionSend = 0;

    @EventTarget
    private void fasdfasd (EventSwapWorld eventSwapWorld) {
     //   if (NativeAPI.getRole() == Role.BETA || NativeAPI.getRole() == Role.YOUTUBE || NativeAPI.getRole() == Role.ADMIN) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                checkAndUpdateUsername();
            }).start();
       // }
    };
    @EventTarget
    private void asdasd (EventUpdate eventSwapWorld) {
        ircClient = SkyCore.getInstance().getIrcClient();

            if (!ircClient.isConnected()) {
                long now = System.currentTimeMillis();
                if (now - lastPositionSend >= 5000) {
                    lastPositionSend = now;
                    ircClient.updateUsername("ABOBAPASTER");
                    ircClient.connect();
                }
        }
    };
    @EventTarget
    private void asdasd (EventRender2D eventSwapWorld) {
            if (this.ircClient != null && !this.ircClient.isConnected()) {
                mc.fontRenderer.drawStringWithShadow(eventSwapWorld.getStack(), "Не подключен к серверам Apathy. Обратитесь в тех. поддержку", 3, mc.getMainWindow().getScaledHeight() - 12, -1);

        }
    };

    private void checkAndUpdateUsername() {
        if (ircClient == null) return;

        String currentName = getIRCUsername();
        String ircName = ircClient.getUsername();

        if (!currentName.equals(ircName)) {
            ircClient.updateUsername(currentName);
        }
    }
    private String getIRCUsername() {
        try {
            String nativeName = "ABOBAPASTER";
            if (nativeName != null && !nativeName.isEmpty()) {
                return nativeName;
            }
        } catch (Exception e) {

        }

        if (mc.player != null) {
            String playerName = mc.player.getName().getString();
            if (playerName != null && !playerName.isEmpty()) {
                return playerName;
            }
        }

        try {
            if (mc.getSession() != null) {
                String sessionName = mc.getSession().getUsername();
                if (sessionName != null && !sessionName.isEmpty()) {
                    return sessionName;
                }
            }
        } catch (Exception e) {
        }

        return "null";
    }
}
