package loggamja.modselector;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.BooleanSupplier;

// ButtonWidget.DEFAULT_NARRATION_SUPPLIER가 protected라서 바깥 클래스에서 익명 서브클래스로는 접근이 안 되므로,
// 서브클래스 자신의 생성자 안에서 접근하도록 이름 있는 클래스로 분리했다
final class PresetIconButton extends ButtonWidget {
    private static final int SELECTED_BORDER_COLOR = 0xFFFFD700;

    private final Identifier texture;
    private final BooleanSupplier selected;

    // selected는 매 프레임 다시 평가한다. 유저가 옵션을 직접 바꾸면 다음 렌더에서 바로 강조가 풀리도록
    PresetIconButton(int x, int y, int width, int height, Text message, Identifier texture, BooleanSupplier selected, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.texture = texture;
        this.selected = selected;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(RenderLayer::getGuiTextured, texture,
                getX(), getY(), 0f, 0f, getWidth(), getHeight(),
                PresetTable.ICON_TEXTURE_WIDTH, PresetTable.ICON_TEXTURE_HEIGHT,
                PresetTable.ICON_TEXTURE_WIDTH, PresetTable.ICON_TEXTURE_HEIGHT,
                0xFFFFFFFF);
        if (isHovered()) {
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
        }
        if (selected.getAsBoolean()) {
            context.drawBorder(getX() - 2, getY() - 2, getWidth() + 4, getHeight() + 4, SELECTED_BORDER_COLOR);
        }
    }
}
