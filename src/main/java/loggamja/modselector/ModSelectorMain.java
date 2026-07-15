package loggamja.modselector;

import com.kite.mcdrift.client.api.MCDriftHudAPI;
import loggamja.mcrider.api.MCRiderAPI;
import loggamja.sync.api.TickSyncAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ModSelectorMain implements ClientModInitializer {
    static final String MCRIDER_MOD_ID = "mcrider-official";
    static final String TICKSYNC_MOD_ID = "ticksync";
    static final String MCDRIFTHUD_MOD_ID = "mcdrifthud-lite";

    @Override
    public void onInitializeClient() {
        // 세 모드 다 선택적 의존성이므로, 실제로 설치된 경우에만 그 모드의 API 클래스를 건드린다.
        // isModLoaded()가 false인 분기 안의 API 참조는 실행되지 않으므로
        // 클래스 로딩 자체가 일어나지 않아, 해당 모드가 없어도 NoClassDefFoundError 없이 안전하다.
        if (FabricLoader.getInstance().isModLoaded(MCRIDER_MOD_ID)) {
            MCRiderAPI.setMenuButtonHidden(true);
        }
        if (FabricLoader.getInstance().isModLoaded(TICKSYNC_MOD_ID)) {
            TickSyncAPI.setMenuButtonHidden(true);
        }
        if (FabricLoader.getInstance().isModLoaded(MCDRIFTHUD_MOD_ID)) {
            MCDriftHudAPI.setMenuButtonHidden(true);
        }
    }
}
