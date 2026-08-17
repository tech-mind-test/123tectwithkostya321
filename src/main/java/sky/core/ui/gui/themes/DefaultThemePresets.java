package sky.core.ui.gui.themes;

import com.adl.nativeprotect.Native;
import sky.core.utils.render.ColorUtil;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultThemePresets {
    private static final LinkedHashMap<String, int[]> PRESETS = new LinkedHashMap<>();

    @Native
    private static void put(String val, int[] key) {
        PRESETS.put(val, key);
    }

    static {
        put(
                "Имя: Фиолетовая",
                new int[]{
                        ColorUtil.hex("#130C19E1"),
                        ColorUtil.hex("#AD73FF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#FFFFFFAF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#BDA966FF"),
                        ColorUtil.hex("#764C99FF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#764C99FF"),
                        ColorUtil.hex("#BDFF00FF"),
                        ColorUtil.hex("#FF0000FF"),
                        ColorUtil.hex("#C584FFFF"),
                        ColorUtil.hex("#3B264C78"),
                        ColorUtil.hex("#67459932"),
                        ColorUtil.hex("#FFFFFF23"),
                        ColorUtil.hex("#B37EFD1E"),
                        ColorUtil.hex("#56397F33"),
                        ColorUtil.hex("#56397F03"),
                        ColorUtil.hex("#C685FFFF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#130C19E1"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#130C19A7"),
                        ColorUtil.hex("#FFFFFFFF")
                }
        );

        put(
                "Имя: Прозрачная",
                new int[]{
                        ColorUtil.hex("#030304B5"),
                        ColorUtil.hex("#5F71BF"),
                        ColorUtil.hex("#D0DAF0FF"),
                        ColorUtil.hex("#A4ABABA8"),
                        ColorUtil.hex("#D3E4FFCE"),
                        ColorUtil.hex("#D7E4FF8A"),
                        ColorUtil.hex("#7C83FF3B"),
                        ColorUtil.hex("#52549DFF"),
                        ColorUtil.hex("#52549D1C"),
                        ColorUtil.hex("#7577D7FF"),
                        ColorUtil.hex("#52549DFF"),
                        ColorUtil.hex("#8A8DFFFF"),
                        ColorUtil.hex("#262B4C43"),
                        ColorUtil.hex("#52549D4C"),
                        ColorUtil.hex("#52549D54"),
                        ColorUtil.hex("#52549DA6"),
                        ColorUtil.hex("#52549D32"),
                        ColorUtil.hex("#52549D0D"),
                        ColorUtil.hex("#858CFFFF"),
                        ColorUtil.hex("#D7E4FFFF"),
                        ColorUtil.hex("#010312AF"),
                        ColorUtil.hex("#D7E4FFFF"),
                        ColorUtil.hex("#03030460"),
                        ColorUtil.hex("#FFFFFFFF")
                }
        );
        put(
                "Имя: Aqua",
                new int[]{
                        ColorUtil.hex("#A5ADFFFF"), // основная
                        ColorUtil.hex("#99A2FFFF"), // визуал
                        ColorUtil.hex("#FFFFFFFF"), // text
                        ColorUtil.hex("#FFFFFFAF"), // неактив
                        ColorUtil.hex("#FFFFFFFF"), // zagolook
                        ColorUtil.hex("#BDA966FF"), // premka
                        ColorUtil.hex("#A5ADFFFF"), // slider
                        ColorUtil.hex("#FFFFFFFF"), // krug
                        ColorUtil.hex("#AFB6FFFF"), // okno
                        ColorUtil.hex("#ACB3FFFF"), // indikator
                        ColorUtil.hex("#FF0000FF"), // neaktiv
                        ColorUtil.hex("#A5ADFFFF"), // knopka
                        ColorUtil.hex("#5256785E"),// neaktiv knopka
                        ColorUtil.hex("#A5ADFF36"), // obvodka
                        ColorUtil.hex("#393C548A"), // razdelitel
                        ColorUtil.hex("#8187CEFF"), // scroll bar
                        ColorUtil.hex("#A5ADFF7D"), // pole
                        ColorUtil.hex("#4647471A"), // neaktiv pole
                        ColorUtil.hex("#A5ADFFFF"), // logo
                        ColorUtil.hex("#FFFFFFFF"), // text logo
                        ColorUtil.hex("#A5ADFFFF"), // podskazka text
                        ColorUtil.hex("#FFFFFFFF"), // pidor text
                        ColorUtil.hex("#1A1B28BC"), // fon okon
                        ColorUtil.hex("#FFFFFFFF"), // texttt
                }
        );
                put(
                "Имя: Синяя",
                new int[]{
                        ColorUtil.hex("#0B0C1CCB"),
                        ColorUtil.hex("#7387FF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#CBCBCBE0"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#667ABDFF"),
                        ColorUtil.hex("#4C5099FF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#4C5099FF"),
                        ColorUtil.hex("#619AFFFF"),
                        ColorUtil.hex("#FF0000FF"),
                        ColorUtil.hex("#9395FFFF"),
                        ColorUtil.hex("#262B4C78"),
                        ColorUtil.hex("#4546991D"),
                        ColorUtil.hex("#FFFFFF23"),
                        ColorUtil.hex("#7E90FD1E"),
                        ColorUtil.hex("#2A2A5E59"),
                        ColorUtil.hex("#2A2A5E22"),
                        ColorUtil.hex("#858CFFFF"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#0D0C19E1"),
                        ColorUtil.hex("#FFFFFFFF"),
                        ColorUtil.hex("#101224A7"),
                        ColorUtil.hex("#FFFFFFFF")
                }

        );
    }

    private DefaultThemePresets() {
    }

    public static Map<String, int[]> getDefaultPresets() {
        return Collections.unmodifiableMap(PRESETS);
    }
}


