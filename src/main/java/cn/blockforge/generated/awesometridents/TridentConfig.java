package cn.blockforge.generated.awesometridents;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;

/**
 * Awesome Tridents 的服务端玩法配置。
 * 所有选项默认开启；子选项的原始值会保留，但只有父选项开启时才生效。
 */
public final class TridentConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("awesome_tridents.json");
    private static final Map<Option, Boolean> VALUES = new EnumMap<>(Option.class);

    static {
        resetToDefaults();
    }

    private TridentConfig() {
    }

    public enum Option {
        TRIDENT_CRAFTING("tridentCrafting", "config.awesome_tridents.option.trident_crafting", "config.awesome_tridents.tooltip.trident_crafting", null),
        THROWING_AND_LOYALTY("throwingAndLoyalty", "config.awesome_tridents.option.throwing_loyalty", "config.awesome_tridents.tooltip.throwing_loyalty", null),
        CHANNELING("channeling", "config.awesome_tridents.option.channeling", "config.awesome_tridents.tooltip.channeling", null),
        CHANNELING_SUNNY("channelingSunny", "config.awesome_tridents.option.channeling_sunny", "config.awesome_tridents.tooltip.channeling_sunny", CHANNELING),
        CHANNELING_SINGLE_TARGET("channelingSingleTarget", "config.awesome_tridents.option.channeling_single_target", "config.awesome_tridents.tooltip.channeling_single_target", CHANNELING),
        CHANNELING_NO_INVULNERABILITY("channelingNoInvulnerability", "config.awesome_tridents.option.channeling_no_invulnerability", "config.awesome_tridents.tooltip.channeling_no_invulnerability", CHANNELING),
        CHANNELING_PROTECT_DROPS("channelingProtectDrops", "config.awesome_tridents.option.channeling_protect_drops", "config.awesome_tridents.tooltip.channeling_protect_drops", CHANNELING),
        CHANNELING_PROTECT_BLOCKS("channelingProtectBlocks", "config.awesome_tridents.option.channeling_protect_blocks", "config.awesome_tridents.tooltip.channeling_protect_blocks", CHANNELING),
        CHANNELING_COOK_DROPS("channelingCookDrops", "config.awesome_tridents.option.channeling_cook_drops", "config.awesome_tridents.tooltip.channeling_cook_drops", CHANNELING),
        IMPALING_LOGIC("impalingLogic", "config.awesome_tridents.option.impaling_logic", "config.awesome_tridents.tooltip.impaling_logic", null),
        IMPALING_BUCKET_TARGET("impalingBucketTarget", "config.awesome_tridents.option.impaling_bucket_target", "config.awesome_tridents.tooltip.impaling_bucket_target", IMPALING_LOGIC),
        IMPALING_BALANCE("impalingBalance", "config.awesome_tridents.option.impaling_balance", "config.awesome_tridents.tooltip.impaling_balance", IMPALING_LOGIC),
        RIPTIDE("riptide", "config.awesome_tridents.option.riptide", "config.awesome_tridents.tooltip.riptide", null),
        RIPTIDE_BUCKET_SELF("riptideBucketSelf", "config.awesome_tridents.option.riptide_bucket_self", "config.awesome_tridents.tooltip.riptide_bucket_self", RIPTIDE),
        RIPTIDE_MELEE_GUARD("riptideMeleeGuard", "config.awesome_tridents.option.riptide_melee_guard", "config.awesome_tridents.tooltip.riptide_melee_guard", RIPTIDE),
        RIPTIDE_COLLISION_FIX("riptideCollisionFix", "config.awesome_tridents.option.riptide_collision_fix", "config.awesome_tridents.tooltip.riptide_collision_fix", RIPTIDE),
        RIPTIDE_MOVEMENT_FIX("riptideMovementFix", "config.awesome_tridents.option.riptide_movement_fix", "config.awesome_tridents.tooltip.riptide_movement_fix", RIPTIDE);

        private final String key;
        private final String labelKey;
        private final String tooltipKey;
        private final Option parent;

        Option(String key, String labelKey, String tooltipKey, Option parent) {
            this.key = key;
            this.labelKey = labelKey;
            this.tooltipKey = tooltipKey;
            this.parent = parent;
        }

        public String labelKey() {
            return labelKey;
        }

        public String tooltipKey() {
            return tooltipKey;
        }

        public Option parent() {
            return parent;
        }
    }

    /** 在模组初始化时加载配置文件；不存在时自动生成全为 true 的默认配置。 */
    public static synchronized void load() {
        resetToDefaults();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    for (Option option : Option.values()) {
                        JsonElement value = root.get(option.key);
                        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                            VALUES.put(option, value.getAsBoolean());
                        }
                    }
                }
            } catch (Exception exception) {
                GeneratedMod.LOGGER.error("无法读取 Awesome Tridents 配置文件，已使用默认配置：{}", CONFIG_PATH, exception);
                resetToDefaults();
            }
        }
        save();
    }

    private static void resetToDefaults() {
        VALUES.clear();
        for (Option option : Option.values()) {
            VALUES.put(option, true);
        }
    }

    /** 返回选项在文件中的原始值；子选项即使被父选项关闭也会保留自己的值。 */
    public static synchronized boolean rawValue(Option option) {
        return VALUES.getOrDefault(option, true);
    }

    /** 返回选项的实际生效值；父选项关闭时，其所有子选项均视为关闭。 */
    public static synchronized boolean isEnabled(Option option) {
        if (!VALUES.getOrDefault(option, true)) {
            return false;
        }
        return option.parent == null || isEnabled(option.parent);
    }

    /** 立即更新并持久化一个选项。 */
    public static synchronized void set(Option option, boolean enabled) {
        VALUES.put(option, enabled);
        save();
    }

    private static void save() {
        JsonObject root = new JsonObject();
        for (Option option : Option.values()) {
            root.addProperty(option.key, VALUES.getOrDefault(option, true));
        }

        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            GeneratedMod.LOGGER.error("无法保存 Awesome Tridents 配置文件：{}", CONFIG_PATH, exception);
        }
    }
}
