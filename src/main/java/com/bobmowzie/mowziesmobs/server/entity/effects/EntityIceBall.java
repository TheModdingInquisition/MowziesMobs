package com.bobmowzie.mowziesmobs.server.entity.effects;

import com.bobmowzie.mowziesmobs.MowziesMobs;
import com.bobmowzie.mowziesmobs.client.particle.ParticleHandler;
import com.bobmowzie.mowziesmobs.client.particle.ParticleCloud;
import com.bobmowzie.mowziesmobs.client.particle.ParticleRing;
import com.bobmowzie.mowziesmobs.client.particle.ParticleSnowFlake;
import com.bobmowzie.mowziesmobs.server.capability.CapabilityHandler;
import com.bobmowzie.mowziesmobs.server.capability.FrozenCapability;
import com.bobmowzie.mowziesmobs.server.capability.FrozenCapability.IFrozenCapability;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import net.minecraft.world.entity.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Created by BobMowzie on 9/2/2018.
 */
public class EntityIceBall extends EntityMagicEffect {
    public EntityIceBall(Level world) {
        super(EntityHandler.ICE_BALL, world);
    }

    public EntityIceBall(EntityType<? extends EntityIceBall> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityIceBall(EntityType<? extends EntityIceBall> type, Level worldIn, LivingEntity caster) {
        this(type, worldIn);
        if (!level.isClientSide) {
            this.setCasterID(caster.getEntityId());
        }
    }

    @Override
    public void tick() {
        super.tick();
        move(MoverType.SELF, getDeltaMovement());

        if (tickCount == 1) {
            if (level.isClientSide) {
                MowziesMobs.PROXY.playIceBreathSound(this);
            }
        }

        List<LivingEntity> entitiesHit = getEntityLivingBaseNearby(2);
        if (!entitiesHit.isEmpty()) {
            for (LivingEntity entity : entitiesHit) {
                if (entity == caster) continue;
                List<? extends String> freezeImmune = ConfigHandler.COMMON.GENERAL.freeze_blacklist.get();
                ResourceLocation mobName = EntityType.getKey(entity.getType());
                if (freezeImmune.contains(mobName.toString())) continue;
                if (entity.hurt(DamageSource.causeIndirectMagicDamage(this, caster), 3f * ConfigHandler.COMMON.MOBS.FROSTMAW.combatConfig.attackMultiplier.get().floatValue())) {
                    IFrozenCapability capability = CapabilityHandler.getCapability(entity, FrozenCapability.FrozenProvider.FROZEN_CAPABILITY);
                    if (capability != null) capability.addFreezeProgress(entity, 1);
                }
            }
        }

        if (!world.noCollision(this, getBoundingBox().grow(0.15))) {
            explode();
        }

        if (level.isClientSide) {
            float scale = 2f;
            double x = getX();
            double y = getY() + getHeight() / 2;
            double z = getZ();
            double motionX = getDeltaMovement().x;
            double motionY = getDeltaMovement().y;
            double motionZ = getDeltaMovement().z;
            for (int i = 0; i < 4; i++) {
                double xSpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                double ySpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                double zSpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                float value = random.nextFloat() * 0.15f;
                level.addParticle(new ParticleCloud.CloudData(ParticleHandler.CLOUD.get(), 0.75f + value, 0.75f + value,1f, scale * (10f + random.nextFloat() * 20f), 20, ParticleCloud.EnumCloudBehavior.SHRINK, 1f), x + xSpeed, y + ySpeed, z + zSpeed, xSpeed, ySpeed, zSpeed);
            }
            for (int i = 0; i < 1; i++) {
                double xSpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                double ySpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                double zSpeed = scale * 0.01 * (random.nextFloat() * 2 - 1);
                level.addParticle(new ParticleCloud.CloudData(ParticleHandler.CLOUD.get(), 1f, 1f, 1f, scale * (5f + random.nextFloat() * 10f), 40, ParticleCloud.EnumCloudBehavior.SHRINK, 1f), x, y, z, xSpeed, ySpeed, zSpeed);
            }

            for (int i = 0; i < 5; i++) {
                double xSpeed = scale * 0.05 * (random.nextFloat() * 2 - 1);
                double ySpeed = scale * 0.05 * (random.nextFloat() * 2 - 1);
                double zSpeed = scale * 0.05 * (random.nextFloat() * 2 - 1);
                level.addParticle(new ParticleSnowFlake.SnowflakeData(40, false), x - 20 * (xSpeed) + motionX, y - 20 * ySpeed + motionY, z - 20 * zSpeed + motionZ, xSpeed, ySpeed, zSpeed);
            }

            float yaw = (float) Math.atan2(motionX, motionZ);
            float pitch = (float) (Math.acos(motionY / Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)) + Math.PI / 2);
            if (tickCount % 3 == 0) {
                level.addParticle(new ParticleRing.RingData(yaw, pitch, 40, 0.9f, 0.9f, 1f, 0.4f, scale * 16f, false, ParticleRing.EnumRingBehavior.GROW_THEN_SHRINK), x + 1.5f * motionX, y + 1.5f *motionY, z + 1.5f * motionZ, 0, 0, 0);
            }

            if (tickCount == 1) {
                level.addParticle(new ParticleRing.RingData(yaw, pitch, 20, 0.9f, 0.9f, 1f, 0.4f, scale * 16f, false, ParticleRing.EnumRingBehavior.GROW), x, y, z, 0, 0, 0);
            }
        }
        if (tickCount > 50) remove();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {

    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        setDeltaMovement(x * velocity, y * velocity, z * velocity);
    }

    private void explode() {
        if (level.isClientSide) {
            for (int i = 0; i < 8; i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 0.3, 0, 0);
                particlePos = particlePos.rotateYaw((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.rotatePitch((float) (random.nextFloat() * 2 * Math.PI));
                float value = random.nextFloat() * 0.15f;
                level.addParticle(new ParticleCloud.CloudData(ParticleHandler.CLOUD.get(), 0.75f + value, 0.75f + value, 1f, 10f + random.nextFloat() * 20f, 40, ParticleCloud.EnumCloudBehavior.GROW, 1f), getX() + particlePos.x, getY() + particlePos.y, getZ() + particlePos.z, particlePos.x, particlePos.y, particlePos.z);
            }
            for (int i = 0; i < 10; i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 0.3, 0, 0);
                particlePos = particlePos.rotateYaw((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.rotatePitch((float) (random.nextFloat() * 2 * Math.PI));
                level.addParticle(new ParticleSnowFlake.SnowflakeData(40, false), getX() + particlePos.x, getY() + particlePos.y, getZ() + particlePos.z, particlePos.x, particlePos.y, particlePos.z);
            }
        }
        remove();
    }
}
