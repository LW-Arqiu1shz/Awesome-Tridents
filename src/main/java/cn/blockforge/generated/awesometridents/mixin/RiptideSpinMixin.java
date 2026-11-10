package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.RiptidePathAccess;
import cn.blockforge.generated.awesometridents.TridentRules;
import cn.blockforge.generated.awesometridents.TridentConfig;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** 服务端权威的激流扫掠碰撞：命中路径上的第一个生物后，在接触点攻击并轻微回弹。 */
@Mixin(LivingEntity.class)
public abstract class RiptideSpinMixin implements RiptidePathAccess {

    @Shadow
    protected int autoSpinAttackTicks;
    @Shadow
    protected float autoSpinAttackDmg;
    @Shadow
    protected ItemStack autoSpinAttackItemStack;

    @Shadow
    protected abstract void doAutoAttackOnTouch(LivingEntity target);
    @Shadow
    protected abstract void setLivingEntityFlag(int flag, boolean value);

    @Unique
    private int awesomeTridents$lastSpinTicks;
    @Unique
    private int awesomeTridents$spinAge;
    @Unique
    private Vec3 awesomeTridents$previousFeet;

    @Override
    public void awesomeTridents$beginRiptidePath(Vec3 feet) {
        this.awesomeTridents$previousFeet = feet;
        this.awesomeTridents$lastSpinTicks = 0;
        this.awesomeTridents$spinAge = 0;
    }

    @Inject(method = "checkAutoSpinAttack", at = @At("HEAD"), cancellable = true)
    private void awesomeTridents$checkSweptRiptideHit(AABB travelStart, AABB travelEnd, CallbackInfo ci) {
        if (!TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE)) {
            return;
        }
        boolean collisionFix = TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_COLLISION_FIX);
        boolean movementFix = TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_MOVEMENT_FIX);
        if (!collisionFix && !movementFix) {
            return; // 两个修复子项都关闭时完全交还原版 checkAutoSpinAttack。
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) {
            return;
        }

        int ticks = this.autoSpinAttackTicks;
        if (ticks > this.awesomeTridents$lastSpinTicks) {
            this.awesomeTridents$spinAge = 0;
        } else {
            this.awesomeTridents$spinAge++;
        }

        Vec3 end = player.position();
        Vec3 start = this.awesomeTridents$previousFeet;
        if (start == null || start.distanceToSqr(end) > TridentRules.RIPTIDE_MAX_SWEEP_DISTANCE_SQR) {
            start = new Vec3((travelStart.minX + travelStart.maxX) * 0.5, travelStart.minY,
                    (travelStart.minZ + travelStart.maxZ) * 0.5);
        }

        RiptideHit hit = collisionFix && player.isAutoSpinAttack()
                ? awesomeTridents$findFirstHit(player, start, end) : null;
        Vec3 recoil = Vec3.ZERO;
        boolean snapped = false;
        boolean stoppedByObstacle = false;
        if (hit != null) {
            Vec3 movement = end.subtract(start);
            Vec3 direction = movement.lengthSqr() > 1.0E-8 ? movement.normalize() : player.getDeltaMovement().normalize();
            Vec3 stop = hit.contact().subtract(direction.scale(TridentRules.RIPTIDE_CONTACT_GAP));
            player.setPos(stop);
            this.doAutoAttackOnTouch(hit.target());
            this.autoSpinAttackTicks = 0;
            recoil = direction.scale(-TridentRules.RIPTIDE_RECOIL_SPEED);
            player.setDeltaMovement(recoil);
            snapped = true;
        } else if (movementFix && ((this.awesomeTridents$spinAge >= TridentRules.RIPTIDE_SNAP_GRACE_TICKS
                && player.onGround() && !player.isInWater()) || player.horizontalCollision)) {
            this.autoSpinAttackTicks = 0;
            player.setDeltaMovement(Vec3.ZERO);
            stoppedByObstacle = true;
        }

        // 未触发修复时不能取消原版方法，否则原版旋转攻击/碰撞维护会被整体跳过。
        if (!snapped && !stoppedByObstacle) {
            this.awesomeTridents$lastSpinTicks = this.autoSpinAttackTicks;
            this.awesomeTridents$previousFeet = player.position();
            return;
        }

        if (this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
            this.autoSpinAttackDmg = 0.0F;
            this.autoSpinAttackItemStack = null;
        }

        if (snapped) {
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), recoil));
        } else {
            player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), Vec3.ZERO));
        }

        this.awesomeTridents$lastSpinTicks = this.autoSpinAttackTicks;
        this.awesomeTridents$previousFeet = player.position();
        ci.cancel();
    }

    @Unique
    private static RiptideHit awesomeTridents$findFirstHit(ServerPlayer player, Vec3 start, Vec3 end) {
        double halfWidth = player.getBbWidth() * 0.5;
        double height = player.getBbHeight();
        AABB startBox = AABB.ofSize(start.add(0.0, height * 0.5, 0.0),
                player.getBbWidth(), height, player.getBbWidth());
        AABB searchBox = startBox.minmax(startBox.move(end.subtract(start))).inflate(0.001);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target != player && target.isAlive() && target.isAttackable() && !target.isSpectator());

        RiptideHit nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity target : targets) {
            AABB box = target.getBoundingBox();
            AABB contactBox = new AABB(
                    box.minX - halfWidth, box.minY - height, box.minZ - halfWidth,
                    box.maxX + halfWidth, box.maxY, box.maxZ + halfWidth);
            Vec3 contact = contactBox.contains(start) ? start : contactBox.clip(start, end).orElse(null);
            if (contact == null) {
                continue;
            }
            double distance = start.distanceToSqr(contact);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new RiptideHit(target, contact);
            }
        }
        return nearest;
    }

    @Unique
    private record RiptideHit(LivingEntity target, Vec3 contact) {
    }
}
