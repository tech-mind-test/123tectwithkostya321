package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiAfk extends Module {

    private String savedAnarchyId = null;
    private int reconnectState = 0;
    private int timer = 0;

    public AntiAfk() {
        super("AntiAfk", "Reconnect AutoBuy", Category.Movement);
    }

    @Native
    @Override
    public void onEnable() {
        super.onEnable();
        reconnectState = 0;
        timer = 0;
        parseScoreboard();
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (reconnectState == 0) {
            parseScoreboard();
        } else {
            handleReconnectSequence();
        }
    }

    @Native
    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        IPacket<?> packet = e.getPacket();
        if (packet instanceof SChatPacket) {
            SChatPacket chatPacket = (SChatPacket) packet;
            String text = chatPacket.getChatComponent().getString();
            String cleanText = text.replaceAll("§.", "").trim();

            if (cleanText.contains("недоступна в режиме AFK") || cleanText.contains("AFK")) {
                if (reconnectState == 0) {
                    if (savedAnarchyId != null) {
                        reconnectState = 1;
                        timer = 0;
                    }
                }
            }
        }
    }

    @Native
    private void handleReconnectSequence() {
        timer++;

        switch (reconnectState) {
            case 1:
                mc.player.sendChatMessage("/hub");
                timer = 0;
                reconnectState = 2;
                break;

            case 2:
                if (timer > 60) {
                    timer = 0;
                    reconnectState = 3;
                }
                break;

            case 3:
                mc.player.sendChatMessage("/an" + savedAnarchyId);
                timer = 0;
                reconnectState = 4;
                break;

            case 4:
                if (timer > 100) {
                    timer = 0;
                    reconnectState = 5;
                }
                break;

            case 5:
                mc.player.sendChatMessage("/ah");
                timer = 0;
                reconnectState = 0;
                break;
        }
    }

    @Native
    private void parseScoreboard() {
        try {
            if (mc.world.getScoreboard() != null) {
                Scoreboard sb = mc.world.getScoreboard();
                ScoreObjective obj = sb.getObjectiveInDisplaySlot(1);
                if (obj != null) {
                    String title = obj.getDisplayName().getString();
                    String cleanTitle = title.replaceAll("§.", "").trim();

                    Pattern pattern = Pattern.compile("Анархия-(\\d+)");
                    Matcher matcher = pattern.matcher(cleanTitle);

                    if (matcher.find()) {
                        String id = matcher.group(1);
                        if (!id.equals(savedAnarchyId)) {
                            savedAnarchyId = id;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}