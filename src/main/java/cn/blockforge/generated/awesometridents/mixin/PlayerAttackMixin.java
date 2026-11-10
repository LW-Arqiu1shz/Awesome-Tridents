package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 近战攻击的穿刺重做：主手武器带穿刺且目标潮湿（水中/雨中，或攻击者带水桶）时，
 * 在原版附魔增伤之上追加 每级 +1.5 点伤害。
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @WrapOperation(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
    private float awesomeTridents$impalingMelee(Player self, Entity target, float damage, DamageSource source,
                                                Operation<Float> original) {
        float base = original.call(self, target, damage, source);
        return base + TridentRules.impalingBonus(self.level(), self.getWeaponItem(), target, self);
    }
}
