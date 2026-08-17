package sky.core.utils.render.font;

import com.mojang.blaze3d.platform.GlStateManager;
import sky.core.utils.Wrapper;
import lombok.Getter;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public abstract class AbstractFont implements Wrapper {
    protected final Map<Character, Glyph> glyphs = new HashMap<>();
    protected int texId, imgWidth, imgHeight;
    protected float fontHeight;
    protected String fontName;
    protected boolean antialiasing;

    // Font batching state
    private boolean fontBatchActive = false;
    private Matrix4f batchMatrix = null;

    public abstract float getSpacing();

    protected final void setTexture(BufferedImage img) {
        int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);
        try {
            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            buffer.flip();
        } catch (BufferOverflowException | ReadOnlyBufferException ex) {
            texId = -1;
            return;
        }
        int textureID = GlStateManager.genTexture();
        GlStateManager.bindTexture(textureID);
        GlStateManager.texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GlStateManager.texParameter(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, img.getWidth(), img.getHeight(), 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, buffer);
        GlStateManager.bindTexture(0);
        texId = textureID;
    }

    public final void bindTex() {
        GlStateManager.bindTexture(texId);
    }

    public final void unbindTex() {
        GlStateManager.bindTexture(0);
    }

    public static Font getFont(String fileName, int style, int size) {
        String path = Fonts.FONT_DIR.concat(fileName);
        Font font = null;

        try {
            font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Fonts.class.getResourceAsStream(path))).deriveFont(style, size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        return font;
    }

    public final Graphics2D setupGraphics(BufferedImage img, Font font) {
        Graphics2D graphics = img.createGraphics();

        graphics.setFont(font);
        graphics.setColor(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, imgWidth, imgHeight);
        graphics.setColor(Color.WHITE);
        if (antialiasing) {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        return graphics;
    }

    public float renderGlyph(Matrix4f matrix, char c, float x, float y, float red, float green, float blue, float alpha) {
        Glyph glyph = glyphs.get(c);
        if (glyph == null) return 0;
        float pageX = glyph.x / (float) imgWidth;
        float pageY = glyph.y / (float) imgHeight;
        float pageWidth = glyph.width / (float) imgWidth;
        float pageHeight = glyph.height / (float) imgHeight;
        float width = glyph.width;
        float height = glyph.height;

        Matrix4f usedMatrix = fontBatchActive && batchMatrix != null ? batchMatrix : matrix;

        if (!fontBatchActive) {
            BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        }
        BUFFER.pos(usedMatrix, x, y + height, 0).color(red, green, blue, alpha).tex(pageX, pageY + pageHeight).endVertex();
        BUFFER.pos(usedMatrix, x + width, y + height, 0).color(red, green, blue, alpha).tex(pageX + pageWidth, pageY + pageHeight).endVertex();
        BUFFER.pos(usedMatrix, x + width, y, 0).color(red, green, blue, alpha).tex(pageX + pageWidth, pageY).endVertex();
        BUFFER.pos(usedMatrix, x, y, 0).color(red, green, blue, alpha).tex(pageX, pageY).endVertex();
        if (!fontBatchActive) {
            TESSELLATOR.draw();
        }

        return width + getSpacing();
    }

    public void beginFontBatch(Matrix4f matrix) {
        if (fontBatchActive) {
            endFontBatch();
        }
        fontBatchActive = true;
        batchMatrix = matrix;
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        bindTex();
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
    }

    public void endFontBatch() {
        if (!fontBatchActive) return;
        TESSELLATOR.draw();
        unbindTex();
        GlStateManager.disableBlend();
        fontBatchActive = false;
        batchMatrix = null;
    }

    public float getWidth(char ch) {
        Glyph glyph = glyphs.get(ch);
        return (glyph != null) ? glyph.width : 0;
    }

    public static class Glyph {
        public int x;
        public int y;
        public int width;
        public int height;
    }
}