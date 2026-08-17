package sky.core.utils.render.font;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.utils.Wrapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.shader.ShaderUtil;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

public final class StyledFont extends AbstractFont implements Wrapper {
    private final float spacing;
    private static final int[] ATLAS_SIZES = {256, 512, 1024, 2048};
    private static final int[] CODES = {31, 256, 1024, 1106, 0x2600, 0x2700, 0x25A0, 0x2600};
    private static final char[] CHARS;

    static {
        int len = (CODES[1] - CODES[0]) + (CODES[3] - CODES[2]) + (CODES[5] - CODES[4]) + (CODES[7] - CODES[6]);
        CHARS = new char[len];
        int idx = 0;
        for (int i = CODES[0]; i < CODES[1]; i++) {
            CHARS[idx++] = (char) i;
        }
        for (int i = CODES[2]; i < CODES[3]; i++) {
            CHARS[idx++] = (char) i;
        }
        for (int i = CODES[4]; i < CODES[5]; i++) {
            CHARS[idx++] = (char) i;
        }
        for (int i = CODES[6]; i < CODES[7]; i++) {
            CHARS[idx++] = (char) i;
        }
    }

    public float drawString(MatrixStack matrixStack, String text, double x, double y, int color) {
        return renderString(matrixStack, this, text, x, y, color);
    }

    public float drawText(MatrixStack matrixStack, ITextComponent component, double x, double y, int color) {
        return renderText(matrixStack, component, x, y, color);
    }

    public void drawOutlineString(MatrixStack stack, String text, double x, double y, int color, boolean left, boolean right, boolean up, boolean down, int outline_color) {
        if (left) renderString(stack, this, text, x - 0.5, y, outline_color);
        if (right) renderString(stack, this, text, x + 0.5, y, outline_color);
        if (up) renderString(stack, this, text, x, y - 0.5f, outline_color);
        if (down) renderString(stack, this, text, x, y + 0.5f, outline_color);

        drawString(stack, text, x, y, color);
    }

    public void drawSubString(MatrixStack matrixStack, String text, float x, float y, int color, float maxWidth) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderUtil.substring.attach();
        ShaderUtil.substring.setUniformf("inColor", (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, (color >> 24 & 255) / 255F);
        ShaderUtil.substring.setUniform("width", maxWidth);
        ShaderUtil.substring.setUniform("maxWidth", (x + maxWidth) * 2);

        drawString(matrixStack, text, x, y, color);

        ShaderUtil.substring.detach();

        RenderSystem.disableBlend();
    }

    public static class TextScrollState {
        public boolean isScrolling = false;
        public long scrollStartTime = 0L;
    }

    public void drawScrolledString(MatrixStack matrixStack, String text, float x, float y, float maxWidth, int color, boolean isHovered, TextScrollState scrollState) {
        float textWidth = getWidth(text);
        float fontHeight = getHeight();

        ScissorUtil.start(x, y - 1, maxWidth, fontHeight + 3);

        long currentTime = System.currentTimeMillis();

        float speed = 40f;
        float loopWidth = textWidth + 10;
        float scrollDuration = (loopWidth / speed) * 1000f;
        float pauseDuration = 600f;
        float totalCycle = scrollDuration + pauseDuration;

        float scrollOffset = 0;
        boolean shouldScroll = textWidth > maxWidth;

        if (shouldScroll) {
            if (isHovered && !scrollState.isScrolling) {
                scrollState.scrollStartTime = currentTime;
                scrollState.isScrolling = true;
            }

            if (scrollState.isScrolling) {
                long elapsed = currentTime - scrollState.scrollStartTime;

                if (elapsed < totalCycle) {
                    if (elapsed < scrollDuration) {
                        scrollOffset = (elapsed / 1000f) * speed;
                    } else {
                        scrollOffset = 0;
                    }
                } else {
                    scrollState.isScrolling = false;
                    scrollOffset = 0;
                }
            }
        }

        drawString(matrixStack, text, x - scrollOffset, y, color);

        if (shouldScroll && scrollOffset > 0) {
            drawString(matrixStack, text, x - scrollOffset + textWidth + 10, y, color);
        }

        ScissorUtil.end();
    }

