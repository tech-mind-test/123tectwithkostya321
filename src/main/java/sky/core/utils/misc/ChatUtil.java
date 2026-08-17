package sky.core.utils.misc;

import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.render.ColorUtil;
import net.minecraft.util.text.*;

public class ChatUtil implements Wrapper {
    public static final char COLOR_CODE = '§';

    public static void addText(final Object message, final Object... objects) {
        if (mc.player == null) return;
        if (message == null) {
            addText("Object is null");
            return;
        }

        final String prefix = "SkyCore Client ⇨";
        final int startColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        final int endColor = ColorUtil.darken(startColor, 0.5f);
        IFormattableTextComponent finalText = new StringTextComponent("");

        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            StringTextComponent letter = new StringTextComponent(String.valueOf(c));
            Style style;

            if (c == '⇨') {
                style = Style.EMPTY.setColor(Color.fromInt(0x888888));
            } else {
                float progress = (float) i / (prefix.length() - 1);
                int gradientColor = ColorUtil.interpolate(startColor, endColor, progress);
                style = Style.EMPTY.setColor(Color.fromInt(gradientColor & 0xFFFFFF)).applyFormatting(TextFormatting.BOLD);
            }

            letter.setStyle(style);
            finalText.append(letter);
        }

        finalText.append(new StringTextComponent(" "));

        if (message instanceof ITextComponent) {
            finalText.append((ITextComponent) message);
        } else {
            String msg = String.format(message.toString(), objects).replace('&', COLOR_CODE);
            String cleanMsg = msg.replaceAll("§[0-9a-fk-or]", "");
            StringTextComponent mainText = new StringTextComponent(cleanMsg);
            mainText.setStyle(Style.EMPTY.setColor(Color.fromInt(0xFFFFFF)));
            finalText.append(mainText);
        }

        mc.ingameGUI.getChatGUI().printChatMessage(finalText);
    }

    public static void addChatMessage(String message) {
        if (mc.player == null) return;

        final String prefix = "SkyCore Client ⇨";
        final int startColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        final int endColor = ColorUtil.darken(startColor, 0.5f);
        IFormattableTextComponent finalText = new StringTextComponent("");

        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            StringTextComponent letter = new StringTextComponent(String.valueOf(c));
            Style style;

            if (c == '⇨') {
                style = Style.EMPTY.setColor(Color.fromInt(0x888888));
            } else {
                float progress = (float) i / (prefix.length() - 1);
                int gradientColor = ColorUtil.interpolate(startColor, endColor, progress);
                style = Style.EMPTY.setColor(Color.fromInt(gradientColor & 0xFFFFFF)).applyFormatting(TextFormatting.BOLD);
            }

            letter.setStyle(style);
            finalText.append(letter);
        }

        finalText.append(new StringTextComponent(" "));

        String msg = message.replace('&', COLOR_CODE);
        StringTextComponent mainText = new StringTextComponent(msg);
        finalText.append(mainText);

        mc.ingameGUI.getChatGUI().printChatMessage(finalText);
    }
}