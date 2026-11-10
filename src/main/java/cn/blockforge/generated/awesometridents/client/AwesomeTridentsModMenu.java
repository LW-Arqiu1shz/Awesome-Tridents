package cn.blockforge.generated.awesometridents.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

/** Mod Menu 的可选配置入口；不安装 Mod Menu 时不会加载此客户端类。 */
@Environment(EnvType.CLIENT)
public final class AwesomeTridentsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AwesomeTridentsConfigScreen::new;
    }
}
