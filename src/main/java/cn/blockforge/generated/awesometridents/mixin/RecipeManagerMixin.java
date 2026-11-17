package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.RecipeGateHandler;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** 过滤直接的 RecipeManager 查询，覆盖合成表、配方书等不经过 RecipeCache 的入口。 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = true)
    private void awesomeTridents$disableTridentRecipe(RecipeType<?> type, RecipeInput input, Level level,
            CallbackInfoReturnable<Optional<RecipeHolder<?>>> cir) {
        if (cir.getReturnValue().map(RecipeGateHandler::shouldHide).orElse(false)) {
            cir.setReturnValue(Optional.empty());
        }
    }

    /**
     * 过滤「指定配方」查询重载。
     *
     * <p>{@code CraftingMenu.slotChangedCraftingGrid} 算结果槽时用的是
     * {@code getRecipeFor(type, input, level, holder)}：holder 非空且 matches 就直接返回，
     * 完全不会经过上面那个三参重载。而 holder 来自 {@code AbstractCraftingMenu.usedRecipe}
     * —— 配方书一键摆放（{@code handlePlaceRecipe} → {@code finishPlacingRecipe}）与
     * 连续合成都会写它。所以只挡三参版本时，关掉选项 1 仍能靠配方书把三叉戟合出来。</p>
     *
     * <p>在 HEAD 拦下即可：被隐藏的配方连「回退到全量搜索」都不该走。</p>
     */
    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void awesomeTridents$disableChosenTridentRecipe(
            RecipeType<T> type, I input, Level level, RecipeHolder<T> holder,
            CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        if (RecipeGateHandler.shouldHide(holder)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
