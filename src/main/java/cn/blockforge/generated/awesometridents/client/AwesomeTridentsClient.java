package cn.blockforge.generated.awesometridents.client;

import cn.blockforge.generated.awesometridents.GeneratedMod;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;

/**
 * 仅客户端加载的入口（专用服务器不会加载此类）：
 * <ul>
 *   <li>把配置界面挂到 NeoForge 模组列表的"配置"按钮上（相当于 Fabric 版的 Mod Menu 入口）；</li>
 *   <li>模组列表里的模组名称按当前语言显示（{@value #NAME_KEY}），
 *       描述沿用 NeoForge 默认的 {@code neoforge.screen.mods.info.description.<modid>} 翻译键。</li>
 * </ul>
 */
@Mod(value = GeneratedMod.MOD_ID, dist = Dist.CLIENT)
public final class AwesomeTridentsClient {
    private static final String NAME_KEY = "neoforge.screen.mods.info.displayname." + GeneratedMod.MOD_ID;

    public AwesomeTridentsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new AwesomeTridentsConfigScreen(parent));
        container.registerExtensionPoint(ModDisplayInfo.class, new DefaultModDisplayInfo(container) {
            @Override
            public Component displayName() {
                return Component.translatableWithFallback(NAME_KEY, container.getModInfo().getDisplayName());
            }
        });
    }
}
