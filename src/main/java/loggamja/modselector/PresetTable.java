package loggamja.modselector;

import com.kite.mcdrift.client.api.MCDriftHudAPI;
import loggamja.mcrider.api.MCRiderAPI;
import loggamja.sync.api.TickSyncAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

// 4개 프리셋의 번역 키 + 아이콘 텍스처 + 적용 로직 정의 테이블
// 프리셋 하나가 tick sync/MCRider/MCDriftHUD-Lite 세 모드의 옵션을 동시에 건드리는 "통합 프리셋"이므로,
// 각 모드별 테이블이 아니라 이 테이블 하나로 관리한다.
// 모든 프리셋은 tick sync의 틱 동기화/정확도 자동만 명시하고, 나머지 tick sync 옵션과 MCRider/MCDriftHUD-Lite 옵션은 각자 정의한 값을 따른다.
final class PresetTable {
    private PresetTable() {}

    static final int ICON_TEXTURE_WIDTH = 320;
    static final int ICON_TEXTURE_HEIGHT = 180;

    // id에 목표 값(토글 인덱스)을 함께 묶어 둔다 — "적용"과 "현재 상태와 일치하는지 비교" 양쪽이 같은 값을 참조하므로 어긋날 일이 없다
    record OptionValue(String id, int value) {}

    // MCDriftHUD-Lite는 토글 테이블 형태가 아니라 타입이 섞인 필드라 별도 레코드로 다룬다.
    // designStyleId/textDisplayMode는 일부러 HudDesignStyle/HudConfigManager.TextDisplayMode(mcdrifthudlite의 실제 타입)가 아니라
    // 문자열로 저장한다 — PRESETS 배열은 mcdrifthudlite 설치 여부와 상관없이 무조건 만들어지는 static 데이터라,
    // 여기 mcdrifthudlite의 실제 타입을 직접 참조하면(예: HudDesignStyle.VANILLA) 그 클래스가 무조건 로드돼버려서
    // mcdrifthudlite가 없을 때 화면을 여는 순간 NoClassDefFoundError가 난다. 실제 enum으로의 변환은
    // isModLoaded(MCDRIFTHUD_MOD_ID) 가드 안(apply/isActive)에서만 이루어진다.
    // designStyleId는 HudDesignStyle.getId()와 같은 값("vanilla"/"mcrider"/"original"),
    // textDisplayMode는 HudConfigManager.TextDisplayMode의 enum 이름("SHOW_ALL"/"HIDE_SELF"/"HIDE_ALL")과 같은 문자열이다.
    // windowedHeight가 0이면 해상도는 건드리지 않는다는 뜻이다 (mcdrifthudlite 자체의 "미설정" 관례와 동일)
    // fallbackMinMonitorHeight가 0이면 fallback 없이 windowed 해상도를 그대로 적용한다.
    // 0보다 크면, 주 모니터 세로 해상도가 이 값보다 작을 때 fallback 해상도를 대신 적용한다.
    record HudOptions(String designStyleId, boolean hideInvisibleEntities,
                       String textDisplayMode, int gaugeSmoother,
                       float windowedRatioX, float windowedRatioY, int windowedHeight,
                       int fallbackMinMonitorHeight, float fallbackRatioX, float fallbackRatioY, int fallbackHeight) {}

    record Preset(String id, String nameKey, String descKey, Identifier texture,
                  boolean requiresMCRiderHUD, OptionValue[] mcriderOptions, HudOptions hudOptions) {}

    // tick sync는 모든 프리셋이 같은 값(틱 동기화 켜짐, 정확도 자동)을 명시한다 — 기준 스레드/디버그 화면은 프리셋이 건드리지 않으므로
    // resetInstalledMods()로 기본값이 되긴 하지만, 이 값이 바뀌어도 강조 판정에는 영향을 주지 않는다
    private static final OptionValue[] TICKSYNC_OPTIONS = {
            new OptionValue("tick_sync", 1),
            new OptionValue("auto_margin", 1),
    };

