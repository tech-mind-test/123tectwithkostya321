package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.shader.ShaderUtil;

import java.awt.Color;
import java.util.Collections;
import java.util.Locale;

public class Chams extends Module {
    private static final String MODE_FILL = "Заливка";
    private static final String MODE_OUTLINE = "Контур";
    private static final String MODE_BOTH = "Оба";

    private static final String DRAW_NORMAL = "Обычная";
    private static final String DRAW_SHADER = "Шейдер";
    private static final String DRAW_TOON_INK = "Тун-обводка";

    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";
    private static final String COLOR_RAINBOW = "Радуга";

    private static final String SHADER_AUTO = "Из неба";
    private static final String SHADER_PLASMA = "Плазма";
    private static final String SHADER_BALATRO = "Балатро";

    private static final String LEGACY_MODE_FILL = "Fill";
    private static final String LEGACY_MODE_OUTLINE = "Outline";
    private static final String LEGACY_MODE_BOTH = "Both";
    private static final String LEGACY_DRAW_SHADER = "Shader";
    private static final String LEGACY_DRAW_TOON_INK = "Toon Ink";
    private static final String LEGACY_COLOR_CLIENT = "Client";
    private static final String LEGACY_COLOR_CUSTOM = "Custom";
    private static final String LEGACY_COLOR_RAINBOW = "Rainbow";
    private static final String LEGACY_SHADER_PLASMA = "Plasma";
    private static final String LEGACY_SHADER_BALATRO = "Balatro";

    private static Chams instance;

    private final BooleanSetting showPlayers = new BooleanSetting("Игроки", true);
    private final BooleanSetting showMonsters = new BooleanSetting("Монстры", false);
    private final BooleanSetting showFriends = new BooleanSetting("Друзья", true);
    private final BooleanSetting showAnimals = new BooleanSetting("Животные", false);
    private final BooleanSetting showVillagers = new BooleanSetting("Жители", false);
    private final BooleanSetting showCrystals = new BooleanSetting("Кристаллы", true);
    private final BooleanSetting showSelf = new BooleanSetting("Себя", false);

    private final ModeSetting mode = new ModeSetting("Режим", MODE_BOTH, MODE_FILL, MODE_OUTLINE, MODE_BOTH);
    private final ModeSetting drawing = new ModeSetting("Отрисовка", DRAW_NORMAL, DRAW_NORMAL, DRAW_SHADER, DRAW_TOON_INK);
    private final ModeSetting shaderMode = new ModeSetting("Шейдер", SHADER_AUTO,
            this::isShaderDrawing, SHADER_AUTO, SHADER_PLASMA, SHADER_BALATRO);

    private final SliderSetting toonSteps = new SliderSetting("Шаги тона", 3.0f, 2.0f, 5.0f, 1.0f,
            this::isToonInkDrawing);
    private final SliderSetting toonShadow = new SliderSetting("Тень тона", 0.62f, 0.35f, 0.9f, 0.01f,
            this::isToonInkDrawing);
    private final ColorSetting inkColor = new ColorSetting("Цвет чернил", true, new Color(20, 20, 24, 255).getRGB(),
            this::isToonInkDrawing);
    private final SliderSetting inkWidth = new SliderSetting("Толщина чернил", 2.2f, 1.0f, 4.0f, 0.1f,
            this::isToonInkDrawing);

    private final ModeSetting colorMode = new ModeSetting("Цвет", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM, COLOR_RAINBOW);
    private final ColorSetting fillColor = new ColorSetting("Цвет заливки", true, new Color(100, 180, 255, 80).getRGB(),
            () -> isColorCustom() && wantsFill());
    private final ColorSetting outlineColor = new ColorSetting("Цвет контура", true, new Color(150, 220, 255, 255).getRGB(),
            () -> isColorCustom() && wantsOutline());
    private final SliderSetting lineWidth = new SliderSetting("Толщина контура", 1.5f, 0.5f, 5.0f, 0.1f,
            this::wantsOutline);

    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final SliderSetting glowStrength = new SliderSetting("Сила свечения", 2.0f, 1.0f, 5.0f, 0.1f,
            glow::get);
    private final SliderSetting glowLayers = new SliderSetting("Слои свечения", 3.0f, 1.0f, 6.0f, 1.0f,
            glow::get);

