package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.LivingEntityLastHurtAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 开放 {@code LivingEntity.lastHurt}（protected）给 {@code TridentRules} 使用：
 * 闪电无视无敌帧结算后需要把目标的“上次受伤数值”还原，避免改变它原有的受伤记录。
 *
 * <p>无敌帧字段本身（{@code damageCooldownTime}）在 26.3 是 public，不需要访问器。</p>
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor extends LivingEntityLastHurtAccess {

    @Override
    @Accessor("lastHurt")
    float awesomeTridents$getLastHurt();

    @Override
    @Accessor("lastHurt")
    void awesomeTridents$setLastHurt(float lastHurt);
}
