package sky.core.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public class ColorUtil {

    public static void setManalColor(int color) {
        setAlphaManalColor(color, (float) (color >> 24 & 255) / 255.0F);
    }
    public static void setAlphaManalColor(final int color, final float alpha) {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        RenderSystem.color4f(red, green, blue, alpha);
    }
    public static int applyOpacity(int color, float alpha) {
        int alphaValue = Math.min(255, Math.max(0, (int) (alpha * 255)));
        return getColor(red(color), green(color), blue(color), alphaValue);
    }

    public static int applyOpacity(int color, int alpha) {
        return getColor(red(color), green(color), blue(color), alpha);
    }

    public static float[] getColor(int color) {
        return new float[]{red(color) / 255f, green(color) / 255f, blue(color) / 255f, alphaf(color)};
    }
    public static Color random() {
        return new Color(Color.HSBtoRGB((float)Math.random(), (float)((double)0.75F + Math.random() / (double)4.0F), (float)((double)0.75F + Math.random() / (double)4.0F)));
    }

    public static void glHexColor(int hex, int alpha) {
        float red = (float) (hex >> 16 & 255) / 255.0F;
        float green = (float) (hex >> 8 & 255) / 255.0F;
        float blue = (float) (hex & 255) / 255.0F;
        RenderSystem.color4f(red, green, blue, (float) alpha / 255);
    }
    public static int getColorWithDarkness(int color, float darkness) {
        float[] rgb = getRGBAf(color);
        return rgba((int) ((rgb[0] * 255.0f) / darkness), (int) ((rgb[1] * 255.0f) / darkness), (int) ((rgb[2] * 255.0f) / darkness), (int) (rgb[3] * 255.0f));
    }


    public static int rgb(int r, int g, int b) {
        return 255 << 24 | r << 16 | g << 8 | b;
    }
    public static class IntColor {

        public static float[] rgb(final int color) {
            return new float[]{
                    (color >> 16 & 0xFF) / 255f,
                    (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    (color >> 24 & 0xFF) / 255f
            };
        }

        public static int rgba(final int r,
                               final int g,
                               final int b,
                               final int a) {
            return a << 24 | r << 16 | g << 8 | b;
        }

        public static int getRed(final int hex) {
            return hex >> 16 & 255;
        }

        public static int getGreen(final int hex) {
            return hex >> 8 & 255;
        }

        public static int getBlue(final int hex) {
            return hex & 255;
        }

        public static int getAlpha(final int hex) {
            return hex >> 24 & 255;
        }
    }
    public static int gradient2(int speed, int index, int... colors) {
        if (colors == null || colors.length == 0) {
            return 0xFFFFFFFF;
        }

        if (colors.length == 1) {
            return colors[0];
        }

        if (speed <= 0) {
            speed = 1;
        }

        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;

        float angleNormalized = angle / 360f * colors.length;
        int colorIndex = (int) angleNormalized;

        if (colorIndex >= colors.length) {
            colorIndex = colors.length - 1;
        }

        if (colorIndex < 0) {
            colorIndex = 0;
        }

        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];

        float progress = angleNormalized - colorIndex;
        progress = MathHelper.clamp(progress, 0f, 1f);

        return interpolate(color1, color2, progress);
    }
    public static int getColorWithAlpha(int color, float alpha) {
        float[] rgb = getRGBAf(color);
        return rgba((int) (rgb[0] * 255.0f), (int) (rgb[1] * 255.0f), (int) (rgb[2] * 255.0f), (int) (alpha));
    }

    public int getColorWithAlpha(int color, double alpha) {
        float[] rgb = getRGBAf(color);
        return rgba((int) (rgb[0] * 255.0f), (int) (rgb[1] * 255.0f), (int) (rgb[2] * 255.0f), (int) (alpha));
    }
    public static float[] getRGBAf(int color) {
        return new float[]{(color >> 16 & 0xFF) / 255.0f, (color >> 8 & 0xFF) / 255.0f, (color & 0xFF) / 255.0f,
                (color >> 24 & 0xFF) / 255.0f};
    }

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static float[] rgba(final int color) {
        return new float[] {
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int gradientModule(int index) {
        return ColorUtil.gradient2(10, index, ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), darken(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 0.5f));
    }

    public static int gradientInterface(int index) {
        return ColorUtil.gradient2(10, index, ThemeEditor.getColor(ThemeSettings.LOGO), darken(ThemeEditor.getColor(ThemeSettings.LOGO), 0.3f));
    }
    public static int interpolate(int start, int end, float value) {
        float[] startColor = getColor(start);
        float[] endColor = getColor(end);
        return getColor((int) MathUtil.interpolate(startColor[0] * 255, endColor[0] * 255, value), (int) MathUtil.interpolate(startColor[1] * 255, endColor[1] * 255, value), (int) MathUtil.interpolate(startColor[2] * 255, endColor[2] * 255, value), (int) MathUtil.interpolate(startColor[3] * 255, endColor[3] * 255, value));
    }

    public static int darken(int color, float factor) {
        float[] rgb = getColor(color);
        float[] hsb = Color.RGBtoHSB((int) (rgb[0] * 255), (int) (rgb[1] * 255), (int) (rgb[2] * 255), null);

        hsb[2] *= factor;
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2]));

        int darkenedRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return applyOpacity(darkenedRGB, (int) (rgb[3] * 255));
    }

    public static int interpolateSuccessColor(int percentage) {
        int red = Color.GREEN.getRGB();
        int green = Color.RED.getRGB();
        float factor = MathHelper.clamp(percentage / 100.0f, 0.0f, 1.0f);
        return interpolate(red, green, factor);
    }
    public static int setAlpha(int color, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | color & 0xFFFFFF;
    }
    public static int hex(String hex) {
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);

        if (s.length() == 3 || s.length() == 4) {
            char r = s.charAt(0);
            char g = s.charAt(1);
            char b = s.charAt(2);
            char a = s.length() == 4 ? s.charAt(3) : 'F';
            s = ("" + r + r + g + g + b + b + a + a);
        }

        if (s.length() == 6) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return ColorUtil.getColor(r, g, b, 255);
        }
        if (s.length() == 8) {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            int a = Integer.parseInt(s.substring(6, 8), 16);
            return ColorUtil.getColor(r, g, b, a);
        }
        throw new IllegalArgumentException("Unsupported hex format: " + hex);
    }


    public static int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), alphaf(color) * percent01);
    }

    public static int getColor(int red, int green, int blue, float alpha) {
        return getColor(red, green, blue, (int) (alpha * 255.F));
    }

    public static int getColor(int r, int g, int b) {
        return getColor(r, g, b, 255);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        return computeColor(red, green, blue, alpha);
    }

    private static int computeColor(int red, int green, int blue, int alpha) {
        return ((MathHelper.clamp(alpha, 0, 255) << 24) | (MathHelper.clamp(red, 0, 255) << 16) | (MathHelper.clamp(green, 0, 255) << 8) | MathHelper.clamp(blue, 0, 255));
    }

    public static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static float alphaf(int c) {
        return alpha(c) / 255.0f;
    }

    public static int alpha(int c) {
        return (c >> 24) & 0xFF;
    }

    public static int fade(int speed, int index, int first, int second) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = angle >= 180 ? 360 - angle : angle;
        return overCol(first, second, angle / 180f);
    }

    public static int overCol(int color1, int color2, float percent01) {
        final float percent = MathHelper.clamp(percent01, 0F, 1F);
        return getColor(calc(red(color1), red(color2), percent), calc(green(color1), green(color2), percent), calc(blue(color1), blue(color2), percent), calc(alpha(color1), alpha(color2), percent));
    }

    public static int calc(int input, int target, double step) {
        return (int) (input + step * (target - input));
    }
}