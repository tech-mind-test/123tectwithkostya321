package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventKey;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;

public class ClickFriend extends Module {
    private final BindSetting key = new BindSetting("Кнопка взаимодействия");

    public ClickFriend() {
        super("ClickFriend", "Упрощает добавление друзей", Category.Miscellaneous);
        addSettings(key);
    }

    @EventTarget
    public void onEvent(EventKey event) {
        if (event.getKey() == key.get() && !event.isHold() && mc.pointedEntity instanceof PlayerEntity entity) {
            String entityName = entity.getGameProfile() != null ? entity.getGameProfile().getName() : entity.getName().getString();
            if (entityName == null || entityName.isEmpty() || entityName.equalsIgnoreCase("Protected")) {
                return;
            }

            if (SkyCore.getInstance().getFriendManager().isFriend(entityName)) {
                SkyCore.getInstance().getFriendManager().removeFriend(entityName);
                ChatUtil.addText(TextFormatting.RESET + entityName + " Удален из списка друзей!");
            } else {
                SkyCore.getInstance().getFriendManager().addFriend(entityName);
                ChatUtil.addText(TextFormatting.RESET + entityName + " Добавлен в список друзей!");
            }
        }
    }
}