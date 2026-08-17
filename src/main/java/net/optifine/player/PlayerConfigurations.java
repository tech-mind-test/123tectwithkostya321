package net.optifine.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import net.optifine.http.FileDownloadThread;
import net.optifine.http.HttpUtils;
import sky.core.SkyCore;
import sky.core.modules.impl.visuals.SantaHat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mods.baritone.api.api.java.baritone.api.utils.Helper.mc;

public class PlayerConfigurations {
    private static Map mapConfigurations = null;
    private static final boolean reloadPlayerItems = Boolean.getBoolean("player.models.reload");
    private static long timeReloadPlayerItemsMs = System.currentTimeMillis();
    private static PlayerConfiguration santaHatConfiguration = new PlayerConfiguration();

    public static void renderPlayerItems(BipedModel modelBiped, AbstractClientPlayerEntity player, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, int packedOverlayIn) {
        PlayerConfiguration playerconfiguration = getPlayerConfiguration(player);

        if (playerconfiguration != null) {
            playerconfiguration.renderPlayerItems(modelBiped, player, matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        }
    }

    public static void renderHatItem(BipedModel modelBiped, AbstractClientPlayerEntity player, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, int packedOverlayIn) {
        SantaHat playerHat = SkyCore.getInstance().getModuleManager().getSantaHat();
        if (playerHat == null || !playerHat.isEnabled()) {
            return;
        }

        if (player != mc.player) {
            return;
        }

        PlayerConfiguration playerconfiguration;
        playerconfiguration = santaHatConfiguration;


        if (playerconfiguration != null) {
            playerconfiguration.renderPlayerItems(modelBiped, player, matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
        }
    }

    public static synchronized PlayerConfiguration getPlayerConfiguration(AbstractClientPlayerEntity player) {
        if (reloadPlayerItems && System.currentTimeMillis() > timeReloadPlayerItemsMs + 5000L) {
            AbstractClientPlayerEntity abstractclientplayerentity = mc.player;

            if (abstractclientplayerentity != null) {
                setPlayerConfiguration(abstractclientplayerentity.getNameClear(), null);
                timeReloadPlayerItemsMs = System.currentTimeMillis();
            }
        }

        String s1 = player.getNameClear();

        if (s1 == null) {
            return null;
        } else {
            PlayerConfiguration playerconfiguration = (PlayerConfiguration) getMapConfigurations().get(s1);

            if (playerconfiguration == null) {
                playerconfiguration = new PlayerConfiguration();
                getMapConfigurations().put(s1, playerconfiguration);
                PlayerConfigurationReceiver playerconfigurationreceiver = new PlayerConfigurationReceiver(s1);
                String s = HttpUtils.getPlayerItemsUrl() + "/users/" + s1 + ".cfg";
                FileDownloadThread filedownloadthread = new FileDownloadThread(s, playerconfigurationreceiver);
                filedownloadthread.start();
            }

            return playerconfiguration;
        }
    }

    public static synchronized void setPlayerConfiguration(String player, PlayerConfiguration pc) {
        getMapConfigurations().put(player, pc);
    }

    private static Map getMapConfigurations() {
        if (mapConfigurations == null) {
            mapConfigurations = new HashMap();
        }

        return mapConfigurations;
    }

    private static PlayerItemModel downloadModel() {
        try {
            String s1 = """
                    {
                    \t"type" : "PlayerItem",
                    \t"texture": "optifine:textures/features/hat_santa.png",
                    \t"textureSize": [64, 32],
                    \t"models": [
                    \t\t{
                    \t\t\t"part": "santa_hat",
                    \t\t\t"id": "santa_hat",
                    \t\t\t"type": "ModelBox",
                    \t\t\t"attachTo": "head",\s
                    \t\t\t"invertAxis": "xy",
                    \t\t\t"translate": [0, 0, 0],
                    \t\t\t"rotate": [0, -90, 0],
                    \t\t\t"submodels": [
                    \t\t\t\t{
                    \t\t\t\t\t"id": "sant_hat_top2",
                    \t\t\t\t\t"invertAxis": "xy",
                    \t\t\t\t\t"translate": [0.53024, 10.61642, 0],
                    \t\t\t\t\t"rotate": [0, 0, -60],
                    \t\t\t\t\t"boxes": [
                    \t\t\t\t\t\t{"coordinates": [-0.5, 2, -1.5, 3, 3, 3], "textureOffset": [0, 0]}
                    \t\t\t\t\t]
                    \t\t\t\t},
                    \t\t\t\t{
                    \t\t\t\t\t"id": "sant_hat_top1",
                    \t\t\t\t\t"invertAxis": "xy",
                    \t\t\t\t\t"translate": [1.03024, 9.75039, 0],
                    \t\t\t\t\t"rotate": [0, 0, -50],
                    \t\t\t\t\t"boxes": [
                    \t\t\t\t\t\t{"coordinates": [-3, -1, -3, 6, 4, 6], "textureOffset": [0, 0]}
                    \t\t\t\t\t]
                    \t\t\t\t},
                    \t\t\t\t{
                    \t\t\t\t\t"id": "santa_hat_top0",
                    \t\t\t\t\t"invertAxis": "xy",
                    \t\t\t\t\t"translate": [0.2892, 8.72718, 0],
                    \t\t\t\t\t"rotate": [0, 0, -20],
                    \t\t\t\t\t"boxes": [
                    \t\t\t\t\t\t{"coordinates": [-4, -1, -4, 9, 3, 8], "textureOffset": [0, 0]}
                    \t\t\t\t\t]
                    \t\t\t\t},
                    \t\t\t\t{
                    \t\t\t\t\t"id": "sant_hat_top3",
                    \t\t\t\t\t"invertAxis": "xy",
                    \t\t\t\t\t"translate": [5.78024, 12.11642, 0],
                    \t\t\t\t\t"rotate": [0, 0, 15],
                    \t\t\t\t\t"boxes": [
                    \t\t\t\t\t\t{"coordinates": [-0.90192, -1.83013, -2, 4, 4, 4], "textureOffset": [0, 16]}
                    \t\t\t\t\t]
                    \t\t\t\t},
                    \t\t\t\t{
                    \t\t\t\t\t"id": "santa_hat_base",
                    \t\t\t\t\t"invertAxis": "xy",
                    \t\t\t\t\t"translate": [0.2892, 8.72718, 0],
                    \t\t\t\t\t"rotate": [0, 0, -15],
                    \t\t\t\t\t"boxes": [
                    \t\t\t\t\t\t{"coordinates": [-4.5, -3, -4.5, 10, 4, 9], "textureOffset": [0, 16], "sizeAdd": 0.1}
                    \t\t\t\t\t]
                    \t\t\t\t}
                    \t\t\t]
                    \t\t}
                    \t]
                    }""";
            JsonParser jsonparser = new JsonParser();
            JsonObject jsonobject = (JsonObject) jsonparser.parse(s1);
            return PlayerItemParser.parseItemModel(jsonobject);
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    static {
        santaHatConfiguration = new PlayerConfiguration();

        PlayerItemModel playeritemmodel = downloadModel();

        ResourceLocation resourcelocation = new ResourceLocation("SkyCore/icons/hats/santa.png");
        NativeImage nativeimage;
        try {
            nativeimage = NativeImage.read(Minecraft.getInstance().getResourceManager().getResource(resourcelocation).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        playeritemmodel.setTextureImage(nativeimage);
        playeritemmodel.setTextureLocation(resourcelocation);

        santaHatConfiguration.addPlayerItemModel(playeritemmodel);
    }
}