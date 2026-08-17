package net.minecraft.client.gui.screen;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AccessibilityScreen;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridgeScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.storage.SaveFormat;
import net.minecraft.world.storage.WorldSummary;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import sky.core.ui.altmanager.NewAltManager;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.ui.screens.AccountManager;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import sky.core.utils.Wrapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public class MainMenuScreen extends Screen implements Wrapper {

    private static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
    private static final ResourceLocation ACCESSIBILITY_TEXTURES = new ResourceLocation("textures/gui/accessibility.png");
    private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
    public static final RenderSkyboxCube PANORAMA_RESOURCES = new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama"));
    private static final ResourceLocation MENU_BACKGROUND = new ResourceLocation("minecraft", "SkyCore/mainmenu/background.png");

    private final RenderSkybox panorama = new RenderSkybox(PANORAMA_RESOURCES);
    private final java.util.Map<String, Float> hoverMap = new java.util.HashMap<>();
    private final java.util.Map<String, Long>  hoverTimeMap = new java.util.HashMap<>();
    private float fadeAlpha = 0f;
    private boolean fadingOut = false;
    private Screen pendingScreen = null;
    private long lastFrameTime = -1;
    private final boolean showFadeInAnimation;
    private long firstRenderTime;
    private boolean hasCheckedForRealmsNotification;
    private Screen realmsNotification;
    private Screen modUpdateNotification;
    private final boolean showTitleWronglySpelled;
    private String splashText;
    private int widthCopyright;
    private int widthCopyrightRest;

    public MainMenuScreen() {
        this(false);
    }

    public MainMenuScreen(boolean fadeIn) {
        super(new TranslationTextComponent("narrator.screen.title"));
        this.showFadeInAnimation = fadeIn;
        this.showTitleWronglySpelled = (double)(new Random()).nextFloat() < 1.0E-4D;
    }

    private boolean areRealmsNotificationsEnabled() {
        return this.minecraft.gameSettings.realmsNotifications && this.realmsNotification != null;
    }

    @Override
    public void tick() {
        if (this.areRealmsNotificationsEnabled()) {
            this.realmsNotification.tick();
        }
    }

    public static CompletableFuture<Void> loadAsync(net.minecraft.client.renderer.texture.TextureManager texMngr, Executor backgroundExecutor) {
        return CompletableFuture.allOf(
                texMngr.loadAsync(MINECRAFT_TITLE_TEXTURES, backgroundExecutor),
                texMngr.loadAsync(MINECRAFT_TITLE_EDITION, backgroundExecutor),
                texMngr.loadAsync(PANORAMA_OVERLAY_TEXTURES, backgroundExecutor),
                PANORAMA_RESOURCES.loadAsync(texMngr, backgroundExecutor)
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        this.minecraft.setConnectedToRealms(false);
    }

    private void addSingleplayerMultiplayerButtons(int yIn, int rowHeightIn) {}

    private void addDemoButtons(int yIn, int rowHeightIn) {
        boolean hasDemoWorld = this.func_243319_k();

        this.addButton(new net.minecraft.client.gui.widget.button.Button(this.width / 2 - 100, yIn, 200, 20,
                new TranslationTextComponent("menu.playdemo"), (btn) -> {
            if (hasDemoWorld) {
                this.minecraft.loadWorld("Demo_World");
            } else {
                net.minecraft.util.registry.DynamicRegistries.Impl dynamicRegistries = net.minecraft.util.registry.DynamicRegistries.func_239770_b_();
                this.minecraft.createWorld("Demo_World", net.minecraft.server.MinecraftServer.DEMO_WORLD_SETTINGS,
                        dynamicRegistries, net.minecraft.world.gen.settings.DimensionGeneratorSettings.func_242752_a(dynamicRegistries));
            }
        }));

        net.minecraft.client.gui.widget.button.Button resetBtn = this.addButton(
                new net.minecraft.client.gui.widget.button.Button(this.width / 2 - 100, yIn + rowHeightIn, 200, 20,
                        new TranslationTextComponent("menu.resetdemo"), (btn) -> {
                    net.minecraft.world.storage.SaveFormat saveFormat = this.minecraft.getSaveLoader();
                    try (net.minecraft.world.storage.SaveFormat.LevelSave levelSave = saveFormat.getLevelSave("Demo_World")) {
                        net.minecraft.world.storage.WorldSummary summary = levelSave.readWorldSummary();
                        if (summary != null) {
                            this.minecraft.displayGuiScreen(new ConfirmScreen(this::deleteDemoWorld,
                                    new TranslationTextComponent("selectWorld.deleteQuestion"),
                                    new TranslationTextComponent("selectWorld.deleteWarning", summary.getDisplayName()),
                                    new TranslationTextComponent("selectWorld.deleteButton"),
                                    DialogTexts.GUI_CANCEL));
                        }
                    } catch (java.io.IOException e) {
                        net.minecraft.client.gui.toasts.SystemToast.func_238535_a_(this.minecraft, "Demo_World");
                    }
                }));
        resetBtn.active = hasDemoWorld;
    }

    private boolean func_243319_k() {
        try (net.minecraft.world.storage.SaveFormat.LevelSave levelSave = this.minecraft.getSaveLoader().getLevelSave("Demo_World")) {
            return levelSave.readWorldSummary() != null;
        } catch (java.io.IOException e) {
            net.minecraft.client.gui.toasts.SystemToast.func_238535_a_(this.minecraft, "Demo_World");
            return false;
        }
    }



    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        long now = System.currentTimeMillis();
        float delta = lastFrameTime < 0 ? 0f : Math.min((now - lastFrameTime) / 1000f, 0.1f);
        lastFrameTime = now;
        fadeAlpha = 1f;

        float sx = this.width / 1920f;
        float sy = this.height / 1080f;

        fill(matrixStack, 0, 0, this.width, this.height, 0xFF151418);
        RenderSystem.color4f(1f, 1f, 1f, fadeAlpha);
        this.minecraft.getTextureManager().bindTexture(MENU_BACKGROUND);
        blit(matrixStack, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        float bw  = 143.5f;
        float bh  = 25f;
        float gap = 4f;
        float bx  = (this.width - bw) / 2f;
        float by  = this.height * 0.40f;
        float radius = 6f;

        int themeColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        float logoSize = 81f;
        float logoW = Fonts.skycore[80].getWidth("a");
        float logoH = Fonts.skycore[80].getHeight();
        float logoX = (float) Math.round((this.width - logoW) / 2f);
        float logoY = (float) Math.round(by - logoH - gap * 2);
        Fonts.skycore[80].drawString(matrixStack, "a", logoX, logoY - 60, ColorUtil.getColor(255, 255, 255, (int)(225 * fadeAlpha)));

        float bwVar = bw; float bhVar = bh; float gapVar = gap; float bxVar = bx; float byVar = by;

        drawFigmaButton(matrixStack, bxVar, byVar,                        bwVar,        bhVar, radius, themeColor, "Singleplayer", "l", 0, mouseX, mouseY, fadeAlpha);
        drawFigmaButton(matrixStack, bxVar, byVar + (bhVar + gapVar),     bwVar,        bhVar, radius, themeColor, "Multiplayer",  "h", 0, mouseX, mouseY, fadeAlpha);
        drawFigmaButton(matrixStack, bxVar, byVar + (bhVar + gapVar) * 2, bwVar,        bhVar, radius, themeColor, "AltManager",  "y", 2, mouseX, mouseY, fadeAlpha);
        float exitSize = 25f;
        float rowY4 = byVar + (bhVar + gapVar) * 3;
        float optW = bwVar - exitSize - 4;
        drawFigmaButton(matrixStack, bxVar, rowY4, optW, bhVar, radius, themeColor, "Options", "i", 1, mouseX, mouseY, fadeAlpha);

        float exitX = bxVar + optW + 4;
        float exitY = rowY4 + (bhVar - exitSize) / 2f;
        int exitA = (int)(0xC9 * fadeAlpha);
        RenderUtil.drawRoundedRectangle(exitX, exitY, exitSize, exitSize, 9f, ColorUtil.getColor(0xC9, 0x7A, 0x7E, exitA));
        float exitIconW = Fonts.icons[16].getWidth("A");
        float exitIconH = Fonts.icons[16].getHeight();
        Fonts.krisa[16].drawString(matrixStack, "A",
                (float) Math.round(exitX + (exitSize - exitIconW) / 2f) - 0.5f,
                (float) Math.round(exitY + (exitSize - exitIconH) / 2f) + 1,
                ColorUtil.getColor(255, 255, 255, (int)(fadeAlpha * 255)));


        float footerX = (this.width - Fonts.sf_regular[13].getWidth("SkyCore 2026. All rights reserved.")) / 2f;
        float footerY = this.height - Fonts.sf_regular[13].getHeight() * 2 - 8;
        int footerColor = ColorUtil.getColor(103, 106, 114, (int)(0.85f * 255 * fadeAlpha));
        Fonts.sf_regular[13].drawString(matrixStack, "SkyCore 2026. All rights reserved.", footerX, footerY, footerColor);
        Fonts.sf_regular[13].drawString(matrixStack, "Before playing, please read the Rules of use.", footerX - 12, footerY + Fonts.sf_regular[13].getHeight() + 2, footerColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        float bw  = 143.5f, bh = 25f, gap = 4f;
        float bx  = (this.width - bw) / 2f;
        float by  = this.height * 0.40f;
        float halfW = (bw - 4) / 2f;

        if (isHit(mouseX, mouseY, bx, by, bw, bh)) {
            this.minecraft.displayGuiScreen(new WorldSelectionScreen(this)); return true;
        }
        if (isHit(mouseX, mouseY, bx, by + (bh + gap), bw, bh)) {
            Screen scr = this.minecraft.gameSettings.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
            this.minecraft.displayGuiScreen(scr); return true;
        }
        if (isHit(mouseX, mouseY, bx, by + (bh + gap) * 2, bw, bh)) {
            this.minecraft.displayGuiScreen(new NewAltManager(true)); return true;
        }
        float exitSize = 25f;
        float rowY4 = by + (bh + gap) * 3;
        float optW = bw - exitSize - 4;
        if (isHit(mouseX, mouseY, bx, rowY4, optW, bh)) {
            this.minecraft.displayGuiScreen(new OptionsScreen(this, this.minecraft.gameSettings)); return true;
        }
        float exitX = bx + optW + 4;
        float exitY = rowY4 + (bh - exitSize) / 2f;
        if (isHit(mouseX, mouseY, exitX, exitY, exitSize, exitSize)) {
            this.minecraft.shutdown(); return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void onClose() {
        if (this.realmsNotification != null) {
            this.realmsNotification.onClose();
        }
    }

    private void deleteDemoWorld(boolean confirm) {
        if (confirm) {
            try (net.minecraft.world.storage.SaveFormat.LevelSave levelSave = this.minecraft.getSaveLoader().getLevelSave("Demo_World")) {
                levelSave.deleteSave();
            } catch (java.io.IOException e) {
                net.minecraft.client.gui.toasts.SystemToast.func_238538_b_(this.minecraft, "Demo_World");
            }
        }
        this.minecraft.displayGuiScreen(this);
    }

    private void drawFigmaButton(MatrixStack ms, float x, float y, float w, float h, float radius,
                                  int themeColor, String text, String icon, int fontType, int mouseX, int mouseY, float fadeAlpha) {
        boolean hovered = isHit(mouseX, mouseY, x, y, w, h);

        long nowBtn = System.currentTimeMillis();
        float prev = hoverMap.getOrDefault(text, 0f);
        long lastT = hoverTimeMap.getOrDefault(text, nowBtn);
        float deltaBtn = Math.min((nowBtn - lastT) / 1000f, 0.05f);
        hoverTimeMap.put(text, nowBtn);
        float speed = 5f;
        float t = hovered ? Math.min(prev + deltaBtn * speed, 1f) : Math.max(prev - deltaBtn * speed, 0f);
        hoverMap.put(text, t);

        int bgR = (int)(30  + (154 - 22)  * t);
        int bgG = (int)(30  + (192 - 23)  * t);
        int bgB = (int)(35  + (255 - 27)  * t);
        int bgA = (int)(255 * fadeAlpha);
        int bgColor = ColorUtil.getColor(bgR, bgG, bgB, bgA);

        int txR = (int)(140 + (255 - 140) * t);
        int txG = (int)(140 + (255 - 140) * t);
        int txB = (int)(140 + (255 - 140) * t);
        int txA = (int)(255 * fadeAlpha);
        int textColor = ColorUtil.getColor(txR, txG, txB, txA);

        RenderUtil.drawBlurredRoundedRectangle(x, y, w, h, radius + 2, ColorUtil.getColor(bgR, bgG, bgB, 255), 0.55f * fadeAlpha + 0.15f * t);

        sky.core.utils.render.font.StyledFont iconFont = fontType == 1 ? Fonts.mototanya[16] : fontType == 2 ? Fonts.krisa[16] : Fonts.divine_icons[16];
        float tw = Fonts.sf_regular[17].getWidth(text);
        float th = Fonts.sf_regular[17].getHeight();
        float iconH = iconFont.getHeight();
        float iconW = iconFont.getWidth(icon);
        float spacing = 5f;
        float totalW = iconW + spacing + tw;
        float startX = (float) Math.round(x + (w - totalW) / 2f);
        float centerY = (float) Math.round(y + h / 2f);
        iconFont.drawString(ms, icon, startX + 2, (float) Math.round(centerY - iconH / 2f) + 0.3f, textColor);
        Fonts.sf_regular[17].drawString(ms, text, (float) Math.round(startX + iconW + spacing), (float) Math.round(centerY - th / 2f), textColor);

        int dotA = (int)(t * fadeAlpha * 255);
        if (dotA > 0) {
            int dotColor = ColorUtil.getColor(255, 255, 255, dotA);
            float dotRadius = 1.2f;
            float dotSpacing = 2.5f;
            float dotsX = x + w - 10f + 3;
            RenderUtil.drawCircle(dotsX - dotSpacing * 2, centerY - 5, 0f, 360f, dotRadius, 0.8f, true, dotColor);
            RenderUtil.drawCircle(dotsX - dotSpacing, centerY - 5, 0f, 360f, dotRadius, 0.8f, true, dotColor);
            RenderUtil.drawCircle(dotsX, centerY - 5, 0f, 360f, dotRadius, 0.8f, true, dotColor);
        }
    }
}