    private final BooleanSetting pulse = new BooleanSetting("Пульсация", false);
    private final SliderSetting pulseSpeed = new SliderSetting("Скорость пульсации", 2.0f, 0.5f, 5.0f, 0.1f,
            pulse::get);

    private final BooleanSetting hurtEffect = new BooleanSetting("Эффект урона", true);
    private final ColorSetting hurtColor = new ColorSetting("Цвет урона", false, new Color(255, 50, 50, 255).getRGB(),
            hurtEffect::get);

    private final BooleanSetting customFriendColor = new BooleanSetting("Свой цвет друзей", false);
    private final ColorSetting friendFillColor = new ColorSetting("Заливка друзей", true, new Color(85, 255, 85, 60).getRGB(),
            customFriendColor::get);
    private final ColorSetting friendOutlineColor = new ColorSetting("Контур друзей", true, new Color(100, 255, 100, 255).getRGB(),
            customFriendColor::get);

    private final BooleanSetting hideOriginal = new BooleanSetting("Скрывать оригинал", false);
    private final BooleanSetting hidePlayers = new BooleanSetting("Скрывать игроков", true, hideOriginal::get);
    private final BooleanSetting hideSelf = new BooleanSetting("Скрывать себя", false, hideOriginal::get);
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", true);

    private long enabledAtMs = System.currentTimeMillis();

    public Chams() {
        super("Chams", "Подсветка сущностей через стены", Category.Visuals);
        instance = this;
        addSettings(
                showPlayers,
                showMonsters,
                showFriends,
                showAnimals,
                showVillagers,
                showCrystals,
                showSelf,
                mode,
                drawing,
                shaderMode,
                toonSteps,
                toonShadow,
                inkColor,
                inkWidth,
                colorMode,
                fillColor,
                outlineColor,
                lineWidth,
                glow,
                glowStrength,
                glowLayers,
                pulse,
                pulseSpeed,
                hurtEffect,
                hurtColor,
                customFriendColor,
                friendFillColor,
                friendOutlineColor,
                hideOriginal,
                hidePlayers,
                hideSelf,
                throughWalls
        );
    }

