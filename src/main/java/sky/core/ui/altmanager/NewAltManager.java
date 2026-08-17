package sky.core.ui.altmanager;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.texture.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import com.adl.nativeprotect.Native;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NewAltManager extends Screen {
    private static final ResourceLocation MENU_BACKGROUND = new ResourceLocation("minecraft", "SkyCore/mainmenu/background.png");

    private float fade = 1f;
    private boolean fadingOut = false;
    private Screen pendingScreen = null;
    private long fadeFrameTime = -1;
    private final boolean showFadeIn;

    private final List<Account> accounts = new ArrayList<>();
    private String selectedString = "";
    private float scroll;
    private float scrollAnimation;
    private boolean loaded;
    private long lastClickTime;
    private String lastClickedAccount = "";

    private float addHover = 0f;
    private long addHoverTime = -1;

    private long lastFrameTime = -1;
    private float modalAnim = 0f;
    private long modalAnimTime = -1;
    private final Map<String, Float> hoverMap = new HashMap<>();
    private final Map<String, Long>  hoverTimeMap = new HashMap<>();

    private final AnimationUtil scrollAnim = new AnimationUtil(0f, 8f, Easings.CUBIC_OUT);
    private final Map<String, AnimationUtil> selectAnimMap = new HashMap<>();
    private final Map<String, AnimationUtil> deleteAnimMap = new HashMap<>();
    private final Map<String, Long> deletingMap = new HashMap<>();

    private boolean showAddModal = false;
    private String modalInput = "";
    private boolean modalFieldFocused = true;
    private boolean modalCursorVisible = true;
    private long modalLastBlink = System.currentTimeMillis();
    private float modalCreateHover = 0f;
    private long modalCreateHoverTime = -1;
    private float modalGenHover = 0f;
    private long modalGenHoverTime = -1;
    private static final String[] RANDOM_NAMES = {
            "leoaer", "rixando", "lucarde", "hengiro", "bevunho", "thionme",
            "andmurgo", "guivuson", "reilherpe", "ferano", "Alemurer", "cloonca",
            "Stepanh", "vanosik", "madboss", "wh1tolox", "roflPen", "Gentelman"
    };

    private static final String SERVER_NAME = "Лучший ХвХ сервер";
    private static final String SERVER_IP   = "mc.nighthvh.space";
    private static final String SERVER2_NAME = "ReallyWorld";
    private static final String SERVER2_IP   = "mc.reallyworld.ru";
    private final ServerData serverData = new ServerData(SERVER_NAME, SERVER_IP, false);
    private final ServerData serverData2 = new ServerData(SERVER2_NAME, SERVER2_IP, false);
    private final ServerPinger serverPinger = new ServerPinger();
    private ResourceLocation serverIconLoc;
    private DynamicTexture serverIcon;
    private String lastIconB64;
    private ResourceLocation serverIconLoc2;
    private DynamicTexture serverIcon2;
    private String lastIconB642;

    public NewAltManager() {
        this(true);
    }

    public NewAltManager(boolean fadeIn) {
        super(new StringTextComponent("AltManager"));
        this.showFadeIn = fadeIn;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Native
    @Override
    protected void init() {
        super.init();
        if (showFadeIn) {
            fade = 0f;
        } else {
            fade = 1f;
        }
        fadingOut = false;
        pendingScreen = null;
        fadeFrameTime = -1;

        if (!loaded) {
            AltConfig.init(this.accounts);
            loaded = true;
            if (this.minecraft.session != null) {
                this.selectedString = this.minecraft.session.getUsername();
            }
        }
        serverIconLoc = new ResourceLocation("servers/" + Hashing.sha1().hashUnencodedChars(SERVER_IP) + "/icon");
        serverIcon = (DynamicTexture) this.minecraft.getTextureManager().getTexture(serverIconLoc);
        serverIconLoc2 = new ResourceLocation("servers/" + Hashing.sha1().hashUnencodedChars(SERVER2_IP) + "/icon");
        serverIcon2 = (DynamicTexture) this.minecraft.getTextureManager().getTexture(serverIconLoc2);
        pingServerIfNeeded(serverData, "ServerPinger-AltManager-1");
        pingServerIfNeeded(serverData2, "ServerPinger-AltManager-2");
    }

    private void pingServerIfNeeded(ServerData data, String threadName) {
        if (data.pinged) return;
        data.pinged = true;
        data.pingToServer = -2L;
        new Thread(() -> {
            try { serverPinger.ping(data, () -> {}); } catch (Exception ignored) {}
        }, threadName).start();
    }

    private void updateServerIconFromPing(ServerData data, ResourceLocation iconLoc, DynamicTexture currentTexture, String lastB64, IconUpdateResult out) {
        String iconB64 = data.getBase64EncodedIconData();
        if (!Objects.equals(iconB64, lastB64)) {
            out.lastB64 = iconB64;
            if (iconB64 != null) {
                try {
                    NativeImage img = NativeImage.readBase64(iconB64);
                    DynamicTexture tex = currentTexture;
                    if (tex == null) {
                        tex = new DynamicTexture(img);
                    } else {
                        tex.setTextureData(img);
                        tex.updateDynamicTexture();
                    }
                    this.minecraft.getTextureManager().loadTexture(iconLoc, tex);
                    out.texture = tex;
                } catch (Exception ignored) {}
            }
        }
    }

    private static final class IconUpdateResult {
        String lastB64;
        DynamicTexture texture;
    }

    public void addAccount(String name) {
        if (name == null || name.trim().isEmpty()) return;
        Account account = new Account(name.trim());
        this.accounts.add(account);
        account.loadSkinAsync();
        AltConfig.updateFile(this.accounts);
    }

    private void loginSelected() {
        if (this.selectedString == null || this.selectedString.isEmpty()) return;
        for (Account account : this.accounts) {
            if (account.accountName.equals(this.selectedString)) {
                this.minecraft.session = new Session(account.accountName, "", "", "mojang");
                AltConfig.updateFile(this.accounts);
                break;
            }
        }
    }

    private void deleteSelected() {
        if (this.selectedString == null || this.selectedString.isEmpty()) return;
        Iterator<Account> it = this.accounts.iterator();
        while (it.hasNext()) {
            Account a = it.next();
            if (a.accountName.equals(this.selectedString)) {
                it.remove();
                this.selectedString = "";
                AltConfig.updateFile(this.accounts);
                break;
            }
        }
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        long nowFade = System.currentTimeMillis();
        float fadeDelta = fadeFrameTime < 0 ? 0f : Math.min((nowFade - fadeFrameTime) / 1000f, 0.1f);
        fadeFrameTime = nowFade;
        float fadeSpeed = 6.5f;
        if (fadingOut) {
            fade = Math.max(fade - fadeDelta * fadeSpeed, 0f);
            if (fade <= 0f && pendingScreen != null) {
                this.minecraft.displayGuiScreen(pendingScreen);
                return;
            }
        } else {
            fade = Math.min(fade + fadeDelta * fadeSpeed, 1f);
        }

        long nowMs = System.currentTimeMillis();
        float delta = lastFrameTime < 0 ? 0f : Math.min((nowMs - lastFrameTime) / 1000f, 0.1f);
        lastFrameTime = nowMs;
        float fa = fade;
        int fa255 = (int)(255f * fa);

        int bgFill = ((int)(255f * fade) << 24) | 0x151418;
        fill(ms, 0, 0, this.width, this.height, bgFill);
        RenderSystem.color4f(1f, 1f, 1f, fade);
        this.minecraft.getTextureManager().bindTexture(MENU_BACKGROUND);
        blit(ms, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        IconUpdateResult icon1 = new IconUpdateResult();
        icon1.lastB64 = lastIconB64;
        icon1.texture = serverIcon;
        updateServerIconFromPing(serverData, serverIconLoc, serverIcon, lastIconB64, icon1);
        lastIconB64 = icon1.lastB64;
        serverIcon = icon1.texture;

        IconUpdateResult icon2 = new IconUpdateResult();
        icon2.lastB64 = lastIconB642;
        icon2.texture = serverIcon2;
        updateServerIconFromPing(serverData2, serverIconLoc2, serverIcon2, lastIconB642, icon2);
        lastIconB642 = icon2.lastB64;
        serverIcon2 = icon2.texture;

        int gray   = ColorUtil.getColor(103, 106, 114, (int)(0.85f * fa255));
        int white  = ColorUtil.getColor(255, 255, 255, fa255);
        int cardBorder = ColorUtil.getColor(103, 106, 114, (int)(0.15f * fa255));
        int cba    = (int)(0.15f * fa255);

        float cardW      = 160f;
        float cardH      = 28f;
        float cardRadius = 4f;
        float cardGapX   = 6f;
        float cardGapY   = 6f;
        float skinSize   = 18f;
        float blockW     = cardW * 2 + cardGapX;
        float blockX     = (float) Math.round((this.width - blockW) / 2f);

        float secH   = Fonts.sf_regular[16].getHeight();
        int   maxRowsConst = 3;
        float headerH = Fonts.sf_regular[20].getHeight() + Fonts.sf_regular[14].getHeight() + 1f;
        float accountsH = secH + 5f + maxRowsConst * (cardH + cardGapY) - cardGapY;
        float serversH  = secH + 5f + cardH;
        float addH      = 20f;
        float totalH    = headerH + 12f + accountsH + 8f + serversH + 8f + addH;
        float footerBlock = Fonts.sf_regular[13].getHeight() * 2f + 10f;
        float titleY    = Math.min((this.height - totalH) / 2f, this.height - totalH - footerBlock);
        titleY = Math.max(titleY, 16f);
        boolean blockBgInteraction = showAddModal || modalAnim > 0.01f;

        Fonts.sf_regular[25].drawString(ms, "AltManager", blockX, titleY - 6, white);
        Fonts.sf_regular[18].drawString(ms, "Create or select account", blockX, titleY + Fonts.sf_regular[20].getHeight() + 1f, gray);

        float blockY = titleY + headerH + 12f;

        drawSectionHeader(ms, blockX - 3, blockY, "Accounts", white, fa255);
        float rowY = blockY + secH + 5f;

        scrollAnim.update(this.scroll);
        this.scrollAnimation = scrollAnim.getValue();
        float scrollOff = this.scrollAnimation;
        int count = 0;
        float col0X = blockX;
        float col1X = blockX + cardW + cardGapX;

        int maxRows = 3;
        float scissorTop = rowY;
        float scissorBot = rowY + maxRows * (cardH + cardGapY) - cardGapY;
        sky.core.utils.render.ScissorUtil.start(0, scissorTop, this.width, scissorBot - scissorTop);

        for (Account account : this.accounts) {
            int col = count % 2;
            int row = count / 2;
            float cx2 = col == 0 ? col0X : col1X;
            float cy2 = rowY + row * (cardH + cardGapY) + scrollOff * (cardH + cardGapY);

            if (cy2 + cardH > scissorTop && cy2 < scissorBot) {
                boolean isSelected = account.accountName.equals(this.selectedString);
                boolean isLoggedIn = this.minecraft.session != null
                        && account.accountName.equalsIgnoreCase(this.minecraft.session.getUsername());

                long nt = System.currentTimeMillis();
                float ph = hoverMap.getOrDefault(account.accountName, 0f);
                long lt = hoverTimeMap.getOrDefault(account.accountName, nt);
                float dh = Math.min((nt - lt) / 1000f, 0.05f);
                hoverTimeMap.put(account.accountName, nt);
                boolean hov = !blockBgInteraction && mouseX >= cx2 && mouseX <= cx2 + cardW && mouseY >= cy2 && mouseY <= cy2 + cardH;
                float th = blockBgInteraction ? Math.max(ph - dh * 10f, 0f) : (hov ? Math.min(ph + dh * 7f, 1f) : Math.max(ph - dh * 7f, 0f));
                hoverMap.put(account.accountName, th);

                AnimationUtil selAnim = selectAnimMap.computeIfAbsent(account.accountName,
                        k -> new AnimationUtil(0f, 6f, Easings.CUBIC_OUT));
                selAnim.update(isSelected ? 1f : 0f);
                float sel = selAnim.getValue();

                int bgR = (int)(30 + (154 - 30) * sel + (50 - 30) * th * (1f - sel));
                int bgG = (int)(30 + (192 - 30) * sel + (50 - 30) * th * (1f - sel));
                int bgB = (int)(35 + (255 - 35) * sel + (55 - 35) * th * (1f - sel));
                float bgAlpha = 0.55f + 0.15f * sel + 0.1f * th * (1f - sel);
                RenderUtil.drawBlurredRoundedRectangle(cx2, cy2, cardW, cardH, cardRadius + 2, ColorUtil.getColor(bgR, bgG, bgB, 255), bgAlpha * fade);

                float skinX = cx2 + 5f;
                float skinY = cy2 + (cardH - skinSize) / 2f;

                this.minecraft.getTextureManager().bindTexture(account.skin);
                RenderUtil.drawRoundedHead(account.skin, null, skinX, skinY, skinSize, skinSize, 2f, fade);

                float textX = skinX + skinSize + 5f;
                int nameCol = isSelected
                        ? ColorUtil.getColor(255, 255, 255, fa255)
                        : ColorUtil.getColor((int)(140 + (255 - 140) * th), (int)(140 + (255 - 140) * th), (int)(140 + (255 - 140) * th), fa255);
                Fonts.sf_regular[16].drawString(ms, account.accountName, (float) Math.round(textX), (float) Math.round(skinY) + 2.5f, nameCol);
                String dateStr = new SimpleDateFormat("dd.MM.yyyy").format(new Date(account.dateAdded));
                int dateCol = isSelected ? ColorUtil.getColor(255, 255, 255, fa255) : gray;
                Fonts.sf_regular[13].drawString(ms, dateStr, (float) Math.round(textX), (float) Math.round(skinY + Fonts.sf_regular[14].getHeight() + 1f) + 6.5f, dateCol);

                float sBtnW = Fonts.krisa[16].getWidth("F") + 10f;
                float sBtnX = cx2 + cardW - sBtnW - 4f;
                float sBtnY = cy2 + (cardH - Fonts.sf_regular[13].getHeight()) / 2f;
                boolean sHov = mouseX >= sBtnX - 2 && mouseX <= sBtnX + sBtnW + 2 && mouseY >= cy2 && mouseY <= cy2 + cardH;
                int fBtnTxC = isLoggedIn ? 255 : 103;
                int sCol = ColorUtil.getColor(fBtnTxC, fBtnTxC, fBtnTxC, fa255);
                Fonts.krisa[16].drawString(ms, "F", (float) Math.round(sBtnX + 5f), (float) Math.round(sBtnY), sCol);

            }
            count++;
        }
        sky.core.utils.render.ScissorUtil.end();

        int visibleRows = (int) Math.ceil(count / 2f);
        this.scroll = MathHelper.clamp(this.scroll, -(float) Math.max(0, visibleRows - maxRows), 0f);
        this.scrollAnimation = MathHelper.clamp(this.scrollAnimation, -(float) Math.max(0, visibleRows - maxRows), 0f);

        float serversY = scissorBot + 8f;
        drawSectionHeader(ms, blockX - 3, serversY, "Servers", white, fa255);
        float srvRowY = serversY + secH + 5f;

        drawServerCard(ms, col0X, srvRowY, cardW, cardH, cardRadius, skinSize, "__srv0",
                SERVER_NAME, SERVER_IP, serverIconLoc, serverIcon, blockBgInteraction, mouseX, mouseY, fade, fa255, gray);
        drawServerCard(ms, col1X, srvRowY, cardW, cardH, cardRadius, skinSize, "__srv1",
                SERVER2_NAME, SERVER2_IP, serverIconLoc2, serverIcon2, blockBgInteraction, mouseX, mouseY, fade, fa255, gray);

        float addBtnH  = 20f;
        float addBtnW  = blockW;
        float addBtnX  = blockX;
        float addBtnY  = srvRowY + cardH + 8f;

        long nowA = System.currentTimeMillis();
        if (addHoverTime < 0) addHoverTime = nowA;
        float dA = Math.min((nowA - addHoverTime) / 1000f, 0.05f);
        addHoverTime = nowA;
        boolean addHov = !blockBgInteraction && mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH;
        addHover = blockBgInteraction ? Math.max(addHover - dA * 10f, 0f) : (addHov ? Math.min(addHover + dA * 7f, 1f) : Math.max(addHover - dA * 7f, 0f));

        int addBgR = (int)(30 + (50 - 30) * addHover);
        int addBgG = (int)(30 + (50 - 30) * addHover);
        int addBgB = (int)(35 + (55 - 35) * addHover);
        RenderUtil.drawBlurredRoundedRectangle(addBtnX, addBtnY, addBtnW, addBtnH, cardRadius + 2,
                ColorUtil.getColor(addBgR, addBgG, addBgB, 255), (0.55f + 0.1f * addHover) * fade);

        float plusW = Fonts.sf_regular[16].getWidth("+");
        float plusH = Fonts.sf_regular[16].getHeight();
        int addTxC = (int)(140 + (255 - 140) * addHover);
        int plusCol = ColorUtil.getColor(addTxC, addTxC, addTxC, fa255);
        Fonts.sf_regular[16].drawString(ms, "+",
                (float) Math.round(addBtnX + (addBtnW - plusW) / 2f),
                (float) Math.round(addBtnY + (addBtnH - plusH) / 2f),
                plusCol);

        String fLine1 = "SkyCore 2026. All rights reserved.";
        String fLine2 = "Before playing, please read the Rules of use.";
        float fh = Fonts.sf_regular[13].getHeight();
        float footerX = (this.width - Fonts.sf_regular[13].getWidth(fLine1)) / 2f;
        float footerY = this.height - fh * 2f - 8f;
        Fonts.sf_regular[13].drawString(ms, fLine1, footerX, footerY, gray);
        Fonts.sf_regular[13].drawString(ms, fLine2,
                footerX - 12f,
                footerY + fh + 2f, gray);

        if (showAddModal || modalAnim > 0f) {
            long nowModal = System.currentTimeMillis();
            float dModal = modalAnimTime < 0 ? 0f : Math.min((nowModal - modalAnimTime) / 1000f, 0.1f);
            modalAnimTime = nowModal;
            modalAnim = showAddModal
                    ? Math.min(modalAnim + dModal * 12f, 1f)
                    : Math.max(modalAnim - dModal * 12f, 0f);
            float modalGlobalAnim = modalAnim * fade;
            if (modalGlobalAnim > 0f) renderAddModal(ms, mouseX, mouseY, modalGlobalAnim);
        }

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void startFadeOut(Screen next) {
        if (fadingOut) return;
        fadingOut = true;
        pendingScreen = next;
    }

    private void renderAddModal(MatrixStack ms, int mouseX, int mouseY, float anim) {
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), 0, 0, this.width, this.height, ColorUtil.applyOpacity(-1, (int)(130 * anim)));

        float mw = 200f, mh = 95f;
        float mx = (this.width - mw) / 2f, my = (this.height - mh) / 2f + 10;
        float pad = 12f, gap = 8f;

        int modalAlpha = (int)(255 * anim);
        RenderUtil.drawRoundedRectangle(mx, my, mw, mh - 5, 7f, ColorUtil.getColor(21, 20, 24, modalAlpha));

        int gray85 = ColorUtil.getColor(103, 106, 114, (int)(0.85f * 255 * anim));
        int white  = ColorUtil.getColor(255, 255, 255, (int)(255 * anim));
        float cy = my + pad;

        float titleH = Fonts.sf_regular[18].getHeight();
        Fonts.sf_regular[18].drawString(ms, "Менеджер аккаунтов", (float) Math.round(mx + pad), (float) Math.round(cy), gray85);
        cy += titleH + gap;

        float fieldW = mw - pad * 2f, fieldH = 21f;
        RenderUtil.drawBlurredRoundedRectangle(mx + pad, cy, fieldW, fieldH, 5f, ColorUtil.getColor(103, 106, 114, 255), 0.1f * anim);
        RenderUtil.drawOutlineRectangle(mx + pad, cy, fieldW, fieldH, 6, ColorUtil.getColor(103, 106, 114, 255), 0.15f * 255f * anim);

        float textY = cy + (fieldH - Fonts.sf_regular[18].getHeight()) / 2f;
        if (modalInput.isEmpty() && !modalFieldFocused) {
            Fonts.sf_regular[18].drawString(ms, "Введите никнейм", (float) Math.round(mx + pad + 8f), (float) Math.round(textY), gray85);
        } else {
            Fonts.sf_regular[18].drawString(ms, modalInput, (float) Math.round(mx + pad + 8f), (float) Math.round(textY) - 0.5f, white);
        }
        cy += fieldH + gap;

        float btnW = (fieldW - gap) / 2f, btnH = 21f;

        long nowC = System.currentTimeMillis();
        if (modalCreateHoverTime < 0) modalCreateHoverTime = nowC;
        float dC = Math.min((nowC - modalCreateHoverTime) / 1000f, 0.05f);
        modalCreateHoverTime = nowC;
        boolean onCreate = mouseX >= mx + pad && mouseX <= mx + pad + btnW && mouseY >= cy && mouseY <= cy + btnH;
        modalCreateHover = onCreate ? Math.min(modalCreateHover + dC * 10f, 1f) : Math.max(modalCreateHover - dC * 10f, 0f);

        int createBgR = (int)(154 + (220 - 154) * modalCreateHover);
        int createBgG = (int)(192 + (230 - 192) * modalCreateHover);
        RenderUtil.drawRoundedRectangle(mx + pad, cy, btnW, btnH, 5f, ColorUtil.getColor(createBgR, createBgG, 255, modalAlpha));
        float createTxtW = Fonts.sf_regular[18].getWidth("Создать");
        Fonts.sf_regular[18].drawString(ms, "Создать",
                (float) Math.round(mx + pad + (btnW - createTxtW) / 2f),
                (float) Math.round(cy + (btnH - Fonts.sf_regular[18].getHeight()) / 2f),
                ColorUtil.getColor(16, 17, 18, modalAlpha));

        float genX = mx + pad + btnW + gap;
        long nowG = System.currentTimeMillis();
        if (modalGenHoverTime < 0) modalGenHoverTime = nowG;
        float dG = Math.min((nowG - modalGenHoverTime) / 1000f, 0.05f);
        modalGenHoverTime = nowG;
        boolean onGen = mouseX >= genX && mouseX <= genX + btnW && mouseY >= cy && mouseY <= cy + btnH;
        modalGenHover = onGen ? Math.min(modalGenHover + dG * 10f, 1f) : Math.max(modalGenHover - dG * 10f, 0f);

        int genBg = (int)(25 + (45 - 25) * modalGenHover);
        RenderUtil.drawRoundedRectangle(genX, cy, btnW, btnH, 5f, ColorUtil.getColor(genBg, genBg, genBg + 5, modalAlpha));
        RenderUtil.drawOutlineRectangle(genX, cy, btnW, btnH, 6, ColorUtil.getColor(103, 106, 114, (int)(0.15f * modalAlpha)), 125);
        float genTxtW = Fonts.sf_regular[18].getWidth("Сгенерировать");
        int genTxC = (int)(103 + (160 - 103) * modalGenHover);
        Fonts.sf_regular[18].drawString(ms, "Сгенерировать",
                (float) Math.round(genX + (btnW - genTxtW) / 2f),
                (float) Math.round(cy + (btnH - Fonts.sf_regular[18].getHeight()) / 2f),
                ColorUtil.getColor(genTxC, genTxC, genTxC, modalAlpha));

    }

    private void openAddModal() {
        showAddModal = true;
        modalInput = "";
        modalFieldFocused = true;
        modalCreateHover = 0f;
        modalGenHover = 0f;
        modalAnimTime = -1;
        hoverMap.clear();
        hoverTimeMap.clear();
    }

    private void closeAddModal() {
        showAddModal = false;
        modalInput = "";
    }

    private void confirmAddModal() {
        String text = modalInput.trim();
        if (text.isEmpty()) {
            text = RANDOM_NAMES[new java.util.Random().nextInt(RANDOM_NAMES.length)] + new java.util.Random().nextInt(1500);
        }
        addAccount(text);
        closeAddModal();
    }

    private void drawSectionHeader(MatrixStack ms, float x, float y, String title, int color, int fa255) {
        Fonts.sf_regular[18].drawString(ms, title, (float) Math.round(x) + 3, (float) Math.round(y), color);
    }

    private void drawServerCard(MatrixStack ms, float x, float y, float cardW, float cardH, float cardRadius,
                                float iconSize, String hoverKey, String name, String ip,
                                ResourceLocation iconLoc, DynamicTexture iconTex,
                                boolean blockBgInteraction, int mouseX, int mouseY,
                                float fade, int fa255, int gray) {
        long now = System.currentTimeMillis();
        float ph = hoverMap.getOrDefault(hoverKey, 0f);
        long lt = hoverTimeMap.getOrDefault(hoverKey, now);
        float dh = Math.min((now - lt) / 1000f, 0.05f);
        hoverTimeMap.put(hoverKey, now);
        boolean hov = !blockBgInteraction && mouseX >= x && mouseX <= x + cardW && mouseY >= y && mouseY <= y + cardH;
        float th = blockBgInteraction ? Math.max(ph - dh * 10f, 0f) : (hov ? Math.min(ph + dh * 7f, 1f) : Math.max(ph - dh * 7f, 0f));
        hoverMap.put(hoverKey, th);

        int bgR = (int)(30 + (50 - 30) * th);
        int bgG = (int)(30 + (50 - 30) * th);
        int bgB = (int)(35 + (55 - 35) * th);
        RenderUtil.drawBlurredRoundedRectangle(x, y, cardW, cardH, cardRadius + 2,
                ColorUtil.getColor(bgR, bgG, bgB, 255), (0.55f + 0.1f * th) * fade);

        float iconX = x + 5f;
        float iconY = y + (cardH - iconSize) / 2f;
        ResourceLocation tex = iconTex != null ? iconLoc : new ResourceLocation("textures/misc/unknown_server.png");
        RenderUtil.drawRoundedTexture(ms, tex, iconX, iconY, iconSize, iconSize, 3f, fade);

        float textX = iconX + iconSize + 5f;
        int txC = (int)(140 + (255 - 140) * th);
        int nameCol = ColorUtil.getColor(txC, txC, txC, fa255);
        Fonts.sf_regular[16].drawString(ms, name, (float) Math.round(textX), (float) Math.round(iconY) + 2, nameCol);
        Fonts.sf_regular[13].drawString(ms, ip, (float) Math.round(textX) + 0.5f,
                (float) Math.round(iconY + Fonts.sf_regular[14].getHeight() + 1f) + 6.5f, gray);
    }

    private float[] getLayoutCoords() {
        float cardW    = 160f;
        float cardH    = 28f;
        float cardGapX = 6f;
        float cardGapY = 6f;
        float blockW   = cardW * 2 + cardGapX;
        float blockX   = (float) Math.round((this.width - blockW) / 2f);
        float secH     = Fonts.sf_regular[16].getHeight();
        int   maxRows  = 3;
        float headerH = Fonts.sf_regular[20].getHeight() + Fonts.sf_regular[14].getHeight() + 1f;
        float accountsH = secH + 5f + maxRows * (cardH + cardGapY) - cardGapY;
        float serversH  = secH + 5f + cardH;
        float addH      = 20f;
        float totalH    = headerH + 12f + accountsH + 8f + serversH + 8f + addH;
        float footerBlock = Fonts.sf_regular[13].getHeight() * 2f + 10f;
        float titleY    = Math.min((this.height - totalH) / 2f, this.height - totalH - footerBlock);
        titleY = Math.max(titleY, 16f);
        float blockY   = titleY + headerH + 12f;
        float rowY     = blockY + secH + 5f;
        float scissorBot = rowY + maxRows * (cardH + cardGapY) - cardGapY;
        float srvRowY  = scissorBot + 8f + secH + 5f;
        float addBtnY  = srvRowY + cardH + 8f;
        return new float[]{cardW, cardH, cardGapX, cardGapY, blockW, blockX, rowY, srvRowY, addBtnY};
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showAddModal || modalAnim > 0.01f) {
            if (!showAddModal) {
                return true;
            }
            float mw = 200f, mh = 95f;
            float mx = (this.width - mw) / 2f, my = (this.height - mh) / 2f;
            float pad = 12f, gap = 8f;
            float titleH = Fonts.sf_regular[18].getHeight();
            float fieldW = mw - pad * 2f, fieldH = 21f, btnH = 21f;
            float btnW = (fieldW - gap) / 2f;
            float fieldY = my + pad + titleH + gap;
            float btnY = fieldY + fieldH + gap;
            if (button == 0) {
                if (mouseX >= mx + pad && mouseX <= mx + pad + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    confirmAddModal(); return true;
                }
                float genX = mx + pad + btnW + gap;
                if (mouseX >= genX && mouseX <= genX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    modalInput = RANDOM_NAMES[new java.util.Random().nextInt(RANDOM_NAMES.length)] + new java.util.Random().nextInt(1500);
                    return true;
                }
                if (mouseX >= mx + pad && mouseX <= mx + mw - pad && mouseY >= fieldY && mouseY <= fieldY + fieldH) {
                    modalFieldFocused = true; return true;
                }
                if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                    closeAddModal(); return true;
                }
            }
            return true;
        }
        float[] L = getLayoutCoords();
        float cardW = L[0], cardH = L[1], cardGapX = L[2], cardGapY = L[3];
        float blockW = L[4], blockX = L[5], rowY = L[6], srvRowY = L[7], addBtnY = L[8];
        float col0X = blockX, col1X = blockX + cardW + cardGapX;
        float scrollOff = this.scrollAnimation;
        int count = 0;

        float scissorTop2 = rowY;
        float scissorBot2 = rowY + 3 * (cardH + cardGapY) - cardGapY;

        for (Account account : this.accounts) {
            int col = count % 2, row = count / 2;
            float cx2 = col == 0 ? col0X : col1X;
            float cy2 = rowY + row * (cardH + cardGapY) + scrollOff * (cardH + cardGapY);

            if (cy2 + cardH <= scissorTop2 || cy2 >= scissorBot2) { count++; continue; }

            float sBtnW = Fonts.sf_regular[13].getWidth("S") + 10f;
            float sBtnX = cx2 + cardW - sBtnW - 4f;
            boolean onSBtn = mouseX >= sBtnX - 2 && mouseX <= sBtnX + sBtnW + 2 && mouseY >= cy2 && mouseY <= cy2 + cardH;

            if (button == 0 && onSBtn) {
                String name = account.accountName;
                this.accounts.remove(account);
                if (name.equals(this.selectedString)) this.selectedString = "";
                AltConfig.updateFile(this.accounts);
                selectAnimMap.remove(name);
                hoverMap.remove(name);
                hoverTimeMap.remove(name);
                return true;
            }

            if (mouseX >= cx2 && mouseX <= cx2 + cardW && mouseY >= cy2 && mouseY <= cy2 + cardH) {
                if (button == 0) {
                    this.selectedString = account.accountName;
                    loginSelected();
                }
            }
            count++;
        }

        float addBtnH = 20f;
        if (button == 0 && mouseX >= col0X && mouseX <= col0X + cardW && mouseY >= srvRowY && mouseY <= srvRowY + cardH) {
            this.minecraft.displayGuiScreen(new ConnectingScreen(this, this.minecraft, serverData));
            return true;
        }
        if (button == 0 && mouseX >= col1X && mouseX <= col1X + cardW && mouseY >= srvRowY && mouseY <= srvRowY + cardH) {
            this.minecraft.displayGuiScreen(new ConnectingScreen(this, this.minecraft, serverData2));
            return true;
        }
        if (button == 0 && mouseX >= blockX && mouseX <= blockX + blockW && mouseY >= addBtnY && mouseY <= addBtnY + addBtnH) {
            openAddModal();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showAddModal || modalAnim > 0.01f) {
            return true;
        }
        this.scroll += (float)(delta * 2f);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showAddModal) {
            if (keyCode == 256) { closeAddModal(); return true; }
            if (keyCode == 257 || keyCode == 335) { confirmAddModal(); return true; }
            if (keyCode == 259 && !modalInput.isEmpty()) { modalInput = modalInput.substring(0, modalInput.length() - 1); return true; }
            return true;
        }
        if (keyCode == 256) { startFadeOut(new net.minecraft.client.gui.screen.MainMenuScreen(true)); return true; }
        if (keyCode == 257 || keyCode == 335) { loginSelected(); return true; }
        if (keyCode == 261) { deleteSelected(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (showAddModal && modalInput.length() < 32) { modalInput += c; return true; }
        return super.charTyped(c, modifiers);
    }
}
