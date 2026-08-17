package sky.core.modules;

import com.darkmagician6.eventapi.EventManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.impl.miscellaneous.ToggleSounds;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.misc.SoundUtil;

import java.util.List;

@Getter
@Setter
public abstract class Module implements Wrapper {
    String name;
    String description;
    Category category;

    int bind;
    boolean enabled;

    private ToggleMode toggleMode = ToggleMode.TOGGLE;
    private boolean keybindvisible = true;

    List<Setting<?>> settings = new ObjectArrayList<>();
    private final AnimationUtil animation = new AnimationUtil(0.0f, 8f, Easings.LINEAR);

    @Getter
    @Setter
    private static boolean suppressToggleEffects = false;

    public enum ToggleMode {
        TOGGLE, HOLD
    }

    public Module(String name, Category category) {
        this(name, "NULL", category);
    }

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.bind = -100;
    }

    public void addSettings(Setting<?>... settings) {
        this.settings.addAll(List.of(settings));
    }

    public void onEnable() {
        animation.update(1f);
        if (!suppressToggleEffects && keybindvisible) {
            SoundUtil.playSound(ToggleSounds.getSoundFile(true));
            if (NotificationRender.module.get()) {
                if (sky.core.modules.impl.visuals.Interface.isOldHud()) {
                    NotificationManager.addNotification("J", "" + name + " enabled!", -1);
                } else {
                    String categoryIcon = NotificationRender.getCategoryIcon(this);
                    NotificationManager.addNotification(categoryIcon, "Module " + name + " enabled!", -1);
                }
            }
        }
        EventManager.register(this);
    }

    public void onDisable() {
        animation.update(0f);
        if (!suppressToggleEffects && keybindvisible) {
            SoundUtil.playSound(ToggleSounds.getSoundFile(false));
            if (NotificationRender.module.get()) {
                if (sky.core.modules.impl.visuals.Interface.isOldHud()) {
                    NotificationManager.addNotification("K", "" + name + " disabled!", -1);
                } else {
                    String categoryIcon = NotificationRender.getCategoryIcon(this);
                    NotificationManager.addNotification(categoryIcon, "Module " + name + " disabled!", -1);
                }
            }
        }
        EventManager.unregister(this);
    }

    public final void toggle() {
        settoggled(!enabled);
    }

    public final void settoggled(boolean toggle) {
        if (enabled == toggle) {
            return;
        }
        enabled = toggle;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }
}