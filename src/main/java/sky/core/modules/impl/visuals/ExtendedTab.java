package sky.core.modules.impl.visuals;

import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;


public class ExtendedTab extends Module {
    public static SliderSetting maxplayers = new SliderSetting("Максимальное кол-во игроков", 25, 20, 50, 5);
    public static SliderSetting maxcolumns = new SliderSetting("Максимальное кол-во колонок", 4, 3, 5, 1);

    public ExtendedTab() {
        super("BetterTab", "Увеличивает отображаемое кол-во игроков в табе", Category.Visuals);
        addSettings(maxplayers, maxcolumns);
    }
}
