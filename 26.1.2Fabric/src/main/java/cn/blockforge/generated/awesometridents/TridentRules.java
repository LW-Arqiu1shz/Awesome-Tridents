package cn.blockforge.generated.awesometridents;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Awesome Tridents 规则工具类。
 *
 * <p>集中实现：投掷冷却（忠诚每级 -0.5 秒）、水桶激流、潮湿穿刺增伤、精准引雷、激流蓝色粒子、
 * 激流撞防（飞行途中与命中后 2 tick 内免疫近战伤害）。全部逻辑只在服务端执行，
 * 原版客户端可通过同步的粒子/实体/数据包看到效果。</p>
 */
public final class TridentRules {

    /** 三叉戟投掷后的基础冷却：2 秒。 */
    public static final int BASE_THROW_COOLDOWN_TICKS = 40;
    /** 忠诚附魔每级减少 0.5 秒（10 tick）冷却。 */
    public static final int TICKS_CUT_PER_LOYALTY_LEVEL = 10;
    /** 穿刺：目标潮湿时每级 +1.5 点伤害。 */
    public static final float IMPALING_DAMAGE_PER_LEVEL = 1.5F;
    public static final float VANILLA_IMPALING_DAMAGE_PER_LEVEL = 2.5F;
    public static final String CHANNELED_BOLT_TAG = GeneratedMod.MOD_ID + ":channeled_bolt";

    /** 激流旋转的总时长（tick）：原版 TridentItem 硬编码 startAutoSpinAttack(20, …)。 */
    public static final int RIPTIDE_SPIN_TOTAL_TICKS = 20;
    /** 激流"落地即停"的宽限期（tick）：起跳后前几 tick，服务端持有的还是起跳前"站在地面"的旧上报，先不作数。 */
    public static final int RIPTIDE_SNAP_GRACE_TICKS = 4;
    /** 激流碰撞时留在目标碰撞箱前的极小间隙。 */
    public static final double RIPTIDE_CONTACT_GAP = 0.01;
    /** 激流命中后的轻微反向弹速。 */
    public static final double RIPTIDE_RECOIL_SPEED = 0.2;
    /** 异常大位移不参与跨 tick 扫掠，避免传送路径误伤实体。 */
    public static final double RIPTIDE_MAX_SWEEP_DISTANCE_SQR = 256.0;
    /** 激流撞防：旋转命中后，这么多 tick 内玩家受到的<b>近战</b>伤害按未命中处理（只护命中前后那一瞬间）。 */
    public static final int RIPTIDE_GUARD_TICKS = 2;
    /** 三叉戟防穿透扫掠判定的容差（格）：比穿透箭矢稳定期的 0.3 略宽，且从弹射物第 1 tick 就全量生效。 */
    public static final float TRIDENT_SWEEP_MARGIN = 0.35F;

    public static final String LOYALTY = "loyalty";
    public static final String IMPALING = "impaling";
    public static final String CHANNELING = "channeling";

    /**
     * 同步窗口标记：正在结算"引雷召出的闪电"的 thunderHit 时为 true。
     * thunderHit → 伤害 → die → dropAllDeathLoot 是同一条调用栈上的同步过程，
     * 因此在死亡掉落处读这个标记，就能区分"引雷触发的闪电"与天气自然落雷（后者不煮熟）。
     */
    private static final ThreadLocal<Boolean> IN_CHANNELED_BOLT = new ThreadLocal<>();

    /** 激流触发时手部泛起的蓝光（天蓝色粉尘粒子）。 */
    private static final int RIPTIDE_GLOW_COLOR = 0xFF74D3FF;

    private static final String COOLDOWN_TAG_KEY = GeneratedMod.MOD_ID + "_throw_ready_at";

    private TridentRules() {
    }

    // ------------------------------------------------------------------
    // 附魔等级读取（按原版附魔 id，经注册表 Holder 查询）
    // ------------------------------------------------------------------

