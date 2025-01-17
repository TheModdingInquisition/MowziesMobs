package com.bobmowzie.mowziesmobs.server.entity.foliaath;

import com.bobmowzie.mowziesmobs.client.model.tools.ControlledAnimation;
import com.bobmowzie.mowziesmobs.client.model.tools.IntermittentAnimation;
import com.bobmowzie.mowziesmobs.server.ai.animation.AnimationAttackAI;
import com.bobmowzie.mowziesmobs.server.ai.animation.AnimationDieAI;
import com.bobmowzie.mowziesmobs.server.ai.animation.AnimationTakeDamage;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.MowzieEntity;
import com.bobmowzie.mowziesmobs.server.loot.LootTableHandler;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import com.ilexiconn.llibrary.server.animation.Animation;
import com.ilexiconn.llibrary.server.animation.AnimationHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class EntityFoliaath extends MowzieEntity implements Enemy {
    public static final Animation DIE_ANIMATION = Animation.create(50);
    public static final Animation HURT_ANIMATION = Animation.create(10);
    public static final Animation ATTACK_ANIMATION = Animation.create(14);
    private static final EntityDataAccessor<Boolean> CAN_DESPAWN = SynchedEntityData.defineId(EntityFoliaath.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVATE_TARGET = SynchedEntityData.defineId(EntityFoliaath.class, EntityDataSerializers.INT);
    private static final int ACTIVATE_DURATION = 30;
    public IntermittentAnimation<EntityFoliaath> openMouth = new IntermittentAnimation<>(this, 15, 30, 50, !level.isClientSide);
    public ControlledAnimation activate = new ControlledAnimation(ACTIVATE_DURATION);
    public ControlledAnimation deathFlail = new ControlledAnimation(5);
    public ControlledAnimation stopDance = new ControlledAnimation(10);
    public int lastTimeDecrease = 0;
    private int resettingTargetTimer = 0;
    private double prevOpenMouth;
    private double prevActivate;
    private int activateTarget;

    public EntityFoliaath(EntityType<? extends EntityFoliaath> type, Level world) {
        super(type, world);
        this.xpReward = 5;
        this.addIntermittentAnimation(openMouth);
    }
    
    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        super.push(0, y, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new AnimationAttackAI<>(this, ATTACK_ANIMATION, MMSounds.ENTITY_FOLIAATH_BITE_1.get(), null, 2, 4F, ConfigHandler.COMMON.MOBS.FOLIAATH.combatConfig.attackMultiplier.get().floatValue(), 3));
        this.goalSelector.addGoal(1, new AnimationTakeDamage<>(this));
        this.goalSelector.addGoal(1, new AnimationDieAI<>(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, LivingEntity.class, 0, true, false, e ->
                (PathfinderMob.class.isAssignableFrom(e.getClass())) && !(e instanceof EntityFoliaath || e instanceof EntityBabyFoliaath || e instanceof Creeper))
        );
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true, false));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        getEntityData().define(CAN_DESPAWN, true);
        getEntityData().define(ACTIVATE_TARGET, 0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MowzieEntity.createAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MAX_HEALTH, 10 * ConfigHandler.COMMON.MOBS.FOLIAATH.combatConfig.healthMultiplier.get())
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MMSounds.ENTITY_FOLIAATH_HURT.get();
    }

    @Override
    public SoundEvent getDeathSound() {
        return MMSounds.ENTITY_FOLIAATH_DIE.get();
    }

    @Override
    public boolean isPushableByEntity(Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
//        this.posX = xo;
//        this.posZ = zo;
        setDeltaMovement(0, getDeltaMovement().y, 0);
        // Open mouth animation
        if (getAnimation() == NO_ANIMATION && !activate.canIncreaseTimer()) {
            openMouth.update();
        } else {
            openMouth.stop();
        }

        if (activate.getAnimationFraction() >= 0.8F) {
            if (!active) {
                active = true;
            }
        } else if (activate.getAnimationFraction() < 0.8F) {
            if (active) {
                active = false;
            }
        }

        // Sounds
        if (frame % 13 == 3 && getAnimation() != DIE_ANIMATION) {
            if (openMouth.getTimeRunning() >= 10) {
                playSound(MMSounds.ENTITY_FOLIAATH_PANT_1.get(), 1, 1);
            } else if (activate.getTimer() >= 25) {
                playSound(MMSounds.ENTITY_FOLIAATH_PANT_2.get(), 1, 1);
            }
        }

        int openMouthTime = openMouth.getTimeRunning();
        if (prevOpenMouth - openMouthTime < 0) {
            if (openMouthTime == 1) {
                playSound(MMSounds.ENTITY_FOLIAATH_RUSTLE.get(), 1, 1);
            } else if (openMouthTime == 13) {
                playSound(MMSounds.ENTITY_FOLIAATH_GRUNT.get(), 1, 1);
            }
        }

        prevOpenMouth = openMouthTime;

        int activateTime = activate.getTimer();
        if (!level.isClientSide) {
            SoundEvent sound = null;
            if (prevActivate - activateTime < 0) {
                switch (activateTime) {
                    case 1:
                        sound = MMSounds.ENTITY_FOLIAATH_RUSTLE.get();
                        break;
                    case 5:
                        sound = MMSounds.ENTITY_FOLIAATH_MERGE.get();
                        break;
                }
            } else if (prevActivate - activateTime > 0) {
                switch (activateTime) {
                    case 24:
                        sound = MMSounds.ENTITY_FOLIAATH_RETREAT.get();
                        break;
                    case 28:
                        sound = MMSounds.ENTITY_FOLIAATH_RUSTLE.get();
                        break;
                }
            }
            if (sound != null) {
                playSound(sound, 1, 1);
            }
        }

        prevActivate = activateTime;

        // Targetting, attacking, and activating
        yBodyRot = 0;
        setYRot(0);

//        if (getTarget() instanceof EntityFoliaath || getTarget() instanceof EntityBabyFoliaath) {
//            setTarget(null);
//        }

        if (resettingTargetTimer > 0 && !level.isClientSide) {
            yHeadRot = yHeadRotO;
        }

        if (getTarget() != null) {
            yHeadRot = targetAngle;

            if (targetDistance <= 4 && getTarget().getY() - getY() >= -1 && getTarget().getY() - getY() <= 2 && getAnimation() == NO_ANIMATION && active) {
                AnimationHandler.INSTANCE.sendAnimationMessage(this, ATTACK_ANIMATION);
            }

            if (targetDistance <= 10.5 && getTarget().getY() - getY() >= -1.5 && getTarget().getY() - getY() <= 2) {
                setActivateTarget(ACTIVATE_DURATION);
                lastTimeDecrease = 0;
            } else if (lastTimeDecrease <= 30 && getAnimation() == NO_ANIMATION) {
                setActivateTarget(0);
                lastTimeDecrease++;
            }
        } else if (!level.isClientSide && lastTimeDecrease <= 30 && getAnimation() == NO_ANIMATION && resettingTargetTimer == 0) {
            setActivateTarget(0);
            lastTimeDecrease++;
        }

        if (getAnimation() == DIE_ANIMATION) {
            if (getAnimationTick() <= 12) {
                deathFlail.increaseTimer();
            } else {
                deathFlail.decreaseTimer();
            }
            stopDance.increaseTimer();
            setActivateTarget(ACTIVATE_DURATION);
        }

        if (resettingTargetTimer > 0) {
            resettingTargetTimer--;
        }

        if (getTarget() != null && frame % 20 == 0 && getAnimation() == NO_ANIMATION) {
            setTarget(null);
            resettingTargetTimer = 20;
        }
        if (activateTarget == activateTime) {
            activateTarget = getActivateTarget();
        } else if (activateTime < activateTarget && activate.canIncreaseTimer() || activateTime > activateTarget && activate.canDecreaseTimer()) {
            activate.increaseTimer(activateTime < activateTarget ? 1 : -2);
        }

        if (!this.level.isClientSide && this.level.getDifficulty() == Difficulty.PEACEFUL)
        {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        openMouth.resetTimeRunning();
        return (damageSource.isBypassInvul() || active) && super.hurt(damageSource, amount);
    }

    @Override
    public Animation getDeathAnimation() {
        return DIE_ANIMATION;
    }

    @Override
    public Animation getHurtAnimation() {
        return HURT_ANIMATION;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected ConfigHandler.SpawnConfig getSpawnConfig() {
        return ConfigHandler.COMMON.MOBS.FOLIAATH.spawnConfig;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType reason) {
        Biome biome = world.getBiome(blockPosition());
        int i = Mth.floor(this.getX());
        int j = Mth.floor(this.getBoundingBox().minY);
        int k = Mth.floor(this.getZ());
        BlockPos pos = new BlockPos(i, j, k);
        Block floor = level.getBlockState(pos.below()).getBlock();
        BlockState floorDown1 = level.getBlockState(pos.below(2));
        BlockState floorDown2 = level.getBlockState(pos.below(3));
        boolean notInTree = true;
        BlockState topBlock = biome.getGenerationSettings().getSurfaceBuilder().get().config().getTopMaterial();
        if (floor instanceof LeavesBlock && floorDown1 != topBlock && floorDown2 != topBlock) notInTree = false;
        return super.checkSpawnRules(world, reason) && notInTree && getEntitiesNearby(Animal.class, 5, 5, 5, 5).isEmpty() && world.getDifficulty() != Difficulty.PEACEFUL;
    }

    @Override
    public void killed(ServerLevel world, LivingEntity killedEntity) {
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1, true, true));
        super.killed(world, killedEntity);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || !getEntityData().get(CAN_DESPAWN);
    }

    public void setCanDespawn(boolean canDespawn) {
        getEntityData().set(CAN_DESPAWN, canDespawn);
    }

    public int getActivateTarget() {
        return getEntityData().get(ACTIVATE_TARGET);
    }

    public void setActivateTarget(int activateTarget) {
        getEntityData().set(ACTIVATE_TARGET, activateTarget);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("canDespawn", getEntityData().get(CAN_DESPAWN));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setCanDespawn(compound.getBoolean("canDespawn"));
    }

    @Override
    protected void playStepSound(BlockPos p_180429_1_, BlockState p_180429_2_) {

    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{DIE_ANIMATION, HURT_ANIMATION, ATTACK_ANIMATION};
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return LootTableHandler.FOLIAATH;
    }
}
