package net.minecraft.client.gui;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.resources.IAsyncReloader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.VanillaPack;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.config.ShaderPackParser;
import net.optifine.util.PropertiesOrdered;
import sky.core.ui.screens.ResourceLoadingScreen;

public class ResourceLoadProgressGui extends LoadingGui {
    private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
    private static final int field_238627_b_ = ColorHelper.PackedColor.packColor(255, 239, 50, 61);
    private static final int field_238628_c_ = field_238627_b_ & 16777215;
    private final Minecraft mc;
    private final IAsyncReloader asyncReloader;
    private final Consumer<Optional<Throwable>> completedCallback;
    private final boolean reloading;
    private float progress;
    private float targetProgress;
    private float displayProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;
    private int colorBackground = field_238628_c_;
    private int colorBar = field_238628_c_;
    private int colorOutline = 16777215;
    private int colorProgress = 16777215;
    private GlBlendState blendState = null;
    private boolean fadeOut = false;

    public ResourceLoadProgressGui(Minecraft p_i225928_1_, IAsyncReloader p_i225928_2_, Consumer<Optional<Throwable>> p_i225928_3_, boolean p_i225928_4_) {
        this.mc = p_i225928_1_;
        this.asyncReloader = p_i225928_2_;
        this.completedCallback = p_i225928_3_;
        this.reloading = p_i225928_4_;
        this.progress = 0f;
        this.targetProgress = 0f;
        this.displayProgress = 0f;
    }

