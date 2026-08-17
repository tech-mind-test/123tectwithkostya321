package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.ITextComponent;
import sky.core.SkyCore;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.managers.impl.FriendManager;
import sky.core.utils.misc.ServerUtil;

import java.util.Arrays;

public class AutoAccept extends Module {
    private static final String[] TELEPORT_REQUESTS = {
            "телепортироваться",
            "has requested teleport",
            "просит к вам телепортироваться",
            "запрашивает телепорт к вам"
    };

    private final BooleanSetting onlyFriend = new BooleanSetting("Принимать запросы только от друзей", true);

    public AutoAccept() {
        super("AutoAccept", "Принимает запросы на телепортацию", Category.Miscellaneous);
        addSettings(onlyFriend);
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null || ServerUtil.isPvP() || !(event.getPacket() instanceof SChatPacket chatPacket)) {
            return;
        }

        String message = getRealChatText(chatPacket.getChatComponent()).toLowerCase();
        if (Arrays.stream(TELEPORT_REQUESTS).noneMatch(message::contains)) {
            return;
        }

        if (onlyFriend.get() && !isFriendRequest(message)) {
            return;
        }

        mc.player.sendChatMessage("/tpaccept");
    }

    /** Текст из пакета — реальные ники, без NameProtect. */
    private static String getRealChatText(ITextComponent component) {
        if (component == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendUnformatted(component, sb);
        return sb.toString();
    }

    private static void appendUnformatted(ITextComponent component, StringBuilder sb) {
        sb.append(component.getUnformattedComponentText());
        for (ITextComponent sibling : component.getSiblings()) {
            appendUnformatted(sibling, sb);
        }
    }

    private boolean isFriendRequest(String message) {
        for (FriendManager.FriendEntry friend : SkyCore.getInstance().getFriendManager().getFriends()) {
            String friendName = friend.getName();
            if (friendName != null && message.contains(friendName.toLowerCase())) {
                return true;
            }
        }

        NameProtect nameProtect = (NameProtect) SkyCore.getInstance().getModuleManager().getModule(NameProtect.class);
        if (nameProtect == null || !nameProtect.isEnabled() || !NameProtect.protectfriends.get()) {
            return false;
        }

        String alias = NameProtect.getReplacementName().toLowerCase();
        return !alias.isEmpty() && message.contains(alias);
    }
}
