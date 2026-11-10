package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.TridentConfig;
import cn.blockforge.generated.awesometridents.TridentRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * 三叉戟防穿透命中判定：高速飞行的三叉戟不再穿过碰撞体积较小的目标。
 *
 * <p>原版 {@code AbstractArrow.findHitEntity} 走单发射线版 {@code ProjectileUtil.getEntityHitResult}，
 * 容差 margin 由 {@code computeMargin} 从弹射物第 2 tick 起才线性升到 0.3 —— 刚离手（速度最高）的
 * 前几 tick margin 为 0，判定是一条零宽射线，容易从鸡、盔甲架这类小目标身上穿过去。
 * 这里改为穿透箭矢 {@code findHitEntities} 同款的多目标扫掠版 {@code getManyEntityHitResult}：
 * 固定 0.35 容差、从第一 tick 生效，支持"本 tick 起点已在目标体内"的贴脸命中，
 * 并把容差命中点投影回目标真实碰撞箱表面（投影前还会按方块阻挡截断，不会穿墙命中）；
 * 候选里取沿射程最近的一个，命中过滤沿用原版虚方法 {@code canHitEntity}。</p>
 *
 * <p>注入点选在声明处 {@code AbstractArrow}（而非子类），命中过滤直接调用原版虚方法避免语义漂移；
 * 用 {@code ThrownTrident} 闸门保证只改三叉戟，箭矢行为分毫不差。
 * 已撞伤过目标后原版 {@code ThrownTrident.findHitEntity} 会短路（{@code dealtDamage}），
 * 不会走到本注入，"单次命中"语义保持不变。</p>
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowSweptHitMixin {

    /** 原版命中过滤（排除主人、同车、旁观者、不可命中类型等），直接虚调用，零语义漂移。 */
    @Shadow
    protected abstract boolean canHitEntity(Entity entity);

    @Inject(method = "findHitEntity", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$sweptEntityHit(final Vec3 from, final Vec3 to,
                                                final CallbackInfoReturnable<EntityHitResult> cir) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self instanceof ThrownTrident)) {
            return;
        }
        // 属于选项 2「三叉戟投掷更改」：关闭时整套投掷改动交还原版，含弹道命中判定。
        if (!TridentConfig.isEnabled(TridentConfig.Option.THROWING_AND_LOYALTY)) {
            return;
        }
        Collection<EntityHitResult> hits = ProjectileUtil.getManyEntityHitResult(
                self.level(), self, from, to,
                new AABB(from, to).inflate(2.0),
                this::canHitEntity,
                TridentRules.TRIDENT_SWEEP_MARGIN, ClipContext.Block.COLLIDER, true, true);
        EntityHitResult nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (EntityHitResult hit : hits) {
            double d = from.distanceToSqr(hit.getLocation());
            if (d < nearestDist) {
                nearestDist = d;
                nearest = hit;
            }
        }
        cir.setReturnValue(nearest);
    }
}
