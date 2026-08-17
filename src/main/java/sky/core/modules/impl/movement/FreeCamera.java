package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.*;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.glfw.GLFW;

public class FreeCamera extends Module {
    private final SliderSetting speedXZ = new SliderSetting("Скорость XZ", 1.5F, 0.1F, 5.0F, 0.1F);
    private final SliderSetting speedY = new SliderSetting("Скорость Y", 1.0F, 0.1F, 5.0F, 0.1F);

    private Vector3d cameraPosition;
    private Vector3d prevCameraPosition;
    private Vector2f rotation;
    private float inputForward, inputStrafe;
    private boolean inputJump, inputSneak;
    private PointOfView prevPointOfView = PointOfView.FIRST_PERSON;

    public FreeCamera() {
        super("Free Camera", "Позволяет перемещаться в свободной камере", Category.Movement);
        addSettings(speedXZ, speedY);
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        cameraPosition = mc.player.getEyePosition(mc.getRenderPartialTicks());
        prevCameraPosition = cameraPosition;
        prevPointOfView = mc.gameSettings.getPointOfView();
    }

    public Vector3d getInterpolatedCameraPosition(float partialTicks) {
        if (cameraPosition == null) return null;
        if (prevCameraPosition == null) return cameraPosition;
        double x = prevCameraPosition.x + (cameraPosition.x - prevCameraPosition.x) * partialTicks;
        double y = prevCameraPosition.y + (cameraPosition.y - prevCameraPosition.y) * partialTicks;
        double z = prevCameraPosition.z + (cameraPosition.z - prevCameraPosition.z) * partialTicks;
        return new Vector3d(x, y, z);
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        cameraPosition = null;
        prevCameraPosition = null;
        inputForward = inputStrafe = 0.0F;
        inputJump = inputSneak = false;
        mc.gameSettings.setPointOfView(prevPointOfView);
    }

    @EventTarget
    public void onEvent(EventSwapWorld eventSwapWorld) {
        toggle();
    }

    @EventTarget
    public void onEvent(EventThirdPersonRender eventThirdDistance) {
        eventThirdDistance.setThirdperson(true);
    }

    @EventTarget
    public void onEvent(EventCancelThirdPerson eventThirdDistance) {
        eventThirdDistance.setCancelled(true);
    }

    @EventTarget
    public void onEvent(EventThirdPersonDistance eventThirdDistance) {
        eventThirdDistance.setDistance(0);
    }

    @EventTarget
    public void onEvent(EventRotation eventRotation) {
        rotation = eventRotation.getRotation();
        Vector3d renderPos = prevCameraPosition == null
                ? cameraPosition
                : prevCameraPosition.add(cameraPosition.subtract(prevCameraPosition).scale(mc.getRenderPartialTicks()));
        eventRotation.setPosition(renderPos);
        eventRotation.setCancelled(true);
    }


    @EventTarget
    public void onEvent(EventUpdate event) {
        prevCameraPosition = cameraPosition;
        mc.gameSettings.setPointOfView(PointOfView.THIRD_PERSON_BACK);
        if (inputForward != 0.0F || inputStrafe != 0.0F || inputJump || inputSneak) {
            double speedHorizontal = speedXZ.get();
            double speedVertical = speedY.get();

            double yawRad = Math.toRadians(rotation.x);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);

            double dx = (inputForward * -sin + inputStrafe * cos) * speedHorizontal;
            double dz = (inputForward * cos + inputStrafe * sin) * speedHorizontal;
            double dy = (inputJump ? speedVertical : 0.0D) - (inputSneak ? speedVertical : 0.0D);

            cameraPosition = cameraPosition.add(dx, dy, dz);
        }
    }

    @EventTarget
    public void onEvent(EventInput eventInput) {
        if (cameraPosition == null) return;

        inputForward = eventInput.getForward();
        inputStrafe = eventInput.getStrafe();
        inputJump = eventInput.isJump();
        inputSneak = eventInput.isSneak();

        long windowHandle = mc.getMainWindow().getHandle();
        boolean up = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean down = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS;

        float playerForward = 0.0F;
        if (up) playerForward += 1.0F;
        if (down) playerForward -= 1.0F;

        float playerStrafe = 0.0F;
        if (left) playerStrafe += 1.0F;
        if (right) playerStrafe -= 1.0F;

        eventInput.setForward(playerForward);
        eventInput.setStrafe(playerStrafe);
        eventInput.setJump(false);
        eventInput.setSneak(false);
    }
}