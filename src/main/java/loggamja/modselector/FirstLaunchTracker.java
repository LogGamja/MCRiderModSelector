package loggamja.modselector;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 게임을 처음 실행했을 때 타이틀 화면에서 프리셋 선택 창을 한 번만 띄우기 위한 표시 파일 관리.
// 표시 파일이 없으면 아직 안 띄운 것으로 보고, 실제로 창을 띄우기로 결정한 시점에만 파일을 만들어
// 이후 실행(또는 같은 실행 중 타이틀 화면 재진입)에서 다시 뜨지 않게 한다.
public final class FirstLaunchTracker {
    private FirstLaunchTracker() {}

    private static final Path FLAG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("mcridermodselector_intro_shown");

    public static boolean consumeFirstLaunch() {
        if (Files.exists(FLAG_FILE)) return false;
        try {
            Files.createFile(FLAG_FILE);
        } catch (IOException e) {
            // 표시 파일을 못 만들면 다음 판단도 못 하게 되므로, 매번 뜨는 쪽보다 안전하게 이번엔 띄우지 않는다
            return false;
        }
        return true;
    }
}
