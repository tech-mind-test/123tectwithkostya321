package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.text.ITextComponent;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.impl.miscellaneous.NameProtect;
import sky.core.modules.impl.visuals.Interface;
import sky.core.modules.impl.miscellaneous.ScoreboardHealth;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class TargetHudRender implements ElementRender {

    public static final ModeSetting hpbar = new ModeSetting("Цвет здоровья", "Клиентский", "Клиентский", "Здоровье");
    public static final BooleanSetting goldhealth = new BooleanSetting("Золотые сердца", true);
    public static final BooleanSetting particles2 = new BooleanSetting("Частицы", true);
    public static final BooleanSetting ontarget = new BooleanSetting("При наведении", false);
    public static final BooleanSetting armor = new BooleanSetting("Отображать броню", false);
    public static final BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);

    private final Dragging dragging;
    private final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private final AnimationUtil healthAnimation = new AnimationUtil(0.0f, 3, Easings.CUBIC_OUT);
    private final AnimationUtil secondaryHealthAnimation = new AnimationUtil(0.0f, 3, Easings.LINEAR);
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10f, Easings.CUBIC_OUT);
    private final AnimationUtil armorScaleAnimation = new AnimationUtil(1.0f, 15f, Easings.CUBIC_OUT);
    private final AnimationUtil absorptionAnimation = new AnimationUtil(0.0f, 3, Easings.CUBIC_OUT);
    private final AnimationUtil secondaryAbsorptionAnimation = new AnimationUtil(0.0f, 3, Easings.LINEAR);
    private Entity lastTarget = null;
    private float lastHurtTime = 0;
    @Getter
    private final CopyOnWriteArrayList<HeadParticle> particles = new CopyOnWriteArrayList<>();

    @Override
    public void render(EventRender2D.Post event) {
        if (Interface.isOldHud()) {
            renderOld(event);
            return;
        }

        float posX = dragging.getX(), posY = dragging.getY();
        MatrixStack ms = event.getStack();

        LivingEntity auraTarget = SkyCore.getInstance().getModuleManager().getAttackAura().getTarget();
        Entity target = determineTarget(auraTarget);
        float targetAlpha = target != null ? 1.0f : 0.0f;

        Entity entityToUpdate = target != null ? target : lastTarget;
        if (entityToUpdate instanceof LivingEntity livingEntityToUpdate) {
            float currentHP = getHealth(livingEntityToUpdate);
            float absorption = livingEntityToUpdate.getAbsorptionAmount();

            if (target != lastTarget && target != null) {
                healthAnimation.setValue(currentHP);
                secondaryHealthAnimation.setValue(currentHP);
                absorptionAnimation.setValue(absorption);
                secondaryAbsorptionAnimation.setValue(absorption);
            }

            healthAnimation.update(currentHP);
            secondaryHealthAnimation.update(currentHP);
            absorptionAnimation.update(absorption);
            secondaryAbsorptionAnimation.update(absorption);
        }

        updateAnimations(targetAlpha);

        if (alphaAnimation.getValue() <= 0 && !particles.isEmpty()) {
            particles.clear();
        }

        if (alphaAnimation.getValue() > 0) {
            renderTargetHud(ms, target != null ? target : lastTarget, posX, posY);
        }

        lastTarget = target != null ? target : lastTarget;
        if (alphaAnimation.getValue() <= 0.01f) {
            dragging.setHeight(60f / 2f);
            dragging.setWidth(190f / 2f);
        }
    }

    private void renderOld(EventRender2D.Post event) {
        float posX = this.dragging.getX();
        float posY = this.dragging.getY();
        MatrixStack ms = event.getStack();
        LivingEntity auraTarget = SkyCore.getInstance().getModuleManager().getAttackAura().getTarget();
        Entity target = this.determineTarget(auraTarget);
        float targetAlpha = target != null ? 1.0F : 0.0F;
        Entity entityToUpdate = target != null ? target : this.lastTarget;
        if (entityToUpdate instanceof LivingEntity livingEntityToUpdate) {
            float currentHP = this.getHealth(livingEntityToUpdate);
            float absorption = livingEntityToUpdate.getAbsorptionAmount();
            if (target != this.lastTarget && target != null) {
                this.healthAnimation.setValue(currentHP);
                this.secondaryHealthAnimation.setValue(currentHP);
                this.absorptionAnimation.setValue(absorption);
                this.secondaryAbsorptionAnimation.setValue(absorption);
            }

            this.healthAnimation.update(currentHP);
            this.secondaryHealthAnimation.update(currentHP);
            this.absorptionAnimation.update(absorption);
            this.secondaryAbsorptionAnimation.update(absorption);
        }

        this.updateAnimations(targetAlpha);
        if (this.alphaAnimation.getValue() <= 0.0F && !this.particles.isEmpty()) {
            this.particles.clear();
        }

        if (this.alphaAnimation.getValue() > 0.0F) {
            this.renderTargetHudOld(ms, target != null ? target : this.lastTarget, posX, posY);
        }

        this.lastTarget = target != null ? target : this.lastTarget;
        this.dragging.setHeight(38.0F);
        this.dragging.setWidth(100.0F);
    }

    private Entity determineTarget(LivingEntity auraTarget) {
        if (ontarget.get()) {
            if (mc.currentScreen instanceof ChatScreen) {
                return mc.player;
            }
            RayTraceResult ray = mc.objectMouseOver;
            if (ray != null && ray.getType() == RayTraceResult.Type.ENTITY) {
                Entity ent = ((EntityRayTraceResult) ray).getEntity();
                return ent instanceof LivingEntity ? ent : auraTarget;
            }
            return auraTarget;
        }
        return auraTarget == null && mc.currentScreen instanceof ChatScreen ? mc.player : auraTarget;
    }

    private float getHealth(LivingEntity entity) {
        boolean useScoreboard = SkyCore.getInstance().getModuleManager().getModule(ScoreboardHealth.class).isEnabled();
        if (useScoreboard) {
            float scoreboardHealth = ServerUtil.getHealth(entity);
            float normalHealth = entity.getHealth() + entity.getAbsorptionAmount();
            if (scoreboardHealth == Math.floor(normalHealth)) {
                return entity.getHealth();
            } else {
                return Math.max(0, scoreboardHealth);
            }
        } else {
            return entity.getHealth();
        }
    }

    private void updateAnimations(float targetAlpha) {
        alphaAnimation.update(targetAlpha);
        armorScaleAnimation.update(armor.get() && targetAlpha > 0 ? 1 : 0.0f);
    }

    private void renderTargetHud(MatrixStack ms, Entity renderTarget, float posX, float posY) {
        if (!(renderTarget instanceof LivingEntity livingTarget)) return;

        float alpha = alphaAnimation.getValue();
        float currentHurtTime = livingTarget.hurtTime > 0 ? Math.min(0.5f, (float) livingTarget.hurtTime / livingTarget.maxHurtTime) : 0;

        if (particles2.get() && currentHurtTime > lastHurtTime) {
            for (int i = 0; i < 16; ++i) {
                particles.add(new HeadParticle(new Vector3d(20, 20, 0.0)));
            }
        }
        lastHurtTime = currentHurtTime;

        float currentHP = getHealth(livingTarget);
        float absorption = livingTarget.getAbsorptionAmount();
        float maxHP = livingTarget.getMaxHealth();

        float padding = 10f / 2f;
        float gap = 10f / 2f;
        float avatarSize = 40f / 2f;
        float healthSize = 20;
        float height = 60f / 2f;

        ITextComponent displayName = renderTarget.getDisplayName();
        String fullName = displayName.getString();
        String plainName = TextFormatting.getTextWithoutFormattingCodes(fullName);

        String cleanName;
        if (renderTarget instanceof PlayerEntity) {
            cleanName = ((PlayerEntity) renderTarget).getGameProfile().getName();
        } else {
            cleanName = plainName;
        }
        String privilege = resolvePrivilege(renderTarget, cleanName);
        cleanName = protectNameIfNeeded(cleanName);

        float nameWidth = Fonts.sfregular[12].getWidth(cleanName);
        float privilegeWidth = Fonts.sfregular[9].getWidth(privilege);
        float minContentWidth = avatarSize + gap + Math.max(nameWidth, privilegeWidth) + gap + healthSize + padding;
        float width = Math.max(190f / 2f, minContentWidth + padding);

        int bgColor = ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.LOGO), 0.1f);
        RenderUtil.drawRoundedRectangle(posX, posY, width, height, 5, ColorUtil.applyOpacity(bgColor, alpha));

        float avatarX = posX + padding;
        float avatarY = posY + padding;
        if (renderTarget instanceof PlayerEntity) {
            RenderUtil.drawRoundedHead(mc.getRenderManager().getRenderer(renderTarget).getEntityTexture((LivingEntity) renderTarget),
                    (LivingEntity) renderTarget, avatarX, avatarY, avatarSize, avatarSize, 2.5f, alpha);
        } else {
            RenderUtil.drawRoundedRectangle(avatarX, avatarY, avatarSize, avatarSize, 2.5f, ColorUtil.applyOpacity(ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.LOGO), 0.1f), alpha));
        }

        float infoX = avatarX + avatarSize + gap;
        float infoY = avatarY + 1;

        Fonts.sfregular[9].drawString(ms, privilege, infoX, infoY + 1.5f, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.LOGO), alpha));

        Fonts.sfregular[12].drawString(ms, cleanName, infoX - 0.2f, infoY + 6.5f, ColorUtil.applyOpacity(0xFFFFFFFF, alpha));

        if (armor.get() && livingTarget instanceof PlayerEntity playerTarget) {
            renderArmorIcons(ms, playerTarget, infoX, infoY + 28f / 2f, alpha);
        }

        float healthX = posX + width - healthSize - padding + 2;
        float healthY = posY + (height - healthSize) / 2f;
        float animatedHP = healthAnimation.getValue();
        float secondaryHP = secondaryHealthAnimation.getValue();
        float animatedAbsorption = absorptionAnimation.getValue();
        float secondaryAbsorption = secondaryAbsorptionAnimation.getValue();
        renderCircularHealth(ms, healthX, healthY, healthSize, animatedHP, secondaryHP, animatedAbsorption, secondaryAbsorption, maxHP, alpha);

        dragging.setWidth(width);
        dragging.setHeight(height);
    }

    private void renderTargetHudOld(MatrixStack ms, Entity renderTarget, float posX, float posY) {
        if (renderTarget instanceof LivingEntity livingTarget) {
            float alpha = this.alphaAnimation.getValue();
            float currentHurtTime = livingTarget.hurtTime > 0 ? Math.min(0.5F, (float)livingTarget.hurtTime / (float)livingTarget.maxHurtTime) : 0.0F;
            if ((Boolean)particles2.get() && currentHurtTime > this.lastHurtTime) {
                for(int i = 0; i < 16; ++i) {
                    this.particles.add(new HeadParticle(new Vector3d(20.0, 20.0, 0.0)));
                }
            }

            this.lastHurtTime = currentHurtTime;
            float currentHP = this.getHealth(livingTarget);
            float absorption = livingTarget.getAbsorptionAmount();
            float maxHP = livingTarget.getMaxHealth();
            float hpPercentage = Math.min(this.healthAnimation.getValue() / maxHP, 1.0F);
            float secondaryHpPercentage = Math.min(this.secondaryHealthAnimation.getValue() / maxHP, 1.0F);
            float hpBarWidth = 58.0F * hpPercentage;
            float secondaryHpBarWidth = 58.0F * secondaryHpPercentage;
            int textColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_TEXT), ThemeEditor.getAlpha(ThemeSettings.WINDOW_TEXT) / 255.0F * alpha);
            RenderUtil.drawBlurredRoundedRectangle(posX, posY, 100.0F, 38.0F, 6.5F, (Boolean)alphabg.get() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.WINDOW_BG), 0) : ThemeEditor.getColor(ThemeSettings.WINDOW_BG), alpha);
            this.renderParticlesOld(posX, posY, alpha);
            this.renderEntityHeadOld(ms, renderTarget, posX, posY, textColor, alpha);
            if ((Boolean)armor.get()) {
                this.renderArmorOld(livingTarget, posX, posY);
            }

            this.renderHealthBarOld(posX, posY, hpBarWidth, secondaryHpBarWidth, currentHP, absorption, maxHP, alpha);
            this.renderTextOld(ms, renderTarget, posX, posY, currentHP, absorption, textColor);
        }
    }

    private void renderParticlesOld(float posX, float posY, float alpha) {
        this.particles.removeIf((particlex) -> System.currentTimeMillis() - particlex.time > particlex.lifetime);

        for(HeadParticle particle : this.particles) {
            particle.update(posX, posY);
            float size = 1.0F - (float)(System.currentTimeMillis() - particle.time) / (float)particle.lifetime;
            float radius = 2.3F;
            RenderUtil.drawRoundedRectangle((float)particle.pos.x - 3.0F, (float)particle.pos.y - 3.0F, radius * 2.0F, radius * 2.0F, radius - 1.0F, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), (int)(255.0F * particle.alpha * size * alpha)));
        }
    }

    private void renderEntityHeadOld(MatrixStack ms, Entity renderTarget, float posX, float posY, int textColor, float alpha) {
        if (renderTarget instanceof PlayerEntity) {
            RenderUtil.drawRoundedHead(mc.getRenderManager().getRenderer(renderTarget).getEntityTexture((LivingEntity)renderTarget), (LivingEntity)renderTarget, posX + 3.0F, posY + 3.0F, 32.0F, 32.0F, 4.0F, alpha);
        } else {
            Fonts.icons[40].drawString(ms, "N", posX + 10.0F, posY + 12.0F, textColor);
        }
    }

    private void renderArmorOld(LivingEntity livingTarget, float posX, float posY) {
        List<ItemStack> armorStacks = new ArrayList<>();

        for(ItemStack stack : livingTarget.getArmorInventoryList()) {
            if (!stack.isEmpty()) {
                armorStacks.add(stack);
            }
        }

        int armorCount = armorStacks.size();
        ItemStack offhand = livingTarget.getHeldItemOffhand();
        ItemStack mainhand = livingTarget.getHeldItemMainhand();
        int totalItems = armorCount + (offhand.isEmpty() ? 0 : 1) + (mainhand.isEmpty() ? 0 : 1);
        if (totalItems > 0) {
            float panelWidth = this.dragging.getWidth();
            float usedWidth = (8f * totalItems + (totalItems - 1));
            float handX = posX + panelWidth - usedWidth - 3.5F;
            if (!offhand.isEmpty()) {
                RenderUtil.scaleStart(handX + 4.0F, posY - 6.0F, this.armorScaleAnimation.getValue());
                RenderUtil.drawStack(offhand, handX, posY - 10.0F, 0.5F);
                RenderUtil.scaleEnd();
                handX += 9.0F;
            }

            if (!mainhand.isEmpty()) {
                RenderUtil.scaleStart(handX + 4.0F, posY - 6.0F, this.armorScaleAnimation.getValue());
                RenderUtil.drawStack(mainhand, handX, posY - 10.0F, 0.5F);
                RenderUtil.scaleEnd();
                handX += 9.0F;
            }

            for(int i = 0; i < armorCount; ++i) {
                float itemX = handX + (i * 9f);
                RenderUtil.scaleStart(itemX + 4.0F, posY - 6.0F, this.armorScaleAnimation.getValue());
                RenderUtil.drawStack(armorStacks.get(i), itemX, posY - 10.0F, 0.5F);
                RenderUtil.scaleEnd();
            }
        }
    }

    private void renderHealthBarOld(float posX, float posY, float hpBarWidth, float secondaryHpBarWidth, float currentHP, float absorption, float maxHP, float alpha) {
        if (!(hpBarWidth <= 0.0F)) {
            float barX = posX + 37.0F;
            float barY = posY + 26.0F;
            float barWidth = 59.0F;
            float barHeight = 8.0F;
            float radius = 2.3F;
            if (hpbar.is("Клиентский")) {
                int activeColor = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
                int inactiveColor = ColorUtil.darken(activeColor, 0.5F);
                float baseAlpha = ThemeEditor.getAlpha(ThemeSettings.MODULE_VISUAL) / 255.0F * alpha;
                RenderUtil.drawRoundedRectangleGradient(barX, barY, barWidth, barHeight, radius, inactiveColor, inactiveColor, activeColor, activeColor, baseAlpha * 0.3F);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, secondaryHpBarWidth, barHeight, radius, inactiveColor, inactiveColor, activeColor, activeColor, baseAlpha * 0.75F);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, hpBarWidth + 1.0F, barHeight, radius, inactiveColor, inactiveColor, activeColor, activeColor, baseAlpha);
            } else {
                int[] colors = this.getHealthBarColorsOld(currentHP, maxHP);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, barWidth, barHeight, radius, colors[0], colors[0], colors[1], colors[1], 0.3137255F * alpha);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, secondaryHpBarWidth, barHeight, radius, colors[2], colors[2], colors[3], colors[3], 0.54901963F * alpha);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, hpBarWidth + 1.0F, barHeight, radius, colors[2], colors[2], colors[4], colors[4], alpha);
            }

            if ((Boolean)goldhealth.get() && absorption > 0.0F) {
                float animatedAbsorption = this.absorptionAnimation.getValue();
                float absorptionBarWidth = 58.0F * Math.min(animatedAbsorption / maxHP, 1.0F);
                float lagAbsorption = this.secondaryAbsorptionAnimation.getValue();
                float secondaryAbsorptionBarWidth = 58.0F * Math.min(lagAbsorption / maxHP, 1.0F);
                int goldTop = ColorUtil.getColor(255, 210, 0);
                int goldBottom = ColorUtil.darken(goldTop, 0.5F);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, secondaryAbsorptionBarWidth, barHeight, radius, ColorUtil.darken(goldBottom, 0.6F), ColorUtil.darken(goldBottom, 0.6F), ColorUtil.darken(goldTop, 0.8F), ColorUtil.darken(goldTop, 0.8F), alpha * 0.6F);
                RenderUtil.drawRoundedRectangleGradient(barX, barY, absorptionBarWidth, barHeight, radius, goldBottom, goldBottom, goldTop, goldTop, alpha);
            }
        }
    }

    private int[] getHealthBarColorsOld(float currentHP, float maxHP) {
        if ((double)currentHP >= (double)maxHP * 0.7) {
            return new int[]{ColorUtil.getColor(0, 40, 8), ColorUtil.getColor(0, 80, 15), ColorUtil.getColor(0, 60, 12), ColorUtil.getColor(0, 160, 40), ColorUtil.getColor(0, 190, 45)};
        } else {
            return (double)currentHP >= (double)maxHP * 0.35 ? new int[]{ColorUtil.getColor(50, 55, 25), ColorUtil.getColor(85, 70, 50), ColorUtil.getColor(55, 50, 22), ColorUtil.getColor(140, 130, 60), ColorUtil.getColor(160, 150, 70)} : new int[]{ColorUtil.getColor(50, 35, 25), ColorUtil.getColor(70, 45, 40), ColorUtil.getColor(80, 42, 32), ColorUtil.getColor(160, 90, 70), ColorUtil.getColor(180, 100, 75)};
        }
    }

    private void renderTextOld(MatrixStack ms, Entity renderTarget, float posX, float posY, float currentHP, float absorption, int textColor) {
        String plainName = TextFormatting.getTextWithoutFormattingCodes(renderTarget.getName().getString());
        Fonts.sf_medium[18].drawSubString(ms, plainName, posX + 37.5F, posY + 7.3F, textColor, 47.0F);
        Fonts.sf_medium[14].drawString(ms, "HP: " + this.healthFormat.format(currentHP), posX + 37.8F, posY + 18.9F, textColor);
        if ((Boolean)goldhealth.get() && absorption > 0.0F) {
            String hpText = "HP: " + this.healthFormat.format(currentHP);
            String goldText = "(" + this.healthFormat.format(absorption) + ")";
            float goldX = posX + 38.0F + Fonts.sf_medium[14].getWidth(hpText) + 1.5F;
            Fonts.sf_medium[14].drawString(ms, goldText, goldX, posY + 18.5F, textColor);
        }
    }

    private String resolvePrivilege(Entity renderTarget, String playerName) {
        if (renderTarget instanceof PlayerEntity && mc.getConnection() != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(renderTarget.getUniqueID());
            if (info == null && playerName != null) {
                info = mc.getConnection().getPlayerInfo(playerName);
            }

            if (info != null) {
                ScorePlayerTeam team = info.getPlayerTeam();
                String privilege = getPrivilege(team != null ? team.getPrefix().getString() : null, playerName);
                if (!isDefaultPrivilege(privilege)) return privilege;

                privilege = getPrivilege(team != null ? team.func_230427_d_(new StringTextComponent(playerName)).getString() : null, playerName);
                if (!isDefaultPrivilege(privilege)) return privilege;

                privilege = getPrivilege(info.getDisplayName() != null ? info.getDisplayName().getString() : null, playerName);
                if (!isDefaultPrivilege(privilege)) return privilege;
            }
        }

        if (renderTarget instanceof PlayerEntity && mc.world != null) {
            ScorePlayerTeam team = mc.world.getScoreboard().getPlayersTeam(playerName);
            String privilege = getPrivilege(team != null ? team.getPrefix().getString() : null, playerName);
            if (!isDefaultPrivilege(privilege)) return privilege;

            privilege = getPrivilege(team != null ? team.func_230427_d_(new StringTextComponent(playerName)).getString() : null, playerName);
            if (!isDefaultPrivilege(privilege)) return privilege;
        }

        return getPrivilege(renderTarget.getDisplayName().getString(), playerName);
    }

    private String getPrivilege(String rawText, String playerName) {
        String plainName = stripMinecraftFormatting(rawText);
        if (plainName == null || plainName.isEmpty()) return "Player";

        String normalizedPlayerName = stripMinecraftFormatting(playerName);
        if (normalizedPlayerName != null && !normalizedPlayerName.isEmpty()) {
            plainName = removePlayerName(plainName, normalizedPlayerName);
        }

        int startBracket = plainName.indexOf('[');
        int endBracket = plainName.indexOf(']');
        if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
            return normalizePrivilege(plainName.substring(startBracket + 1, endBracket));
        }

        int spaceIndex = plainName.indexOf(' ');
        if (spaceIndex > 0 && spaceIndex < plainName.length() - 1) {
            String possiblePrefix = plainName.substring(0, spaceIndex).trim();
            String afterSpace = plainName.substring(spaceIndex + 1).trim();
            if (!possiblePrefix.isEmpty() && !afterSpace.isEmpty()) {
                return normalizePrivilege(possiblePrefix);
            }
        }

        return normalizePrivilege(plainName);
    }

    private String stripMinecraftFormatting(String text) {
        if (text == null) return "";
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7') {
                i++;
                continue;
            }
            clean.append(c);
        }
        return clean.toString().replaceAll("\\s+", " ").trim();
    }

    private String removePlayerName(String text, String playerName) {
        if (text == null || text.isEmpty() || playerName == null || playerName.isEmpty()) return text;
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        int nameIndex = lowerText.indexOf(lowerName);
        if (nameIndex == -1) return text;
        return (text.substring(0, nameIndex) + text.substring(nameIndex + playerName.length())).replaceAll("\\s+", " ").trim();
    }

    private String normalizePrivilege(String privilege) {
        if (privilege == null) return "Player";
        String cleaned = privilege
                .replace("[", "")
                .replace("]", "")
                .replace("(", "")
                .replace(")", "")
                .replace("|", "")
                .replace("»", "")
                .replace("«", "")
                .replace(">", "")
                .replace("<", "")
                .trim();
        cleaned = cleaned.replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) return "Player";
        if (cleaned.equalsIgnoreCase("player")) return "Player";
        return toTitleCase(cleaned);
    }

    private boolean isDefaultPrivilege(String privilege) {
        return privilege == null || privilege.isEmpty() || privilege.equalsIgnoreCase("Player");
    }

    private String toTitleCase(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            } else {
                result.append(c);
                capitalizeNext = true;
            }
        }

        return result.toString();
    }


    public static class HeadParticle {
        private Vector3d offset;
        private Vector3d pos;
        private final Vector3d endOffset;
        private final long time;
        private float alpha;
        private final long lifetime;

        public HeadParticle(Vector3d offset) {
            this.offset = offset;
            this.pos = offset;
            this.endOffset = offset.add(-ThreadLocalRandom.current().nextFloat(-75.0f, 75.0f), -ThreadLocalRandom.current().nextFloat(-75.0f, 75.0f), -ThreadLocalRandom.current().nextFloat(-75.0f, 75.0f));
            this.time = System.currentTimeMillis();
            this.lifetime = 1250 + ThreadLocalRandom.current().nextLong(750);
        }

        public void update(float hudX, float hudY) {
            this.alpha = MathUtil.lerp(this.alpha, 1.0f, 10.0f);
            this.offset = MathUtil.fast(this.offset, this.endOffset, 0.75f);
            this.pos = new Vector3d(hudX + this.offset.x, hudY + this.offset.y, this.offset.z);
        }
    }

    private void renderArmorIcons(MatrixStack ms, PlayerEntity player, float x, float y, float alpha) {
        float slotSize = 7 / 2f;
        float armorGap = 8f / 2f;

        ItemStack[] armorSlots = {
                player.inventory.armorInventory.get(3),
                player.inventory.armorInventory.get(2),
                player.inventory.armorInventory.get(1),
                player.inventory.armorInventory.get(0)
        };

        ItemStack offhand = player.getHeldItemOffhand();

        float currentX = x + 1.3f;
        float slotY = y + 1;

        if (!offhand.isEmpty()) {
            RenderUtil.scaleStart(currentX + slotSize / 2f, slotY + slotSize / 2f, armorScaleAnimation.getValue());
            RenderUtil.drawStack(offhand, currentX - 2.5f, slotY - 3f, slotSize / 16f * 2f);
            RenderUtil.scaleEnd();
        } else {
            float crossWidth = Fonts.skycore[10].getWidth("k");
            float crossX = currentX + (slotSize - crossWidth) / 2f + 0.5f;
            float crossY = slotY + slotSize / 2f - 1f;
            Fonts.skycore[10].drawString(ms, "k", crossX - 1.2f, crossY, ColorUtil.applyOpacity(0xFFFFFFFF, 0.48f * alpha));
        }
        currentX += slotSize + armorGap;

        for (int i = 0; i < 4; i++) {
            if (armorSlots[i].isEmpty()) {
                float crossWidth = Fonts.skycore[10].getWidth("k");
                float crossX = currentX + (slotSize - crossWidth) / 2f + 0.5f;
                float crossY = slotY + slotSize / 2f - 1f;
                Fonts.skycore[10].drawString(ms, "k", crossX - 1.2f, crossY, ColorUtil.applyOpacity(0xFFFFFFFF, 0.48f * alpha));
            } else {
                RenderUtil.scaleStart(currentX + slotSize / 2f, slotY + slotSize / 2f, armorScaleAnimation.getValue());
                RenderUtil.drawStack(armorSlots[i], currentX - 2.5f, slotY - 3f, slotSize / 16f * 2f);
                RenderUtil.scaleEnd();
            }
            currentX += slotSize + armorGap;
        }
    }


    private int getArmorColor(ItemStack stack) {
        if (stack.getItem() == net.minecraft.item.Items.DIAMOND_HELMET ||
                stack.getItem() == net.minecraft.item.Items.DIAMOND_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.DIAMOND_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.DIAMOND_BOOTS) {
            return 0xFF2CB5A0;
        } else if (stack.getItem() == net.minecraft.item.Items.IRON_HELMET ||
                stack.getItem() == net.minecraft.item.Items.IRON_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.IRON_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.IRON_BOOTS) {
            return 0xFF8A8A8A;
        } else if (stack.getItem() == net.minecraft.item.Items.GOLDEN_HELMET ||
                stack.getItem() == net.minecraft.item.Items.GOLDEN_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.GOLDEN_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.GOLDEN_BOOTS) {
            return 0xFFD4A017;
        } else if (stack.getItem() == net.minecraft.item.Items.LEATHER_HELMET ||
                stack.getItem() == net.minecraft.item.Items.LEATHER_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.LEATHER_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.LEATHER_BOOTS) {
            return 0xFF7B4A2D;
        } else if (stack.getItem() == net.minecraft.item.Items.NETHERITE_HELMET ||
                stack.getItem() == net.minecraft.item.Items.NETHERITE_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.NETHERITE_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.NETHERITE_BOOTS) {
            return 0xFF3D3D3D;
        } else if (stack.getItem() == net.minecraft.item.Items.CHAINMAIL_HELMET ||
                stack.getItem() == net.minecraft.item.Items.CHAINMAIL_CHESTPLATE ||
                stack.getItem() == net.minecraft.item.Items.CHAINMAIL_LEGGINGS ||
                stack.getItem() == net.minecraft.item.Items.CHAINMAIL_BOOTS) {
            return 0xFF6B6B6B;
        }
        return 0xFF555555;
    }

    private String getArmorIcon(int slotIndex) {

        switch (slotIndex) {
            case 0:
                return "n";
            case 1:
                return "o";
            case 2:
                return "p";
            case 3:
                return "q";
            default:
                return "k";
        }
    }

    private int getItemColor(ItemStack stack) {
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("diamond")) return 0xFF2CB5A0;
        if (name.contains("iron")) return 0xFF8A8A8A;
        if (name.contains("golden") || name.contains("gold")) return 0xFFD4A017;
        if (name.contains("netherite")) return 0xFF3D3D3D;
        if (name.contains("wood") || name.contains("wooden")) return 0xFF7B4A2D;
        if (name.contains("stone")) return 0xFF6B6B6B;
        return 0xFF5A6DCD;
    }

    private String getItemShortName(ItemStack stack) {
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("sword")) return "l";
        if (name.contains("axe")) return "l";
        if (name.contains("bow")) return "l";
        if (name.contains("crossbow")) return "l";
        if (name.contains("trident")) return "l";
        if (name.contains("shield")) return "l";
        if (name.contains("totem")) return "l";
        return "l";
    }

    private void renderCircularHealth(MatrixStack ms, float x, float y, float size,
                                      float health, float secondaryHealth,
                                      float absorption, float secondaryAbsorption,
                                      float maxHealth, float alpha) {
        int playerColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int healthColor = ColorUtil.applyOpacity(playerColor, alpha);
        int secondaryColor = ColorUtil.applyOpacity(playerColor, 0.4f * alpha);
        int borderColor = ColorUtil.applyOpacity(0xFFFFFFFF, 0.08f * alpha);
        int absorptionColor = ColorUtil.getColor(255, 210, 0);
        int secondaryAbsorptionColor = ColorUtil.applyOpacity(ColorUtil.darken(absorptionColor, 0.4f), 0.65f * alpha);

        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        float radius = size / 2f - 2;

        RenderUtil.drawCircle(centerX, centerY, 0f, 360f, radius, 1, false, borderColor);

        if (maxHealth > 0) {
            float secondaryPercent = Math.min(secondaryHealth / maxHealth, 1.0f);
            if (secondaryPercent > 0.01f) {
                RenderUtil.drawCircle(centerX, centerY, 0f, 360 * secondaryPercent, radius, 1, false, secondaryColor);
            }

            float healthPercent = Math.min(health / maxHealth, 1.0f);
            if (healthPercent > 0.01f) {
                RenderUtil.drawCircle(centerX, centerY, 0f, 360 * healthPercent, radius, 1, false, ColorUtil.applyOpacity(healthColor, alpha));
            }

            if (goldhealth.get() && absorption > 0.01f) {
                float secondaryAbsorptionPercent = Math.min(secondaryAbsorption / maxHealth, 1.0f);
                if (secondaryAbsorptionPercent > 0.01f) {
                    RenderUtil.drawCircle(centerX, centerY, 0f, 360 * secondaryAbsorptionPercent, radius, 1, false, secondaryAbsorptionColor);
                }

                float absorptionPercent = Math.min(absorption / maxHealth, 1.0f);
                if (absorptionPercent > 0.01f) {
                    RenderUtil.drawCircle(centerX, centerY, 0f, 360 * absorptionPercent, radius, 1, false, ColorUtil.applyOpacity(absorptionColor, alpha));
                }
            }
        }

        String healthText = String.valueOf((int) health);
        float maxTextWidth = Fonts.sf_medium[12].getWidth("20");
        float textWidth = Fonts.sf_medium[12].getWidth(healthText);
        float textHeight = Fonts.sf_medium[12].getHeight();
        float blockX = centerX - maxTextWidth / 2f;
        float textX = blockX + (maxTextWidth - textWidth) / 2f;
        float textY = centerY - textHeight / 2f + 1f;

        Fonts.sf_medium[12].drawString(ms, healthText, textX + 0.2f, textY + 0.2f, ColorUtil.applyOpacity(0xFFFFFFFF, alpha));
    }

    private String protectNameIfNeeded(String name) {
        if (name == null || name.isEmpty()) return name;
        if (SkyCore.getInstance() == null) return name;
        if (!SkyCore.getInstance().getModuleManager().getModule(NameProtect.class).isEnabled()) return name;
        return NameProtect.replaceName(name);
    }
}
