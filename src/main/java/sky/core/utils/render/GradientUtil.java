package sky.core.utils.render;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.Color;


public final class GradientUtil {

    public static IFormattableTextComponent gradient(String text, int startColor, int endColor) {
        IFormattableTextComponent component = new StringTextComponent("");
        if (text == null || text.isEmpty()) {
            return component;
        }
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            float ratio = length == 1 ? 0.0f : (float) i / (length - 1);
            int color = ColorUtil.interpolate(startColor, endColor, ratio);
            IFormattableTextComponent letter = new StringTextComponent(String.valueOf(c));
            letter.setStyle(Style.EMPTY.setColor(Color.fromInt(color)));
            component.append(letter);
        }
        return component;
    }

    public static IFormattableTextComponent gradient(String text, int startColor, int endColor, int speed) {
        IFormattableTextComponent component = new StringTextComponent("");
        if (text == null || text.isEmpty()) {
            return component;
        }
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            int color = ColorUtil.gradient2(speed, i, startColor, endColor);
            IFormattableTextComponent letter = new StringTextComponent(String.valueOf(c));
            letter.setStyle(Style.EMPTY.setColor(Color.fromInt(color)));
            component.append(letter);
        }
        return component;
    }

    public static IFormattableTextComponent gradient(String text, int startColor, int endColor, int speed, float ratio) {
        IFormattableTextComponent component = new StringTextComponent("");
        if (text == null || text.isEmpty()) {
            return component;
        }
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            int index = (int) (i * ratio);
            int color = ColorUtil.gradient2(speed, index, startColor, endColor);
            IFormattableTextComponent letter = new StringTextComponent(String.valueOf(c));
            letter.setStyle(Style.EMPTY.setColor(Color.fromInt(color)));
            component.append(letter);
        }
        return component;
    }
} 