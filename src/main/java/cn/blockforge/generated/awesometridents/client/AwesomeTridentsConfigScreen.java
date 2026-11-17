package cn.blockforge.generated.awesometridents.client;

import cn.blockforge.generated.awesometridents.TridentConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 原版风格的配置界面：顶部搜索框，下方是一条连续滚动的选项列表（鼠标滚轮 / 右侧滚动条），
 * 左侧显示选项名称，右侧显示开关，子选项缩进显示在所属大类下方。
 *
 * <p>每个选项对应一个固定的列表条目与开关按钮，在 {@link #init()} 时一次建好。
 * 切换开关只更新按钮的文案与可用状态，不会把"正在被点击的那个按钮"从控件树里移除
 * （否则焦点会落在已消失的控件上，回车/空格会再次触发它，键盘输入也进不了搜索框）；
 * 搜索只替换列表里显示哪些条目，条目对象本身复用。</p>
 */
@Environment(EnvType.CLIENT)
public final class AwesomeTridentsConfigScreen extends Screen {
    /** 展示顺序即配置顺序：大类在前，它的子项紧随其后。 */
    private static final List<TridentConfig.Option> ALL_OPTIONS = List.of(
            TridentConfig.Option.TRIDENT_CRAFTING,
            TridentConfig.Option.THROWING_AND_LOYALTY,
            TridentConfig.Option.CHANNELING,
            TridentConfig.Option.CHANNELING_SUNNY,
            TridentConfig.Option.CHANNELING_SINGLE_TARGET,
            TridentConfig.Option.CHANNELING_NO_INVULNERABILITY,
            TridentConfig.Option.CHANNELING_PROTECT_DROPS,
            TridentConfig.Option.CHANNELING_PROTECT_BLOCKS,
            TridentConfig.Option.CHANNELING_COOK_DROPS,
            TridentConfig.Option.IMPALING_LOGIC,
            TridentConfig.Option.IMPALING_BUCKET_TARGET,
            TridentConfig.Option.IMPALING_BALANCE,
            TridentConfig.Option.RIPTIDE,
            TridentConfig.Option.RIPTIDE_BUCKET_SELF,
            TridentConfig.Option.RIPTIDE_MELEE_GUARD,
            TridentConfig.Option.RIPTIDE_COLLISION_FIX,
            TridentConfig.Option.RIPTIDE_MOVEMENT_FIX
    );

    private static final int SEARCH_TOP = 39;
    private static final int SEARCH_HEIGHT = 20;
    private static final int LIST_GAP = 6;
    /** 列表底部到窗口底部的空间，放"完成"按钮。 */
    private static final int FOOTER_HEIGHT = 34;
    private static final int ITEM_HEIGHT = 24;
    private static final int ROW_HEIGHT = 20;
    private static final int TOGGLE_WIDTH = 64;
    /** 子项标签相对大类的缩进量（缩进引导线画在行左侧 + 5 处）。 */
    private static final int CHILD_INDENT = 16;
    /** 行右侧给滚动条留出的空间。 */
    private static final int SCROLLBAR_SPACE = 10;

    private final Screen parent;
    private final Map<TridentConfig.Option, OptionEntry> entries = new EnumMap<>(TridentConfig.Option.class);

    private EditBox searchBox;
    private OptionList optionList;
    private String searchQuery = "";
    private int leftMargin;
    private boolean emptyResult;

    public AwesomeTridentsConfigScreen(Screen parent) {
        super(Component.translatable("config.awesome_tridents.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 窗口缩放时 init 会被重新调用，保留之前的滚动位置。
        double previousScroll = this.optionList != null ? this.optionList.scrollAmount() : 0.0;
        this.leftMargin = Math.max(12, this.width / 16);

        this.searchBox = new EditBox(this.font, this.leftMargin, SEARCH_TOP,
                this.width - this.leftMargin * 2, SEARCH_HEIGHT,
                Component.translatable("config.awesome_tridents.search"));
        this.searchBox.setHint(Component.translatable("config.awesome_tridents.search_hint"));
        this.searchBox.setMaxLength(100);
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(value -> {
            this.searchQuery = value;
            applyFilter();
            this.optionList.setScrollAmount(0.0);
        });
        this.addRenderableWidget(this.searchBox);

        int listTop = SEARCH_TOP + SEARCH_HEIGHT + LIST_GAP;
        int listHeight = Math.max(ITEM_HEIGHT, this.height - FOOTER_HEIGHT - listTop);
        this.optionList = new OptionList(this.minecraft, this.width, listHeight, listTop);
        this.entries.clear();
        for (TridentConfig.Option option : ALL_OPTIONS) {
            this.entries.put(option, new OptionEntry(option));
        }
        this.addRenderableWidget(this.optionList);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());

        applyFilter();
        refreshStates();
        this.optionList.setScrollAmount(previousScroll);
    }

    /** 搜索结果保留父项上下文：父项命中时带出全部子项；只有子项命中时带出父项与命中的子项。 */
    private List<TridentConfig.Option> getFilteredOptions() {
        String query = this.searchQuery.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return ALL_OPTIONS;
        }

        List<TridentConfig.Option> result = new ArrayList<>();
        for (TridentConfig.Option option : ALL_OPTIONS) {
            if (option.parent() != null) {
                continue;
            }

            boolean parentMatches = matches(option, query);
            List<TridentConfig.Option> matchingChildren = ALL_OPTIONS.stream()
                    .filter(child -> child.parent() == option && (parentMatches || matches(child, query)))
                    .toList();

            if (parentMatches || !matchingChildren.isEmpty()) {
                result.add(option);
                result.addAll(matchingChildren);
            }
        }
        return result;
    }

    private static boolean matches(TridentConfig.Option option, String query) {
        String label = Component.translatable(option.labelKey()).getString().toLowerCase(Locale.ROOT);
        String tooltip = Component.translatable(option.tooltipKey()).getString().toLowerCase(Locale.ROOT);
        return label.contains(query) || tooltip.contains(query);
    }

    /** 按搜索词替换列表中显示的条目（条目对象复用，不新建按钮）。 */
    private void applyFilter() {
        List<OptionEntry> shown = new ArrayList<>();
        for (TridentConfig.Option option : getFilteredOptions()) {
            shown.add(this.entries.get(option));
        }
        this.emptyResult = shown.isEmpty();
        this.optionList.replaceEntries(shown);
    }

    /** 刷新所有开关的文案与可用状态（父项切换会影响子项是否可编辑）。 */
    private void refreshStates() {
        for (OptionEntry entry : this.entries.values()) {
            entry.refresh();
        }
    }

    private static boolean isEditable(TridentConfig.Option option) {
        return option.parent() == null || TridentConfig.isEnabled(option.parent());
    }

    private static Component toggleText(TridentConfig.Option option, boolean editable) {
        if (!editable) {
            return Component.translatable("config.awesome_tridents.parent_disabled").withStyle(ChatFormatting.GRAY);
        }
        return TridentConfig.rawValue(option)
                ? Component.translatable("config.awesome_tridents.enabled").withStyle(ChatFormatting.GREEN)
                : Component.translatable("config.awesome_tridents.disabled").withStyle(ChatFormatting.RED);
    }

    /**
     * 背景由 {@link Screen#extractRenderStateWithTooltipAndSubtitles} 在本方法之前抽取完毕
     * （全景 + 模糊 + 羊皮纸底纹，并单独占一个 stratum）。这里绝不能再次调用
     * {@code extractBackground}：其中的 {@code extractBlurredBackground} 会在同一帧第二次
     * 触发 {@code blurBeforeThisStratum}，直接抛出 {@code IllegalStateException:
     * Can only blur once per frame}。所以这里只叠一层半透明黑底再画文字，观感与暗化背景一致。
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x44000000);
        graphics.centeredText(this.font, this.title, this.width / 2, 13, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (this.emptyResult) {
            graphics.centeredText(this.font, Component.translatable("config.awesome_tridents.search_empty"),
                    this.width / 2, this.optionList.getY() + 10, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    /** 连续滚动的选项列表；行宽铺满左右边距，右侧留出滚动条位置。 */
    private final class OptionList extends ContainerObjectSelectionList<OptionEntry> {
        OptionList(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, ITEM_HEIGHT);
        }

        @Override
        public int getRowWidth() {
            return Math.max(TOGGLE_WIDTH + 40,
                    AwesomeTridentsConfigScreen.this.width - AwesomeTridentsConfigScreen.this.leftMargin * 2 - SCROLLBAR_SPACE);
        }

        @Override
        protected int scrollBarX() {
            return this.getRowRight() + 4;
        }
    }

    /** 一行：左侧选项名（子项缩进 + 引导线），右侧开关。 */
    private final class OptionEntry extends ContainerObjectSelectionList.Entry<OptionEntry> {
        private final TridentConfig.Option option;
        private final Button toggle;

        OptionEntry(TridentConfig.Option option) {
            this.option = option;
            this.toggle = Button.builder(Component.empty(), button -> {
                        if (!isEditable(this.option)) {
                            return;
                        }
                        TridentConfig.set(this.option, !TridentConfig.rawValue(this.option));
                        refreshStates();
                    })
                    .bounds(0, 0, TOGGLE_WIDTH, ROW_HEIGHT)
                    .build();
            this.toggle.setTooltip(Tooltip.create(Component.translatable(option.tooltipKey())));
        }

        void refresh() {
            boolean editable = isEditable(this.option);
            this.toggle.active = editable;
            this.toggle.setMessage(toggleText(this.option, editable));
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int left = this.getContentX();
            int top = this.getContentY();
            int middle = this.getContentYMiddle();
            boolean child = this.option.parent() != null;
            boolean parentOff = child && !TridentConfig.isEnabled(this.option.parent());

            if (child) {
                graphics.fill(left + 5, top + 1, left + 6, this.getContentBottom() - 1,
                        parentOff ? 0x55777777 : 0x66999999);
            }
            int color = parentOff ? 0xFF888888 : child ? 0xFFE0E0E0 : 0xFFFFFFFF;
            int labelX = left + (child ? CHILD_INDENT : 0);
            graphics.text(AwesomeTridentsConfigScreen.this.font, Component.translatable(this.option.labelKey()),
                    labelX, middle - 4, color);

            int toggleX = this.getContentRight() - TOGGLE_WIDTH;
            this.toggle.setPosition(toggleX, middle - ROW_HEIGHT / 2);
            this.toggle.extractRenderState(graphics, mouseX, mouseY, partialTick);

            // 标签区域也可悬停显示完整说明，不必把鼠标精准移到右侧开关上。
            if (hovering && mouseX >= left && mouseX < toggleX - 6) {
                graphics.setTooltipForNextFrame(Component.translatable(this.option.tooltipKey()), mouseX, mouseY);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.toggle);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.toggle);
        }
    }
}
