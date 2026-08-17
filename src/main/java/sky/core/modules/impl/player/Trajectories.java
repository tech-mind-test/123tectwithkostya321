package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;

import static org.lwjgl.opengl.GL11.*;

public class Trajectories extends Module {
    private static final Minecraft mc = Minecraft.getInstance();

    private static final int MAX_SHOTS = 3;

    private final PredictionSlot[] slots = new PredictionSlot[MAX_SHOTS];


    public Trajectories() {
        super("Trajectories", Category.Visuals);

        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i] = new PredictionSlot();
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            clearSlots();
            return;
        }

        PlayerEntity player = mc.player;
        ItemStack stack = player.getHeldItemMainhand();
        if (stack == null || stack.isEmpty()) stack = player.getHeldItemOffhand();
        if (stack == null || stack.isEmpty()) {
            clearSlots();
            return;
        }

        ProjectileParams params = getParams(player, stack);
        if (params == null) {
            clearSlots();
            return;
        }

        if (stack.getItem() == Items.CROSSBOW && EnchantmentHelper.getEnchantmentLevel(Enchantments.MULTISHOT, stack) > 0) {
            Vector3d baseDir = player.getLookVec().normalize();
            float baseYaw = (float) (MathHelper.atan2(baseDir.z, baseDir.x) * (180.0 / Math.PI)) - 90.0f;
            float basePitch = (float) (-(MathHelper.atan2(baseDir.y, MathHelper.sqrt(baseDir.x * baseDir.x + baseDir.z * baseDir.z)) * (180.0 / Math.PI)));

            float[] yawOff = new float[]{-10.0f, 0.0f, 10.0f};
            for (int i = 0; i < MAX_SHOTS; i++) {
                Vector3d dir = getDirectionFromYawPitch(baseYaw + yawOff[i], basePitch);
                PredictionResult pr = predict(player, params, dir);
                updateSlot(this.slots[i], pr);
            }
        } else {
            Vector3d dir = player.getLookVec().normalize();
            PredictionResult pr = predict(player, params, dir);
            updateSlot(this.slots[0], pr);
            for (int i = 1; i < MAX_SHOTS; i++) {
                this.slots[i].clear();
            }
        }
    }

    @EventTarget
    private void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        float pt = event.getPartialTicks();
        Vector3d cam = mc.getRenderManager().info.getProjectedView();

        for (int i = 0; i < MAX_SHOTS; i++) {
            PredictionSlot s = this.slots[i];
            if (!s.hasHit || s.prevHit == null || s.currHit == null || s.prevFace == null || s.currFace == null)
                continue;

            boolean entityHit = s.hitEntity != null && s.hitEntity.isAlive() && s.prevEntityHit != null && s.currEntityHit != null;
            if (entityHit) {
                Vector3d entityHitVec = lerpVec(s.prevEntityHit, s.currEntityHit, pt);
                renderBillboardMarker(entityHitVec.subtract(cam), ColorUtil.rgb(255, 50, 50));
                renderEntityBox(event, s.hitEntity, ColorUtil.rgb(255, 50, 50));
            } else {
                Vector3d hitVec = lerpVec(s.prevHit, s.currHit, pt);
                renderMarker(event, hitVec.subtract(cam), s.prevFace, s.currFace, pt, ColorUtil.rgb(63, 233, 53));
            }
        }
    }

    private static ProjectileParams getParams(PlayerEntity player, ItemStack stack) {
        Item item = stack.getItem();

        if (item == Items.ENDER_PEARL) {
            return new ProjectileParams(1.5f, 0.03f, 0.99f);
        }

        if (item == Items.BOW) {
            float power = 1.0f;
            if (player.isHandActive() && player.getActiveItemStack() == stack) {
                int use = stack.getUseDuration() - player.getItemInUseCount();
                float f = (float) use / 20.0f;
                f = (f * f + f * 2.0f) / 3.0f;
                if (f > 1.0f) f = 1.0f;
                power = f;
            }

            float velocity = 3.0f * power;
            if (velocity <= 0.01f) return null;
            return new ProjectileParams(velocity, 0.05f, 0.99f);
        }

        if (item == Items.CROSSBOW) {
            if (!CrossbowItem.isCharged(stack)) return null;
            float velocity = 3.15f;
            return new ProjectileParams(velocity, 0.05f, 0.99f);
        }

        if (item == Items.TRIDENT) {
            float velocity = 2.5f;
            return new ProjectileParams(velocity, 0.05f, 0.99f);
        }

        return null;
    }

    private static BlockRayTraceResult predictHit(PlayerEntity player, ProjectileParams params) {
        Vector3d pos = player.getEyePosition(1.0f);
        Vector3d motion = player.getLookVec().normalize().scale(params.velocity);

        Vector3d prev;
        for (int i = 0; i < 200; i++) {
            prev = pos;
            pos = pos.add(motion);

            RayTraceContext ctx = new RayTraceContext(prev, pos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player);
            BlockRayTraceResult blockHit = mc.world.rayTraceBlocks(ctx);
            if (blockHit.getType() == RayTraceResult.Type.BLOCK) {
                return blockHit;
            }

            motion = motion.scale(params.drag);
            motion = motion.subtract(0.0, params.gravity, 0.0);

            if (pos.y < 0) break;
        }

        return null;
    }

    private void renderMarker(EventRender3D event,
                              Vector3d hitPosView,
                              Direction prevFace,
                              Direction currFace,
                              float partialTicks,
                              int color) {

        double eps = 0.002;

        Vector3d n0 = Vector3d.copy(prevFace.getDirectionVec()).normalize();
        Vector3d n1 = Vector3d.copy(currFace.getDirectionVec()).normalize();

        Vector3d u0 = (prevFace == Direction.UP || prevFace == Direction.DOWN)
                ? new Vector3d(1, 0, 0)
                : n0.crossProduct(new Vector3d(0, 1, 0)).normalize();

        Vector3d u1 = (currFace == Direction.UP || currFace == Direction.DOWN)
                ? new Vector3d(1, 0, 0)
                : n1.crossProduct(new Vector3d(0, 1, 0)).normalize();

        Vector3d n = lerpVec(n0, n1, partialTicks).normalize();
        Vector3d u = lerpVec(u0, u1, partialTicks).normalize();
        Vector3d v = n.crossProduct(u).normalize();
        u = v.crossProduct(n).normalize();

        Vector3d center = hitPosView.add(n.scale(eps));

        float[] rgba = ColorUtil.rgba(color);
        float a = 0.95f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();

        RenderSystem.pushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_LINE_SMOOTH);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        glLineWidth(2.5f);

        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int segments = 48;
        double r = 0.35;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * ((double) i / (double) segments);
            Vector3d p = center.add(u.scale(Math.cos(t) * r)).add(v.scale(Math.sin(t) * r));
            buffer.pos(p.x, p.y, p.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        }
        tess.draw();

        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double lineLen = r;

        Vector3d p1 = center.add(u.scale(-lineLen));
        Vector3d p2 = center.add(u.scale(lineLen));
        buffer.pos(p1.x, p1.y, p1.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        buffer.pos(p2.x, p2.y, p2.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();

        Vector3d p3 = center.add(v.scale(-lineLen));
        Vector3d p4 = center.add(v.scale(lineLen));
        buffer.pos(p3.x, p3.y, p3.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        buffer.pos(p4.x, p4.y, p4.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();

        tess.draw();

        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        RenderSystem.popMatrix();
    }

    private void renderBillboardMarker(Vector3d posView, int color) {
        Vector3d n = posView.scale(-1.0);
        if (n.lengthSquared() < 1.0E-6) return;
        n = n.normalize();

        Vector3d u = n.crossProduct(new Vector3d(0, 1, 0));
        if (u.lengthSquared() < 1.0E-6) {
            u = new Vector3d(1, 0, 0);
        }
        u = u.normalize();
        Vector3d v = n.crossProduct(u).normalize();

        float[] rgba = ColorUtil.rgba(color);
        float a = 0.95f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();

        RenderSystem.pushMatrix();
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_LINE_SMOOTH);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        glLineWidth(2.5f);

        buffer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int segments = 48;
        double r = 0.35;
        for (int i = 0; i <= segments; i++) {
            double t = (Math.PI * 2.0) * ((double) i / (double) segments);
            Vector3d p = posView.add(u.scale(Math.cos(t) * r)).add(v.scale(Math.sin(t) * r));
            buffer.pos(p.x, p.y, p.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        }
        tess.draw();

        buffer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        double lineLen = r;

        Vector3d p1 = posView.add(u.scale(-lineLen));
        Vector3d p2 = posView.add(u.scale(lineLen));
        buffer.pos(p1.x, p1.y, p1.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        buffer.pos(p2.x, p2.y, p2.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();

        Vector3d p3 = posView.add(v.scale(-lineLen));
        Vector3d p4 = posView.add(v.scale(lineLen));
        buffer.pos(p3.x, p3.y, p3.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();
        buffer.pos(p4.x, p4.y, p4.z).color(rgba[0], rgba[1], rgba[2], a).endVertex();

        tess.draw();

        glDisable(GL_LINE_SMOOTH);
        glDisable(GL_BLEND);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        RenderSystem.popMatrix();
    }

    private static Vector3d lerpVec(Vector3d a, Vector3d b, float t) {
        return new Vector3d(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    private static Vector3d getDirectionFromYawPitch(float yawDeg, float pitchDeg) {
        float yaw = yawDeg * ((float) Math.PI / 180F);
        float pitch = pitchDeg * ((float) Math.PI / 180F);
        float f = MathHelper.cos(-yaw - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch);
        float f3 = MathHelper.sin(-pitch);
        return new Vector3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    private void updateSlot(PredictionSlot slot, PredictionResult pr) {
        if (pr == null || pr.blockHit == null || pr.blockHit.getType() != RayTraceResult.Type.BLOCK) {
            slot.clear();
            return;
        }

        Vector3d hitVec = pr.blockHit.getHitVec();
        Direction face = pr.blockHit.getFace();
        slot.hitEntity = pr.entityHit;

        if (pr.entityHitPos != null) {
            if (slot.currEntityHit == null) {
                slot.prevEntityHit = pr.entityHitPos;
                slot.currEntityHit = pr.entityHitPos;
            } else {
                slot.prevEntityHit = slot.currEntityHit;
                slot.currEntityHit = pr.entityHitPos;
            }
        } else {
            slot.prevEntityHit = null;
            slot.currEntityHit = null;
        }

        if (!slot.hasHit || slot.currHit == null || slot.currFace == null) {
            slot.prevHit = hitVec;
            slot.currHit = hitVec;
            slot.prevFace = face;
            slot.currFace = face;
            slot.hasHit = true;
        } else {
            slot.prevHit = slot.currHit;
            slot.prevFace = slot.currFace;
            slot.currHit = hitVec;
            slot.currFace = face;
        }
    }

    private void clearSlots() {
        for (int i = 0; i < MAX_SHOTS; i++) {
            this.slots[i].clear();
        }
    }

    private static PredictionResult predict(PlayerEntity player, ProjectileParams params, Vector3d dirNorm) {
        Vector3d pos = player.getEyePosition(1.0f);
        Vector3d motion = dirNorm.normalize().scale(params.velocity);

        Entity firstEntityHit = null;
        Vector3d firstEntityHitPos = null;

        Vector3d prev;
        for (int i = 0; i < 200; i++) {
            prev = pos;
            pos = pos.add(motion);

            EntityHit entityHit = rayTraceEntities(prev, pos, player);
            if (entityHit != null && firstEntityHit == null) {
                firstEntityHit = entityHit.entity;
                firstEntityHitPos = entityHit.hitPos;
            }

            RayTraceContext ctx = new RayTraceContext(prev, pos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player);
            BlockRayTraceResult blockHit = mc.world.rayTraceBlocks(ctx);
            if (blockHit.getType() == RayTraceResult.Type.BLOCK) {
                return new PredictionResult(blockHit, firstEntityHit, firstEntityHitPos);
            }

            motion = motion.scale(params.drag);
            motion = motion.subtract(0.0, params.gravity, 0.0);

            if (pos.y < 0) break;
        }

        return null;
    }

    private static EntityHit rayTraceEntities(Vector3d from, Vector3d to, Entity owner) {
        AxisAlignedBB aabb = new AxisAlignedBB(from, to).grow(1.0);
        Entity closest = null;
        Vector3d closestHit = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntitiesInAABBexcluding(owner, aabb, entity -> entity != null && entity.isAlive() && entity.canBeCollidedWith())) {
            AxisAlignedBB bb = e.getBoundingBox().grow(0.3);
            java.util.Optional<Vector3d> hit = bb.rayTrace(from, to);
            if (hit.isPresent()) {
                double dist = from.squareDistanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                    closestHit = hit.get();
                }
            }
        }

        return closest == null ? null : new EntityHit(closest, closestHit);
    }

    private void renderEntityBox(EventRender3D event, Entity entity, int color) {
        float partialTicks = event.getPartialTicks();
        Vector3d cam = mc.getRenderManager().info.getProjectedView();

        double interpX = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks;
        double interpY = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks;
        double interpZ = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks;

        AxisAlignedBB box = entity.getBoundingBox().offset(
                -cam.x + (interpX - entity.getPosX()),
                -cam.y + (interpY - entity.getPosY()),
                -cam.z + (interpZ - entity.getPosZ())
        );

        RenderUtil.box(box, color);
    }

    private static final class PredictionResult {
        private final BlockRayTraceResult blockHit;
        private final Entity entityHit;
        private final Vector3d entityHitPos;

        private PredictionResult(BlockRayTraceResult blockHit, Entity entityHit, Vector3d entityHitPos) {
            this.blockHit = blockHit;
            this.entityHit = entityHit;
            this.entityHitPos = entityHitPos;
        }
    }

    private static final class EntityHit {
        private final Entity entity;
        private final Vector3d hitPos;

        private EntityHit(Entity entity, Vector3d hitPos) {
            this.entity = entity;
            this.hitPos = hitPos;
        }
    }

    private static final class ProjectileParams {
        private final float velocity;
        private final float gravity;
        private final float drag;

        private ProjectileParams(float velocity, float gravity, float drag) {
            this.velocity = velocity;
            this.gravity = gravity;
            this.drag = drag;
        }
    }

    private static final class PredictionSlot {
        private Vector3d prevHit;
        private Vector3d currHit;
        private Direction prevFace;
        private Direction currFace;
        private boolean hasHit;

        private Entity hitEntity;
        private Vector3d prevEntityHit;
        private Vector3d currEntityHit;

        private void clear() {
            this.prevHit = null;
            this.currHit = null;
            this.prevFace = null;
            this.currFace = null;
            this.hasHit = false;
            this.hitEntity = null;
            this.prevEntityHit = null;
            this.currEntityHit = null;
        }
    }
}
