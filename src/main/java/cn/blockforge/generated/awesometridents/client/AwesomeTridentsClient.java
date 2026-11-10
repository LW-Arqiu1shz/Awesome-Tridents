package cn.blockforge.generated.awesometridents.client;

import cn.blockforge.generated.awesometridents.GeneratedMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 仅客户端加载的入口：把配置界面挂到 NeoForge 模组列表的"配置"按钮上
 * （相当于 Fabric 版的 Mod Menu 入口）。专用服务器不会加载此类。
 */
@Mod(value = GeneratedMod.MOD_ID, dist = Dist.CLIENT)
public final class AwesomeTridentsClient {
    public AwesomeTridentsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new AwesomeTridentsConfigScreen(parent));
    }
}
