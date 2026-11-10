package cn.blockforge.generated.awesometridents;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeneratedMod implements ModInitializer {
    public static final String MOD_ID = "awesome_tridents";

    public static final Logger LOGGER = LoggerFactory.getLogger("Awesome Tridents");

    @Override
    public void onInitialize() {
        TridentConfig.load();
        // 全部玩法改动由 mixin/ 下的注入点与 data/ 下的数据包完成：
        //  - TridentItemMixin           投掷不消耗本体、2 秒冷却（忠诚减冷却）、水桶激流、激流蓝光、
        //                               仅服务端安装时把激流速度/位移补给控制端
        //  - ThrownTridentMixin         弹射物按箭矢规则存续、触碰即消失、单体引雷、潮湿穿刺
        //  - PlayerAttackMixin          近战穿刺潮湿增伤
        //  - LivingEntityDeathLootMixin 引雷击杀掉熟肉（烟熏炉配方映射，仅服务端）
        //  - data/minecraft/enchantment/{impaling,channeling}.json 清空数据驱动效果，语义由代码与配置开关接管
        //  - data/awesome_tridents/recipe/ 海晶三叉戟合成配方
        LOGGER.info("[Awesome Tridents] 三叉戟服务端优化已加载（配方/弹射/引雷/穿刺/激流/熟肉）");
    }
}
