package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.handlers.richidog.RichiBrain;
import sky.core.handlers.richidog.RichiModel;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;

public class RichiDog extends Module {

    private final ModeSetting variant = new ModeSetting("Вид", "Такса", "Такса", "DJ");

    private final RichiModel model = new RichiModel(RenderType::getEntityCutoutNoCull);
    private final RichiBrain brain = new RichiBrain();

    public RichiDog() {
        super("Pets", "Питомец-собака рядом с игроком", Category.Visuals);
        addSettings(variant);
    }

    @Override
    public void onEnable() {
        brain.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        brain.reset();
        super.onDisable();
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player != null) {
            brain.setEntity(mc.player);
        }
        brain.onUpdate(event);
    }

    @EventTarget
    private void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack ms = new MatrixStack();
        float partialTicks = event.getPartialTicks();
        Vector3d cam = mc.getRenderManager().info.getProjectedView();

        ms.push();
        ms.translate(brain.getPos().x - cam.x, brain.getPos().y - cam.y, brain.getPos().z - cam.z);

        model.setRotationAngles(mc.player.ticksExisted + partialTicks, brain);
        ResourceLocation tex = variant.is("DJ") ? RichiModel.TEXTURE_DJ : RichiModel.TEXTURE_TAKSA;

        IRenderTypeBuffer.Impl buffers = mc.getRenderTypeBuffers().getBufferSource();
        int packedLight = WorldRenderer.getCombinedLight(mc.world, new BlockPos(brain.getPos()));
        model.render(ms, buffers, packedLight, OverlayTexture.NO_OVERLAY, tex, brain);
        buffers.finish(RenderType.getEntityTranslucent(tex));

        ms.pop();
    }
}

