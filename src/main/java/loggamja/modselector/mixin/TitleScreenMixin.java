package loggamja.modselector.mixin;

import loggamja.modselector.FirstLaunchTracker;
import loggamja.modselector.ModSelectorMain;
import loggamja.modselector.ModSelectorSettingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    // 타이틀 화면은 창 크기 조절 등으로 init()이 여러 번 불릴 수 있으므로,
    // 최초 1회 여부는 FirstLaunchTracker의 표시 파일로 판단한다 (여기서 매번 새로 판단하지 않음)
    @Inject(method = "init", at = @At("RETURN"))
    private void modselector$showIntroPreset(CallbackInfo ci) {
        if (!ModSelectorMain.shouldShowMenuButton()) return;
        if (!FirstLaunchTracker.consumeFirstLaunch()) return;
        ModSelectorMain.applyDefaultPresetForFirstLaunch();
        assert this.client != null;
        this.client.setScreen(new ModSelectorSettingScreen(this));
    }
}
