package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.adl.nativeprotect.Native;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.utils.math.TimeUtil;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.TextFormatting;
import sky.core.SkyCore;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoDuel extends Module {
    private static final Pattern pattern = Pattern.compile("^\\w{3,16}$");

    private final MultiBooleanSetting mode = new MultiBooleanSetting("Кит",
            new BooleanSetting("Щит", true),
            new BooleanSetting("Шипы 3", true),
            new BooleanSetting("Лук", true),
            new BooleanSetting("Тотемы", true),
            new BooleanSetting("Исцеление", true),
            new BooleanSetting("Шары", true),
            new BooleanSetting("Классик", true),
            new BooleanSetting("Читерский рай", true),
            new BooleanSetting("Незерка", true)
    );
    private final BooleanSetting noFriends = new BooleanSetting("Пропускать друзей", true);

    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private final List<String> sent = Lists.newArrayList();
    private final TimeUtil counter = new TimeUtil();
    private final TimeUtil counter2 = new TimeUtil();
    private final TimeUtil counterChoice = new TimeUtil();
    private final TimeUtil counterTo = new TimeUtil();
    private final HashMap<String, Long> duelTimestamps = new HashMap<>();

    public AutoDuel() {
        super("Auto Duel", "Автоматически вызывает игрока на дуэль", Category.Miscellaneous);
        addSettings(mode, noFriends);
    }

    @Native
    @EventTarget
    public void update(EventUpdate e) {
        List<String> players = getOnlinePlayers();

        double distance = Math.sqrt(
                Math.pow(this.lastPosX - mc.player.getPosX(), 2.0D) +
                        Math.pow(this.lastPosY - mc.player.getPosY(), 2.0D) +
                        Math.pow(this.lastPosZ - mc.player.getPosZ(), 2.0D)
        );

        if (distance > 500.0D) {
            this.toggle();
        }

        this.lastPosX = mc.player.getPosX();
        this.lastPosY = mc.player.getPosY();
        this.lastPosZ = mc.player.getPosZ();

        if (counter2.hasTimeElapsed(80L * players.size())) {
            sent.clear();
            counter2.reset();
        }

        for (String player : players) {
            if (this.sent.contains(player)) continue;
            if (player.equals(mc.session.getProfile().getName())) continue;
            if (!counter.hasTimeElapsed(600L)) continue;
            if (this.noFriends.get() && SkyCore.getInstance().getFriendManager().isFriend(player)) continue;

            if (this.duelTimestamps.containsKey(player)) {
                long lastDuelTime = this.duelTimestamps.get(player);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastDuelTime < 31000L) continue;
            }

            mc.player.sendChatMessage("/duel " + player);
            this.sent.add(player);
            this.duelTimestamps.put(player, System.currentTimeMillis());
            this.counter.reset();
        }

        Container container = mc.player.openContainer;
        if (container instanceof ChestContainer chest) {
            if (mc.currentScreen != null && mc.currentScreen.getTitle().getString().contains("Выбор набора (1/1)")) {
                List<Integer> slotsID = new ArrayList<>();
                int index = 0;

                for (int i = 0; i < mode.get().size(); i++) {
                    if (!mode.getIndex(i).get()) {
                        index++;
                    } else {
                        slotsID.add(index);
                        index++;
                    }
                }

                Collections.shuffle(slotsID);
                if (!slotsID.isEmpty() && counterChoice.hasTimeElapsed(80L)) {
                    int slotID = slotsID.get(0);
                    mc.playerController.windowClick(chest.windowId, slotID, 0, ClickType.QUICK_MOVE, mc.player);
                    counterChoice.reset();
                }
            } else if (mc.currentScreen != null && mc.currentScreen.getTitle().getString().contains("Настройка поединка") && counterTo.hasTimeElapsed(80L)) {
                mc.playerController.windowClick(chest.windowId, 0, 0, ClickType.QUICK_MOVE, mc.player);
                counterTo.reset();
            }
        }
    }

    @Native
    @EventTarget
    public void send(EventPacket event) {
        if (!event.isReceive()) return;
        if (event.getPacket() instanceof SChatPacket chat) {
            String text = TextFormatting.getTextWithoutFormattingCodes(chat.getChatComponent().getString()).toLowerCase();
            if ((text.contains("начало") && text.contains("через") && text.contains("секунд!")) ||
                    text.equals("дуэли » во время поединка запрещено использовать команды")) {
                toggle();
            }
        }
    }

    private List<String> getOnlinePlayers() {
        return mc.player.connection.getPlayerInfoMap().stream()
                .map(NetworkPlayerInfo::getGameProfile)
                .map(GameProfile::getName)
                .filter(profileName -> pattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }
}