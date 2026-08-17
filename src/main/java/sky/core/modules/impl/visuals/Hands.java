package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import sky.core.events.EventHandsRender;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.shader.KawaseBlur;
import sky.core.utils.render.shader.ShaderUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hands extends Module {


    private final ModeSetting mode = new ModeSetting("Режим", "Заливка", "Зеркало", "Заливка");


    private final ModeSetting mirrorColorMode = new ModeSetting("Цвет зеркала", "Свой",
            () -> mode.is("Зеркало"), "Интерфейс", "Свой");
    private final ColorSetting mirrorColor = new ColorSetting("Цвет", false,
            ColorUtil.hex("#8A98FFFF"),
            () -> mode.is("Зеркало") && mirrorColorMode.is("Свой"));
    private final SliderSetting mixFactor = new SliderSetting("Смешивание", 0f, 0f, 0.5f, 0.01f,
            () -> mode.is("Зеркало"));


    private final BooleanSetting fillRainbow = new BooleanSetting("Радужная", false,
            () -> mode.is("Заливка"));
    private final SliderSetting rainbowSpeed = new SliderSetting("Скорость радуги", 0.4f, 0f, 2f, 0.05f,
            () -> mode.is("Заливка") && fillRainbow.get());
    private final SliderSetting rainbowScale = new SliderSetting("Масштаб радуги", 1f, 0.2f, 3f, 0.1f,
            () -> mode.is("Заливка") && fillRainbow.get());
    private final ModeSetting fillColorMode = new ModeSetting("Цвет заливки", "Свой",
            () -> mode.is("Заливка") && !fillRainbow.get(), "Интерфейс", "Свой");
    private final ColorSetting fillColor = new ColorSetting("Цвет заливки (свой)", false,
            ColorUtil.hex("#FF4444FF"),
            () -> mode.is("Заливка") && !fillRainbow.get() && fillColorMode.is("Свой"));
    private final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки", 0.8f, 0f, 1f, 0.05f,
            () -> mode.is("Заливка"));
    private final BooleanSetting keepShading = new BooleanSetting("Сохранить тени", true,
            () -> mode.is("Заливка"));
    private final SliderSetting shadingStrength = new SliderSetting("Сила теней", 0.3f, 0f, 1f, 0.05f,
            () -> mode.is("Заливка") && keepShading.get());


    private final BooleanSetting outlineEnabled = new BooleanSetting("Обводка", false);
    private final SliderSetting outlineWidth;
    private final ColorSetting outlineColor;

    private final BooleanSetting glowEnabled = new BooleanSetting("Глов", true);
    private final SliderSetting glowRadius = new SliderSetting("Размытие", 4f, 1f, 6f, 1f,
            () -> glowEnabled.get());
    private final BooleanSetting outerGlow = new BooleanSetting("Внешний глов", true,
            () -> glowEnabled.get());
    private final SliderSetting glowExposure = new SliderSetting("Яркость", 2f, 0.5f, 5f, 0.1f,
            () -> glowEnabled.get() && outerGlow.get());

    private final BooleanSetting autoColor = new BooleanSetting("Авто цвет", false,
            () -> glowEnabled.get() || outlineEnabled.get());
    private final SliderSetting saturation = new SliderSetting("Насыщенность", 1.4f, 0.5f, 3f, 0.1f,
            () -> (glowEnabled.get() || outlineEnabled.get()) && autoColor.get());
    private final ModeSetting effectColorMode = new ModeSetting("Цвет эффекта", "Свой",
            () -> (glowEnabled.get() || outlineEnabled.get()) && !autoColor.get(),
            "Интерфейс", "Свой");
    private final ColorSetting glowColor1 = new ColorSetting("Цвет глова 1", false,
            ColorUtil.hex("#8A98FFFF"),
            () -> glowEnabled.get() && !autoColor.get() && effectColorMode.is("Свой"));
    private final ColorSetting glowColor2 = new ColorSetting("Цвет глова 2", false,
            ColorUtil.hex("#FF6BACFF"),
            () -> glowEnabled.get() && !autoColor.get() && effectColorMode.is("Свой"));


    private final BooleanSetting trailEnabled = new BooleanSetting("Шлейф", true,
            () -> glowEnabled.get() && outerGlow.get());
    private final SliderSetting trailFade = new SliderSetting("Скорость затухания", 0.009f, 0.002f, 0.2f, 0.002f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailRise = new SliderSetting("Подъём", 0.14f, 0f, 1.5f, 0.05f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailSway = new SliderSetting("Качание", 0.025f, 0f, 0.2f, 0.005f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailTurb = new SliderSetting("Турбулентность", 0f, 0f, 0.6f, 0.01f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailFlicker = new SliderSetting("Мерцание", 0f, 0f, 0.2f, 0.01f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final BooleanSetting trailBurst = new BooleanSetting("Сдув при ударе", true,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailBurstPower = new SliderSetting("Сила сдува", 2.5f, 1f, 10f, 0.5f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get() && trailBurst.get());
    private final BooleanSetting trailModel = new BooleanSetting("Шлейф модели", true,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get());
    private final SliderSetting trailModelAlpha = new SliderSetting("Прозрачность модели", 0.4f, 0.1f, 1f, 0.05f,
            () -> glowEnabled.get() && outerGlow.get() && trailEnabled.get() && trailModel.get());


    private Framebuffer handsBuffer;
    private Framebuffer trailRead;
    private Framebuffer trailWrite;
    private final List<Framebuffer> bloomBuffers = new ArrayList<>();


    private int downProg = -1;
    private int upProg = -1;
    private int glowProg = -1;
    private int fillProg = -1;
    private int outlineProg = -1;
    private int trailFadeProg = -1;
    private int trailColorProg = -1;

    // by niorze
    private long lastTrailTime = 0L;
    private float smoothDt = 1f / 60f;
    private float smoothTrailRise;
    private float smoothTrailSway;
    private float smoothBurst;
    private long lastSwingMs = -10000L;
    private boolean wasSwinging = false;
    private final float[] autoCol = {1f, 1f, 1f, 1f};
    private int autoTick = 0;
    private int lastBloomTex = -1;
    private final Map<Long, Integer> uniformCache = new HashMap<>();

    private static final boolean MAC = Minecraft.IS_RUNNING_ON_MAC;

    public Hands() {
        super("Hands", "Накладывает эффект на ваши руки", Category.Visuals);

        outlineWidth = new SliderSetting("Толщина обводки", 1f, 0.5f, 3f, 0.5f,
                () -> outlineEnabled.get());
        outlineColor = new ColorSetting("Цвет обводки", false,
                ColorUtil.hex("#8A98FFFF"),
                () -> outlineEnabled.get() && effectColorMode.is("Свой"));

        addSettings(mode,
                mirrorColorMode, mirrorColor, mixFactor,
                fillRainbow, rainbowSpeed, rainbowScale, fillColorMode, fillColor, fillAlpha,
                keepShading, shadingStrength,
                glowEnabled, glowRadius, outerGlow, glowExposure,
                autoColor, saturation, effectColorMode,
                glowColor1, glowColor2,
                trailEnabled, trailFade, trailRise, trailSway, trailTurb, trailFlicker,
                trailBurst, trailBurstPower, trailModel, trailModelAlpha,
                outlineEnabled, outlineWidth, outlineColor);
    }



    private int uloc(int p, String n) {
        long key = ((long) p << 32) | ((long) n.hashCode() & 0xFFFFFFFFL);
        Integer c = uniformCache.get(key);
        if (c != null) return c;
        int l = GL20.glGetUniformLocation(p, n);
        uniformCache.put(key, l);
        return l;
    }

    private void u1i(int p, String n, int v) { int l = uloc(p, n); if (l >= 0) GL20.glUniform1i(l, v); }
    private void u1f(int p, String n, float v) { int l = uloc(p, n); if (l >= 0) GL20.glUniform1f(l, v); }
    private void u2f(int p, String n, float a, float b) { int l = uloc(p, n); if (l >= 0) GL20.glUniform2f(l, a, b); }
    private void u3f(int p, String n, float a, float b, float c) { int l = uloc(p, n); if (l >= 0) GL20.glUniform3f(l, a, b, c); }

    private void bindTex(int unit, int tex) {
        GlStateManager.activeTexture(unit);
        GlStateManager.bindTexture(tex);
    }

    private int themeColor() {
        return ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
    }

    private Framebuffer ensureBuffer(Framebuffer buf, boolean depth) {
        return ensureBuffer(buf, depth, 1);
    }

    private Framebuffer ensureBuffer(Framebuffer buf, boolean depth, int divisor) {
        int fw = mc.getMainWindow().getFramebufferWidth();
        int fh = mc.getMainWindow().getFramebufferHeight();
        int w = Math.max(2, fw / divisor);
        int h = Math.max(2, fh / divisor);
        if (buf == null) {
            buf = new Framebuffer(w, h, depth, MAC);
            buf.setFramebufferFilter(GL11.GL_LINEAR);
            buf.setFramebufferColor(0f, 0f, 0f, 0f);
            buf.framebufferClear(MAC);
        } else if (buf.framebufferWidth != w || buf.framebufferHeight != h) {
            buf.resize(w, h, MAC);
            buf.setFramebufferFilter(GL11.GL_LINEAR);
            buf.framebufferClear(MAC);
        }
        return buf;
    }


    @EventTarget
    private void onHandsPre(EventHandsRender.Pre e) {
        if (fillProg == -1) initShaders();

        if (glowEnabled.get() || outlineEnabled.get()) {
            handsBuffer = ensureBuffer(handsBuffer, true);
            handsBuffer.framebufferClear(MAC);
            handsBuffer.bindFramebuffer(true);
        }

        if (mode.is("Зеркало")) {
            if (KawaseBlur.blur.BLURRED == null) return;
            ShaderUtil.hands.attach();
            GL13.glActiveTexture(GL13.GL_TEXTURE5);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, KawaseBlur.blur.BLURRED.framebufferTexture);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            ShaderUtil.hands.setUniform("originalTexture", 0);
            ShaderUtil.hands.setUniform("blurredTexture", 5);

            int mc_clr = mirrorColorMode.is("Интерфейс") ? themeColor() : mirrorColor.get();
            float[] c = ColorUtil.getColor(mc_clr);
            ShaderUtil.hands.setUniformf("multiplier", c[0], c[1], c[2], c[3]);
            ShaderUtil.hands.setUniformf("resolution",
                    (float) KawaseBlur.blur.BLURRED.framebufferWidth,
                    (float) KawaseBlur.blur.BLURRED.framebufferHeight);
            ShaderUtil.hands.setUniformf("mixFactor", mixFactor.get());

        } else if (mode.is("Заливка")) {
            GL20.glUseProgram(fillProg);
            int fc = fillColorMode.is("Интерфейс") ? themeColor() : fillColor.get();
            float[] c = ColorUtil.getColor(fc);
            u3f(fillProg, "fillColor", c[0], c[1], c[2]);
            u1f(fillProg, "fillAlpha", fillAlpha.get());
            u1i(fillProg, "keepShading", keepShading.get() ? 1 : 0);
            u1f(fillProg, "shadingStrength", shadingStrength.get());
            u1i(fillProg, "originalTexture", 0);
            u1i(fillProg, "rainbow", fillRainbow.get() ? 1 : 0);
            u1f(fillProg, "rainbowTime", (System.currentTimeMillis() % 100000L) / 1000f);
            u1f(fillProg, "rainbowSpeed", rainbowSpeed.get());
            u1f(fillProg, "rainbowScale", rainbowScale.get());
            u1f(fillProg, "screenH", (float) mc.getMainWindow().getFramebufferHeight());
        }
    }

    @EventTarget
    private void onHandsPost(EventHandsRender.Post e) {
        if (mode.is("Зеркало")) {
            ShaderUtil.hands.detach();
        } else {
            GL20.glUseProgram(0);
        }

        if ((glowEnabled.get() || outlineEnabled.get()) && handsBuffer != null) {
            mc.getFramebuffer().bindFramebuffer(true);
            renderEffects();
        }
    }



    private void renderEffects() {
        setup2D();
        lastBloomTex = -1;

        boolean doBloom = glowEnabled.get() && outerGlow.get();
        if (doBloom) {
            doBloomChain();
        } else if (outlineEnabled.get() && autoColor.get()) {
            doSmallBlurForAuto();
        }


        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        handsBuffer.bindFramebufferTexture();
        drawQuads();

        if (outlineEnabled.get()) {
            renderOutline();
        }

        restore3D();
    }

    private void doBloomChain() {
        int iter = (int) (float) glowRadius.get();
        int cur = handsBuffer.framebufferTexture;
        setupBloomBuffers(iter);

        GL20.glUseProgram(downProg);
        u1i(downProg, "inTexture", 0);
        for (int i = 0; i < iter; i++) {
            Framebuffer b = bloomBuffers.get(i);
            b.framebufferClear(MAC);
            b.bindFramebuffer(true);
            u2f(downProg, "uSize", b.framebufferWidth, b.framebufferHeight);
            u2f(downProg, "uOffset", 1f + i, 1f + i);
            u2f(downProg, "uHalfPixel", 0.5f / b.framebufferWidth, 0.5f / b.framebufferHeight);
            bindTex(GL13.GL_TEXTURE0, cur);
            drawQuads();
            cur = b.framebufferTexture;
        }

        GL20.glUseProgram(upProg);
        u1i(upProg, "inTexture", 0);
        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = bloomBuffers.get(i - 1);
            b.bindFramebuffer(true);
            u2f(upProg, "uSize", b.framebufferWidth, b.framebufferHeight);
            u2f(upProg, "uOffset", 1f + i, 1f + i);
            u2f(upProg, "uHalfPixel", 0.5f / b.framebufferWidth, 0.5f / b.framebufferHeight);
            u3f(upProg, "color", 1f, 1f, 1f);
            bindTex(GL13.GL_TEXTURE0, cur);
            drawQuads();
            cur = b.framebufferTexture;
        }
        GL20.glUseProgram(0);
        lastBloomTex = cur;

        if (autoColor.get()) {
            updateAutoColor();
        }

        int g1 = effectColorMode.is("Интерфейс") ? themeColor() : glowColor1.get();
        int g2 = effectColorMode.is("Интерфейс") ? themeColor() : glowColor2.get();
        float[] c1 = ColorUtil.getColor(g1);
        float[] c2 = ColorUtil.getColor(g2);

        if (trailEnabled.get()) {
            renderTrail(cur, c1, c2);
        } else {
            mc.getFramebuffer().bindFramebuffer(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL20.glUseProgram(glowProg);
            u1i(glowProg, "bloomTexture", 0);
            u1i(glowProg, "maskTexture", 1);
            u3f(glowProg, "glowColor1", c1[0], c1[1], c1[2]);
            u3f(glowProg, "glowColor2", c2[0], c2[1], c2[2]);
            u1f(glowProg, "exposure", glowExposure.get());
            u1i(glowProg, "autoColor", autoColor.get() ? 1 : 0);
            u1f(glowProg, "saturation", saturation.get());
            bindTex(GL13.GL_TEXTURE1, handsBuffer.framebufferTexture);
            bindTex(GL13.GL_TEXTURE0, cur);
            drawQuads();
            GL20.glUseProgram(0);
            bindTex(GL13.GL_TEXTURE1, 0);
            bindTex(GL13.GL_TEXTURE0, 0);
            RenderSystem.defaultBlendFunc();
        }
    }

    private void doSmallBlurForAuto() {
        int iter = 3;
        setupBloomBuffers(iter);
        int cur = handsBuffer.framebufferTexture;

        GL20.glUseProgram(downProg);
        u1i(downProg, "inTexture", 0);
        for (int i = 0; i < iter; i++) {
            Framebuffer b = bloomBuffers.get(i);
            b.framebufferClear(MAC);
            b.bindFramebuffer(true);
            u2f(downProg, "uSize", b.framebufferWidth, b.framebufferHeight);
            u2f(downProg, "uOffset", 1f + i, 1f + i);
            u2f(downProg, "uHalfPixel", 0.5f / b.framebufferWidth, 0.5f / b.framebufferHeight);
            bindTex(GL13.GL_TEXTURE0, cur);
            drawQuads();
            cur = b.framebufferTexture;
        }

        GL20.glUseProgram(upProg);
        u1i(upProg, "inTexture", 0);
        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = bloomBuffers.get(i - 1);
            b.bindFramebuffer(true);
            u2f(upProg, "uSize", b.framebufferWidth, b.framebufferHeight);
            u2f(upProg, "uOffset", 1f + i, 1f + i);
            u2f(upProg, "uHalfPixel", 0.5f / b.framebufferWidth, 0.5f / b.framebufferHeight);
            u3f(upProg, "color", 1f, 1f, 1f);
            bindTex(GL13.GL_TEXTURE0, cur);
            drawQuads();
            cur = b.framebufferTexture;
        }
        GL20.glUseProgram(0);
        lastBloomTex = cur;
        updateAutoColor();
        mc.getFramebuffer().bindFramebuffer(true);
    }


    private void renderTrail(int bloomTex, float[] c1, float[] c2) {
        trailRead = ensureBuffer(trailRead, false, 2);
        trailWrite = ensureBuffer(trailWrite, false, 2);

        long now = System.currentTimeMillis();
        float rawDt = lastTrailTime > 0L ? (now - lastTrailTime) / 1000f : 1f / 60f;
        lastTrailTime = now;
        if (rawDt <= 0.0f || rawDt > 0.05f) {
            rawDt = smoothDt;
        }
        rawDt = Math.max(1f / 144f, Math.min(1f / 60f, rawDt));
        smoothDt += (rawDt - smoothDt) * 0.08f;
        float dt = smoothDt;
        float t = (now % 100000L) / 1000f;

        float swayFreq = 3.6f;
        float upDelta = trailRise.get() * dt;
        float amp = trailSway.get();
        float swayDelta = (float)(Math.sin(t * swayFreq) - Math.sin((t - dt) * swayFreq)) * amp;
        float trailSmooth = 1.0f - (float) Math.exp(-dt * 8.0f);
        smoothTrailRise += (upDelta - smoothTrailRise) * trailSmooth;
        smoothTrailSway += (swayDelta - smoothTrailSway) * trailSmooth;

        boolean swinging = mc.player != null && mc.player.isSwingInProgress;
        if (trailBurst.get() && swinging && !wasSwinging) {
            lastSwingMs = now;
        }
        wasSwinging = swinging;

        float burstAge = (now - lastSwingMs) / 1000f;
        float burst = Math.max(0f, 1f - burstAge / 0.45f);
        smoothBurst += (burst - smoothBurst) * (1.0f - (float) Math.exp(-dt * 6.0f));
        float fadeAdd = smoothBurst * trailBurstPower.get() * 0.012f;


        trailWrite.bindFramebuffer(true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();

        GL20.glUseProgram(trailFadeProg);
        u1i(trailFadeProg, "inTex", 0);
        u2f(trailFadeProg, "offset", smoothTrailSway, smoothTrailRise);
        u1f(trailFadeProg, "fade", trailFade.get() + fadeAdd);
        u1f(trailFadeProg, "t", t);
        u1f(trailFadeProg, "dt", dt);
        u1f(trailFadeProg, "turb", trailTurb.get());
        u1f(trailFadeProg, "flickAmp", trailFlicker.get());
        u2f(trailFadeProg, "texSize", trailRead.framebufferWidth, trailRead.framebufferHeight);
        bindTex(GL13.GL_TEXTURE0, trailRead.framebufferTexture);
        drawQuads();
        GL20.glUseProgram(0);
        bindTex(GL13.GL_TEXTURE0, 0);


        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUseProgram(trailColorProg);
        u1i(trailColorProg, "bloomTexture", 0);
        u3f(trailColorProg, "glowColor1", c1[0], c1[1], c1[2]);
        u3f(trailColorProg, "glowColor2", c2[0], c2[1], c2[2]);
        u1f(trailColorProg, "exposure", glowExposure.get());
        u1i(trailColorProg, "autoColor", autoColor.get() ? 1 : 0);
        u1f(trailColorProg, "saturation", saturation.get());
        bindTex(GL13.GL_TEXTURE0, bloomTex);
        drawQuads();
        GL20.glUseProgram(0);
        bindTex(GL13.GL_TEXTURE0, 0);


        if (trailModel.get()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.color4f(1f, 1f, 1f, trailModelAlpha.get());
            handsBuffer.bindFramebufferTexture();
            drawQuads();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
        }

        mc.getFramebuffer().bindFramebuffer(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        trailWrite.bindFramebufferTexture();
        drawQuads();
        RenderSystem.defaultBlendFunc();

        // swap
        Framebuffer tmp = trailRead;
        trailRead = trailWrite;
        trailWrite = tmp;
    }


    private void renderOutline() {
        boolean rainbow = fillRainbow.get() && mode.is("Заливка");
        boolean auto = autoColor.get() && lastBloomTex != -1;
        int colorMode = rainbow ? 1 : (auto ? 2 : 0);

        int oc = effectColorMode.is("Интерфейс") ? themeColor() : outlineColor.get();
        Color col = new Color(oc, true);
        float a = col.getAlpha() / 255f;

        GL20.glUseProgram(outlineProg);
        u1i(outlineProg, "inTex", 0);
        u1i(outlineProg, "bloomTex", 1);
        u1i(outlineProg, "colorMode", colorMode);
        u1f(outlineProg, "width", outlineWidth.get());
        u2f(outlineProg, "texelSize",
                1f / mc.getMainWindow().getFramebufferWidth(),
                1f / mc.getMainWindow().getFramebufferHeight());
        u1f(outlineProg, "alpha", a);
        u1f(outlineProg, "rainbowTime", (System.currentTimeMillis() % 100000L) / 1000f);
        u1f(outlineProg, "rainbowSpeed", rainbowSpeed.get());
        u1f(outlineProg, "rainbowScale", rainbowScale.get());
        u1f(outlineProg, "screenH", (float) mc.getMainWindow().getFramebufferHeight());
        u1f(outlineProg, "saturation", saturation.get());
        u3f(outlineProg, "solidColor",
                col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);

        if (auto) bindTex(GL13.GL_TEXTURE1, lastBloomTex);
        bindTex(GL13.GL_TEXTURE0, handsBuffer.framebufferTexture);
        drawQuads();
        if (auto) bindTex(GL13.GL_TEXTURE1, 0);
        bindTex(GL13.GL_TEXTURE0, 0);
        GL20.glUseProgram(0);
    }


    private void updateAutoColor() {
        if (bloomBuffers.isEmpty()) return;
        if (autoTick++ % 6 != 0) return;

        Framebuffer small = bloomBuffers.get(bloomBuffers.size() - 1);
        int w = small.framebufferWidth;
        int h = small.framebufferHeight;
        int prev = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        small.bindFramebuffer(false);
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);

        float sumR = 0, sumG = 0, sumB = 0, sumA = 0;
        for (int i = 0; i < w * h; i++) {
            int o = i * 4;
            sumR += (buf.get(o) & 0xFF) / 255f;
            sumG += (buf.get(o + 1) & 0xFF) / 255f;
            sumB += (buf.get(o + 2) & 0xFF) / 255f;
            sumA += (buf.get(o + 3) & 0xFF) / 255f;
        }
        if (sumA < 0.001f) return;

        float r = sumR / sumA, g = sumG / sumA, b = sumB / sumA;
        float m = Math.max(r, Math.max(g, b));
        if (m > 0.001f) { r /= m; g /= m; b /= m; }
        float sat = saturation.get();
        float l = 0.299f * r + 0.587f * g + 0.114f * b;
        autoCol[0] = clamp01(l + (r - l) * sat);
        autoCol[1] = clamp01(l + (g - l) * sat);
        autoCol[2] = clamp01(l + (b - l) * sat);
        autoCol[3] = 1f;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }



    private void setupBloomBuffers(int n) {
        int fw = mc.getMainWindow().getFramebufferWidth();
        int fh = mc.getMainWindow().getFramebufferHeight();
        if (bloomBuffers.size() < n) {
            bloomBuffers.forEach(Framebuffer::deleteFramebuffer);
            bloomBuffers.clear();
            for (int i = 0; i < n; i++) {
                Framebuffer f = new Framebuffer(
                        Math.max(2, fw >> (i + 1)),
                        Math.max(2, fh >> (i + 1)),
                        false, MAC);
                f.setFramebufferFilter(GL11.GL_LINEAR);
                f.setFramebufferColor(0f, 0f, 0f, 0f);
                bloomBuffers.add(f);
            }
        }
        for (int i = 0; i < n; i++) {
            int w = Math.max(2, fw >> (i + 1));
            int h = Math.max(2, fh >> (i + 1));
            Framebuffer b = bloomBuffers.get(i);
            if (b.framebufferWidth != w || b.framebufferHeight != h) {
                b.resize(w, h, MAC);
                b.setFramebufferFilter(GL11.GL_LINEAR);
            }
        }
    }

    // 2d


    private void drawQuads() {
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        float w = mc.getMainWindow().getScaledWidth();
        float h = mc.getMainWindow().getScaledHeight();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        b.pos(0, h, 0).tex(0, 0).endVertex();
        b.pos(w, h, 0).tex(1, 0).endVertex();
        b.pos(w, 0, 0).tex(1, 1).endVertex();
        b.pos(0, 0, 0).tex(0, 1).endVertex();
        Tessellator.getInstance().draw();
    }

    private void setup2D() {
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0, mc.getMainWindow().getScaledWidth(),
                mc.getMainWindow().getScaledHeight(), 0, 1000, 3000);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0, 0, -2000);
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
    }

    private void restore3D() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.bindTexture(0);
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }

    // sheidera iz pouch

    private void initShaders() {
        String V = "#version 120\nvoid main(){gl_TexCoord[0]=gl_MultiTexCoord0;gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;}";

        downProg = createProgram(V,
                "#version 120\nuniform sampler2D inTexture;uniform vec2 uOffset,uHalfPixel,uSize;void main(){vec2 u=gl_TexCoord[0].xy,h=uHalfPixel*uOffset;gl_FragColor=(texture2D(inTexture,u)*4.0+texture2D(inTexture,u-h)+texture2D(inTexture,u+h)+texture2D(inTexture,u+vec2(h.x,-h.y))+texture2D(inTexture,u-vec2(h.x,-h.y)))/8.0;}");

        upProg = createProgram(V,
                "#version 120\nuniform sampler2D inTexture;uniform vec2 uOffset,uHalfPixel,uSize;uniform vec3 color;void main(){vec2 u=gl_TexCoord[0].xy,h=uHalfPixel*uOffset;vec4 s=texture2D(inTexture,u+vec2(-h.x*2.,0.))+texture2D(inTexture,u+vec2(-h.x,h.y))*2.+texture2D(inTexture,u+vec2(0.,h.y*2.))+texture2D(inTexture,u+vec2(h.x,h.y))*2.+texture2D(inTexture,u+vec2(h.x*2.,0.))+texture2D(inTexture,u+vec2(h.x,-h.y))*2.+texture2D(inTexture,u+vec2(0.,-h.y*2.))+texture2D(inTexture,u+vec2(-h.x,-h.y))*2.;vec4 r=s/12.;gl_FragColor=vec4(r.rgb*color,r.a);}");

        glowProg = createProgram(V,
                "#version 120\nuniform sampler2D bloomTexture, maskTexture;\n" +
                        "uniform vec3 glowColor1, glowColor2;\nuniform float exposure;\nuniform int autoColor;\nuniform float saturation;\n" +
                        "void main(){\n  vec2 u = gl_TexCoord[0].xy;\n  vec4 b = texture2D(bloomTexture, u);\n" +
                        "  float i = b.a * (1.0 - texture2D(maskTexture, u).a) * exposure;\n  vec3 col;\n" +
                        "  if (autoColor == 1) {\n    vec3 c = b.rgb / max(b.a, 0.001);\n    float m = max(c.r, max(c.g, c.b));\n" +
                        "    if (m > 0.001) c /= m;\n    float l = dot(c, vec3(0.299, 0.587, 0.114));\n" +
                        "    col = clamp(mix(vec3(l), c, saturation), 0.0, 1.0);\n  } else {\n" +
                        "    col = mix(glowColor1, glowColor2, u.y);\n  }\n  gl_FragColor = vec4(col, i);\n}");

        outlineProg = createProgram(V,
                "#version 120\nuniform sampler2D inTex;\nuniform sampler2D bloomTex;\nuniform vec2 texelSize;\n" +
                        "uniform float width, alpha, rainbowTime, rainbowSpeed, rainbowScale, screenH, saturation;\n" +
                        "uniform int colorMode;\nuniform vec3 solidColor;\n" +
                        "vec3 hue2rgb(float h){\n  vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);\n  return clamp(p - 1.0, 0.0, 1.0);\n}\n" +
                        "void main(){\n  vec2 uv = gl_TexCoord[0].xy;\n  vec4 c = texture2D(inTex, uv);\n  if (c.a > 0.01) discard;\n" +
                        "  float maxA = 0.0;\n  for (int x = -2; x <= 2; x++) {\n    for (int y = -2; y <= 2; y++) {\n      if (x == 0 && y == 0) continue;\n" +
                        "      vec2 d = vec2(float(x), float(y)) * texelSize * width;\n      maxA = max(maxA, texture2D(inTex, uv + d).a);\n    }\n  }\n" +
                        "  if (maxA < 0.01) discard;\n  vec3 col;\n  if (colorMode == 1) {\n    float yN = gl_FragCoord.y / max(screenH, 1.0);\n" +
                        "    float h = fract(yN * rainbowScale + rainbowTime * rainbowSpeed);\n    col = hue2rgb(h);\n" +
                        "  } else if (colorMode == 2) {\n    vec4 b = texture2D(bloomTex, uv);\n    vec3 cb = b.rgb / max(b.a, 0.001);\n" +
                        "    float m = max(cb.r, max(cb.g, cb.b));\n    if (m > 0.001) cb /= m;\n    float l = dot(cb, vec3(0.299, 0.587, 0.114));\n" +
                        "    col = clamp(mix(vec3(l), cb, saturation), 0.0, 1.0);\n  } else {\n    col = solidColor;\n  }\n" +
                        "  gl_FragColor = vec4(col, maxA * alpha);\n}");

        fillProg = createProgram(V,
                "#version 120\nuniform sampler2D originalTexture;\nuniform vec3 fillColor;\n" +
                        "uniform float fillAlpha, shadingStrength;\nuniform int keepShading;\nuniform int rainbow;\n" +
                        "uniform float rainbowTime, rainbowSpeed, rainbowScale, screenH;\n" +
                        "vec3 hue2rgb(float h){\n  vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);\n  return clamp(p - 1.0, 0.0, 1.0);\n}\n" +
                        "void main(){\n  vec4 s = texture2D(originalTexture, gl_TexCoord[0].xy);\n  if (s.a < 0.01) discard;\n  vec3 f;\n" +
                        "  if (rainbow == 1) {\n    float yN = gl_FragCoord.y / max(screenH, 1.0);\n    float h = fract(yN * rainbowScale + rainbowTime * rainbowSpeed);\n    f = hue2rgb(h);\n" +
                        "  } else {\n    f = fillColor;\n  }\n" +
                        "  if (keepShading == 1) f *= mix(1.0, dot(s.rgb, vec3(0.299, 0.587, 0.114)), shadingStrength);\n" +
                        "  gl_FragColor = vec4(mix(s.rgb, f, fillAlpha), s.a);\n}");

        trailFadeProg = createProgram(V,
                "#version 120\nuniform sampler2D inTex;\nuniform vec2 offset;\nuniform vec2 texSize;\n" +
                        "uniform float fade;\nuniform float t;\nuniform float dt;\nuniform float turb;\nuniform float flickAmp;\n" +
                        "float wob(float y, float tt){\n  return sin(y*9.0 + tt*4.3)*0.6 + sin(y*17.0 - tt*2.1)*0.3 + sin(y*4.0 + tt*1.3)*0.4 + sin(y*28.0 + tt*5.7)*0.15;\n}\n" +
                        "vec4 blurSample(vec2 p){\n  vec2 px = 1.0 / max(texSize, vec2(1.0));\n  vec4 c = texture2D(inTex, p) * 0.36;\n" +
                        "  c += texture2D(inTex, p + vec2( px.x, 0.0)) * 0.16;\n  c += texture2D(inTex, p + vec2(-px.x, 0.0)) * 0.16;\n" +
                        "  c += texture2D(inTex, p + vec2(0.0,  px.y)) * 0.16;\n  c += texture2D(inTex, p + vec2(0.0, -px.y)) * 0.16;\n  return c;\n}\n" +
                        "void main(){\n  vec2 uv = gl_TexCoord[0].xy;\n  float dxTurb = (wob(uv.y, t) - wob(uv.y, t - dt)) * turb;\n" +
                        "  float yFactor = 0.7 + uv.y * 0.8;\n  vec2 disp = vec2(offset.x + dxTurb, offset.y * yFactor);\n" +
                        "  vec4 c = blurSample(uv - disp);\n" +
                        "  float flick = 1.0 - flickAmp + flickAmp * (0.5 + 0.5*sin(t*14.0) + 0.25*sin(t*9.3));\n" +
                        "  float oldA = c.a;\n  float newA = max(0.0, oldA * (0.985 * flick) - fade);\n" +
                        "  float scale = oldA > 0.001 ? newA / oldA : 0.0;\n  gl_FragColor = vec4(c.rgb * scale, newA);\n}");

        trailColorProg = createProgram(V,
                "#version 120\nuniform sampler2D bloomTexture;\nuniform vec3 glowColor1;\nuniform vec3 glowColor2;\n" +
                        "uniform float exposure;\nuniform int autoColor;\nuniform float saturation;\n" +
                        "void main(){\n  vec2 u = gl_TexCoord[0].xy;\n  vec4 b = texture2D(bloomTexture, u);\n" +
                        "  float a = clamp(b.a * exposure, 0.0, 1.0);\n  vec3 col;\n" +
                        "  if (autoColor == 1) {\n    vec3 c = b.rgb / max(b.a, 0.001);\n    float m = max(c.r, max(c.g, c.b));\n" +
                        "    if (m > 0.001) c /= m;\n    float l = dot(c, vec3(0.299, 0.587, 0.114));\n" +
                        "    col = clamp(mix(vec3(l), c, saturation), 0.0, 1.0);\n  } else {\n    col = mix(glowColor1, glowColor2, u.y);\n  }\n" +
                        "  gl_FragColor = vec4(col * a, a);\n}");
    }

    private int createProgram(String vs, String fs) {
        int v = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int f = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        int p = GL20.glCreateProgram();
        GL20.glShaderSource(v, vs);
        GL20.glCompileShader(v);
        if (GL20.glGetShaderi(v, GL20.GL_COMPILE_STATUS) == 0)
            System.err.println("[Hands] VS error: " + GL20.glGetShaderInfoLog(v, 1024));
        GL20.glShaderSource(f, fs);
        GL20.glCompileShader(f);
        if (GL20.glGetShaderi(f, GL20.GL_COMPILE_STATUS) == 0)
            System.err.println("[Hands] FS error: " + GL20.glGetShaderInfoLog(f, 1024));
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        return p;
    }



    @Override
    public void onDisable() {
        super.onDisable();
        if (handsBuffer != null) { handsBuffer.deleteFramebuffer(); handsBuffer = null; }
        if (trailRead != null) { trailRead.deleteFramebuffer(); trailRead = null; }
        if (trailWrite != null) { trailWrite.deleteFramebuffer(); trailWrite = null; }
        bloomBuffers.forEach(Framebuffer::deleteFramebuffer);
        bloomBuffers.clear();
        lastTrailTime = 0L;
        smoothDt = 1f / 60f;
        smoothTrailRise = 0f;
        smoothTrailSway = 0f;
        smoothBurst = 0f;

        if (downProg != -1) {
            GL20.glDeleteProgram(downProg);
            GL20.glDeleteProgram(upProg);
            GL20.glDeleteProgram(glowProg);
            GL20.glDeleteProgram(fillProg);
            GL20.glDeleteProgram(trailFadeProg);
            GL20.glDeleteProgram(trailColorProg);
            if (outlineProg != -1) GL20.glDeleteProgram(outlineProg);
            downProg = upProg = glowProg = fillProg = outlineProg = trailFadeProg = trailColorProg = -1;
            uniformCache.clear();
        }
    }
}
