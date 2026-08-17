package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.WaypointManager;
import sky.core.utils.Wrapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;

import java.util.List;

public class WaypointRenderHandler implements Wrapper {

    @EventTarget
    public void render(EventRender2D eventRender2D) {
        if (mc.player == null) return;

        String serverKey = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "singleplayer";
        List<WaypointManager.WaypointEntry> waypoints = SkyCore.getInstance().getWaypointManager().getWaypoints(serverKey);
        if (waypoints.isEmpty()) return;

        double px = mc.player.getPosX();
        double pz = mc.player.getPosZ();

        for (WaypointManager.WaypointEntry wp : waypoints) {
            Vector3d worldPos = new Vector3d(wp.getX(), wp.getY(), wp.getZ());
            Vector2f screen = ProjectUtil.project2D(worldPos);
            if (Float.isInfinite(screen.x) || Float.isInfinite(screen.y) || screen.x == Float.MAX_VALUE || screen.y == Float.MAX_VALUE)
                continue;

            String icon = "B";
            String label = wp.getName() + ": " + (int) Math.round(Math.hypot(px - wp.getX(), pz - wp.getZ())) + "m";

            float iconWidth = Fonts.waypoint_icons[36].getWidth(icon);
            float iconHeight = Fonts.waypoint_icons[36].getHeight();

            float drawX = screen.x - iconWidth / 2f;
            float drawY = screen.y - iconHeight / 2f;

            Fonts.waypoint_icons[36].drawOutlineString(eventRender2D.getStack(), icon, drawX, drawY, -1, false, true, false, true, ColorUtil.getColor(0, 0, 0, 255));

            float textWidth = Fonts.sf_semibold[15].getWidth(label);
            float textX = screen.x - textWidth / 2f;
            float textY = drawY + iconHeight + 2.0f + 3;
            Fonts.sf_semibold[15].drawOutlineString(eventRender2D.getStack(), label, textX, textY, -1, false, true, false, true, ColorUtil.getColor(0, 0, 0, 255));
        }
    }
}