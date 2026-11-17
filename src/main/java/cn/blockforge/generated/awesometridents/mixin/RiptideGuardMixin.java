package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentRules;
import cn.blockforge.generated.awesometridents.TridentConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 激流撞防：只护"命中前后那一瞬间"——激流飞行途中，以及旋转命中生物后的
 * {@code RIPTIDE_GUARD_TICKS}（2）tick 内，玩家受到的<b>近战</b>伤害（mob_attack /
 * mob_attack_no_aggro / player_attack）按未命中处理，弹射物与其它伤害类型不受影响；
 * 静默挡下，不播放任何音效。
 *
 * <p>登记走 {@code Player.attack} 里唯一的 {@code hurtOrSimulate} 调用点（旋转撞击与普通
 * 近战共用此路径，用 {@code isAutoSpinAttack} 区分；PvP 玩家目标不登记，防滥用）；
 * 拦截走 {@code Player.hurtServer} 入口。纯服务端逻辑。</p>
 */
@Mixin(Player.class)
public abstract class RiptideGuardMixin {

    /** 旋转撞击把伤害打出去的那一刻，登记"命中后 2 tick"的近战防护窗口。 */
    @WrapOperation(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean awesomeTridents$recordGuardOnSpinHit(final Entity target, final DamageSource source, final float damage,
                                                         final Operation<Boolean> original) {
        boolean hurt = original.call(target, source, damage);
        Player self = (Player) (Object) this;
        if (TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_MELEE_GUARD)
                && hurt && self.isAutoSpinAttack() && !(target instanceof Player)
                && self.level() instanceof ServerLevel level) {
            TridentRules.noteRiptideGuard(level, self);
        }
        return hurt;
    }

    /** 飞行途中或防护窗口内，近战伤害直接按未命中处理（无音效、无反馈）。 */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$blockGuardedDamage(final ServerLevel level, final DamageSource source, final float amount,
                                                    final CallbackInfoReturnable<Boolean> cir) {
        if (!TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_MELEE_GUARD)
                || amount <= 0.0F || !awesomeTridents$isMelee(source)) {
            return;
        }
        Player self = (Player) (Object) this;
        if (self.isAutoSpinAttack() || TridentRules.isRiptideGuardActive(self, level.getGameTime())) {
            cir.setReturnValue(false);
        }
    }

    /** 近战伤害 = 生物/玩家的直接接触攻击；弹射物（direct 是弹射物）与火焰、摔落等其它来源都不算。 */
    private static boolean awesomeTridents$isMelee(final DamageSource source) {
        return source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.PLAYER_ATTACK);
    }
}