    public static void loadLogoTexture(Minecraft mc) {
        mc.getTextureManager().loadTexture(MOJANG_LOGO_TEXTURE, new ResourceLoadProgressGui.MojangLogoTexture());
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int width = this.mc.getMainWindow().getScaledWidth();
        int height = this.mc.getMainWindow().getScaledHeight();
        long now = Util.milliTime();

        if (this.reloading && (this.asyncReloader.asyncPartDone() || this.mc.currentScreen != null) && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }

        float fadeOutT = this.fadeOutStart > -1L ? (float) (now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInT = this.fadeInStart > -1L ? (float) (now - this.fadeInStart) / 500.0F : -1.0F;
        float screenAlpha;

        if (fadeOutT >= 1.0F) {
            this.fadeOut = true;
            if (this.mc.currentScreen != null) {
                this.mc.currentScreen.render(matrixStack, 0, 0, partialTicks);
            }
            int overlayA = MathHelper.ceil((1.0F - MathHelper.clamp(fadeOutT - 1.0F, 0.0F, 1.0F)) * 255.0F);
            fill(matrixStack, 0, 0, width, height, this.colorBackground | overlayA << 24);
            screenAlpha = 1.0F - MathHelper.clamp(fadeOutT - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.mc.currentScreen != null && fadeInT < 1.0F) {
                this.mc.currentScreen.render(matrixStack, mouseX, mouseY, partialTicks);
            }
            int overlayA = MathHelper.ceil(MathHelper.clamp((double) fadeInT, 0.15D, 1.0D) * 255.0D);
            fill(matrixStack, 0, 0, width, height, this.colorBackground | overlayA << 24);
            screenAlpha = MathHelper.clamp(fadeInT, 0.0F, 1.0F);
        } else {
            screenAlpha = 1.0F;
        }

        float speed = this.asyncReloader.estimateExecutionSpeed();
        this.targetProgress = MathHelper.clamp(speed, 0.0F, 1.0F);
        this.progress = MathHelper.lerp(0.1F, this.progress, this.targetProgress);
        this.displayProgress = MathHelper.lerp(0.055f, this.displayProgress, this.progress);

        Reflector.ClientModLoader_renderProgressText.call();

        ResourceLoadingScreen.render(this.mc, matrixStack, width, height, screenAlpha, this.progress, this.displayProgress);

        RenderSystem.defaultBlendFunc();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.disableBlend();

        if (fadeOutT >= 2.0F) {
            this.mc.setLoadingGui(null);
        }

        if (this.fadeOutStart == -1L && this.asyncReloader.fullyDone() && (!this.reloading || fadeInT >= 2.0F)) {
            this.fadeOutStart = Util.milliTime();
            this.progress = 1.0F;
            this.targetProgress = 1.0F;
            this.displayProgress = 1.0F;

            try {
                this.asyncReloader.join();
                this.completedCallback.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.completedCallback.accept(Optional.of(throwable));
            }

            if (this.mc.currentScreen != null) {
                this.mc.currentScreen.init(this.mc, width, height);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
    @EventTarget
    public void update() {
        this.colorBackground = field_238628_c_;
        this.colorBar = field_238628_c_;
        this.colorOutline = 16777215;
        this.colorProgress = 16777215;

        if (Config.isCustomColors()) {
            try {
                String s = "optifine/color.properties";
                ResourceLocation resourcelocation = new ResourceLocation(s);

                if (!Config.hasResource(resourcelocation)) {
                    return;
                }

                InputStream inputstream = Config.getResourceStream(resourcelocation);
                Config.dbg("Loading " + s);
                Properties properties = new PropertiesOrdered();
                properties.load(inputstream);
                inputstream.close();
                this.colorBackground = readColor(properties, "screen.loading", this.colorBackground);
                this.colorOutline = readColor(properties, "screen.loading.outline", this.colorOutline);
                this.colorBar = readColor(properties, "screen.loading.bar", this.colorBar);
                this.colorProgress = readColor(properties, "screen.loading.progress", this.colorProgress);
                this.blendState = ShaderPackParser.parseBlendState(properties.getProperty("screen.loading.blend"));
            } catch (Exception exception) {
                Config.warn("" + exception.getClass().getName() + ": " + exception.getMessage());
            }
        }
    }

    private static int readColor(Properties p_readColor_0_, String p_readColor_1_, int p_readColor_2_) {
        String s = p_readColor_0_.getProperty(p_readColor_1_);

        if (s == null) {
            return p_readColor_2_;
        } else {
            s = s.trim();
            int i = parseColor(s, p_readColor_2_);

            if (i < 0) {
                Config.warn("Invalid color: " + p_readColor_1_ + " = " + s);
                return i;
            } else {
                Config.dbg(p_readColor_1_ + " = " + s);
                return i;
            }
        }
    }

    private static int parseColor(String p_parseColor_0_, int p_parseColor_1_) {
        if (p_parseColor_0_ == null) {
            return p_parseColor_1_;
        } else {
            p_parseColor_0_ = p_parseColor_0_.trim();

            try {
                return Integer.parseInt(p_parseColor_0_, 16) & 16777215;
            } catch (NumberFormatException numberformatexception) {
                return p_parseColor_1_;
            }
        }
    }

    @EventTarget
    public boolean isFadeOut() {
        return this.fadeOut;
    }

    static class MojangLogoTexture extends SimpleTexture {
        public MojangLogoTexture() {
            super(ResourceLoadProgressGui.MOJANG_LOGO_TEXTURE);
        }

        @Override
        protected SimpleTexture.TextureData getTextureData(IResourceManager resourceManager) {
            Minecraft minecraft = Minecraft.getInstance();
            VanillaPack vanillapack = minecraft.getPackFinder().getVanillaPack();

            try (InputStream inputstream = getLogoInputStream(resourceManager, vanillapack)) {
                return new SimpleTexture.TextureData(new TextureMetadataSection(true, true), NativeImage.read(inputstream));
            } catch (IOException ioexception1) {
                return new SimpleTexture.TextureData(ioexception1);
            }
        }

        private static InputStream getLogoInputStream(IResourceManager p_getLogoInputStream_0_, VanillaPack p_getLogoInputStream_1_) throws IOException {
            return p_getLogoInputStream_0_.hasResource(ResourceLoadProgressGui.MOJANG_LOGO_TEXTURE)
                    ? p_getLogoInputStream_0_.getResource(ResourceLoadProgressGui.MOJANG_LOGO_TEXTURE).getInputStream()
                    : p_getLogoInputStream_1_.getResourceStream(ResourcePackType.CLIENT_RESOURCES, ResourceLoadProgressGui.MOJANG_LOGO_TEXTURE);
        }
    }
}