    // 프리셋을 적용하기 전 설치된 모드의 옵션을 모두 기본값으로 되돌린다
    // 프리셋마다 자신이 필요로 하는 옵션을 처음부터 전부 명시하기 위한 깨끗한 시작점을 만든다
    private static void resetInstalledMods() {
        if (FabricLoader.getInstance().isModLoaded(ModSelectorMain.TICKSYNC_MOD_ID)) {
            TickSyncAPI.resetToDefaults();
        }
        if (FabricLoader.getInstance().isModLoaded(ModSelectorMain.MCRIDER_MOD_ID)) {
            MCRiderAPI.resetToDefaults();
        }
        if (FabricLoader.getInstance().isModLoaded(ModSelectorMain.MCDRIFTHUD_MOD_ID)) {
            MCDriftHudAPI.resetToDefaults();
        }
    }

    // 프리셋이 정의한 MCRider/MCDriftHUD-Lite 옵션 값을 실제로 적용한다
    static void apply(Preset preset) {
        resetInstalledMods();
        FabricLoader loader = FabricLoader.getInstance();

        if (loader.isModLoaded(ModSelectorMain.MCRIDER_MOD_ID)) {
            for (OptionValue option : preset.mcriderOptions()) {
                MCRiderAPI.setToggleOption(option.id(), option.value());
            }
        }

        if (loader.isModLoaded(ModSelectorMain.TICKSYNC_MOD_ID)) {
            for (OptionValue option : TICKSYNC_OPTIONS) {
                TickSyncAPI.setToggleOption(option.id(), option.value());
            }
        }

        if (loader.isModLoaded(ModSelectorMain.MCDRIFTHUD_MOD_ID)) {
            HudOptions hud = preset.hudOptions();
            MCDriftHudAPI.setDesignStyle(hud.designStyleId());
            MCDriftHudAPI.setHideInvisibleEntities(hud.hideInvisibleEntities());
            MCDriftHudAPI.setTextDisplayMode(hud.textDisplayMode());
            MCDriftHudAPI.setGaugeSmoother(hud.gaugeSmoother());
            if (hud.windowedHeight() > 0) {
                if (hud.fallbackMinMonitorHeight() > 0) {
                    MCDriftHudAPI.setWindowedResolution(hud.windowedRatioX(), hud.windowedRatioY(), hud.windowedHeight(),
                            hud.fallbackMinMonitorHeight(), hud.fallbackRatioX(), hud.fallbackRatioY(), hud.fallbackHeight());
                } else {
                    MCDriftHudAPI.setWindowedResolution(hud.windowedRatioX(), hud.windowedRatioY(), hud.windowedHeight());
                }
            }
        }
    }

    // 현재 라이브 옵션 값이 이 프리셋이 요구하는 값과 정확히 일치하는지 확인한다 (유저가 뭔가 건드렸으면 더 이상 일치하지 않는다)
    // 설치되지 않은 모드는 비교 대상에서 제외한다, 설치된 모드끼리만 전부 일치하면 강조한다.
    // 관련 모드가 하나도 없으면 강조할 근거가 없으므로 false.
    static boolean isActive(Preset preset) {
        FabricLoader loader = FabricLoader.getInstance();
        boolean anyLoaded = false;

        if (loader.isModLoaded(ModSelectorMain.MCRIDER_MOD_ID)) {
            anyLoaded = true;
            for (OptionValue option : preset.mcriderOptions()) {
                int current = MCRiderAPI.getToggleOption(option.id()).orElse(Integer.MIN_VALUE);
                if (current != option.value()) return false;
            }
        }

        if (loader.isModLoaded(ModSelectorMain.TICKSYNC_MOD_ID)) {
            anyLoaded = true;
            for (OptionValue option : TICKSYNC_OPTIONS) {
                int current = TickSyncAPI.getToggleOption(option.id()).orElse(Integer.MIN_VALUE);
                if (current != option.value()) return false;
            }
        }

        if (loader.isModLoaded(ModSelectorMain.MCDRIFTHUD_MOD_ID)) {
            anyLoaded = true;
            HudOptions hud = preset.hudOptions();
            if (!MCDriftHudAPI.getDesignStyleId().equals(hud.designStyleId())) return false;
            if (MCDriftHudAPI.isHideInvisibleEntities() != hud.hideInvisibleEntities()) return false;
            if (!MCDriftHudAPI.getTextDisplayModeName().equals(hud.textDisplayMode())) return false;
            if (MCDriftHudAPI.getGaugeSmoother() != hud.gaugeSmoother()) return false;
            // 해상도를 명시한 프리셋(windowedHeight>0)만, 그리고 창 모드에서만 비교한다
            // (windowedHeight==0은 "해상도 미관리"라는 뜻이고, 전체화면에선 setWindowedResolution이 no-op이라 저장값이 갱신되지 않는다)
            if (hud.windowedHeight() > 0
                    && !MinecraftClient.getInstance().getWindow().isFullscreen()
                    && !hudResolutionMatches(hud)) return false;
        }

        return anyLoaded;
    }