    private static float renderString(MatrixStack matrices, StyledFont font, String text, double x, double y, int color) {
        y -= 3;
        GL11.glColor4f(1, 1, 1, 1);
        float startPos = (float) Math.round(x * 2.0);
        float posX = startPos;
        float posY = (float) Math.round(y * 2.0);
        float[] rgb = ColorUtil.getColor(color);
        float red = rgb[0];
        float green = rgb[1];
        float blue = rgb[2];
        float alpha = rgb[3];
        matrices.push();
        matrices.scale(0.5f, 0.5f, 1f);
        Matrix4f matrix = matrices.getLast().getMatrix();
        font.beginFontBatch(matrix);
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c0 = text.charAt(i);
            posX += font.renderGlyph(matrix, c0, posX, posY, red, green, blue, alpha);
        }
        matrices.pop();
        font.endFontBatch();
        return (posX - startPos) / 2.0f;
    }

    public float renderText(MatrixStack matrixStack, ITextComponent component, double x, double y, int defaultColor) {
        y -= 3;
        GL11.glColor4f(1, 1, 1, 1);
        float startPos = (float) x * 2.0f;
        final float[] posX = new float[]{startPos};
        float posY = (float) y * 2.0f;

        matrixStack.push();
        matrixStack.scale(0.5f, 0.5f, 1f);
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        beginFontBatch(matrix);

        IReorderingProcessor processor = component.func_241878_f();
        processor.accept((index, style, codePoint) -> {
            net.minecraft.util.text.Color styleColor = style.getColor();
            int color = defaultColor;
            if (styleColor != null) {
                int rgb = styleColor.getColor();
                int alpha = (defaultColor >> 24) & 0xFF;
                color = (alpha << 24) | (rgb & 0x00FFFFFF);
            }

            float[] rgba = ColorUtil.getColor(color);

            String replacement = ReplaceSymbols.replaceCodePoint(codePoint);
            if (replacement != null) {
                if (!replacement.isEmpty()) {
                    for (int j = 0; j < replacement.length(); j++) {
                        char ch = replacement.charAt(j);
                        int replacementColor = ReplaceSymbols.getGradientColorForReplacement(codePoint, j, replacement.length(), rgba[3], color);
                        float[] replacementRgba = ColorUtil.getColor(replacementColor);
                        posX[0] += this.renderGlyph(matrix, ch, posX[0], posY, replacementRgba[0], replacementRgba[1], replacementRgba[2], replacementRgba[3]);
                    }
                }
                return true;
            }

            char[] chars = Character.toChars(codePoint);
            for (char ch : chars) {
                posX[0] += this.renderGlyph(matrix, ch, posX[0], posY, rgba[0], rgba[1], rgba[2], rgba[3]);
            }
            return true;
        });

        matrixStack.pop();
        endFontBatch();
        return (posX[0] - startPos) / 2.0f;
    }

    public StyledFont(String fileName, int size, float spacing, boolean antialiasing) {
        Font font = AbstractFont.getFont(fileName, Font.PLAIN, size);
        this.fontName = font.getFontName(Locale.ENGLISH);
        this.spacing = spacing;
        this.antialiasing = antialiasing;

        int atlasSize = chooseAtlasSize(font);
        this.imgWidth = atlasSize;
        this.imgHeight = atlasSize;

        BufferedImage image = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = setupGraphics(image, font);

        FontMetrics fontMetrics = graphics.getFontMetrics();
        this.fontHeight = fontMetrics.getHeight() / 2.0f;

        int x = 0;
        int y = 0;
        int rowHeight = 0;
        final int padding = 2;

        for (char character : CHARS) {
            Rectangle2D sizeRect = fontMetrics.getStringBounds(String.valueOf(character), graphics);
            int width = sizeRect.getBounds().width;
            int height = sizeRect.getBounds().height;

            int cellW = width + padding * 2;
            int cellH = height + padding * 2;

            if (x + cellW >= atlasSize) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }

            if (y + cellH >= atlasSize) {
                continue;
            }

            Glyph glyph = new Glyph();
            glyph.width = width;
            glyph.height = height;
            glyph.x = x + padding;
            glyph.y = y + padding;

            graphics.drawString(String.valueOf(character), x + padding, y + padding + fontMetrics.getAscent());
            this.glyphs.put(character, glyph);

            x += cellW;
            rowHeight = Math.max(rowHeight, cellH);
        }

        final BufferedImage finalImage = image;
        graphics.dispose();
        if (RenderSystem.isOnRenderThread()) {
            setTexture(finalImage);
        } else {
            RenderSystem.recordRenderCall(() -> setTexture(finalImage));
        }
    }

    private int chooseAtlasSize(Font font) {
        for (int atlasSize : ATLAS_SIZES) {
            if (canPackAllGlyphs(font, atlasSize)) {
                return atlasSize;
            }
        }
        return ATLAS_SIZES[ATLAS_SIZES.length - 1];
    }

    private boolean canPackAllGlyphs(Font font, int atlasSize) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(font);
        if (antialiasing) {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        FontMetrics fontMetrics = graphics.getFontMetrics();

        int x = 0;
        int y = 0;
        int rowHeight = 0;
        final int padding = 2;

        for (char character : CHARS) {
            Rectangle2D sizeRect = fontMetrics.getStringBounds(String.valueOf(character), graphics);
            int width = sizeRect.getBounds().width;
            int height = sizeRect.getBounds().height;
            int cellW = width + padding * 2;
            int cellH = height + padding * 2;

            if (x + cellW >= atlasSize) {
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }

            if (y + cellH >= atlasSize) {
                graphics.dispose();
                return false;
            }

            x += cellW;
            rowHeight = Math.max(rowHeight, cellH);
        }

        graphics.dispose();
        return true;
    }

    @Override
    public float getSpacing() {
        return spacing;
    }

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        float width = 0.0f;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);

            String replacement = ReplaceSymbols.replaceCodePoint(codePoint);
            if (replacement != null) {
                if (!replacement.isEmpty()) {
                    for (int j = 0; j < replacement.length(); j++) {
                        char ch = replacement.charAt(j);
                        width += getWidth(ch) + getSpacing();
                    }
                }
                continue;
            }

            char[] chars = Character.toChars(codePoint);
            for (char ch : chars) {
                width += getWidth(ch) + getSpacing();
            }
        }

        return Math.round(width - getSpacing()) / 2.0f;
    }

    public float getHeight() {
        return Math.round(getFontHeight()) / 2.0f;
    }
}
