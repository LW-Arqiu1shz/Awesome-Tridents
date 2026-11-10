package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentRules;
import cn.blockforge.generated.awesometridents.TridentConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 三叉戟弹射物改造：
 * <ul>
 *   <li>忠诚不再召回（{@code getLoyaltyFromItem} 恒为 0）——弹射物存续/消失逻辑完全走原版箭矢那套；</li>
 *   <li>投掷者也能触碰弹射物：碰到后弹射物直接消失，不给任何东西；</li>
 *   <li>穿刺重做：目标在水中/雨中（或攻击者带水桶）时每级 +1.5 点伤害；</li>
 *   <li>引雷重做：任意天气触发，只劈命中目标（视觉闪电 + 单体 thunderHit），不点燃方块、不摧毁物品；
 *       闪电伤害无视目标无敌帧，且不会留下新的无敌帧。</li>
 * </ul>
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {

    /** 忠诚每级的意义改为“减少投掷冷却”（在 TridentItemMixin 实现），弹射物本身不再召回。 */
    @Inject(method = "getLoyaltyFromItem", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$noRecall(ItemStack trident, CallbackInfoReturnable<Byte> cir) {
        if (TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)) {
            cir.setReturnValue((byte) 0);
        }
    }

    /** 原版排斥投掷者拾取（等召回）；召回已移除，这里让投掷者也能触碰清理弹射物。 */
    @WrapOperation(method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;ownedBy(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean awesomeTridents$ownerMayTouch(ThrownTrident projectile, Entity player, Operation<Boolean> original) {
        return !TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)
                && original.call(projectile, player);
    }

    /** 拾取不给物品：AbstractArrow.playerTouch 里 tryPickup 返回 true 后会 discard 弹射物。 */
    @Inject(method = "tryPickup", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$pickupJustVanishes(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)) {
            cir.setReturnValue(!player.isSpectator());
        }
    }

    /** 穿刺（弹射物命中）：在原版附魔伤害调整之上追加潮湿增伤。 */
    @WrapOperation(method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float awesomeTridents$impalingOnHit(ServerLevel level, ItemStack weapon, Entity target,
                                                DamageSource source, float damage, Operation<Float> original) {
        float adjusted = original.call(level, weapon, target, source, damage);
        Entity owner = ((ThrownTrident) (Object) this).getOwner();
        return adjusted + TridentRules.impalingBonus(level, weapon, target, owner);
    }

    /** 引雷（弹射物命中）：伤害生效后，若武器带引雷则在目标处劈一道只命中目标的闪电。 */
    @WrapOperation(method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean awesomeTridents$channelingOnHit(Entity target, DamageSource source, float damage,
                                                    Operation<Boolean> original) {
        boolean hurt = original.call(target, source, damage);
        if (hurt && target.level() instanceof ServerLevel level) {
            ThrownTrident self = (ThrownTrident) (Object) this;
            ItemStack weapon = self.getWeaponItem();
            if (TridentRules.enchantmentLevel(level, weapon, TridentRules.CHANNELING) > 0) {
                TridentRules.summonChanneledBolt(level, target.position(), self.getOwner(), target);
            }
        }
        return hurt;
    }
}
