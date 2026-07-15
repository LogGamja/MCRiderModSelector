package loggamja.modselector;

import com.kite.mcdrift.client.api.MCDriftHudAPI;
import loggamja.mcrider.api.MCRiderAPI;
import loggamja.sync.api.TickSyncAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

public class ModSelectorSettingScreen extends Screen {
    private static final int ICON_WIDTH = 100;
    private static final int ICON_HEIGHT = 56;
    private static final int ICON_GAP = 14;
    private static final int NAME_GAP = 4; // 아이콘 밑변과 이름 텍스트 사이 여백
    private static final int NAME_TO_DESC_GAP = 10; // 이름과 설명 첫 줄 사이 여백
    private static final int DESC_LINE_HEIGHT = 9; // 설명 텍스트 한 줄의 높이
    private static final int OK_BUTTON_GAP = 20; // 라벨 영역과 확인 버튼 사이 여백

    // 좌상단에 tick sync/MCRider 설정 버튼이 쌓이는 스택 — 각 모드의 원래 ESC 버튼과 같은 위치·모양으로 위에서부터 채운다
    private static final int TOP_BUTTON_X = 10;
    private static final int TOP_BUTTON_START_Y = 10;
    private static final int TOP_BUTTON_WIDTH = 100;
    private static final int TOP_BUTTON_HEIGHT = 20;
    private static final int TOP_BUTTON_SPACING = 25;
    private static final int TOP_STACK_MARGIN = 20; // 좌상단 버튼 스택과 프리셋 블록 사이 최소 여백

    private final Screen parent;

    // init()에서 계산해 render()와 공유한다 — 좌상단 버튼 개수/설명 줄바꿈 결과에 따라 매 프레임 재계산할 필요는 없다
    private int iconY;
    private int labelAreaHeight;
    private final List<List<OrderedText>> wrappedDescriptions = new java.util.ArrayList<>();

    // ESC 메뉴가 아닌 다른 곳에서도 열릴 수 있으므로, 닫을 때 돌아갈 화면을 직접 기억한다
    public ModSelectorSettingScreen(Screen parent) {
        super(Text.translatable("modselector.setting.title"));
        this.parent = parent;
    }

    private int presetsRowStartX() {
        int rowWidth = PresetTable.PRESETS.length * ICON_WIDTH + (PresetTable.PRESETS.length - 1) * ICON_GAP;
        return this.width / 2 - rowWidth / 2;
    }

    private int okButtonY() {
        return iconY + ICON_HEIGHT + labelAreaHeight + OK_BUTTON_GAP;
    }

    @Override
    protected void init() {
        super.init();

        boolean mcriderLoaded = FabricLoader.getInstance().isModLoaded(ModSelectorMain.MCRIDER_MOD_ID);
        boolean ticksyncLoaded = FabricLoader.getInstance().isModLoaded(ModSelectorMain.TICKSYNC_MOD_ID);
        boolean mcdrifthudLoaded = FabricLoader.getInstance().isModLoaded(ModSelectorMain.MCDRIFTHUD_MOD_ID);

        // 설명 텍스트를 아이콘 너비에 맞춰 줄바꿈해 두고, 프리셋 중 가장 줄이 많은 것을 기준으로 라벨 영역 높이를 정한다
        // (옆 프리셋과 겹치지 않도록, 한 줄로 쭉 그리는 대신 아이콘 너비 안에서 줄바꿈한다)
        wrappedDescriptions.clear();
        int maxDescLines = 1;
        for (var preset : PresetTable.PRESETS) {
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.translatable(preset.descKey()), ICON_WIDTH);
            wrappedDescriptions.add(lines);
            maxDescLines = Math.max(maxDescLines, lines.size());
        }
        labelAreaHeight = NAME_GAP + 9 + NAME_TO_DESC_GAP + maxDescLines * DESC_LINE_HEIGHT;

        // 프리셋 블록(아이콘+라벨+확인 버튼)을 화면 세로 중앙에 두되, 이 값보다 위로는 절대 올라가지 않는다
        int blockHeight = ICON_HEIGHT + labelAreaHeight + OK_BUTTON_GAP + 20;
        int centeredIconY = this.height / 2 - blockHeight / 2;

