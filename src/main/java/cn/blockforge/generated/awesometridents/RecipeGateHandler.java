package cn.blockforge.generated.awesometridents;

import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * 普通业务包中的配方过滤判断。
 *
 * <p>注意：此类必须位于 Mixin 配置包之外。普通代码和 Mixin 注入类都可以安全引用它，
 * 避免触发 Mixin 对专用包类的直接加载限制。</p>
 */
public final class RecipeGateHandler {
    private RecipeGateHandler() {
    }

    public static boolean shouldHide(RecipeHolder<?> holder) {
        return holder != null
                && !TridentConfig.isEnabled(TridentConfig.Option.TRIDENT_CRAFTING)
                && "awesome_tridents:prismarine_trident".equals(holder.id().identifier().toString());
    }
}
