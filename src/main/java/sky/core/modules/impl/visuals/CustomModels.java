package sky.core.modules.impl.visuals;

import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;

public class CustomModels extends Module {

    public final ModeSetting models = new ModeSetting("Модель", "Crazy Rabbit", "Crazy Rabbit", "Freddy Bear", "White Demon", "Red Demon", "Amogus");
    public final BooleanSetting friends = new BooleanSetting("Применять на друзей", true);

    public CustomModels() {
        super("CustomModels", "Кастомные модели игрока", Category.Visuals);
        addSettings(models, friends);
    }
}
