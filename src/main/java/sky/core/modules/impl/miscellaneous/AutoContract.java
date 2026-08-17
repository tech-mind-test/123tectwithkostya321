package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.cmd.impl.AutoContractCommand;
import sky.core.events.EventKey;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.ReallyWorldJoinHandler;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.misc.ServerUtil;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.util.text.TextFormatting;

public class AutoContract extends Module {
    private final SliderSetting griefSelection = new SliderSetting("Гриф", 1, 1, 54, 1);
    private final BindSetting disable = new BindSetting("Клавиша отключения");
    private final TimeUtil delayedRejoinTimer = new TimeUtil();
    private boolean pendingRejoin = false;
    boolean join = false;

    public AutoContract() {
        super("AutoContract", "Автоматически ищет цель на ReallyWorld", Category.Miscellaneous);
        addSettings(griefSelection, disable);
    }
    @Native
    @Override
    public void onEnable() {
        if (AutoContractCommand.targetNickname != null) {
            ReallyWorldJoinHandler.setManualGrief(griefSelection.get().intValue());
            ReallyWorldJoinHandler.startRejoin();
        }

        super.onEnable();
    }

    @Native
    @EventTarget
    public void receive(EventPacket event) {
        if (ServerUtil.isPvP()) {
            ChatUtil.addText("Вы находитесь в PVP-режиме!");
            toggle();
        }

        if (AutoContractCommand.targetNickname == null) {
            ChatUtil.addText("Укажите цель! .autocontract <name> | .act <name>");
            toggle();
        }

        if (event.getPacket() instanceof SJoinGamePacket) {
            join = true;
        }

        if (event.getPacket() instanceof SChatPacket packet) {
            String message = TextFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getString());
            if (message.contains("Ваша цель") && message.contains(AutoContractCommand.targetNickname)) {
                ChatUtil.addText("Найден нужный игрок");
                toggle();
            }

            if (message.contains("Ваша цель") || message.contains("Не спамь!") || message.contains("Не получилось")) {
                join = false;
                pendingRejoin = true;
                delayedRejoinTimer.reset();
            }
        }

        if (event.getPacket() instanceof SChatPacket packet) {
            String message = TextFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getString());
            if (message.contains("К сожалению сервер переполнен") || message.contains("Подождите 20 секунд!") || message.contains("большой поток игроков")) {
                pendingRejoin = true;
                delayedRejoinTimer.reset();
            }
        }
    }
    @Native
    @EventTarget
    public void update(EventUpdate event) {
        if (pendingRejoin && delayedRejoinTimer.hasTimeElapsed(600)) {
            ReallyWorldJoinHandler.setManualGrief(griefSelection.get().intValue());
            ReallyWorldJoinHandler.startRejoin();
            pendingRejoin = false;
        }
        boolean check = true;

        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equals(AutoContractCommand.targetNickname)) {
                check = false;
                break;
            }
        }

        if (join && !check) {
            mc.player.sendChatMessage("/contract get");
            join = false;
        }
    }

    @EventTarget
    public void onEvent(EventKey event) {
        if (disable.get() == event.getKey() && event.isHold()) {
            toggle();
        }
    }
}