//package sky.core.ui.Interface.elements.impl;
//
//import com.mojang.blaze3d.matrix.MatrixStack;
//import dev.redstones.mediaplayerinfo.IMediaSession;
//import dev.redstones.mediaplayerinfo.MediaInfo;
//import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.texture.DynamicTexture;
//import net.minecraft.client.renderer.texture.NativeImage;
//import net.minecraft.util.ResourceLocation;
//import sky.core.events.EventRender2D;
//import sky.core.ui.Interface.elements.ElementRender;
//import sky.core.ui.gui.themes.ThemeEditor;
//import sky.core.ui.gui.themes.ThemeSettings;
//import sky.core.utils.managers.impl.dragmanager.Dragging;
//import sky.core.utils.render.ColorUtil;
//import sky.core.utils.render.RenderUtil;
//import sky.core.utils.render.ScissorUtil;
//import sky.core.utils.render.font.Fonts;
//
//import java.io.ByteArrayInputStream;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MusicRender implements ElementRender {
//
//    private final Dragging dragging;
//    private final ExecutorService executor = Executors.newSingleThreadExecutor();
//
//    private volatile MediaInfo mediaInfo = null;
//    private volatile boolean polling = false;
//    private long lastPoll = 0L;
//
//    private byte[] lastArtworkBytes = null;
//    private final ResourceLocation dynamicArtwork = new ResourceLocation("minecraft", "SkyCore/music_dynamic_art");
//    private ResourceLocation currentArtwork = null;
//
//    private static final float MAX_WIDTH = 155f;
//    private static final float MIN_WIDTH = 65f;
//    private static final float HEIGHT = 18f;
//
//    public MusicRender(Dragging dragging) {
//        this.dragging = dragging;
//    }
//
//    @Override
//    public void render(EventRender2D.Post event) {
//        pollMedia();
//
//        MatrixStack ms = event.getStack();
//
//        float x = dragging.getX();
//        float y = dragging.getY();
//
//        float padding = 3f;
//        float coverSize = 11f;
//        float radius = 6f;
//        float coverRadius = 3f;
//        float textGap = 3f;
//        float separatorGap = 5f;
//        float separatorWidth = 0.75f;
//        float separatorHeight = 12f;
//
//        String title = "No media";
//        String artist = "Nothing is playing";
//        String time = "0:00";
//
//        if (mediaInfo != null) {
//            if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
//                title = mediaInfo.getTitle();
//            }
//            if (mediaInfo.getArtist() != null && !mediaInfo.getArtist().isEmpty()) {
//                artist = mediaInfo.getArtist();
//            } else {
//                artist = "";
//            }
//            time = formatTime(mediaInfo.getPosition());
//        }
//
//        String textSeparator = (artist != null && !artist.isEmpty()) ? " " : "";
//        float titleWidth = Fonts.sfregular[12].getWidth(title);
//        float textSeparatorWidth = Fonts.sfregular[11].getWidth(textSeparator);
//        float artistWidth = (artist != null && !artist.isEmpty()) ? Fonts.sfregular[11].getWidth(artist) : 0f;
//        float timeWidth = Fonts.sfregular[12].getWidth(time);
//        float textLineWidth = titleWidth + textSeparatorWidth + artistWidth;
//
//        float contentWidth = padding + coverSize + textGap + textLineWidth + separatorGap + separatorWidth + separatorGap + timeWidth + padding + 2;
//
//        float width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, contentWidth));
//        boolean needsScroll = contentWidth > MAX_WIDTH;
//
//        dragging.setWidth(width);
//        dragging.setHeight(HEIGHT);
//
//        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
//        int bgColor = ColorUtil.darken(logoColor, 0.1f);
//        int titleColor = ColorUtil.rgb(255, 255, 255);
//        int artistColor = ColorUtil.rgb(113, 113, 113);
//        int timeColor = logoColor;
//        int separatorColor = ColorUtil.applyOpacity(
//                ThemeEditor.getColor(ThemeSettings.SEPARATOR),
//                ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255f * 0.2f
//        );
//        int placeholderColor = ColorUtil.rgb(35, 35, 35);
//        int placeholderIconColor = ColorUtil.rgb(170, 170, 170);
//
//        RenderUtil.drawRoundedRectangle(x, y, width, HEIGHT, radius, bgColor);
//
//        float coverX = x + padding;
//        float coverY = y + (HEIGHT - coverSize) / 2f;
//
//        if (currentArtwork != null) {
//            RenderUtil.drawRoundedTexture(ms, currentArtwork, coverX, coverY, coverSize, coverSize, coverRadius, 1f);
//        } else {
//            RenderUtil.drawRoundedRectangle(coverX, coverY, coverSize, coverSize, coverRadius, placeholderColor);
//            Fonts.divine_icons[11].drawString(ms, "i", coverX + 3.2f, coverY + 5f, placeholderIconColor);
//        }
//
//        float timeX = x + width - padding - timeWidth;
//        float sepX = timeX - separatorGap - separatorWidth - 2;
//        float sepY = y + (HEIGHT - separatorHeight) / 2f;
//
//        RenderUtil.drawMinecraftRectangle(ms, sepX, sepY - 2.5f, separatorWidth, separatorHeight + 5, separatorColor);
//
//        float textX = coverX + coverSize + textGap;
//        float maxLineWidth = Math.max(10f, sepX - separatorGap - textX);
//        float lineY = y + 8f;
//
//        if (needsScroll) {
//            renderScrollingTrackLine(ms, title, artist, textX, lineY, titleColor, artistColor, maxLineWidth);
//        } else {
//            float drawX = textX;
//            Fonts.sfregular[12].drawString(ms, title, drawX, lineY, titleColor);
//            drawX += titleWidth;
//            if (!textSeparator.isEmpty()) {
//                Fonts.sfregular[11].drawString(ms, textSeparator, drawX, lineY + 0.5f, artistColor);
//                drawX += textSeparatorWidth;
//                Fonts.sfregular[11].drawString(ms, artist, drawX, lineY + 0.5f, artistColor);
//            }
//        }
//
//        Fonts.sfregular[12].drawString(ms, time, timeX - 2, y + 8f, timeColor);
//    }
//
//    private void pollMedia() {
//        long now = System.currentTimeMillis();
//        if (polling || now - lastPoll < 500L) {
//            return;
//        }
//
//        lastPoll = now;
//        polling = true;
//
//        executor.execute(() -> {
//            try {
//                List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
//
//                if (sessions == null || sessions.isEmpty()) {
//                    mediaInfo = null;
//                    currentArtwork = null;
//                    return;
//                }
//
//                IMediaSession currentSession = sessions.stream()
//                        .filter(session -> session != null && session.getMedia() != null)
//                        .max(Comparator.comparing(s -> s.getMedia().getPlaying()))
//                        .orElse(null);
//
//                if (currentSession == null) {
//                    mediaInfo = null;
//                    currentArtwork = null;
//                    return;
//                }
//
//                MediaInfo info = currentSession.getMedia();
//                if (info == null) {
//                    mediaInfo = null;
//                    currentArtwork = null;
//                    return;
//                }
//
//                mediaInfo = info;
//
//                byte[] artworkBytes = info.getArtworkPng();
//                if (artworkBytes != null && artworkBytes.length > 0) {
//                    if (!Arrays.equals(lastArtworkBytes, artworkBytes)) {
//                        lastArtworkBytes = artworkBytes.clone();
//                        updateArtwork(artworkBytes);
//                    }
//                } else {
//                    lastArtworkBytes = null;
//                    currentArtwork = null;
//                }
//            } catch (Throwable t) {
//                t.printStackTrace();
//                mediaInfo = null;
//                currentArtwork = null;
//            } finally {
//                polling = false;
//            }
//        });
//    }
//
//    private void updateArtwork(byte[] artworkBytes) {
//        Minecraft.getInstance().execute(() -> {
//            try {
//                ByteArrayInputStream input = new ByteArrayInputStream(artworkBytes);
//                NativeImage image = NativeImage.read(input);
//                DynamicTexture texture = new DynamicTexture(image);
//
//                Minecraft.getInstance().getTextureManager().deleteTexture(dynamicArtwork);
//                Minecraft.getInstance().getTextureManager().loadTexture(dynamicArtwork, texture);
//                currentArtwork = dynamicArtwork;
//            } catch (Exception e) {
//                e.printStackTrace();
//                currentArtwork = null;
//            }
//        });
//    }
//
//    private void renderScrollingTrackLine(MatrixStack ms, String title, String artist, float x, float y, int titleColor, int artistColor, float maxWidth) {
//        String separator = (artist != null && !artist.isEmpty()) ? " " : "";
//
//        float titleWidth = Fonts.sfregular[12].getWidth(title);
//        float separatorWidth = Fonts.sfregular[11].getWidth(separator);
//        float artistWidth = (artist != null && !artist.isEmpty()) ? Fonts.sfregular[11].getWidth(artist) : 0f;
//
//        float fullWidth = titleWidth + separatorWidth + artistWidth;
//        float scroll = 0f;
//
//        if (fullWidth > maxWidth) {
//            float scrollMax = fullWidth - maxWidth;
//            float pause = 1000f;
//            float duration = 4000f;
//            float cycle = pause + duration + pause + duration;
//            float time = System.currentTimeMillis() % (long) cycle;
//
//            if (time < pause) {
//                scroll = 0f;
//            } else if (time < pause + duration) {
//                float t = (time - pause) / duration;
//                scroll = t * scrollMax;
//            } else if (time < pause + duration + pause) {
//                scroll = scrollMax;
//            } else {
//                float t = (time - pause - duration - pause) / duration;
//                scroll = scrollMax * (1f - t);
//            }
//        }
//
//        float drawX = x - scroll;
//
//        ScissorUtil.start(x - 1, y - 2, maxWidth + 2, Fonts.sfregular[12].getHeight() + 6);
//
//        Fonts.sfregular[12].drawString(ms, title, drawX, y, titleColor);
//        drawX += titleWidth;
//
//        if (!separator.isEmpty()) {
//            Fonts.sfregular[11].drawString(ms, separator, drawX, y + 0.5f, artistColor);
//            drawX += separatorWidth;
//            Fonts.sfregular[11].drawString(ms, artist, drawX, y + 0.5f, artistColor);
//        }
//
//        ScissorUtil.end();
//    }
//
//    private String formatTime(long seconds) {
//        long minutes = seconds / 60L;
//        long secs = seconds % 60L;
//        return String.format("%d:%02d", minutes, secs);
//    }
//}