        // 좌상단에 실제로 표시되는 버튼 개수만큼 스택 바닥 위치를 계산 — 버튼이 늘어나도 이 값만 커진다
        int topButtonCount = (ticksyncLoaded ? 1 : 0) + (mcriderLoaded ? 1 : 0) + (mcdrifthudLoaded ? 1 : 0);
        int topStackBottomY = topButtonCount > 0
                ? TOP_BUTTON_START_Y + (topButtonCount - 1) * TOP_BUTTON_SPACING + TOP_BUTTON_HEIGHT
                : TOP_BUTTON_START_Y;
        int requiredIconY = topButtonCount > 0 ? topStackBottomY + TOP_STACK_MARGIN : Integer.MIN_VALUE;

        this.iconY = Math.max(centeredIconY, requiredIconY);

        int startX = presetsRowStartX();
        for (int i = 0; i < PresetTable.PRESETS.length; i++) {
            var preset = PresetTable.PRESETS[i];
            int x = startX + i * (ICON_WIDTH + ICON_GAP);

            PresetIconButton iconButton = new PresetIconButton(x, iconY, ICON_WIDTH, ICON_HEIGHT,
                    Text.translatable(preset.nameKey()), preset.texture(),
                    () -> PresetTable.isActive(preset), button -> PresetTable.apply(preset));
            iconButton.setTooltip(Tooltip.of(Text.translatable(preset.descKey())));
            this.addDrawableChild(iconButton);
        }

        int buttonWidth = 169;
        int buttonStartX = this.width / 2 - buttonWidth / 2;
        this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("modselector.setting.ok"), button -> this.close())
                        .position(buttonStartX, okButtonY())
                        .size(buttonWidth, 20)
                        .build()
        );

        // 원래 tick sync/MCRider/MCDriftHUD-Lite의 ESC 버튼과 동일한 모양으로, 좌상단부터 순서대로 쌓아 재현한다
        int slot = 0;
        if (ticksyncLoaded) {
            this.addDrawableChild(
                    ButtonWidget.builder(Text.translatable("ticksync.setting.menu_button"), button -> TickSyncAPI.openSettings())
                            .position(TOP_BUTTON_X, TOP_BUTTON_START_Y + slot * TOP_BUTTON_SPACING)
                            .size(TOP_BUTTON_WIDTH, TOP_BUTTON_HEIGHT)
                            .build()
            );
            slot++;
        }
        if (mcriderLoaded) {
            this.addDrawableChild(
                    ButtonWidget.builder(Text.translatable("mcrider.setting.menu_button"), button -> MCRiderAPI.openSettings())
                            .position(TOP_BUTTON_X, TOP_BUTTON_START_Y + slot * TOP_BUTTON_SPACING)
                            .size(TOP_BUTTON_WIDTH, TOP_BUTTON_HEIGHT)
                            .build()
            );
            slot++;
        }
        if (mcdrifthudLoaded) {
            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("MCDriftHUD-lite"), button -> MCDriftHudAPI.openSettings())
                            .position(TOP_BUTTON_X, TOP_BUTTON_START_Y + slot * TOP_BUTTON_SPACING)
                            .size(TOP_BUTTON_WIDTH, TOP_BUTTON_HEIGHT)
                            .build()
            );
            slot++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("modselector.setting.title"), this.width / 2, 15, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        int startX = presetsRowStartX();
        for (int i = 0; i < PresetTable.PRESETS.length; i++) {
            var preset = PresetTable.PRESETS[i];
            int centerX = startX + i * (ICON_WIDTH + ICON_GAP) + ICON_WIDTH / 2;

            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(preset.nameKey()), centerX, iconY + ICON_HEIGHT + NAME_GAP, 0xFFFFFF);

            int descY = iconY + ICON_HEIGHT + NAME_GAP + 9 + NAME_TO_DESC_GAP;
            for (OrderedText line : wrappedDescriptions.get(i)) {
                context.drawCenteredTextWithShadow(this.textRenderer, line, centerX, descY, 0xAAAAAA);
                descY += DESC_LINE_HEIGHT;
            }
        }
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
}
