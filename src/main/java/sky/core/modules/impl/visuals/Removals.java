package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventNoRender;
import sky.core.events.EventSound;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class Removals extends Module {
    public static MultiBooleanSetting mode =
            new MultiBooleanSetting("Применять на",
            new BooleanSetting("Тряска камеры", true),
            new BooleanSetting("Скорборд", true),
            new BooleanSetting("Удочка на экране", true),
            new BooleanSetting("Босс-бар", true),
            new BooleanSetting("Частицы разрушения", true),
            new BooleanSetting("Дождь", true),
            new BooleanSetting("Камера клип", true),
            new BooleanSetting("Тени", true),
            new BooleanSetting("Дым", true),
            new BooleanSetting("Снесение тотема", true),
            new BooleanSetting("Виньетка", true),
            new BooleanSetting("Стрелы в игроке", true),
            new BooleanSetting("Голограммы", true),
            new BooleanSetting("Эффект здоровья", true),
            new BooleanSetting("Трава", true),
            new BooleanSetting("Плохие эффекты", true),
                    new BooleanSetting("Свечение игроков", true),
                    new BooleanSetting("Игроки", false),
            new BooleanSetting("Размытие под водой", true),
            new BooleanSetting("Огонь", true),
            new BooleanSetting("Тайтлы", true));

    public static MultiBooleanSetting sound = new MultiBooleanSetting("Уменьшить звук", new BooleanSetting("Трезубец", false), new BooleanSetting("Появление визера", false), new BooleanSetting("Открытие энд-портала", false), new BooleanSetting("Музыкальные пластинки", false), new BooleanSetting("Битье пузырьков опыта", false));

    public static SliderSetting volume = new SliderSetting("Громкость", 0.5f, 0, 1, 0.05f, () -> sound.is("Трезубец") || sound.is("Появление визера") || sound.is("Открытие энд-портала") || sound.is("Музыкальные пластинки") || sound.is("Битье пузырьков опыта"));

    private boolean lastGrass = false;

    public Removals() {
        super("Removals", "Увеличивает FPS, убирая лишние визуальные эффекты", Category.Visuals);
        addSettings(mode);
    }

    @EventTarget
    private void onEvent(EventNoRender event) {
        boolean cancel = switch (event.norenderType) {
            case fire -> mode.is("Огонь");
            case bossbar -> mode.is("Босс-бар");
            case scoreboard -> mode.is("Скорборд");
            case title -> mode.is("Тайтлы");
            case totem -> mode.is("Снесение тотема");
            case hurttime -> mode.is("Тряска камеры");
            case cameraclip -> mode.is("Камера клип");
            case fishingrod -> mode.is("Удочка на экране");
            case destroyparticles -> mode.is("Частицы разрушения");
            case rain -> mode.is("Дождь");
            case shadows -> mode.is("Тени");
            case glowing -> mode.is("Свечение игроков");
            case smoke -> mode.is("Дым");
            case vignette -> mode.is("Виньетка");
            case arrows -> mode.is("Стрелы в игроке");
            case holograms -> mode.is("Голограммы");
            case grass -> mode.is("Трава");
            case health -> mode.is("Эффект здоровья");
            case players -> mode.is("Игроки");
            case bad_effects -> mode.is("Плохие эффекты");
            case underwater_blur -> mode.is("Размытие под водой");
            case container_background -> mode.is("Фон контейнера");
            case tab -> mode.is("Нанесение таба");
            case lava -> mode.is("Видимость лавы");
        };

        if (cancel) {
            event.setCancelled(true);
        }
    }


    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        if (mc.world != null) {
            mc.worldRenderer.loadRenderers();
        }
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        if (mc.world != null) {
            mc.worldRenderer.loadRenderers();
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        boolean grass = mode.is("Трава");
        if (grass != lastGrass) {
            lastGrass = grass;
            if (mc.world != null) {
                mc.worldRenderer.loadRenderers();
            }
        }
    }

    @EventTarget
    public void onSound(EventSound event) {
        String name = event.getISound().getSoundLocation().toString();

        boolean found = sound.is("Битье пузырьков опыта") && name.contains("minecraft:entity.experience_bottle.throw") || name.contains("minecraft:entity.experience_orb.pickup");
        if (sound.is("Трезубец") && name.contains("minecraft:item.trident.throw")) {
            found = true;
        }
        if (sound.is("Появление визера") && name.contains("minecraft:entity.wither.spawn")) {
            found = true;
        }
        if (sound.is("Открытие энд-портала") && name.contains("minecraft:block.end_portal.spawn")) {
            found = true;
        }
        if (sound.is("Музыкальные пластинки") && name.contains("minecraft:item.record.play")) {
            found = true;
        }

        if (found) {
            event.setFactor(volume.get());
        }
    }
}