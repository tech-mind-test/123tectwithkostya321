package sky.core.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import sky.core.utils.Wrapper;


import java.util.*;
import java.util.regex.Pattern;

@UtilityClass
public class PlayerUtil implements Wrapper {

    public final Pattern NAME_REGEX = Pattern.compile("^[A-zА-я0-9_]{3,16}$");

    public boolean isInvalidName(String name) {
        return !NAME_REGEX.matcher(name).matches();
    }

    public boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

//    public boolean isPvp() {
//        for (Map.Entry<UUID, ClientBossInfo> bossInfo : mc.ingameGUI.getBossOverlay().getMapBossInfos().entrySet()) {
//            if (bossInfo.getValue().getName().getString().toLowerCase().contains("pvp") || bossInfo.getValue().getName().getString().toLowerCase().contains("пвп")) {
//                return true;
//            }
//        }
//        return false;
//    }

    public int getAnarchy() {
        final String SCOREBOARD_NAME = "TAB-Scoreboard";
        final String ANARCHY_PREFIX = "анархия";
        if (mc.world == null) {
            return -1;
        }
        ScoreObjective objective = mc.world.getScoreboard().getObjective(SCOREBOARD_NAME);
        if (objective == null) {
            return -1;
        }
        String displayName = objective.getDisplayName().getString().toLowerCase();
        int prefixIndex = displayName.indexOf(ANARCHY_PREFIX);
        if (prefixIndex == -1) {
            return -1;
        }
        try {
            String numberStr = displayName.substring(prefixIndex + ANARCHY_PREFIX.length()).replaceAll("\\D", "");
            if (!numberStr.isEmpty()) {
                return Integer.parseInt(numberStr);
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    public boolean collideWith(LivingEntity entity, float grow) {
        AxisAlignedBB box = mc.player.getBoundingBox();
        AxisAlignedBB targetbox = entity.getBoundingBox().grow(grow, 0, grow); //.expand(-0.1f, 0, -0.1f);

        return box.maxX > targetbox.minX
                && box.maxY > targetbox.minY
                && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX
                && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ;
    }

    public boolean collideWith(LivingEntity entity) {
        return collideWith(entity, 0);
    }




    public String getServerIp() {
        return mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP != null ? mc.getCurrentServerData().serverIP : mc.world != null ? "local" : "none";
    }

    public int getPing(AbstractClientPlayerEntity player) {
        return mc.getConnection() == null || mc.getConnection().getPlayerInfo(player.getUniqueID()) == null ? -1 : mc.getConnection().getPlayerInfo(player.getUniqueID()).getResponseTime();
    }


    public double getEntityArmor(PlayerEntity entity) {
        double totalArmor = 0.0;

        for (ItemStack armorStack : entity.inventory.armorInventory) {
            if (armorStack != null && armorStack.getItem() instanceof ArmorItem) {
                totalArmor += getProtectionLvl(armorStack);
            }
        }
        return totalArmor;
    }


    public double getProtectionLvl(ItemStack stack) {
        double damageReduce = 0.0;
        if (stack.getItem() instanceof ArmorItem armor) {
            damageReduce = armor.getDamageReduceAmount();
            if (stack.isEnchanted()) {
                damageReduce += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
            }
        }
        return damageReduce;
    }

    public Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return mc.world.getBlockState(mc.player.getPosition().add(offsetX, offsetY, offsetZ)).getBlock();
    }

    public Block block(final BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }

    public Block block(final double x, final double y, final double z) {
        return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public Block getBlock() {
        return getBlock(0, 0, 0);
    }

    public Block getBlock(double x, double y, double z) {
        return mc.world.getBlockState(mc.player.getPosition().add(x, y, z)).getBlock();
    }

    public BlockPos getBlock(float distance, Block block) {
        return getSphere(getPlayerPosLocal(), distance, 6, false, true, 0, false).stream().filter(position -> mc.world.getBlockState(position).getBlock() == block).min(Comparator.comparing(blockPos -> getDistanceOfEntityToBlock(mc.player, blockPos))).orElse(null);
    }

    public BlockPos getBlock(BlockPos centerPos, float distance, Block block) {
        return getCube(centerPos, distance, distance).stream().filter(pos -> mc.world.getBlockState(pos).equals(block)).findFirst().orElse(null);
    }

    public List<BlockPos> getSphere(final BlockPos center, final float radius, final float height, final boolean hollow, final boolean fromBottom, final int yOffset, boolean cube) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = centerX - (int) radius; x <= centerX + radius; x++) {
            for (int z = centerZ - (int) radius; z <= centerZ + radius; z++) {
                int yStart = fromBottom ? (centerY - (int) radius) : centerY;
                int yEnd = fromBottom ? (centerY + (int) radius) : (centerY + (int) height);

                for (int y = yStart; y < yEnd; y++) {
                    if (isPositionWithinSphere(centerX, centerY, centerZ, x, y, z, radius, hollow) || cube) {
                        positions.add(new BlockPos(x, y + yOffset, z));
                    }
                }
            }
        }

        return positions;
    }

    public List<BlockPos> getCube(final BlockPos center, final float radiusXZ, final float radiusY) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = centerY - (int) radiusY; y < centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public BlockPos getPlayerPosLocal() {
        if (mc.player == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(Math.floor(mc.player.getPosX()), Math.floor(mc.player.getPosY()), Math.floor(mc.player.getPosZ()));
    }

    public double getDistanceOfEntityToBlock(final Entity entity, final BlockPos blockPos) {
        return MathHelper.getDistance(entity.getPosX(), entity.getPosY(), entity.getPosZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public boolean isPositionWithinSphere(int centerX, int centerY, int centerZ, int x, int y, int z, float radius, boolean hollow) {
        double distanceSq = Math.pow(centerX - x, 2) + Math.pow(centerZ - z, 2) + Math.pow(centerY - y, 2);
        return distanceSq < Math.pow(radius, 2) && (!hollow || distanceSq >= Math.pow(radius - 1.0f, 2));
    }

    public boolean isBlockUnder(final double height) {
        for (int offset = 0; offset < height; offset++) {
            if (blockRelativeToPlayer(0, -offset, 0).getDefaultState().isCollisionShapeLargerThanFullBlock()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockSolid(BlockPos pos) {
        return PlayerUtil.block(pos).getDefaultState().getMaterial().isSolid();
    }

    public boolean isBlockSolid(final double x, final double y, final double z) {
        return PlayerUtil.block(new BlockPos(x, y, z)).getDefaultState().getMaterial().isSolid();
    }

    public boolean isPlayerInWeb() {
        return mc.player != null && mc.player.isInWeb;
    }

    public boolean isPlayerInSoulSand() {
        AxisAlignedBB playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = mc.player.getPosition();

        return getNearbyBlockPositions(playerPosition).stream()
                .anyMatch(pos -> isBlockSoulSand(playerBox, pos));
    }

    private List<BlockPos> getNearbyBlockPositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = center.getX() - 2; x <= center.getX() + 2; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 4; y++) {
                for (int z = center.getZ() - 2; z <= center.getZ() + 2; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    private boolean isBlockCobweb(AxisAlignedBB playerBox, BlockPos blockPos) {
        return playerBox.intersects(new AxisAlignedBB(blockPos)) && mc.world.getBlockState(blockPos).getBlock() == Blocks.COBWEB;
    }

    private boolean isBlockSoulSand(AxisAlignedBB playerBox, BlockPos blockPos) {
        return playerBox.intersects(new AxisAlignedBB(blockPos)) && mc.world.getBlockState(blockPos).getBlock() == Blocks.SOUL_SAND;
    }

//    public boolean isInView(AxisAlignedBB box) {
//        if (mc.getRenderViewEntity() == null) {
//            return false;
//        }
//        return mc.worldRenderer.getClippinghelper().isBoundingBoxInFrustum(box);
//    }

    public boolean isBlockAboveHead() {
        if (nullCheck()) return false;
        float width = mc.player.getWidth() / 2F;
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                mc.player.getPosX() - width, mc.player.getPosY() + mc.player.getEyeHeight(), mc.player.getPosZ() + width,
                mc.player.getPosX() + width, mc.player.getPosY() + 2.5, mc.player.getPosZ() - width
        );
        return !mc.world.getCollisionShapes(mc.player, axisAlignedBB).findAny().isEmpty();
    }
}
