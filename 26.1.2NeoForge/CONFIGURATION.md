# Awesome Tridents 配置说明

- 配置文件：`config/awesome_tridents.json`。首次启动会自动生成，全部选项默认为 `true`。
- 在 NeoForge 自带的模组列表中选中本模组，点击「配置」按钮即可打开配置界面；页面顶部提供搜索框，左侧显示选项名称，右侧显示开关；子选项会缩进显示在所属大类下方。全部选项在一个连续列表中，用鼠标滚轮或右侧滚动条浏览。切换后立即保存并即时应用，无需重启游戏或重启世界。
- 子选项必须先开启对应大类才可编辑；大类关闭时，子选项保留原值但不生效。
- 本模组为服务端玩法模组。单人游戏中，配置界面直接修改当前实例使用的配置；独立专用服务器读取服务器自身的 `config/awesome_tridents.json`，远程客户端的配置界面设置不会跨网络擅自覆盖服务器配置。
- 配置界面只在客户端加载（`client/AwesomeTridentsClient`，`@Mod(dist = Dist.CLIENT)`）；专用服务器不加载任何界面类。
- 选项 1（三叉戟可合成）关闭后：合成表结果槽与配方书一键摆放都会被拦下，无法取出三叉戟。配方书里的配方图标由客户端本地解锁状态决定，可能仍显示，但点它不会出货。

此源码包对应 `1.2.0+26.1.2NeoForge`（由 26.3Fabric 的 1.2.0-r18 同步移植）。

## 1.2.0 配方拦截类加载修复

- 配方判断已移至普通业务包中的 `RecipeGateHandler`；`RecipeManagerMixin` 与 `RecipeCacheMixin` 只调用该普通工具类，不再直接加载 Mixin 专用包里的类。
- `TridentRules` 不再直接引用 Mixin 专用包中的 `LivingEntityAccessor`，改为依赖普通接口 `LivingEntityLastHurtAccess`；Mixin accessor 仅负责把接口注入到 `LivingEntity`。
- 配方开关在每次配方管理器/工作台缓存查询时动态读取，切换后立即生效，不需要重启游戏或世界。

## 1.2.0 配置界面修复

- 行开关改成一批固定控件（最多 8 行），翻页、搜索、切换只改它们的位置与文案，不再从控件树里增删。原实现在按钮自己的点击回调里把"正在被按下的那个按钮"移除，而原版 `ContainerEventHandler#mouseClicked` 回调返回后仍会把焦点交给它：焦点因此落在一个已消失的控件上，之后回车/空格会再次触发这个开关，键盘输入也进不了搜索框。
- 行区高度按窗口实际可用空间计算（搜索框之下、页码与底部按钮之上），空间不足时先减少每页行数（最少 1 行），不会再画到底部按钮和页码上面。
- 搜索无结果时显示"没有匹配的选项 / No matching options"。
- 配置界面依旧只由外层 `Screen` 抽取一次背景，`extractRenderState` 内不得再调 `extractBackground`，否则会触发 `IllegalStateException: Can only blur once per frame`。

## r18 更新

- 崩溃修复：读取附魔改用安全查询，数据包移除/禁用原版忠诚、穿刺、引雷附魔时不再在每次攻击时抛异常导致服务器崩溃。
- 兼容性修复：全部 `@Redirect` 改为 MixinExtras `@WrapOperation`，与其他改同一调用点的模组共存时不再在启动时崩溃。
- 雷击烤熟的掉落物按最大堆叠数拆分，不再生成超出堆叠上限的物品堆。
- 新增「引雷更改」子选项「雷击烤肉」（`channelingCookDrops`，默认开启），控制引雷击杀时掉落物是否被烤熟。
- `channeling.json`（按 26.1.2 的数据格式，条件字段为 `condition`）恢复原版"引雷三叉戟在雷暴中命中避雷针召唤闪电"效果；本模组召出的闪电补回原版的三叉戟雷鸣音效。
- 配置界面：取消分页，改为连续滚动列表；大类名称不再使用粗体。
- 语言文件：新增 NeoForge 模组列表的描述翻译键，中英文分别显示对应语言（名称也跟随语言，见下）。

## r18 补充：模组列表语言统一

- 模组列表名称跟随语言：NeoForge 26.1.2 的模组列表本身不翻译 displayName，这里用客户端 Mixin（`mixin/client/ModList*Mixin`，只在客户端加载）在列表行、信息面板、搜索与按名称排序处，把本模组名称换成翻译键 `fml.menu.mods.info.displayname.awesome_tridents` 的译文（中文「三叉戟强无敌」，英文「Awesome Tridents」）；只影响本模组，没有译文时保持原名。
- 描述继续使用 `fml.menu.mods.info.description.awesome_tridents`。
- 搜索：中文界面下，模组列表搜索同时匹配翻译名和英文原名，搜「Awesome Tridents」或「三叉戟强无敌」都能找到本模组（只影响搜索匹配，不改变显示的名称）。

## 产物命名

- `gradle.properties` 里 `mod_version` 末尾的 `-rN` 是构建平台每轮追加的轮次标记，只出现在 jar 文件名末尾（`awesome_tridents-1.2.0+26.1.2NeoForge-rN.jar`）；`neoforge.mods.toml` 中的 `version` 仍是剥掉标记的纯净版本 `1.2.0+26.1.2NeoForge`。