    public static int enchantmentLevel(Level level, ItemStack stack, String enchantmentPath) {
        if (stack.isEmpty()) {
            return 0;
        }
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace(enchantmentPath));
        // 不能用 getOrThrow：数据包可以移除/禁用原版附魔，那样每次近战或三叉戟命中都会抛异常导致服务器崩溃。
        // 附魔不存在时按 0 级处理即可。
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(key))
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    // ------------------------------------------------------------------
    // 水桶：快捷栏或副手携带水桶 → 视为自己在水中/雨中
    // ------------------------------------------------------------------

    public static boolean carriesWaterBucket(Player player) {
        if (player.getOffhandItem().is(Items.WATER_BUCKET)) {
            return true;
        }
        NonNullList<ItemStack> slots = player.getInventory().getNonEquipmentItems();
        int hotbar = Math.min(Inventory.getSelectionSize(), slots.size());
        for (int i = 0; i < hotbar; i++) {
            if (slots.get(i).is(Items.WATER_BUCKET)) {
                return true;
            }
        }
        return false;
    }

    /** 激流可用性：原版（在水中或雨中）或携带水桶。 */
    public static boolean canUseRiptide(Player player) {
        return player.isInWaterOrRain()
                || (TridentConfig.isEnabled(TridentConfig.Option.RIPTIDE_BUCKET_SELF) && carriesWaterBucket(player));
    }

    /**
     * 穿刺的“潮湿”判定：目标自身处于水中/雨中，
     * 或者攻击者（三叉戟的主人/持有者）携带水桶——此时命中的敌人也视为潮湿。
     */
    public static boolean targetCountsAsWet(Entity target, Entity attacker) {
        return target.isInWaterOrRain()
                || (TridentConfig.isEnabled(TridentConfig.Option.IMPALING_BUCKET_TARGET)
                && attacker instanceof Player player && carriesWaterBucket(player));
    }

    public static float impalingBonus(Level level, ItemStack weapon, Entity target, Entity attacker) {
        int impalingLevel = enchantmentLevel(level, weapon, IMPALING);
        if (impalingLevel <= 0) {
            return 0.0F;
        }

        // 主开关关闭时恢复原版穿刺：对原版“易受穿刺”生物生效，每级 +2.5。
        if (!TridentConfig.isEnabled(TridentConfig.Option.IMPALING_LOGIC)) {
            return target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING)
                    ? VANILLA_IMPALING_DAMAGE_PER_LEVEL * impalingLevel : 0.0F;
        }

        if (!targetCountsAsWet(target, attacker)) {
            return 0.0F;
        }
        float amount = TridentConfig.isEnabled(TridentConfig.Option.IMPALING_BALANCE)
                ? IMPALING_DAMAGE_PER_LEVEL : VANILLA_IMPALING_DAMAGE_PER_LEVEL;
        return amount * impalingLevel;
    }

    // ------------------------------------------------------------------
    // 投掷冷却（写在物品 stack 的 custom_data 上，随原版容器包同步，vanilla 客户端兼容）
    // ------------------------------------------------------------------

    public static boolean isOnCooldown(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        // 26.x 的 getLong 返回 Optional，缺失时按“未冷却”处理
        return level.getGameTime() < tag.getLongOr(COOLDOWN_TAG_KEY, Long.MIN_VALUE);
    }

    /** 投掷成功后调用：按忠诚等级写入冷却截止时间。 */
    public static void startThrowCooldown(Level level, ItemStack stack) {
        int loyalty = enchantmentLevel(level, stack, LOYALTY);
        int cooldown = Math.max(0, BASE_THROW_COOLDOWN_TICKS - TICKS_CUT_PER_LOYALTY_LEVEL * loyalty);
        if (cooldown <= 0) {
            return;
        }
        long readyAt = level.getGameTime() + cooldown;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(COOLDOWN_TAG_KEY, readyAt));
    }

    // ------------------------------------------------------------------
    // 精准引雷：任意天气，只劈命中目标，不点燃方块、不摧毁物品
    // ------------------------------------------------------------------

    /**
     * 在指定位置生成一道“仅视觉”的闪电（有模型、闪光与雷声，但没有原版群伤与点火），
     * 然后手动对命中目标应用 thunderHit（点燃生物 + 闪电伤害），保持原版单体手感。
     *
     * <p>闪电伤害无视目标身上的无敌帧：结算前把无敌帧清零，让这 5 点伤害完整生效；
     * 结算后再恢复进入时的那份无敌帧，所以这道闪电自己不会留下新的无敌帧，
     * 也不会因为刚被三叉戟命中而白白吞掉伤害。</p>
     */
    public static void summonChanneledBolt(ServerLevel level, Vec3 position, Entity owner, Entity struckTarget) {
        // channeling.json 将原版数据驱动效果清空，因此主开关关闭时在代码中恢复原版天气/天空条件。
        boolean customChanneling = TridentConfig.isEnabled(TridentConfig.Option.CHANNELING);
        boolean sunny = customChanneling && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_SUNNY);
        if (!sunny) {
            net.minecraft.core.BlockPos strikePos = struckTarget != null ? struckTarget.blockPosition()
                    : net.minecraft.core.BlockPos.containing(position);
            if (!level.isThundering() || !level.canSeeSky(strikePos)) {
                return;
            }
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (bolt == null) {
            return;
        }
        bolt.setPos(position.x, position.y, position.z);
        bolt.addTag(CHANNELED_BOLT_TAG);
        if (owner instanceof ServerPlayer cause) {
            bolt.setCause(cause);
        }

        boolean singleTarget = customChanneling
                && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_SINGLE_TARGET);
        bolt.setVisualOnly(singleTarget);
        level.addFreshEntity(bolt);
        // channeling.json 去掉了原版 post_attack 效果，连带去掉了它附带的"三叉戟雷鸣"音效，这里补回。
        level.playSound(null, position.x, position.y, position.z, SoundEvents.TRIDENT_THUNDER,
                SoundSource.WEATHER, 5.0F, 1.0F);

        if (singleTarget && struckTarget != null && !struckTarget.isRemoved()) {
            applyLightningHit(level, bolt, struckTarget);
            if (owner instanceof ServerPlayer player) {
                CriteriaTriggers.CHANNELED_LIGHTNING.trigger(player, List.of(struckTarget));
            }
        }
        // 单体以外的闪电由 LightningBolt 自己在后续 tick 中执行原版范围命中与成就逻辑。
    }

    /** 仅用于被本模组标记的引雷闪电；供闪电实体的正常 tick 与单体模式共用。 */
    public static boolean isChanneledBolt(LightningBolt bolt) {
        return bolt.entityTags().contains(CHANNELED_BOLT_TAG);
    }

    /**
     * 执行原版 thunderHit，同时按 3.3 配置临时清除伤害冷却。
     * 标记只在本次同步 thunderHit 调用栈内存在，用来识别本模组引雷造成的死亡掉落。
     */
    public static void applyLightningHit(ServerLevel level, LightningBolt bolt, Entity target) {
        boolean channeled = isChanneledBolt(bolt);
        Boolean previousMarker = IN_CHANNELED_BOLT.get();
        if (channeled) {
            IN_CHANNELED_BOLT.set(Boolean.TRUE);
        }
        try {
            boolean bypassInvulnerability = channeled
                    && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING)
                    && TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_NO_INVULNERABILITY)
                    && target instanceof LivingEntity;
            if (!bypassInvulnerability) {
                target.thunderHit(level, bolt);
                return;
            }

            LivingEntity living = (LivingEntity) target;
            LivingEntityLastHurtAccess accessor = (LivingEntityLastHurtAccess) (Object) living;
            int savedCooldown = living.invulnerableTime;
            float savedLastHurt = accessor.awesomeTridents$getLastHurt();
            living.invulnerableTime = 0;
            try {
                living.thunderHit(level, bolt);
            } finally {
                living.invulnerableTime = savedCooldown;
                accessor.awesomeTridents$setLastHurt(savedLastHurt);
            }
        } finally {
            if (previousMarker == null) {
                IN_CHANNELED_BOLT.remove();
            } else {
                IN_CHANNELED_BOLT.set(previousMarker);
            }
        }
    }

    // ------------------------------------------------------------------
    // 引雷击杀掉熟肉：被带引雷的武器击败、或被引雷召出的闪电劈死
    // ------------------------------------------------------------------

    /**
     * 该次死亡掉落是否应把生肉做熟：
     * <ul>
     *   <li>致死伤害来源的武器（近战=攻击者主手，弹射=三叉戟复制体）带引雷附魔；</li>
     *   <li>或伤害类型是闪电、且正处于"引雷召出的闪电"同步结算窗口内（天气自然落雷不算）。</li>
     * </ul>
     */
    public static boolean shouldCookDeathLoot(ServerLevel level, DamageSource source) {
        // 子选项「雷击烤肉」；isEnabled 会连带检查父选项「引雷更改」。
        if (!TridentConfig.isEnabled(TridentConfig.Option.CHANNELING_COOK_DROPS)) {
            return false;
        }
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            return Boolean.TRUE.equals(IN_CHANNELED_BOLT.get());
        }
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && enchantmentLevel(level, weapon, CHANNELING) > 0;
    }

    /**
     * 按烟熏炉配方把掉落物从生做熟（烟熏炉只收食物，不会误伤矿石/沙子那类熔炉产物）。
     * 没有对应烟熏配方的掉落物（羽毛、皮革等）原样返回；数量按比例保留。
     */
    public static ItemStack cookStack(ServerLevel level, ItemStack raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        SingleRecipeInput input = new SingleRecipeInput(raw.copyWithCount(1));
        ItemStack cooked = level.recipeAccess()
                .getRecipeFor(RecipeType.SMOKING, input, level)
                .map(holder -> holder.value().assemble(input))
                .orElse(null);
        if (cooked == null || cooked.isEmpty() || cooked.is(raw.getItem())) {
            return raw;
        }
        // long 防溢出；数量上限在 emitCooked 里按最大堆叠拆分。
        long total = (long) cooked.getCount() * raw.getCount();
        return cooked.copyWithCount((int) Math.max(1L, Math.min(Integer.MAX_VALUE, total)));
    }

    /**
     * 烤熟后再交给原版消费者，并按熟食的最大堆叠数拆成多组。
     * 熟食的堆叠上限可能比生肉小（数据包/其他模组可改 max_stack_size），
     * 超限的 ItemStack 掉在地上后会在存档编码（数量限制 1~99）或网络同步时出错。
     */
    public static void emitCooked(ServerLevel level, ItemStack raw, java.util.function.Consumer<ItemStack> consumer) {
        ItemStack cooked = cookStack(level, raw);
        if (cooked == raw) {
            consumer.accept(raw);
            return;
        }
        int remaining = cooked.getCount();
        int maxStack = Math.max(1, cooked.getMaxStackSize());
        while (remaining > 0) {
            int part = Math.min(remaining, maxStack);
            consumer.accept(cooked.copyWithCount(part));
            remaining -= part;
        }
    }

    // ------------------------------------------------------------------
    // 激流撞防：命中后极短窗口内免疫近战伤害（飞行途中由旋转状态直接判定，不查表）
    // ------------------------------------------------------------------

    /** 玩家实体 id → 近战防护截止 gameTime（含当刻，命中后第 {@code RIPTIDE_GUARD_TICKS} tick 仍受护）。纯服务端内存态，不落盘。 */
    private static final ConcurrentHashMap<Integer, Long> RIPTIDE_GUARDS = new ConcurrentHashMap<>();

    /** 激流旋转撞伤某生物时登记：命中 tick 起（含）往后 {@link #RIPTIDE_GUARD_TICKS} tick 内近战打不动这名玩家。 */
    public static void noteRiptideGuard(ServerLevel level, Player victim) {
        long now = level.getGameTime();
        RIPTIDE_GUARDS.values().removeIf(expiry -> expiry < now);
        RIPTIDE_GUARDS.put(victim.getId(), now + RIPTIDE_GUARD_TICKS);
    }

    /** 是否处于"命中后 2 tick"近战防护窗口内（飞行途中的防护由 isAutoSpinAttack 直接给出）。 */
    public static boolean isRiptideGuardActive(Player victim, long now) {
        Long expiry = RIPTIDE_GUARDS.get(victim.getId());
        if (expiry == null) {
            return false;
        }
        if (expiry < now) {
            RIPTIDE_GUARDS.remove(victim.getId(), expiry);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 激流触发时的蓝色光效（服务端发粒子，原版客户端可见）
    // ------------------------------------------------------------------

    public static void emitRiptideGlow(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        DustParticleOptions glow = new DustParticleOptions(RIPTIDE_GLOW_COLOR, 1.4F);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        // 手/上半身一圈蓝光
        level.sendParticles(glow, x, y + 1.1, z, 18, 0.35, 0.45, 0.35, 0.015);
        level.sendParticles(glow, x, y + 1.6, z, 8, 0.25, 0.2, 0.25, 0.01);
    }
}
