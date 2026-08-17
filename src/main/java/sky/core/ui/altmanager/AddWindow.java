package sky.core.ui.altmanager;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import com.adl.nativeprotect.Native;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.util.Random;

public class AddWindow extends Screen {
    private static final String[] RANDOM_NAMES = {
            "leoaer", "rixando", "lucarde", "hengiro", "bevunho", "thionme",
            "andmurgo", "guivuson", "reilherpe", "ferano", "Alemurer", "cloonca",
            "Stepanh", "vanosik", "madboss", "wh1tolox", "roflPen", "Gentelman"
    };

    private final NewAltManager parent;
    private String inputText = "";
    private boolean fieldFocused = false;
    private boolean cursorVisible = true;
    private long lastCursorBlink = System.currentTimeMillis();

    private float createHover = 0f;
    private long createHoverTime = -1;
    private float genHover = 0f;
    private long genHoverTime = -1;

    public AddWindow(NewAltManager parent) {
        super(new StringTextComponent(""));
        this.parent = parent;
    }

    @Native
    @Override
    public void tick() {
        super.tick();
    }

    @Native
    @Override
    protected void init() {
        super.init();
        fieldFocused = true;
    }

    private void doCreate() {
        String text = inputText.trim();
        if (text.isEmpty()) {
            text = RANDOM_NAMES[new Random().nextInt(RANDOM_NAMES.length)] + new Random().nextInt(1500);
        }
        parent.addAccount(text);
        minecraft.displayGuiScreen(parent);
    }

    private void doGenerate() {
        inputText = RANDOM_NAMES[new Random().nextInt(RANDOM_NAMES.length)] + new Random().nextInt(1500);
    }

