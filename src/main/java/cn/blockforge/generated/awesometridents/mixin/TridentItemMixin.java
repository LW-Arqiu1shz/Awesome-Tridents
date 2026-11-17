package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.RiptidePathAccess;
import cn.blockforge.generated.awesometridents.TridentConfig;
import cn.blockforge.generated.awesometridents.TridentRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;

/**
 * 三叉戟物品改造：
 * <ul>
 *   <li>蓄力投掷不再消耗三叉戟本体，改为发射一枚“弹射物复制体”；</li>
 *   <li>投掷后进入 2 秒冷却，忠诚每级 -0.5 秒（冷却写在 custom_data，原版客户端兼容）；</li>
 *   <li>携带水桶（快捷栏/副手）视为在水中/雨中 → 陆地可用水桶发动激流；</li>
 *   <li>激流发动时手部泛起蓝色光效（服务端粒子，所有客户端可见）。</li>
 * </ul>
 */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin {

    /** 三叉戟蓄力不降低移动速度，并允许保持疾跑。组件会随物品栏同步，纯服务端安装也生效。 */
    private static final UseEffects AWESOME_TRIDENTS$FULL_SPEED_USE = new UseEffects(true, true, 1.0F);

    /** 冷却期间禁止开始蓄力；成功开始时只给当前三叉戟写入全速使用效果。 */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$blockChargingOnCooldown(Level level, Player player, InteractionHand hand,
                                                         CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)) {
            if (TridentRules.isOnCooldown(stack, level)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
            stack.set(DataComponents.USE_EFFECTS, AWESOME_TRIDENTS$FULL_SPEED_USE);
        } else if (AWESOME_TRIDENTS$FULL_SPEED_USE.equals(stack.get(DataComponents.USE_EFFECTS))) {
            // 若玩家先前开启过选项，关闭后清除本模组写入的临时使用组件，避免残留全速蓄力。
            stack.remove(DataComponents.USE_EFFECTS);
        }
    }

    /** 激流水源判定：在水里/雨中，或快捷栏/副手有水桶。 */
    @WrapOperation(method = {"use", "releaseUsing"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean awesomeTridents$bucketFeedsRiptide(Player player, Operation<Boolean> original) {
        if (original.call(player)) {
            return true;
        }
        return TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_BUCKET_SELF) && TridentRules.carriesWaterBucket(player);
    }

    /**
     * 投掷不消耗自身：原版此处是 stack.consumeAndReturn(1, player)（扣一个本体给弹射物），
     * 改为直接给弹射物一份副本，手上三叉戟保持不动，并写入投掷冷却。
     */
    @WrapOperation(method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack awesomeTridents$throwWithoutConsuming(ItemStack stack, int count, LivingEntity user,
                                                            Operation<ItemStack> original) {
        if (!TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)) {
            return original.call(stack, count, user);
        }
        Level level = user.level();
        if (!level.isClientSide() && user instanceof Player player && !player.hasInfiniteMaterials()) {
            TridentRules.startThrowCooldown(level, stack);
        }
        return stack.copyWithCount(count);
    }

    /** 激流启动瞬间：蓝光辉光。 */
    @WrapOperation(method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startAutoSpinAttack(IFLnet/minecraft/world/item/ItemStack;)V"))
    private void awesomeTridents$riptideGlow(Player player, int duration, float strength, ItemStack stack,
                                             Operation<Void> original) {
        if (TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE)) {
            if (player instanceof RiptidePathAccess path) {
                path.awesomeTridents$beginRiptidePath(player.position());
            }
        }
        original.call(player, duration, strength, stack);
        if (TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE) && player.level() instanceof ServerLevel) {
            TridentRules.emitRiptideGlow(player);
        }
    }

    /**
     * 仅服务端安装的激流位移修复。
     *
     * <p>26.3 的激流位移是客户端本地预测的：原版 {@code releaseUsing} 的激流分支
     * （push 速度、进入旋转、贴地时上跳 1.2 格）两端各跑一份，但方法开头有一道
     * {@code isInWaterOrRain} 闸门——激流三叉戟不在水里/雨中就直接 return。
     * 于是仅服务端安装、玩家带水桶在陆地发动激流时：服务端被本模组的 Redirect 放行
     * 并算好了速度起跳，而原版客户端闸门不过、什么都没做；玩家速度又从来由客户端自己算
     * （原版不会主动给控制端补发速度），于是"只转不走"。</p>
     *
     * <p>修复：服务端激流分支结算完成（return true）时，若玩家<b>实际</b>不在水/雨中
     * （即靠带桶放行，原版客户端必然没有本地预测），把服务端当前的位置与激流速度用
     * 传送包（26.3 的 PositionMoveRotation 自带 deltaMovement）加运动包补给控制端。
     * 客户端拿到速度后，配合已同步的旋转标志位，即可跑出与原版一致的激流位移。</p>
     */
    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void awesomeTridents$syncRiptideMotion(ItemStack stack, Level level, LivingEntity user, int timeLeft,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE)
                || !TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_BUCKET_SELF)
                || !cir.getReturnValueZ() || !(user instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // 只有激流分支成功发动才会走到这里；真在水里/雨中的话原版客户端自己预测过了，不插手
        if (!serverPlayer.isAutoSpinAttack() || serverPlayer.isInWaterOrRain()) {
            return;
        }
        Vec3 riptideVelocity = serverPlayer.getDeltaMovement();
        serverPlayer.connection.teleport(PositionMoveRotation.of(serverPlayer), Collections.emptySet());
        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer.getId(), riptideVelocity));
    }
}
