package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.MathHelper;
import sky.core.cmd.impl.GpsCommand;
import sky.core.events.EventRender2D;
import sky.core.utils.Wrapper;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.util.ResourceLocation;

public class GpsRenderHandler implements Wrapper {

    @EventTarget
    public void render(EventRender2D eventRender2D) {
        if (!GpsCommand.gpsEnabled || mc.player == null) return;

        double diffX = GpsCommand.x - mc.getRenderManager().info.getProjectedView().getX();
        double diffZ = GpsCommand.z - mc.getRenderManager().info.getProjectedView().getZ();

        float yaw = mc.player.rotationYaw;
        double cos = MathHelper.cos((float) (yaw * 0.017453292519943295));
        double sin = MathHelper.sin((float) (yaw * 0.017453292519943295));

        double rotY = -(diffZ * cos - diffX * sin);
        double rotX = -(diffX * cos + diffZ * sin);

        float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);
        double dst = Math.sqrt(Math.pow(GpsCommand.x - mc.player.getPosX(), 2.0) + Math.pow(GpsCommand.z - mc.player.getPosZ(), 2.0));

        float radius = 55.0f;
        float centerX = mc.getMainWindow().getScaledWidth() / 2.0f;
        float centerY = mc.getMainWindow().getScaledHeight() / 2.0f;

        float drawX = centerX;
        float drawY = centerY - radius - 20;

        float arrowSize = 52.0f;

        RenderSystem.pushMatrix();
        RenderSystem.translated((double) drawX, (double) drawY, 0.0);
        RenderSystem.rotatef(angle, 0.0f, 0.0f, 1.0f);

        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/world_render/arrow_gps.png"),
                -arrowSize / 2f, -arrowSize / 2f, arrowSize, arrowSize, -1);
        RenderSystem.popMatrix();

        String text = (int) dst + "m";
        float textWidth = Fonts.sfregular[13].getWidth(text);

        Fonts.sfregular[13].drawString(eventRender2D.getStack(), text,
                drawX - textWidth / 2f + 1, drawY - 15f, -1);
    }
}