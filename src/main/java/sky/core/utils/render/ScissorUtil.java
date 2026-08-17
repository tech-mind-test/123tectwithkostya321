package sky.core.utils.render;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static sky.core.utils.Wrapper.mw;

public class ScissorUtil {
    private static final List<Rectangle> rectStack = new ArrayList<>();
    private static Rectangle currentRect = null;

    public static void end() {
        GL11.glPopAttrib();
        if (!rectStack.isEmpty()) {
            rectStack.remove(rectStack.size() - 1);
            currentRect = rectStack.isEmpty() ? null : rectStack.get(rectStack.size() - 1);
        } else {
            currentRect = null;
        }
    }

    public static void start(double x, double y, double width, double height) {
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        rectStack.add(currentRect == null ? null : new Rectangle(currentRect));
        double scale = mw.getGuiScaleFactor();

        int sx = (int) (x * scale);
        int sy = (int) (y * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        int fbWidth = mw.getFramebufferWidth();
        int fbHeight = mw.getFramebufferHeight();
        int scissorY = fbHeight - sy - sh;

        Rectangle requested = new Rectangle(sx, scissorY, Math.max(0, sw), Math.max(0, sh));
        Rectangle framebuffer = new Rectangle(0, 0, fbWidth, fbHeight);

        Rectangle parent = currentRect != null ? currentRect : framebuffer;
        Rectangle result = requested.intersection(parent).intersection(framebuffer);
        if (result.width < 0) result.width = 0;
        if (result.height < 0) result.height = 0;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(result.x, result.y, result.width, result.height);
        if (!rectStack.isEmpty()) {
            rectStack.set(rectStack.size() - 1, new Rectangle(result));
        }
        currentRect = new Rectangle(result);
    }
}
