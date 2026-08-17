package sky.core.modules.impl.miscellaneous;

import sky.core.SkyCore;
import sky.core.utils.managers.impl.FriendManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.StringSetting;

public class NameProtect extends Module {
    private static final String DEFAULT_ALIAS = "Protected";
    public static StringSetting customNickname = new StringSetting("Protected");
    public static BooleanSetting protectfriends = new BooleanSetting("Скрывать друзей", false);

    public NameProtect() {
        super("Name Protect", "Скрывает ваш ник", Category.Miscellaneous);
        customNickname.set(DEFAULT_ALIAS);
        addSettings(customNickname, protectfriends);
    }

    public static String replaceName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String alias = getReplacementName();
        name = name.replace(mc.session.getUsername(), alias);

        if (protectfriends.get()) {
            for (FriendManager.FriendEntry friend : SkyCore.getInstance().getFriendManager().getFriends()) {
                name = name.replace(friend.getName(), alias);
            }
        }

        return name;
    }

    public static String getReplacementName() {
        String raw = customNickname.get();
        if (raw == null) return DEFAULT_ALIAS;
        String trim = raw.trim();
        return trim.isEmpty() ? DEFAULT_ALIAS : trim;
    }
}
