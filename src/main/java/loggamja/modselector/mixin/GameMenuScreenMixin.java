package loggamja.modselector.mixin;

import loggamja.modselector.ModSelectorSettingScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void modselector$addCustomButton(CallbackInfo ci) {
        this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("modselector.setting.menu_button"), button -> {
                            assert this.client != null;
                            this.client.setScreen(new ModSelectorSettingScreen(this));
                        })
                        .position(10, 10)
                        .size(100, 20)
                        .build()
        );
    }
}
