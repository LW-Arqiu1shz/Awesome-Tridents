# 三叉戟强无敌 · Awesome Tridents

> 本分支 / This branch: **26.3Fabric**

[中文](#中文) · [English](#english)

> Author / 作者：**Arqiu-owo** · Code: `LGPL-3.0-only` · Assets: `CC-BY-NC-SA-4.0` — see [LICENSE.md](LICENSE.md)

---

## 中文

原版的三叉戟简直就是设计成功的武器的反面教材。逆天的获取难度，可爱的伤害，局限性极强的附魔……甚至还会因为手抖丢进虚空永远离你而去。
"为什么不让它有用点呢？"你想道。于是你下载了本模组。希望它可以满足你对三叉戟的所有幻想！

### 功能

所有功能都可以在配置里单独开关（默认全部开启）。

- **三叉戟可合成**：加入三叉戟的合成配方。
- **投掷与忠诚**：像溺尸一样投掷三叉戟。
- **引雷更改**：晴天霹雳（非雷暴也能召唤闪电）、闪电改为单体伤害、无视无敌帧、保护掉落物和方块、**雷击烤肉**（被引雷劈中的生物掉落物会被烤熟）。
- **穿刺更改**：穿刺增伤改为对**碰到水**的敌人生效；快捷栏或副手有水桶时视为敌人浸在水中；增伤从每级 +2.5 平衡为 +1.5。
- **激流更改**：快捷栏或副手有水桶时可在旱地激流；激流期间闪避近战伤害；优化冲撞判定和突进手感。

配置文件位于 `config/awesome_tridents.json`。装了 Mod Menu（Fabric）或在 NeoForge 的模组列表里可以直接打开配置界面。
这是一个服务端玩法模组：专用服务器只需要在服务端安装。

### 支持的版本

| 分支 | Minecraft | 加载器 |
|---|---|---|
| [`26.1.2Fabric`](../../tree/26.1.2Fabric) | 26.1.2 | Fabric（Mod Menu 18.x 可选） |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.112 |
| [`26.3Fabric`](../../tree/26.3Fabric) | 26.3 | Fabric（Mod Menu 21.x 可选） |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.101 |

每个版本是仓库里的一个**分支**（`26.3Fabric` 是默认分支，也是主要开发版本），分支根目录就是 Gradle 工程，`CONFIGURATION.md` 里有详细的配置说明和更新记录。

### 自己编译

需要 **JDK 25** 和 **Gradle 9.7+**（仓库里没有附带 Gradle Wrapper）。切换到想要的版本分支，在根目录运行：

```
gradle build
```

jar 会生成在 `build/libs/` 里。

---

## English

The vanilla Trident is a textbook example of how *not* to design a weapon. It's absurdly difficult to obtain, deals pathetic damage,
and is severely limited in terms of enchantments — not to mention the risk of accidentally throwing it into the void, where it's lost forever.
"Why can't it actually be useful?" you wonder. So, you downloaded this mod — hoping it will finally fulfill all your dreams for what a Trident should be!

### Features

Every feature can be toggled separately in the config (all on by default).

- **Craftable Trident**: adds a crafting recipe for the trident.
- **Throwing & Loyalty**: throw tridents like a Drowned does.
- **Channeling changes**: lightning without a thunderstorm, single-target lightning damage, ignores invulnerability frames,
  protects drops and blocks, and **Lightning Roast** — mobs struck by Channeling lightning drop cooked food.
- **Impaling changes**: bonus damage applies to enemies **touching water** instead of aquatic mobs; a water bucket in your hotbar
  or offhand counts the target as wet; bonus rebalanced from +2.5 to +1.5 per level.
- **Riptide changes**: Riptide on dry land with a water bucket in your hotbar or offhand; dodge melee damage while riptiding;
  better collision and dash handling.

Config file: `config/awesome_tridents.json`. A config screen is available through Mod Menu (Fabric) or the NeoForge mod list.
This is a server-side gameplay mod: on a dedicated server it only needs to be installed on the server.

### Supported versions

| Branch | Minecraft | Loader |
|---|---|---|
| [`26.1.2Fabric`](../../tree/26.1.2Fabric) | 26.1.2 | Fabric (Mod Menu 18.x optional) |
| [`26.1.2NeoForge`](../../tree/26.1.2NeoForge) | 26.1.2 | NeoForge 26.1.2.112 |
| [`26.3Fabric`](../../tree/26.3Fabric) | 26.3 | Fabric (Mod Menu 21.x optional) |
| [`26.3NeoForge`](../../tree/26.3NeoForge) | 26.3 | NeoForge 26.3.0.101 |

Each version lives on its own **branch** (`26.3Fabric` is the default branch and the main development version); the branch root is the Gradle project, and `CONFIGURATION.md` has the detailed config notes and changelog (in Chinese).

### Building

Requires **JDK 25** and **Gradle 9.7+** (no Gradle Wrapper is included). Check out the branch for the version you want and run, in the repository root:

```
gradle build
```

The jar is written to `build/libs/`.

---

## License / 许可证

- Code / 代码：[GNU LGPL v3.0](COPYING.LESSER)（with [GPL v3.0](COPYING)）
- Assets (images) / 素材（图片）：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)

Details / 详情：[LICENSE.md](LICENSE.md)