    // 저장된 해상도 오버라이드가 이 프리셋의 창 해상도와 일치하는지 본다.
    // 모니터가 작아 fallback 해상도가 대신 적용됐을 수 있으므로 목표/폴백 둘 중 하나와 맞으면 일치로 인정한다.
    private static boolean hudResolutionMatches(HudOptions hud) {
        float sx = MCDriftHudAPI.getResolutionRatioX();
        float sy = MCDriftHudAPI.getResolutionRatioY();
        int   sh = MCDriftHudAPI.getResolutionHeight();
        if (resEquals(sx, sy, sh, hud.windowedRatioX(), hud.windowedRatioY(), hud.windowedHeight())) return true;
        if (hud.windowedHeight() > 0 && hud.fallbackMinMonitorHeight() > 0
                && resEquals(sx, sy, sh, hud.fallbackRatioX(), hud.fallbackRatioY(), hud.fallbackHeight())) return true;
        return false;
    }

    private static boolean resEquals(float ax, float ay, int ah, float bx, float by, int bh) {
        return ah == bh && Math.abs(ax - bx) < 1e-4f && Math.abs(ay - by) < 1e-4f;
    }

    // 화면에 실제로 그릴 프리셋. HUD 모드가 없으면 requiresMCRiderHUD 프리셋은 제외한다
    static java.util.List<Preset> visiblePresets() {
        boolean hudLoaded = FabricLoader.getInstance().isModLoaded(ModSelectorMain.MCDRIFTHUD_MOD_ID);
        java.util.List<Preset> result = new java.util.ArrayList<>();
        for (Preset preset : PRESETS) {
            if (preset.requiresMCRiderHUD() && !hudLoaded) continue;
            result.add(preset);
        }
        return result;
    }

