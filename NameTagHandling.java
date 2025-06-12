package com.example.examplemod.nametags;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.Random;

@Mod.EventBusSubscriber
public class NameTagHandling {
    @SubscribeEvent
    public static void onNameTagUse(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            Player player=event.getPlayer();
            Level level=player.getLevel();
            ItemStack itemStack=player.getItemInHand(InteractionHand.MAIN_HAND);
            if(itemStack.getItem()== Items.NAME_TAG&&itemStack.hasCustomHoverName()){
                Component component=itemStack.getHoverName();
                String name=component.getString();
                if("explosion".equalsIgnoreCase(name)&&level instanceof ServerLevel serverLevel){
                    serverLevel.explode(null,mob.getX(),mob.getY(),mob.getZ(),2.0F,false, net.minecraft.world.level.Explosion.BlockInteraction.NONE);
                }
                if("originator".equalsIgnoreCase(name)&&mob instanceof Creeper creeper){
                    if(creeper.level instanceof ServerLevel serverLevel){
                        creeper.discard();
                        Pig pig=new Pig(EntityType.PIG,serverLevel);
                        pig.setPos(creeper.getX(),creeper.getY(),creeper.getZ());
                        pig.setCustomName(component);
                        serverLevel.addFreshEntity(pig);
                    }
                }
                if("peacemaker".equalsIgnoreCase(name)&&mob instanceof Witch witch){
                    if(!witch.level.isClientSide){
                        witch.goalSelector.getAvailableGoals().removeIf(goal->goal.getGoal() instanceof RangedAttackGoal);
                        witch.goalSelector.addGoal(2,new ThrowPotionAtPlayerGoal(witch));
                    }
                }
                if("zerogravity".equalsIgnoreCase(name)){
                    mob.setNoGravity(true);
                    mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION,Integer.MAX_VALUE,0,false,false));
                }
            }
        }
    }
    static class ThrowPotionAtPlayerGoal extends Goal {
        private final Witch witch;
        private int CoolDown=0;
        public ThrowPotionAtPlayerGoal(Witch witch){
            this.witch=witch;
            this.setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
        }
        @Override
        public boolean canUse(){
            return true;
        }
        @Override
        public void tick(){
            if(CoolDown>0){
                CoolDown--;
                return;
            }
            Player nearestPlayer=witch.level.getNearestPlayer(witch,10);
            if(nearestPlayer!=null){
                witch.getLookControl().setLookAt(nearestPlayer,30.0F,30.0F);
                throwPotion(nearestPlayer);
                CoolDown=100;
            }
        }
        private void throwPotion(LivingEntity target){
            Vec3 vec3 = target.getDeltaMovement();
            double d0 = target.getX() + vec3.x - witch.getX();
            double d1 = target.getEyeY() - (double)1.1F - witch.getY();
            double d2 = target.getZ() + vec3.z - witch.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Potion potions[]={Potions.HEALING,
                                Potions.FIRE_RESISTANCE,
                                Potions.LUCK,
                                Potions.LEAPING,
                                Potions.STRENGTH,
                                Potions.SWIFTNESS};
            Random random=new Random();
            ThrownPotion thrownPotion=new ThrownPotion(witch.level,witch);
            thrownPotion.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potions[random.nextInt(potions.length-1)]));
            thrownPotion.setXRot(thrownPotion.getXRot() - -20.0F);
            thrownPotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
            witch.level.addFreshEntity(thrownPotion);
        }
    }
}
