package cn.blockforge.generated.awesometridents.mixin;

import cn.blockforge.generated.awesometridents.RecipeGateHandler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** 过滤工作台的 recipe cache 命中，保证运行中关闭配方后不会继续使用缓存结果。 */
@Mixin(RecipeCache.class)
public abstract class RecipeCacheMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void awesomeTridents$disableCachedTridentRecipe(ServerLevel level, CraftingInput input,
            CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        if (cir.getReturnValue().map(RecipeGateHandler::shouldHide).orElse(false)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
