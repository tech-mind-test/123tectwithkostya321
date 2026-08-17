package sky.core.modules.impl.visuals;

import lombok.Getter;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class TargetESP extends Module {
    @Getter
    private final ModeSetting mode = new ModeSetting(
            "Режим",
            "Кольцо",
            "Не отображать",
            "Кольцо",
            "Ромб",
            "Ромб-2",
            "Кристаллы",
            "Кристалики",
            "Кружочек",
            "Призраки",
            "Души",
            "Души 2",
            "Преследующие призраки",
            "Свинки"
    );
    @Getter
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", false, () -> mode.is("Кружочек"));
    @Getter
    private final BooleanSetting damageRed = new BooleanSetting("Краснение", true, () -> mode.is("Кружочек"));
    @Getter
    private final SliderSetting ghostCount = new SliderSetting("Количество", 3.0F, 2.0F, 5.0F, 1.0F, () -> mode.is("Преследующие призраки"));
    @Getter
    private final SliderSetting ghostLifeTime = new SliderSetting("Время жизни", 350.0F, 150.0F, 500.0F, 25.0F, () -> mode.is("Преследующие призраки"));
    @Getter
    private final SliderSetting strengthXZ = new SliderSetting("Скорость XZ", 2000.0F, 1000.0F, 5000.0F, 100.0F, () -> mode.is("Преследующие призраки"));
    @Getter
    private final SliderSetting strengthY = new SliderSetting("Скорость Y", 1700.0F, 1000.0F, 5000.0F, 100.0F, () -> mode.is("Преследующие призраки"));
    @Getter
    private final SliderSetting pigSpeed = new SliderSetting("Скорость свинок", 1.5F, 0.5F, 3.0F, 0.1F, () -> mode.is("Свинки"));
    @Getter
    private final SliderSetting crystalSize = new SliderSetting("Размер", 2.0F, 0.5F, 3.0F, 0.1F,
            () -> mode.is("Кристаллы") || mode.is("Кристалики") || mode.is("Кристалы"));

    public TargetESP() {
        super("TargetESP", "ESP цели из Attack Aura", Category.Visuals);
        addSettings(mode, throughWalls, damageRed, ghostCount, ghostLifeTime, strengthXZ, strengthY, pigSpeed, crystalSize);
    }
}

