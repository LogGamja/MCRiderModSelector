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
        if (!shouldShowMenuButton()) return;

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

    public static boolean shouldShowMenuButton() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded(MCRIDER_MOD_ID) || loader.isModLoaded(MCDRIFTHUD_MOD_ID);
    }
}
