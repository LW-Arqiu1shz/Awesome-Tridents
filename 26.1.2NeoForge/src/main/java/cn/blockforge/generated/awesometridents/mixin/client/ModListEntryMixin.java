package cn.blockforge.generated.awesometridents.mixin.client;

import cn.blockforge.generated.awesometridents.client.ModListNames;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表左侧每一行的名称（以及旁白朗读）改用本模组的翻译名。 */
@Mixin(targets = "net.neoforged.neoforge.client.gui.widget.ModListWidget$ModEntry")
public abstract class ModListEntryMixin {
    @WrapOperation(method = "*",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private String awesomeTridents$translatedName(IModInfo info, Operation<String> original) {
        return ModListNames.displayName(info, original.call(info));
    }
}