    static final Preset[] PRESETS = {
            // 1. 클래식 — 조작 가속(극한), 패킷 가속만 켜고 나머지는 모두 끔. HUD는 바닐라, 투명 엔티티 최적화 꺼짐, 닉네임 모두 표시
            new Preset(
                    "preset_1", "modselector.preset.1.name", "modselector.preset.1.desc",
                    Identifier.of("mcridermodselector", "textures/gui/preset_1.png"),
                    false,
                    new OptionValue[]{
                            new OptionValue("steer_boost", 2),
                            new OptionValue("packet_boost", 1),
                            new OptionValue("enemy_radar", 0),
                            new OptionValue("draft_gauge", 0),
                            new OptionValue("auto_third_person", 0),
                            new OptionValue("noclip_camera", 0),
                            new OptionValue("camera_mode", 0),
                            new OptionValue("suspension_effect", 0),
                            new OptionValue("bike_suspension", 0),
                            new OptionValue("track_minimap", 0),
                    },
                    new HudOptions("vanilla", false, "SHOW_ALL", 1, 0, 0, 0, 0, 0, 0, 0)
            ),
            // 2. 세미 클래식 — 클래식(조작 가속, 패킷 가속)에 카메라 통과/드래프트 게이지/레이더/미니맵(좌측 중간)을 추가로 켜고,
            //    자동 3인칭을 켜고 카메라 모드를 보통[3]으로 바꾼다 (이후 프리셋도 자동 3인칭은 계속 켜짐)
            //    HUD는 여전히 바닐라, 투명 엔티티 최적화 켜짐, 닉네임 자신만 숨김으로 전환 (이후 프리셋도 계속 유지)
            new Preset(
                    "preset_2", "modselector.preset.2.name", "modselector.preset.2.desc",
                    Identifier.of("mcridermodselector", "textures/gui/preset_2.png"),
                    false,
                    new OptionValue[]{
                            new OptionValue("steer_boost", 2),
                            new OptionValue("packet_boost", 1),
                            new OptionValue("enemy_radar", 1),
                            new OptionValue("draft_gauge", 1),
                            new OptionValue("auto_third_person", 1),
                            new OptionValue("noclip_camera", 1),
                            new OptionValue("camera_mode", 2),
                            new OptionValue("suspension_effect", 0),
                            new OptionValue("bike_suspension", 0),
                            new OptionValue("track_minimap", 3),
                    },
                    new HudOptions("vanilla", true, "HIDE_SELF", 1, 0, 0, 0, 0, 0, 0, 0)
            ),
            // 3. 마크라이더 — 세미 클래식의 모든 옵션을 포함하고, 서스펜션(카트바디)/바이크 서스펜션(현실적)을 켜고 미니맵을 우측 중간으로 옮긴다
            //    HUD는 마크라이더 디자인으로 전환하고, 게이지 보간을 빠르게로 바꾼다 (이후 프리셋도 계속 유지)
            //    창 모드일 때 해상도를 1344x1008로 맞추되, 모니터 세로가 1080보다 작으면 1024x768로 대신 맞춘다
            new Preset(
                    "preset_3", "modselector.preset.3.name", "modselector.preset.3.desc",
                    Identifier.of("mcridermodselector", "textures/gui/preset_3.png"),
                    true,
                    new OptionValue[]{
                            new OptionValue("steer_boost", 2),
                            new OptionValue("packet_boost", 1),
                            new OptionValue("enemy_radar", 1),
                            new OptionValue("draft_gauge", 1),
                            new OptionValue("auto_third_person", 1),
                            new OptionValue("noclip_camera", 1),
                            new OptionValue("camera_mode", 2),
                            new OptionValue("suspension_effect", 1),
                            new OptionValue("bike_suspension", 2),
                            new OptionValue("track_minimap", 4),
                    },
                    new HudOptions("mcrider", true, "HIDE_SELF", 2,
                            4, 3, 1008, 1080, 4, 3, 768)
            ),
            // 4. 오리지널 — 마크라이더의 모든 옵션을 포함하고, 서스펜션(카트와 카메라)/바이크 서스펜션(기본)만 바꾼다
            //    HUD는 원작 디자인으로 전환하고, 창 모드일 때만 해상도를 1024x768로 맞춘다
            new Preset(
                    "preset_4", "modselector.preset.4.name", "modselector.preset.4.desc",
                    Identifier.of("mcridermodselector", "textures/gui/preset_4.png"),
                    true,
                    new OptionValue[]{
                            new OptionValue("steer_boost", 2),
                            new OptionValue("packet_boost", 1),
                            new OptionValue("enemy_radar", 1),
                            new OptionValue("draft_gauge", 1),
                            new OptionValue("auto_third_person", 1),
                            new OptionValue("noclip_camera", 1),
                            new OptionValue("camera_mode", 3),
                            new OptionValue("suspension_effect", 2),
                            new OptionValue("bike_suspension", 0),
                            new OptionValue("track_minimap", 4),
                    },
                    new HudOptions("original", true, "HIDE_SELF", 2, 4, 3, 768, 0, 0, 0, 0)
            ),
    };
}