    @Native
    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderUtil.drawImage2D(new net.minecraft.util.ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), 0, 0, this.width, this.height, ColorUtil.applyOpacity(-1, 80));

        float mw = 200f;
        float mh = 95f;
        float mx = (this.width - mw) / 2f;
        float my = (this.height - mh) / 2f;
        float pad = 12f;
        float gap = 8f;
        float radius = 7f;

        RenderUtil.drawRoundedRectangle(mx, my, mw, mh, radius, ColorUtil.getColor(21, 20, 24, 255));

        float cy = my + pad;

        int gray85 = ColorUtil.getColor(103, 106, 114, (int)(0.85f * 255));
        int white  = ColorUtil.getColor(255, 255, 255, 255);

        float titleH = Fonts.sf_regular[18].getHeight();
        Fonts.sf_regular[18].drawString(ms, "Добавление аккаунта", (float) Math.round(mx + pad), (float) Math.round(cy), gray85);

        cy += titleH + gap;

        float fieldH = 21f;
        float fieldW = mw - pad * 2f;
        RenderUtil.drawBlurredRoundedRectangle(mx + pad, cy, fieldW, fieldH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.1f);
        RenderUtil.drawOutlineRectangle(mx + pad, cy, fieldW, fieldH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.15f * 255f);

        long now = System.currentTimeMillis();
        if (now - lastCursorBlink > 500) { cursorVisible = !cursorVisible; lastCursorBlink = now; }

        String displayText = inputText.isEmpty() ? "" : inputText;
        float textY = cy + (fieldH - Fonts.sf_regular[18].getHeight()) / 2f;
        if (inputText.isEmpty() && !fieldFocused) {
            Fonts.sf_regular[18].drawString(ms, "Введите никнейм", (float) Math.round(mx + pad + 8f), (float) Math.round(textY), gray85);
        } else {
            Fonts.sf_regular[18].drawString(ms, displayText, (float) Math.round(mx + pad + 8f), (float) Math.round(textY), white);
            if (cursorVisible) {
                float curX = mx + pad + 8f + Fonts.sf_regular[18].getWidth(displayText) + 1f;
                fill(ms, (int) curX, (int) textY, (int) curX + 1, (int)(textY + Fonts.sf_regular[18].getHeight()), 0xFFFFFFFF);
            }
        }

        cy += fieldH + gap;

        float btnW = (fieldW - gap) / 2f;
        float btnH = 21f;

        long nowC = System.currentTimeMillis();
        if (createHoverTime < 0) createHoverTime = nowC;
        float dC = Math.min((nowC - createHoverTime) / 1000f, 0.05f);
        createHoverTime = nowC;
        boolean onCreate = mouseX >= mx + pad && mouseX <= mx + pad + btnW && mouseY >= cy && mouseY <= cy + btnH;
        createHover = onCreate ? Math.min(createHover + dC * 10f, 1f) : Math.max(createHover - dC * 10f, 0f);

        RenderUtil.drawBlurredRoundedRectangle(mx + pad, cy, btnW, btnH, 5f, ColorUtil.getColor(154, 192, 255, 255), 0.7f + 0.15f * createHover);
        RenderUtil.drawOutlineRectangle(mx + pad, cy, btnW, btnH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.15f * 255f);
        float createTxtW = Fonts.sf_regular[18].getWidth("Создать");
        Fonts.sf_regular[18].drawString(ms, "Создать",
                (float) Math.round(mx + pad + (btnW - createTxtW) / 2f),
                (float) Math.round(cy + (btnH - Fonts.sf_regular[18].getHeight()) / 2f),
                ColorUtil.getColor(16, 17, 18, 255));

        float genX = mx + pad + btnW + gap;
        long nowG = System.currentTimeMillis();
        if (genHoverTime < 0) genHoverTime = nowG;
        float dG = Math.min((nowG - genHoverTime) / 1000f, 0.05f);
        genHoverTime = nowG;
        boolean onGen = mouseX >= genX && mouseX <= genX + btnW && mouseY >= cy && mouseY <= cy + btnH;
        genHover = onGen ? Math.min(genHover + dG * 10f, 1f) : Math.max(genHover - dG * 10f, 0f);

        int genBgC = (int)(103 * 0.1f + (50 - 103 * 0.1f) * genHover);
        RenderUtil.drawBlurredRoundedRectangle(genX, cy, btnW, btnH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.1f + 0.05f * genHover);
        RenderUtil.drawOutlineRectangle(genX, cy, btnW, btnH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.15f * 255f);
        float genTxtW = Fonts.sf_regular[18].getWidth("Сгенерировать");
        Fonts.sf_regular[18].drawString(ms, "Сгенерировать",
                (float) Math.round(genX + (btnW - genTxtW) / 2f),
                (float) Math.round(cy + (btnH - Fonts.sf_regular[18].getHeight()) / 2f),
                gray85);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    @Native
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        float mw = 200f, mh = 95f;
        float mx = (this.width - mw) / 2f, my = (this.height - mh) / 2f;
        float pad = 12f, gap = 8f;
        float fieldH = 21f, btnH = 21f;
        float titleH = Fonts.sf_regular[18].getHeight();
        float fieldW = mw - pad * 2f;
        float btnW   = (fieldW - gap) / 2f;

        float fieldY = my + pad + titleH + gap;
        float btnY   = fieldY + fieldH + gap;

        if (mouseX >= mx + pad && mouseX <= mx + pad + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            doCreate(); return true;
        }
        float genX = mx + pad + btnW + gap;
        if (mouseX >= genX && mouseX <= genX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            doGenerate(); return true;
        }
        if (mouseX >= mx + pad && mouseX <= mx + mw - pad && mouseY >= fieldY && mouseY <= fieldY + fieldH) {
            fieldFocused = true; return true;
        }
        if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
            minecraft.displayGuiScreen(parent); return true;
        }
        fieldFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Native
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { minecraft.displayGuiScreen(parent); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { doCreate(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !inputText.isEmpty()) {
            inputText = inputText.substring(0, inputText.length() - 1); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Native
    @Override
    public boolean charTyped(char c, int modifiers) {
        if (inputText.length() < 32) { inputText += c; return true; }
        return false;
    }
}