    public static Chams getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        enabledAtMs = System.currentTimeMillis();
        instance = this;
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        renderWorld(event.getPartialTicks());
    }

    public boolean shouldHideOriginal(Entity entity) {
        if (!isEnabled() || !hideOriginal.get() || entity == null) {
            return false;
        }

        if (entity == mc.player) {
            return hideSelf.get() && showSelf.get() && isThirdPersonView();
        }

        if (entity instanceof PlayerEntity) {
            return hidePlayers.get() && isChamsTarget(entity);
        }

        return false;
    }

    public boolean isChamsTarget(Entity entity) {
        if (!isEnabled() || entity == null || entity.removed) {
            return false;
        }

        if (entity instanceof EnderCrystalEntity) {
            return showCrystals.get();
        }

        if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
            return false;
        }

        if (entity == mc.player) {
            return showSelf.get() && isThirdPersonView();
        }

        if (entity instanceof PlayerEntity) {
            return isFriend((PlayerEntity) entity) ? showFriends.get() : showPlayers.get();
        }

        if (entity instanceof MonsterEntity) {
            return showMonsters.get();
        }

        if (entity instanceof AnimalEntity) {
            return showAnimals.get();
        }

        if (entity instanceof VillagerEntity) {
            return showVillagers.get();
        }

        return false;
    }

    private void renderWorld(float partialTicks) {
        if (!isEnabled() || mc.player == null || mc.world == null) {
            return;
        }

        setupRenderState();
        try {
            for (PlayerEntity player : getWorldPlayers()) {
                if (!isChamsTarget(player)) {
                    continue;
                }
                if (player instanceof AbstractClientPlayerEntity) {
                    renderPlayer((AbstractClientPlayerEntity) player, partialTicks);
                } else {
                    renderGenericEntity(player, partialTicks);
                }
            }

            if (shouldRenderGenericTargets()) {
                for (Entity entity : getWorldEntities()) {
                    if (entity instanceof PlayerEntity || !isChamsTarget(entity)) {
                        continue;
                    }
                    renderGenericEntity(entity, partialTicks);
                }
            }
        } finally {
            restoreRenderState();
        }
    }

    private boolean shouldRenderGenericTargets() {
        return showMonsters.get() || showAnimals.get() || showVillagers.get() || showCrystals.get();
    }

    private void renderPlayer(AbstractClientPlayerEntity player, float partialTicks) {
        EntityRenderer<? super AbstractClientPlayerEntity> renderer = mc.getRenderManager().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer)) {
            renderGenericEntity(player, partialTicks);
            return;
        }

        PlayerRenderer playerRenderer = (PlayerRenderer) renderer;
        PlayerModel<AbstractClientPlayerEntity> model = playerRenderer.getEntityModel();
        preparePlayerModel(model, player, partialTicks);

        boolean friend = isFriend(player);
        boolean damaged = hurtEffect.get() && player.hurtTime > 0;
        Color fill = applyPulse(resolveFillColor(friend, damaged));
        Color outline = applyPulse(resolveOutlineColor(friend, damaged));
        if (isToonInkDrawing()) {
            fill = quantizeToonColor(fill);
            Color ink = new Color(inkColor.get(), true);
            outline = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), outline.getAlpha());
        }

        ShaderUtil shader = wantsShaderFill() ? resolveShader() : null;
        boolean shaderFill = shader != null;
        if (shaderFill) {
            bindShader(shader, fill, partialTicks);
        }

        Vector3f shaderOrigin = getShaderOrigin(player, partialTicks);
        try {
            if (glow.get()) {
                int layers = Math.max(1, Math.round(glowLayers.get()));
                float strength = glowStrength.get();
                for (int layer = layers; layer >= 0; layer--) {
                    float expand = layer * 0.5f * strength;
                    float alphaMultiplier = layer == 0 ? 1.0f : 0.7f / (layer + 1.0f);
                    renderPlayerParts(
                            player,
                            model,
                            partialTicks,
                            expand,
                            withAlpha(fill, Math.round(fill.getAlpha() * alphaMultiplier)),
                            withAlpha(outline, Math.round(outline.getAlpha() * alphaMultiplier)),
                            shaderFill,
                            shaderOrigin
                    );
                }
            } else {
                renderPlayerParts(player, model, partialTicks, 0.0f, fill, outline, shaderFill, shaderOrigin);
            }
        } finally {
            if (shaderFill) {
                shader.detach();
            }
        }
    }

    private void renderPlayerParts(AbstractClientPlayerEntity player, PlayerModel<AbstractClientPlayerEntity> model,
                                   float partialTicks, float expand, Color fill, Color outline,
                                   boolean shaderFill, Vector3f shaderOrigin) {
        MatrixStack stack = new MatrixStack();
        applyPlayerTransform(stack, player, partialTicks);
        renderModelBox(stack, model.bipedHead, -4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, expand, fill, outline, shaderFill, shaderOrigin);
        renderModelBox(stack, model.bipedBody, -4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, expand, fill, outline, shaderFill, shaderOrigin);
        renderModelBox(stack, model.bipedRightArm, -3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, fill, outline, shaderFill, shaderOrigin);
        renderModelBox(stack, model.bipedLeftArm, -1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, fill, outline, shaderFill, shaderOrigin);
        renderModelBox(stack, model.bipedRightLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, fill, outline, shaderFill, shaderOrigin);
        renderModelBox(stack, model.bipedLeftLeg, -2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, expand, fill, outline, shaderFill, shaderOrigin);
    }

    private void renderModelBox(MatrixStack stack, ModelRenderer part, float offX, float offY, float offZ,
                                float width, float height, float depth, float expand,
                                Color fill, Color outline, boolean shaderFill, Vector3f shaderOrigin) {
        stack.push();
        part.translateRotate(stack);

        float scale = 0.0625f;
        float expandScale = expand * scale;
        float minX = offX * scale - expandScale;
        float minY = offY * scale - expandScale;
        float minZ = offZ * scale - expandScale;
        float maxX = (offX + width) * scale + expandScale;
        float maxY = (offY + height) * scale + expandScale;
        float maxZ = (offZ + depth) * scale + expandScale;

        renderBox(stack.getLast().getMatrix(), minX, minY, minZ, maxX, maxY, maxZ, fill, outline, shaderFill, shaderOrigin);
        stack.pop();
    }

    private void renderGenericEntity(Entity entity, float partialTicks) {
        boolean friend = entity instanceof PlayerEntity && isFriend((PlayerEntity) entity);
        boolean damaged = hurtEffect.get() && entity instanceof LivingEntity && ((LivingEntity) entity).hurtTime > 0;

        Color fill = applyPulse(resolveFillColor(friend, damaged));
        Color outline = applyPulse(resolveOutlineColor(friend, damaged));
        if (isToonInkDrawing()) {
            fill = quantizeToonColor(fill);
            Color ink = new Color(inkColor.get(), true);
            outline = new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), outline.getAlpha());
        }

        ShaderUtil shader = wantsShaderFill() ? resolveShader() : null;
        boolean shaderFill = shader != null;
        if (shaderFill) {
            bindShader(shader, fill, partialTicks);
        }

        try {
            MatrixStack stack = new MatrixStack();
            AxisAlignedBB box = getInterpolatedBox(entity, partialTicks);
            Vector3d camera = getCameraPosition();
            Vector3f shaderOrigin = getShaderOrigin(box, camera);
            stack.translate(-camera.x, -camera.y, -camera.z);

            if (glow.get()) {
                int layers = Math.max(1, Math.round(glowLayers.get()));
                float strength = glowStrength.get() * 0.015f;
                for (int layer = layers; layer >= 0; layer--) {
                    float expand = layer * strength;
                    float alphaMultiplier = layer == 0 ? 1.0f : 0.7f / (layer + 1.0f);
                    renderBox(
                            stack.getLast().getMatrix(),
                            (float) box.minX - expand,
                            (float) box.minY - expand,
                            (float) box.minZ - expand,
                            (float) box.maxX + expand,
                            (float) box.maxY + expand,
                            (float) box.maxZ + expand,
                            withAlpha(fill, Math.round(fill.getAlpha() * alphaMultiplier)),
                            withAlpha(outline, Math.round(outline.getAlpha() * alphaMultiplier)),
                            shaderFill,
                            shaderOrigin
                    );
                }
            } else {
                renderBox(
                        stack.getLast().getMatrix(),
                        (float) box.minX,
                        (float) box.minY,
                        (float) box.minZ,
                        (float) box.maxX,
                        (float) box.maxY,
                        (float) box.maxZ,
                        fill,
                        outline,
                        shaderFill,
                        shaderOrigin
                );
            }
        } finally {
            if (shaderFill) {
                shader.detach();
            }
        }
    }

    private void renderBox(Matrix4f matrix, float minX, float minY, float minZ,
                           float maxX, float maxY, float maxZ,
                           Color fill, Color outline, boolean shaderFill, Vector3f shaderOrigin) {
        if (wantsFill()) {
            if (shaderFill) {
                drawShaderFill(matrix, minX, minY, minZ, maxX, maxY, maxZ, shaderOrigin);
            } else {
                drawFilledBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, fill);
            }
        }

        if (wantsOutline()) {
            float width = isToonInkDrawing() ? inkWidth.get() : lineWidth.get();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glLineWidth(width);
            drawOutlinedBox(matrix, minX, minY, minZ, maxX, maxY, maxZ, outline);
            if (!isToonInkDrawing()) {
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
            }
        }
    }

    private void drawFilledBox(Matrix4f matrix, float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        putQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        putQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, color);
        putQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, color);
        putQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        putQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, color);
        putQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        tessellator.draw();
    }

    private void drawShaderFill(Matrix4f matrix, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, Vector3f shaderOrigin) {
        Vector3f localOrigin = new Vector3f(
                (minX + maxX) * 0.5f,
                (minY + maxY) * 0.5f,
                (minZ + maxZ) * 0.5f
        );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        putShaderQuad(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, localOrigin);
        putShaderQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, localOrigin);
        putShaderQuad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, localOrigin);
        putShaderQuad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, localOrigin);
        putShaderQuad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, localOrigin);
        putShaderQuad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, localOrigin);
        tessellator.draw();
    }

    private void drawOutlinedBox(Matrix4f matrix, float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        putLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, color);
        putLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, color);
        putLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, color);
        putLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, color);
        putLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, color);
        putLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        putLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        putLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, color);
        putLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, color);
        putLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, color);
        putLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        putLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, color);
        tessellator.draw();
    }

    private void putQuad(BufferBuilder buffer, Matrix4f matrix,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         Color color) {
        putVertex(buffer, matrix, x0, y0, z0, color);
        putVertex(buffer, matrix, x1, y1, z1, color);
        putVertex(buffer, matrix, x2, y2, z2, color);
        putVertex(buffer, matrix, x3, y3, z3, color);
    }

    private void putShaderQuad(BufferBuilder buffer, Matrix4f matrix,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               Vector3f shaderOrigin) {
        putShaderVertex(buffer, matrix, x0, y0, z0, shaderOrigin);
        putShaderVertex(buffer, matrix, x1, y1, z1, shaderOrigin);
        putShaderVertex(buffer, matrix, x2, y2, z2, shaderOrigin);
        putShaderVertex(buffer, matrix, x3, y3, z3, shaderOrigin);
    }

    private void putLine(BufferBuilder buffer, Matrix4f matrix,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         Color color) {
        putVertex(buffer, matrix, x0, y0, z0, color);
        putVertex(buffer, matrix, x1, y1, z1, color);
    }

    private void putVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Color color) {
        buffer.pos(matrix, x, y, z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                .endVertex();
    }

    private void putShaderVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, Vector3f shaderOrigin) {
        Vector4f vertex = new Vector4f(x, y, z, 1.0f);
        vertex.transform(matrix);

        float dx = x - shaderOrigin.getX();
        float dy = y - shaderOrigin.getY();
        float dz = z - shaderOrigin.getZ();
        float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 1.0E-5f) {
            dx /= length;
            dy /= length;
            dz /= length;
        } else {
            dx = 0.0f;
            dy = 1.0f;
            dz = 0.0f;
        }

        int r = MathHelper.clamp(Math.round((dx * 0.5f + 0.5f) * 255.0f), 0, 255);
        int g = MathHelper.clamp(Math.round((dy * 0.5f + 0.5f) * 255.0f), 0, 255);
        int b = MathHelper.clamp(Math.round((dz * 0.5f + 0.5f) * 255.0f), 0, 255);
        buffer.pos(vertex.getX(), vertex.getY(), vertex.getZ())
                .color(r, g, b, 255)
                .endVertex();
    }

    private void preparePlayerModel(PlayerModel<AbstractClientPlayerEntity> model,
                                    AbstractClientPlayerEntity player,
                                    float partialTicks) {
        float bodyYaw = interpolateAngle(partialTicks, player.prevRenderYawOffset, player.renderYawOffset);
        float limbSwingAmount = MathHelper.lerp(partialTicks, player.prevLimbSwingAmount, player.limbSwingAmount);
        float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0f - partialTicks);
        float age = player.ticksExisted + partialTicks;
        float netHeadYaw = interpolateAngle(partialTicks, player.prevRotationYawHead, player.rotationYawHead) - bodyYaw;
        float headPitch = MathHelper.lerp(partialTicks, player.prevRotationPitch, player.rotationPitch);

        model.swingProgress = player.getSwingProgress(partialTicks);
        model.isSneak = player.isCrouching();
        model.isSitting = player.isPassenger();
        model.swimAnimation = player.getSwimAnimation(partialTicks);
        model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);
        model.setRotationAngles(player, limbSwing, limbSwingAmount, age, netHeadYaw, headPitch);
    }

    private void applyPlayerTransform(MatrixStack stack, AbstractClientPlayerEntity player, float partialTicks) {
        Vector3d position = getInterpolatedPosition(player, partialTicks);
        Vector3d camera = getCameraPosition();
        stack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);

        float bodyYaw = interpolateAngle(partialTicks, player.prevRenderYawOffset, player.renderYawOffset);
        float swimAnimation = player.getSwimAnimation(partialTicks);
        if (player.isElytraFlying()) {
            stack.rotate(Vector3f.YP.rotationDegrees(180.0f - bodyYaw));
            float elytraTicks = player.getTicksElytraFlying() + partialTicks;
            float elytraProgress = MathHelper.clamp(elytraTicks * elytraTicks / 100.0f, 0.0f, 1.0f);
            if (!player.isSpinAttacking()) {
                stack.rotate(Vector3f.XP.rotationDegrees(elytraProgress * (-90.0f - player.rotationPitch)));
            }
            Vector3d look = player.getLook(partialTicks);
            Vector3d motion = player.getMotion();
            double motionHorizontal = motion.x * motion.x + motion.z * motion.z;
            double lookHorizontal = look.x * look.x + look.z * look.z;
            if (motionHorizontal > 0.0 && lookHorizontal > 0.0) {
                double dot = (motion.x * look.x + motion.z * look.z) / Math.sqrt(motionHorizontal * lookHorizontal);
                double cross = motion.x * look.z - motion.z * look.x;
                stack.rotate(Vector3f.YP.rotation((float) (Math.signum(cross) * Math.acos(MathHelper.clamp(dot, -1.0, 1.0)))));
            }
        } else if (swimAnimation > 0.0f) {
            stack.rotate(Vector3f.YP.rotationDegrees(180.0f - bodyYaw));
            float swimAngle = player.isInWater() ? -90.0f - player.rotationPitch : -90.0f;
            stack.rotate(Vector3f.XP.rotationDegrees(MathHelper.lerp(swimAnimation, 0.0f, swimAngle)));
            if (player.isActualySwimming()) {
                stack.translate(0.0, -1.0, 0.3);
            }
        } else {
            stack.rotate(Vector3f.YP.rotationDegrees(180.0f - bodyYaw));
        }

        if (player.isCrouching()) {
            stack.translate(0.0, -0.11, 0.0);
        }

        stack.scale(-1.0f, -1.0f, 1.0f);
        stack.scale(0.9375f, 0.9375f, 0.9375f);
        stack.translate(0.0, -1.501, 0.0);
    }

    private AxisAlignedBB getInterpolatedBox(Entity entity, float partialTicks) {
        Vector3d current = entity.getPositionVec();
        Vector3d interpolated = getInterpolatedPosition(entity, partialTicks);
        return entity.getBoundingBox().offset(interpolated.subtract(current));
    }

    private Vector3f getShaderOrigin(Entity entity, float partialTicks) {
        return getShaderOrigin(getInterpolatedBox(entity, partialTicks), getCameraPosition());
    }

    private Vector3f getShaderOrigin(AxisAlignedBB box, Vector3d camera) {
        return new Vector3f(
                (float) ((box.minX + box.maxX) * 0.5 - camera.x),
                (float) ((box.minY + box.maxY) * 0.5 - camera.y),
                (float) ((box.minZ + box.maxZ) * 0.5 - camera.z)
        );
    }

    private Vector3d getInterpolatedPosition(Entity entity, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, entity.lastTickPosX, entity.getPosX());
        double y = MathHelper.lerp(partialTicks, entity.lastTickPosY, entity.getPosY());
        double z = MathHelper.lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ());
        return new Vector3d(x, y, z);
    }

    private Vector3d getCameraPosition() {
        ActiveRenderInfo camera = mc.getRenderManager().info != null
                ? mc.getRenderManager().info
                : mc.gameRenderer.getActiveRenderInfo();
        return camera.getProjectedView();
    }

    private void setupRenderState() {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        if (glow.get()) {
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        GL11.glShadeModel(GL11.GL_SMOOTH);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void restoreRenderState() {
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glLineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.popMatrix();
    }

    private void bindShader(ShaderUtil shader, Color fill, float partialTicks) {
        int theme = getClientColor();
        float time = (mc.world == null ? 0.0f : (mc.world.getGameTime() + partialTicks)) * getAnimationSpeed();
        shader.attach();
        shader.setUniformf("u_Color", fill.getRed() / 255.0f, fill.getGreen() / 255.0f, fill.getBlue() / 255.0f, 1.0f);
        shader.setUniformf("u_Scale", getShaderScale());
        shader.setUniformf("u_Time", time * 0.08f);
        shader.setUniformf("u_Alpha", fill.getAlpha() / 255.0f);
        shader.setUniformf("color",
                ColorUtil.red(theme) / 255.0f,
                ColorUtil.green(theme) / 255.0f,
                ColorUtil.blue(theme) / 255.0f,
                1.0f
        );
    }

    private ShaderUtil resolveShader() {
        if (isSelected(shaderMode, SHADER_BALATRO, LEGACY_SHADER_BALATRO)) {
            return ShaderUtil.block_overlay_sky_balatro;
        }
        if (isSelected(shaderMode, SHADER_PLASMA, LEGACY_SHADER_PLASMA)) {
            return ShaderUtil.block_overlay_sky_plasma;
        }

        String ambienceSky = Ambience.skyMode.get();
        if (ambienceSky != null) {
            String normalized = ambienceSky.toLowerCase(Locale.ROOT);
            if (normalized.contains("balatro") || normalized.contains("балатро")) {
                return ShaderUtil.block_overlay_sky_balatro;
            }
        }
        return ShaderUtil.block_overlay_sky_plasma;
    }

    private boolean wantsFill() {
        return isSelected(mode, MODE_FILL, LEGACY_MODE_FILL)
                || isSelected(mode, MODE_BOTH, LEGACY_MODE_BOTH);
    }

    private boolean wantsOutline() {
        return isSelected(mode, MODE_OUTLINE, LEGACY_MODE_OUTLINE)
                || isSelected(mode, MODE_BOTH, LEGACY_MODE_BOTH);
    }

    private boolean wantsShaderFill() {
        return isShaderDrawing() && wantsFill();
    }

    private boolean isShaderDrawing() {
        return isSelected(drawing, DRAW_SHADER, LEGACY_DRAW_SHADER);
    }

    private boolean isToonInkDrawing() {
        return isSelected(drawing, DRAW_TOON_INK, LEGACY_DRAW_TOON_INK);
    }

    private boolean isColorCustom() {
        return isSelected(colorMode, COLOR_CUSTOM, LEGACY_COLOR_CUSTOM);
    }

    private boolean isColorRainbow() {
        return isSelected(colorMode, COLOR_RAINBOW, LEGACY_COLOR_RAINBOW);
    }

    private boolean isColorClient() {
        return isSelected(colorMode, COLOR_CLIENT, LEGACY_COLOR_CLIENT);
    }

    private boolean isSelected(ModeSetting setting, String value, String legacyValue) {
        String current = setting.get();
        return current != null
                && (current.equalsIgnoreCase(value) || current.equalsIgnoreCase(legacyValue));
    }

    private Color resolveFillColor(boolean friend, boolean damaged) {
        if (damaged) {
            Color damage = new Color(hurtColor.get(), true);
            return new Color(damage.getRed(), damage.getGreen(), damage.getBlue(), 80);
        }
        if (friend && customFriendColor.get()) {
            return new Color(friendFillColor.get(), true);
        }
        if (isColorRainbow()) {
            Color rainbow = Color.getHSBColor((System.currentTimeMillis() % 3000L) / 3000.0f, 0.7f, 1.0f);
            return new Color(rainbow.getRed(), rainbow.getGreen(), rainbow.getBlue(), 60);
        }
        if (isColorClient()) {
            int client = getClientColor();
            return new Color(ColorUtil.red(client), ColorUtil.green(client), ColorUtil.blue(client), 60);
        }
        return new Color(fillColor.get(), true);
    }

    private Color resolveOutlineColor(boolean friend, boolean damaged) {
        if (damaged) {
            return new Color(hurtColor.get(), true);
        }
        if (friend && customFriendColor.get()) {
            return new Color(friendOutlineColor.get(), true);
        }
        if (isColorRainbow()) {
            return Color.getHSBColor((System.currentTimeMillis() % 3000L) / 3000.0f, 0.8f, 1.0f);
        }
        if (isColorClient()) {
            int client = getClientColor();
            return new Color(ColorUtil.red(client), ColorUtil.green(client), ColorUtil.blue(client), 255);
        }
        return new Color(outlineColor.get(), true);
    }

    private Color quantizeToonColor(Color color) {
        int steps = Math.max(2, Math.round(toonSteps.get()));
        float shadow = MathHelper.clamp(toonShadow.get(), 0.2f, 1.0f);
        float step = 255.0f / (steps - 1);
        int r = MathHelper.clamp(Math.round(Math.round(color.getRed() * shadow / step) * step), 0, 255);
        int g = MathHelper.clamp(Math.round(Math.round(color.getGreen() * shadow / step) * step), 0, 255);
        int b = MathHelper.clamp(Math.round(Math.round(color.getBlue() * shadow / step) * step), 0, 255);
        return new Color(r, g, b, color.getAlpha());
    }

    private Color applyPulse(Color color) {
        if (!pulse.get()) {
            return color;
        }
        float time = (System.currentTimeMillis() - enabledAtMs) / 1000.0f;
        float wave = (float) ((Math.sin(time * pulseSpeed.get() * Math.PI) + 1.0) * 0.5);
        float alpha = 0.3f + 0.7f * wave;
        return withAlpha(color, Math.round(color.getAlpha() * alpha));
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                MathHelper.clamp(alpha, 0, 255)
        );
    }

    private float interpolateAngle(float partialTicks, float previous, float current) {
        return previous + MathHelper.wrapDegrees(current - previous) * partialTicks;
    }

    private Iterable<? extends PlayerEntity> getWorldPlayers() {
        return mc.world == null ? Collections.emptyList() : mc.world.getPlayers();
    }

    private Iterable<Entity> getWorldEntities() {
        return mc.world == null ? Collections.emptyList() : mc.world.getAllEntities();
    }

    private boolean isFriend(PlayerEntity player) {
        return player != null
                && SkyCore.getInstance().getFriendManager() != null
                && SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
    }

    private boolean isThirdPersonView() {
        return mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON;
    }

    private int getClientColor() {
        return ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
    }

    private float getShaderScale() {
        return Ambience.skyScale.get();
    }

    private float getAnimationSpeed() {
        return Ambience.skySpeed.get();
    }
}
