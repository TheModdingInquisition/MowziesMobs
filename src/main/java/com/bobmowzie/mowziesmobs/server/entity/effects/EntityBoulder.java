package com.bobmowzie.mowziesmobs.server.entity.effects;

import com.bobmowzie.mowziesmobs.client.particle.ParticleHandler;
import com.bobmowzie.mowziesmobs.client.particle.util.AdvancedParticleBase;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleComponent;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleRotation;
import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.potion.EffectGeomancy;
import com.bobmowzie.mowziesmobs.server.potion.EffectHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.material.PushReaction;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.particles.BlockParticleOption;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by BobMowzie on 4/14/2017.
 */
public class EntityBoulder extends Entity {
    private LivingEntity caster;
    private boolean travelling;
    public BlockState storedBlock;
    private static final EntityDataAccessor<Optional<BlockState>> BLOCK_STATE = SynchedEntityData.defineId(EntityBoulder.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> SHOULD_EXPLODE = SynchedEntityData.defineId(EntityBoulder.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> ORIGIN = SynchedEntityData.defineId(EntityBoulder.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> DEATH_TIME = SynchedEntityData.defineId(EntityBoulder.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(EntityBoulder.class, EntityDataSerializers.INT);
    public float animationOffset = 0;
    private final List<Entity> ridingEntities = new ArrayList<Entity>();
    public BoulderSizeEnum boulderSize = BoulderSizeEnum.SMALL;

    private float speed = 1.5f;
    private int damage = 8;
    private int finishedRisingTick = 4;

    public enum BoulderSizeEnum {
        SMALL,
        MEDIUM,
        LARGE,
        HUGE
    }

    public EntityBoulder(EntityType<? extends EntityBoulder> type, Level world) {
        super(type, world);
        travelling = false;
        damage = 8;
        finishedRisingTick = 4;
        animationOffset = random.nextFloat() * 8;
        this.setOrigin(this.getPosition());
    }

    public EntityBoulder(EntityType<? extends EntityBoulder> type, Level world, LivingEntity caster, BlockState blockState, BlockPos pos) {
        this(type, world);
        this.caster = caster;
        if (type == EntityHandler.BOULDER_SMALL) setBoulderSize(BoulderSizeEnum.SMALL);
        else if (type == EntityHandler.BOULDER_MEDIUM) setBoulderSize(BoulderSizeEnum.MEDIUM);
        else if (type == EntityHandler.BOULDER_LARGE) setBoulderSize(BoulderSizeEnum.LARGE);
        else if (type == EntityHandler.BOULDER_HUGE) setBoulderSize(BoulderSizeEnum.HUGE);
        setSizeParams();
        if (!level.isClientSide && blockState != null) {
            Block block = blockState.getBlock();
            BlockState newBlock = blockState;
            Material mat = blockState.getMaterial();
            if (blockState.getBlock() == Blocks.GRASS_BLOCK || blockState.getBlock() == Blocks.MYCELIUM || mat == Material.EARTH) newBlock = Blocks.DIRT.defaultBlockState();
            else if (mat == Material.ROCK) {
                if (block.getRegistryName() != null && block.getRegistryName().getPath().contains("ore")) newBlock = Blocks.STONE.defaultBlockState();
                if (blockState.getBlock() == Blocks.NETHER_QUARTZ_ORE) newBlock = Blocks.NETHERRACK.defaultBlockState();
                if (blockState.getBlock() == Blocks.FURNACE
                        || blockState.getBlock() == Blocks.DISPENSER
                        || blockState.getBlock() == Blocks.DROPPER
                ) newBlock = Blocks.COBBLESTONE.defaultBlockState();
            }
            else if (mat == Material.CLAY) {
                if (blockState.getBlock() == Blocks.CLAY) newBlock = Blocks.TERRACOTTA.defaultBlockState();
            }
            else if (mat == Material.SAND) {
                if (blockState.getBlock() == Blocks.SAND) newBlock = Blocks.SANDSTONE.defaultBlockState();
                else if (blockState.getBlock() == Blocks.RED_SAND) newBlock = Blocks.RED_SANDSTONE.defaultBlockState();
                else if (blockState.getBlock() == Blocks.GRAVEL) newBlock = Blocks.COBBLESTONE.defaultBlockState();
                else if (blockState.getBlock() == Blocks.SOUL_SAND) newBlock = Blocks.NETHERRACK.defaultBlockState();
            }

            if (!newBlock.isNormalCube(world, pos)) {
                newBlock = Blocks.STONE.defaultBlockState();
            }
            setBlock(newBlock);
        }
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.BLOCK;
    }

    public boolean func_241845_aY() {
        return true;
    }

    public boolean checkCanSpawn() {
        if (!world.getEntitiesWithinAABB(EntityBoulder.class, getBoundingBox().shrink(0.01)).isEmpty()) return false;
        return world.noCollision(this, getBoundingBox().shrink(0.01));
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        getEntityData().define(BLOCK_STATE, Optional.of(Blocks.DIRT.defaultBlockState()));
        getEntityData().define(SHOULD_EXPLODE, false);
        getEntityData().define(ORIGIN, new BlockPos(0, 0, 0));
        getEntityData().define(DEATH_TIME, 1200);
        getEntityData().define(SIZE, 0);
    }

    public void setSizeParams() {
        BoulderSizeEnum size = getBoulderSize();
        if (size == BoulderSizeEnum.MEDIUM) {
            finishedRisingTick = 8;
            damage = 12;
            speed = 1.2f;
        }
        else if (size == BoulderSizeEnum.LARGE) {
            finishedRisingTick = 12;
            damage = 16;
            speed = 1f;
        }
        else if (size == BoulderSizeEnum.HUGE) {
            finishedRisingTick = 90;
            damage = 20;
            speed = 0.65f;
        }

        if (caster instanceof Player) damage *= ConfigHandler.COMMON.TOOLS_AND_ABILITIES.geomancyAttackMultiplier.get();

    }

    @Override
    public boolean isSilent() {
        return false;
    }

    @Override
    public void tick() {
        if (firstUpdate) {
            setSizeParams();
            boulderSize = getBoulderSize();
        }
        if (storedBlock == null) storedBlock = getBlock();
        if (getShouldExplode()) explode();
        if (!travelling) {
            setBoundingBox(getType().getBoundingBoxWithSizeApplied(getX(), getY(), getZ()).expand(0, -0.5, 0));
        }
        super.tick();
        move(MoverType.SELF, getDeltaMovement());
        if (ridingEntities != null) ridingEntities.clear();
        List<Entity> onTopOfEntities = level.getEntities(this, getBoundingBox().contract(0, getHeight() - 1, 0).offset(new Vec3(0, getHeight() - 0.5, 0)).grow(0.6,0.5,0.6));
        for (Entity entity : onTopOfEntities) {
            if (entity != null && entity.canBeCollidedWith() && !(entity instanceof EntityBoulder) && entity.getY() >= this.getY() + 0.2) ridingEntities.add(entity);
        }
        if (travelling){
            for (Entity entity : ridingEntities) {
                entity.move(MoverType.SHULKER_BOX, getDeltaMovement());
            }
        }
        if (boulderSize == BoulderSizeEnum.HUGE && tickCount < finishedRisingTick) {
            float f = this.getBbWidth() / 2.0F;
            AxisAlignedBB aabb = new AxisAlignedBB(getX() - (double)f, getY() - 0.5, getZ() - (double)f, getX() + (double)f, getY() + Math.min(tickCount/(float)finishedRisingTick * 3.5f, 3.5f), getZ() + (double)f);
            setBoundingBox(aabb);
        }

        if (tickCount < finishedRisingTick) {
            List<Entity> popUpEntities = level.getEntities(this, getBoundingBox());
            for (Entity entity:popUpEntities) {
                if (entity.canBeCollidedWith() && !(entity instanceof EntityBoulder)) {
                    if (boulderSize != BoulderSizeEnum.HUGE) entity.move(MoverType.SHULKER_BOX, new Vec3(0, 2 * (Math.pow(2, -tickCount * (0.6 - 0.1 * boulderSize.ordinal()))), 0));
                    else entity.move(MoverType.SHULKER_BOX, new Vec3(0, 0.6f, 0));
                }
            }
        }
        List<LivingEntity> entitiesHit = getEntityLivingBaseNearby(1.7);
        if (travelling && !entitiesHit.isEmpty()) {
            for (Entity entity : entitiesHit) {
                if (level.isClientSide) continue;
                if (entity == caster) continue;
                if (ridingEntities.contains(entity)) continue;
                if (caster != null) entity.hurt(DamageSource.causeIndirectDamage(this, caster), damage);
                else entity.hurt(DamageSource.FALLING_BLOCK, damage);
                if (isAlive() && boulderSize != BoulderSizeEnum.HUGE) setShouldExplode(true);
            }
        }
        List<EntityBoulder> bouldersHit = world.getEntitiesWithinAABB(EntityBoulder.class, getBoundingBox().grow(0.2, 0.2, 0.2).offset(getDeltaMovement().normalize().scale(0.5)));
        if (travelling && !bouldersHit.isEmpty()) {
            for (EntityBoulder entity : bouldersHit) {
                if (!entity.travelling) {
                    entity.hitByEntity(this);
                    explode();
                }
            }
        }

        if (travelling && !world.noCollision(this, getBoundingBox().grow(0.1), (e)->ridingEntities.contains(e))) setShouldExplode(true);

        if (tickCount == 1) {
            for (int i = 0; i < 20 * getWidth(); i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 1.3 * getWidth(), 0, 0);
                particlePos = particlePos.rotateYaw((float) (random.nextFloat() * 2 * Math.PI));
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, storedBlock), getX() + particlePos.x, getY() - 1, getZ() + particlePos.z, particlePos.x, 2, particlePos.z);
            }
            if (boulderSize == BoulderSizeEnum.SMALL) {
                playSound(MMSounds.EFFECT_GEOMANCY_SMALL_CRASH.get(), 1.5f, 1.3f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 1f);
            } else if (boulderSize == BoulderSizeEnum.MEDIUM) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_MEDIUM_2.get(), 1.5f, 1.5f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 0.8f);
            } else if (boulderSize == BoulderSizeEnum.LARGE) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_MEDIUM_1.get(), 1.5f, 0.9f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 1.5f, 1.5f);
                EntityCameraShake.cameraShake(world, getPositionVec(), 10, 0.05f, 0, 20);
            } else if (boulderSize == BoulderSizeEnum.HUGE) {
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 2f, 0.5f);
                playSound(MMSounds.EFFECT_GEOMANCY_RUMBLE_1.get(), 2, 0.8f);
                EntityCameraShake.cameraShake(world, getPositionVec(), 15, 0.05f, 50, 30);
            }
            if (level.isClientSide) {
                AdvancedParticleBase.spawnParticle(world, ParticleHandler.RING2.get(), getX(), getY() - 0.9f, getZ(), 0, 0, 0, false, 0, Math.PI / 2f, 0, 0, 3.5F, 0.83f, 1, 0.39f, 1, 1, (int) (5 + 2 * getWidth()), true, true, new ParticleComponent[]{
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(1f, 0f), false),
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(0f, (1.0f + 0.5f * getWidth()) * 10f), false)
                });
            }
        }
        if (tickCount == 30 && boulderSize == BoulderSizeEnum.HUGE) {
            playSound(MMSounds.EFFECT_GEOMANCY_RUMBLE_2.get(), 2, 0.7f);
        }

        int dripTick = tickCount - 2;
        if (boulderSize == BoulderSizeEnum.HUGE) dripTick -= 20;
        int dripNumber = (int)(getWidth() * 6 * Math.pow(1.03 + 0.04 * 1/getWidth(), -(dripTick)));
        if (dripNumber >= 1 && dripTick > 0) {
            dripNumber *= random.nextFloat();
            for (int i = 0; i < dripNumber; i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 0.6 * getWidth(), 0, 0);
                particlePos = particlePos.rotateYaw((float)(random.nextFloat() * 2 * Math.PI));
                float offsetY;
                if (boulderSize == BoulderSizeEnum.HUGE && tickCount < finishedRisingTick) offsetY = random.nextFloat() * (getHeight()-1) - getHeight() * (finishedRisingTick - tickCount)/finishedRisingTick;
                else offsetY = random.nextFloat() * (getHeight()-1);
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, storedBlock), getX() + particlePos.x, getY() + offsetY, getZ() + particlePos.z, 0, -1, 0);
            }
        }
        int newDeathTime = getDeathTime() - 1;
        setDeathTime(newDeathTime);
        if (newDeathTime < 0) this.explode();
    }

    private void explode() {
        remove();
        for (int i = 0; i < 40 * getWidth(); i++) {
            Vec3 particlePos = new Vec3(random.nextFloat() * 0.7 * getWidth(), 0, 0);
            particlePos = particlePos.rotateYaw((float)(random.nextFloat() * 2 * Math.PI));
            particlePos = particlePos.rotatePitch((float)(random.nextFloat() * 2 * Math.PI));
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, storedBlock), getX() + particlePos.x, getY() + 0.5 + particlePos.y, getZ() + particlePos.z, particlePos.x, particlePos.y, particlePos.z);
        }
        if (boulderSize == BoulderSizeEnum.SMALL) {
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 0.9f);
            playSound(MMSounds.EFFECT_GEOMANCY_BREAK.get(), 1.5f, 1f);
        }
        else if (boulderSize == BoulderSizeEnum.MEDIUM) {
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 0.7f);
            playSound(MMSounds.EFFECT_GEOMANCY_BREAK_MEDIUM_3.get(), 1.5f, 1.5f);
        }
        else if (boulderSize == BoulderSizeEnum.LARGE) {
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 1.5f, 1f);
            playSound(MMSounds.EFFECT_GEOMANCY_BREAK_MEDIUM_1.get(), 1.5f, 0.9f);
            EntityCameraShake.cameraShake(world, getPositionVec(), 15, 0.05f, 0, 20);

            for (int i = 0; i < 5; i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 2, 0, 0);
                particlePos = particlePos.rotateYaw((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.rotatePitch((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.add(new Vec3(0, getHeight() / 4, 0));
//                    ParticleFallingBlock.spawnFallingBlock(world, getX() + particlePos.x, getY() + 0.5 + particlePos.y, getZ() + particlePos.z, 10.f, 90, 1, (float) particlePos.x * 0.3f, 0.2f + (float) random.nextFloat() * 0.6f, (float) particlePos.z * 0.3f, ParticleFallingBlock.EnumScaleBehavior.CONSTANT, getBlock());
                EntityFallingBlock fallingBlock = new EntityFallingBlock(EntityHandler.FALLING_BLOCK, world, 70, getBlock());
                fallingBlock.setPos(getX() + particlePos.x, getY() + 0.5 + particlePos.y, getZ() + particlePos.z);
                fallingBlock.setDeltaMovement((float) particlePos.x * 0.3f, 0.2f + random.nextFloat() * 0.6f, (float) particlePos.z * 0.3f);
                level.addFreshEntity(fallingBlock);
            }
        }
        else if (boulderSize == BoulderSizeEnum.HUGE) {
            playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 1.5f, 0.5f);
            playSound(MMSounds.EFFECT_GEOMANCY_BREAK_LARGE_1.get(), 1.5f, 0.5f);
            EntityCameraShake.cameraShake(world, getPositionVec(), 20, 0.05f, 0, 20);

            for (int i = 0; i < 7; i++) {
                Vec3 particlePos = new Vec3(random.nextFloat() * 2.5f, 0, 0);
                particlePos = particlePos.rotateYaw((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.rotatePitch((float) (random.nextFloat() * 2 * Math.PI));
                particlePos = particlePos.add(new Vec3(0, getHeight() / 4, 0));
//                    ParticleFallingBlock.spawnFallingBlock(world, getX() + particlePos.x, getY() + 0.5 + particlePos.y, getZ() + particlePos.z, 10.f, 70, 1, (float) particlePos.x * 0.3f, 0.2f + (float) random.nextFloat() * 0.6f, (float) particlePos.z * 0.3f, ParticleFallingBlock.EnumScaleBehavior.CONSTANT, getBlock());
                EntityFallingBlock fallingBlock = new EntityFallingBlock(EntityHandler.FALLING_BLOCK, world, 70, getBlock());
                fallingBlock.setPos(getX() + particlePos.x, getY() + 0.5 + particlePos.y, getZ() + particlePos.z);
                fallingBlock.setDeltaMovement((float) particlePos.x * 0.3f, 0.2f + random.nextFloat() * 0.6f, (float) particlePos.z * 0.3f);
                level.addFreshEntity(fallingBlock);
            }
        }
    }

    public BlockState getBlock() {
        Optional<BlockState> bsOp = getEntityData().get(BLOCK_STATE);
        return bsOp.orElse(null);
    }

    public void setBlock(BlockState block) {
        getEntityData().set(BLOCK_STATE, Optional.of(block));
        this.storedBlock = block;
    }

    public boolean getShouldExplode() {
        return getEntityData().get(SHOULD_EXPLODE);
    }

    public void setShouldExplode(boolean shouldExplode) {
        getEntityData().set(SHOULD_EXPLODE, shouldExplode);
    }

    public void setOrigin(BlockPos pos) {
        this.dataManager.set(ORIGIN, pos);
    }

    public BlockPos getOrigin() {
        return this.dataManager.get(ORIGIN);
    }

    public int getDeathTime() {
        return dataManager.get(DEATH_TIME);
    }

    public void setDeathTime(int deathTime) {
        dataManager.set(DEATH_TIME, deathTime);
    }

    public BoulderSizeEnum getBoulderSize() {
        return BoulderSizeEnum.values()[dataManager.get(SIZE)];
    }

    public void setBoulderSize(BoulderSizeEnum size) {
        dataManager.set(SIZE, size.ordinal());
        boulderSize = size;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        BlockState blockState = getBlock();
        if (blockState != null) compound.put("block", NBTUtil.writeBlockState(blockState));
        compound.putInt("deathTime", getDeathTime());
        compound.putInt("size", getBoulderSize().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        INBT blockStateCompound = compound.get("block");
        if (blockStateCompound != null) {
            BlockState blockState = NBTUtil.readBlockState((CompoundTag) blockStateCompound);
            setBlock(blockState);
        }
        setDeathTime(compound.getInt("deathTime"));
        setBoulderSize(BoulderSizeEnum.values()[compound.getInt("size")]);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hitByEntity(Entity entityIn) {
        if (tickCount > finishedRisingTick - 1 && !travelling) {
            if (entityIn instanceof Player
                    && EffectGeomancy.canUse((Player)entityIn)) {
                Player player = (Player) entityIn;
                if (ridingEntities.contains(player)) {
                    Vec3 lateralLookVec = Vec3.fromPitchYaw(0, player.getYRot()).normalize();
                    setDeltaMovement(speed * 0.5 * lateralLookVec.x, getDeltaMovement().y, speed * 0.5 * lateralLookVec.z);
                } else {
                    setDeltaMovement(player.getLookVec().scale(speed * 0.5));
                }
                AbilityHandler.INSTANCE.sendAbilityMessage(player, AbilityHandler.HIT_BOULDER_ABILITY);
            }
            else if (entityIn instanceof EntityBoulder && ((EntityBoulder) entityIn).travelling) {
                EntityBoulder boulder = (EntityBoulder)entityIn;
                Vec3 thisPos = getPositionVec();
                Vec3 boulderPos = boulder.position();
                Vec3 velVec = thisPos.subtract(boulderPos).normalize();
                setDeltaMovement(velVec.scale(speed * 0.5));
            }
            else {
                return super.hitByEntity(entityIn);
            }
            if (!travelling) setDeathTime(60);
            travelling = true;
            setBoundingBox(getType().getBoundingBoxWithSizeApplied(getX(), getY(), getZ()));

            if (boulderSize == BoulderSizeEnum.SMALL) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_SMALL.get(), 1.5f, 1.3f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 0.9f);
            }
            else if (boulderSize == BoulderSizeEnum.MEDIUM) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_SMALL.get(), 1.5f, 0.9f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_SMALL.get(), 1.5f, 0.5f);
            }
            else if (boulderSize == BoulderSizeEnum.LARGE) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_SMALL.get(), 1.5f, 0.5f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 1.5f, 1.3f);
                EntityCameraShake.cameraShake(world, getPositionVec(), 10, 0.05f, 0, 20);
            }
            else if (boulderSize == BoulderSizeEnum.HUGE) {
                playSound(MMSounds.EFFECT_GEOMANCY_HIT_MEDIUM_1.get(), 1.5f, 1f);
                playSound(MMSounds.EFFECT_GEOMANCY_MAGIC_BIG.get(), 1.5f, 0.9f);
                EntityCameraShake.cameraShake(world, getPositionVec(), 15, 0.05f, 0, 20);
            }

            if (level.isClientSide) {
                Vec3 ringOffset = getDeltaMovement().scale(-1).normalize();
                ParticleRotation.OrientVector rotation = new ParticleRotation.OrientVector(ringOffset);
                AdvancedParticleBase.spawnParticle(world, ParticleHandler.RING2.get(), (float) getX() + (float) ringOffset.x, (float) getY() + 0.5f + (float) ringOffset.y, (float) getZ() + (float) ringOffset.z, 0, 0, 0, rotation, 3.5F, 0.83f, 1, 0.39f, 1, 1, (int) (5 + 2 * getWidth()), true, true, new ParticleComponent[]{
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.ALPHA, ParticleComponent.KeyTrack.startAndEnd(0.7f, 0f), false),
                        new ParticleComponent.PropertyControl(ParticleComponent.PropertyControl.EnumParticleProperty.SCALE, ParticleComponent.KeyTrack.startAndEnd(0f, (1.0f + 0.5f * getWidth()) * 8f), false)
                });
            }
        }
        return super.hitByEntity(entityIn);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public List<LivingEntity> getEntityLivingBaseNearby(double radius) {
        return getEntitiesNearby(LivingEntity.class, radius);
    }

    public <T extends Entity> List<T> getEntitiesNearby(Class<T> entityClass, double r) {
        return world.getEntitiesWithinAABB(entityClass, getBoundingBox().grow(r, r, r), e -> e != this && getDistance(e) <= r + e.getBbWidth() / 2f);
    }


    public double getAngleBetweenEntities(Entity first, Entity second) {
        return Math.atan2(second.getZ() - first.getZ(), second.getX() - first.getX()) * (180 / Math.PI) + 90;
    }

    @Override
    public void playSound(SoundEvent soundIn, float volume, float pitch) {
        super.playSound(soundIn, volume, pitch + random.nextFloat() * 0.25f - 0.125f);
    }

    @Override
    public boolean isImmuneToExplosions() {
        return true;
    }
}
