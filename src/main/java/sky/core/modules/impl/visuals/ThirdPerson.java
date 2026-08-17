package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.settings.PointOfView;
import org.joml.Vector2f;
import sky.core.SkyCore;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.events.EventCancelThirdPerson;
import sky.core.events.EventKey;
import sky.core.events.EventThirdPersonDistance;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.LookHandler;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.movement.FreeCamera;

public class ThirdPerson extends Module {
    private ModeSetting povMode = new ModeSetting("Режим обзора", "Сзади", "Спереди", "Сзади");
    private SliderSetting distance = new SliderSetting("Дистанция", 4, 1, 10, 0.1F);
    public BindSetting keySetting = new BindSetting("Бинд");
    private PointOfView prevPointOfView = PointOfView.FIRST_PERSON;
    private boolean pressed;
    private boolean keyDown;
    private Vector2f rotation = new Vector2f(0.0F, 0.0F);


    public ThirdPerson() {
        super("Third Person", "Позволяет осмотреться от третьего лица без отображения другим игрокам", Category.Visuals);
        addSettings(povMode, keySetting, distance);
    }

    @EventTarget
    public void onKey(EventKey event) {
        if (SkyCore.getInstance().getModuleManager().getModule(FreeCamera.class).isEnabled()) return;
        if (event.getKey() == keySetting.get()) {
            keyDown = event.isHold();
        }
    }

    @EventTarget
    public void onKey(EventThirdPersonDistance eventThirdPersonRender) {
        if (pressed) eventThirdPersonRender.setDistance(distance.get());
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        if (!pressed) {
            prevPointOfView = mc.gameSettings.getPointOfView();
            rotation.x = LookHandler.getFreeYaw();
            rotation.y = LookHandler.getFreePitch();
        }
        if (keyDown && mc.currentScreen == null) {
            RotationComponent.update(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch), 360.0F, 1, 1);
            if (povMode.is("Сзади")) {
                mc.gameSettings.setPointOfView(PointOfView.THIRD_PERSON_BACK);
            } else {
                mc.gameSettings.setPointOfView(PointOfView.THIRD_PERSON_FRONT);
            }
            pressed = true;
        } else if (pressed) {
            LookHandler.setFreeYaw(rotation.x);
            LookHandler.setFreePitch(rotation.y);
            mc.gameSettings.setPointOfView(prevPointOfView);
            pressed = false;
        }
    }

    @EventTarget
    public void onEvent(EventCancelThirdPerson eventThirdDistance) {
        if (keyDown) eventThirdDistance.setCancelled(true);
    }
}
