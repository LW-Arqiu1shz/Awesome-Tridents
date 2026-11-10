package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentRules;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * 引雷击杀掉熟肉：被附魔引雷的武器击败、或被引雷召出的闪电劈死的生物，
 * 其战利品表掉落按烟熏炉配方把"生"转成"熟"（生牛肉→牛排、生猪排→熟猪排……）。
 *
 * <p>只改战利品表这一路（{@code dropFromLootTable} 的产物消费者）：
 * 装备掉落（怪物拿着的东西）、经验、非食物掉落都不受影响；
 * 天气自然落雷杀死的生物也不会做熟（判定窗口见
 * {@link TridentRules#shouldCookDeathLoot}）。仅服务端执行。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathLootMixin {

    /**
     * 包住原版传入的战利品消费者：满足"引雷击杀"时，每一份产出先过一遍烟熏炉配方再掉落。
     * 原版 5 参方法本体照常执行，这里只替换落点，不重写任何掉落逻辑。
     */
    @ModifyVariable(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private Consumer<ItemStack> awesomeTridents$smokeMeatOnChannelingKill(Consumer<ItemStack> consumer,
                                                                           ServerLevel level,
                                                                           DamageSource source,
                                                                           boolean willHarvest,
                                                                           ResourceKey<LootTable> lootTableId) {
        if (!TridentRules.shouldCookDeathLoot(level, source)) {
            return consumer;
        }
        return stack -> TridentRules.emitCooked(level, stack, consumer);
    }
}
