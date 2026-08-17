package net.optifine.player;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.optifine.Config;
import sky.core.SkyCore;
import sky.core.modules.impl.visuals.SantaHat;

public class PlayerConfiguration {
    private PlayerItemModel[] playerItemModels = new PlayerItemModel[0];
    private boolean initialized = false;

    public void renderPlayerItems(BipedModel modelBiped, AbstractClientPlayerEntity player, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, int packedOverlayIn) {
        SantaHat playerHat = SkyCore.getInstance().getModuleManager().getSantaHat();
        if (playerHat != null && playerHat.isEnabled()) {
            for (int i = 0; i < this.playerItemModels.length; ++i) {
                PlayerItemModel playeritemmodel = this.playerItemModels[i];
                playeritemmodel.render(modelBiped, player, matrixStackIn, bufferIn, packedLightIn, packedOverlayIn);
            }
        }
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public PlayerItemModel[] getPlayerItemModels() {
        return this.playerItemModels;
    }

    public void addPlayerItemModel(PlayerItemModel playerItemModel) {
        this.playerItemModels = (PlayerItemModel[]) Config.addObjectToArray(this.playerItemModels, playerItemModel);
    }
}