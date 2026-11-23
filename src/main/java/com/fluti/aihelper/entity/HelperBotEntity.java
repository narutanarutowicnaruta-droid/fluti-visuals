package com.fluti.aihelper.entity;

import com.fluti.aihelper.util.BlockDictionary;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HelperBotEntity extends PathAwareEntity {

    private static BlockDictionary.TargetSpec activeTarget = null;
    private BlockPos currentMiningPos = null;
    private int miningProgress = 0;

    public HelperBotEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createBotAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.30);
    }

    public static void setActiveTarget(BlockDictionary.TargetSpec target) {
        activeTarget = target;
    }

    public static void clearActiveTarget() {
        activeTarget = null;
    }

    public static String getStatusText() {
        if (activeTarget == null) {
            return "Idle / Ожидание";
        }
        return "Mining: " + activeTarget.canonicalName();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }

        if (activeTarget == null) {
            currentMiningPos = null;
            miningProgress = 0;
            followNearestPlayer();
            return;
        }

        if (currentMiningPos == null || !activeTarget.matches(this.getWorld().getBlockState(currentMiningPos).getBlock())) {
            currentMiningPos = findNearestTargetBlock(20);
            miningProgress = 0;
        }

        if (currentMiningPos != null) {
            double distSq = this.squaredDistanceTo(
                    currentMiningPos.getX() + 0.5,
                    currentMiningPos.getY() + 0.5,
                    currentMiningPos.getZ() + 0.5
            );

            if (distSq > 16.0) {
                this.getNavigation().startMovingTo(
                        currentMiningPos.getX() + 0.5,
                        currentMiningPos.getY(),
                        currentMiningPos.getZ() + 0.5,
                        1.15
                );
            } else {
                this.getNavigation().stop();
                this.getLookControl().lookAt(
                        currentMiningPos.getX() + 0.5,
                        currentMiningPos.getY() + 0.5,
                        currentMiningPos.getZ() + 0.5
                );
                this.swingHand(Hand.MAIN_HAND);
                miningProgress++;

                if (miningProgress >= 15) {
                    BlockState state = this.getWorld().getBlockState(currentMiningPos);
                    if (!state.isAir()) {
                        this.getWorld().breakBlock(currentMiningPos, true, this);
                    }
                    currentMiningPos = null;
                    miningProgress = 0;
                }
            }
        }
    }

    private void followNearestPlayer() {
        PlayerEntity nearest = this.getWorld().getClosestPlayer(this, 32.0);
        if (nearest != null && this.squaredDistanceTo(nearest) > 9.0) {
            this.getNavigation().startMovingTo(nearest, 1.1);
        }
    }

    private BlockPos findNearestTargetBlock(int radius) {
        BlockPos origin = this.getBlockPos();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -6; dy <= 10; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = origin.add(dx, dy, dz);
                    BlockState state = this.getWorld().getBlockState(candidate);
                    if (activeTarget.matches(state.getBlock())) {
                        double dSq = origin.getSquaredDistance(candidate);
                        if (dSq < bestDistSq) {
                            bestDistSq = dSq;
                            bestPos = candidate;
                        }
                    }
                }
            }
        }
        return bestPos;
    }

    public void dropInventoryToNearestPlayer() {
        if (this.getWorld().isClient) {
            return;
        }
        PlayerEntity nearest = this.getWorld().getClosestPlayer(this, 32.0);
        if (nearest != null) {
            this.dropStack(this.getWorld(), new ItemStack(net.minecraft.item.Items.TORCH, 1));
        }
    }
}
