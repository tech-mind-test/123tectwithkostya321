package sky.core.modules.impl.miscellaneous;

import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;

public class ChatHelper extends Module {

    public static BooleanSetting chatHistory = new BooleanSetting("История чата", true);
    public BooleanSetting antiSpam = new BooleanSetting("Анти Спам", true);


    public ChatHelper() {
        super("ChatHelper", "Помогает в чате", Category.Miscellaneous);
        addSettings(chatHistory, antiSpam);
    }
}
