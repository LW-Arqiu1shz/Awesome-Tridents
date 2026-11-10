package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentConfig;
import cn.blockforge.generated.awesometridents.TridentRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 对本模组创建的引雷闪电单独应用保护选项，不改变天气自然闪电。
 *
 * <p>使用 MixinExtras 的 {@code @WrapOperation} 而不是 {@code @Redirect}：同一调用点被两个模组
 * {@code @Redirect} 时 Mixin 会直接报错导致游戏启动崩溃，WrapOperation 可以与其他模组叠加。</p>
 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
    private void awesomeTridents$filterLightningTarget(Entity target, ServerLevel level, LightningBolt bolt,
                                                       Operation<Void> original) {
        boolean channeled = TridentRules.isChanneledBolt(bolt);
        if (channeled && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_PROTECT_DROPS)
                && target instanceof ItemEntity) {
            return;
        }
        if (channeled) {
            TridentRules.applyLightningHit(level, bolt, target);
        } else {
            original.call(target, level, bolt);
        }
    }

    @Inject(method = "spawnFire(I)V", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$protectImpactBlocks(int additionalSources, CallbackInfo ci) {
        LightningBolt self = (LightningBolt) (Object) this;
        if (TridentRules.isChanneledBolt(self)
                && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_PROTECT_BLOCKS)) {
            ci.cancel();
        }
    }
}
