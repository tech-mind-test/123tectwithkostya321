package sky.core.utils.render.font;

import sky.core.utils.render.ColorUtil;

import java.util.HashMap;
import java.util.Map;

public class ReplaceSymbols {
    private static final Map<Integer, String> REPLACEMENTS = new HashMap<>();
    private static final Map<Integer, Integer> RANK_COLORS = new HashMap<>();

    private static final int[] RANKS = {(int) 'ꔀ', (int) 'ꔄ', (int) 'ꔈ', (int) 'ꔒ', (int) 'ꔖ', (int) 'ꔠ', (int) 'ꔤ', (int) 'ꔨ', (int) 'ꕠ', (int) 'ꔲ', (int) 'ꔶ', (int) 'ꕄ', (int) 'ꕖ', (int) 'ꕈ', (int) 'ꕀ', (int) 'ꕒ', (int) 'ꔉ', (int) 'ꔓ', (int) 'ꔗ', (int) 'ꔡ', (int) 'ꔥ', (int) 'ꔩ', (int) 'ꔳ', (int) 'ꔷ', (int) 'ꔁ', (int) 'ꔅ'};

    static {


        // Symbols / removals
        REPLACEMENTS.put((int) '⚡', "");
        REPLACEMENTS.put((int) '★', "");


        // ReallyWorld ranks
        REPLACEMENTS.put((int) 'ꔀ', "PLAYER");
        REPLACEMENTS.put((int) 'ꔄ', "HERO");
        REPLACEMENTS.put((int) 'ꔈ', "TITAN");
        REPLACEMENTS.put((int) 'ꔒ', "AVENGER");
        REPLACEMENTS.put((int) 'ꔖ', "OVERLORD");
        REPLACEMENTS.put((int) 'ꔠ', "MAGISTER");
        REPLACEMENTS.put((int) 'ꔤ', "IMPERATOR");
        REPLACEMENTS.put((int) 'ꔨ', "DRAGON");
        REPLACEMENTS.put((int) 'ꕠ', "D.HELPER");
        REPLACEMENTS.put((int) 'ꔲ', "BULL");
        REPLACEMENTS.put((int) 'ꔶ', "TIGER");
        REPLACEMENTS.put((int) 'ꕄ', "DRACULA");
        REPLACEMENTS.put((int) 'ꕖ', "BUNNY");
        REPLACEMENTS.put((int) 'ꕈ', "COBRA");
        REPLACEMENTS.put((int) 'ꕀ', "HYDRA");
        REPLACEMENTS.put((int) 'ꕒ', "RABBIT");
        REPLACEMENTS.put((int) 'ꔉ', "HELPER");
        REPLACEMENTS.put((int) 'ꔓ', "ML.MODER");
        REPLACEMENTS.put((int) 'ꔗ', "MODER");
        REPLACEMENTS.put((int) 'ꔡ', "MODER+");
        REPLACEMENTS.put((int) 'ꔥ', "ST.MODER");
        REPLACEMENTS.put((int) 'ꔩ', "GL.MODER");
        REPLACEMENTS.put((int) 'ꔳ', "ML.ADMIN");
        REPLACEMENTS.put((int) 'ꔷ', "ADMIN");
        REPLACEMENTS.put((int) 'ꔁ', "MEDIA");
        REPLACEMENTS.put((int) 'ꔅ', "YT");
        REPLACEMENTS.put((int) 'ꕁ', "GOD");


        REPLACEMENTS.put((int) 'ᴀ', "A");
        REPLACEMENTS.put((int) 'ʙ', "B");
        REPLACEMENTS.put((int) 'ᴄ', "C");
        REPLACEMENTS.put((int) 'ᴅ', "D");
        REPLACEMENTS.put((int) 'ᴇ', "E");
        REPLACEMENTS.put((int) 'ꜰ', "F");
        REPLACEMENTS.put((int) 'ɢ', "G");
        REPLACEMENTS.put((int) 'ʜ', "H");
        REPLACEMENTS.put((int) 'ɪ', "I");
        REPLACEMENTS.put((int) 'ᴊ', "J");
        REPLACEMENTS.put((int) 'ᴋ', "K");
        REPLACEMENTS.put((int) 'ʟ', "L");
        REPLACEMENTS.put((int) 'ᴍ', "M");
        REPLACEMENTS.put((int) 'ɴ', "N");
        REPLACEMENTS.put((int) 'ᴏ', "O");
        REPLACEMENTS.put((int) 'ᴘ', "P");
        REPLACEMENTS.put((int) 'ǫ', "Q");
        REPLACEMENTS.put((int) 'ʀ', "R");
        REPLACEMENTS.put((int) 'ᴛ', "T");
        REPLACEMENTS.put((int) 'ᴜ', "U");
        REPLACEMENTS.put((int) 'ꜱ', "S");
        REPLACEMENTS.put((int) 'ᴠ', "V");
        REPLACEMENTS.put((int) 'ᴡ', "W");
        REPLACEMENTS.put((int) 'ᵡ', "X");
        REPLACEMENTS.put((int) 'ʏ', "Y");
        REPLACEMENTS.put((int) 'ᴢ', "Z");


        RANK_COLORS.put((int) 'ꔀ', ColorUtil.getColor(120,120,120));
        RANK_COLORS.put((int) 'ꔄ', ColorUtil.getColor(100,113,251));
        RANK_COLORS.put((int) 'ꔈ', ColorUtil.getColor(214,200,42));
        RANK_COLORS.put((int) 'ꔒ', ColorUtil.getColor(101,189,56));
        RANK_COLORS.put((int) 'ꔖ', ColorUtil.getColor(85, 255, 255));

                RANK_COLORS.put((int) 'ꔠ', ColorUtil.getColor(202,130,60));
        RANK_COLORS.put((int) 'ꔤ', ColorUtil.getColor(202,60,60));
        RANK_COLORS.put((int) 'ꔨ', ColorUtil.getColor(245, 51, 238));
        RANK_COLORS.put((int) 'ꕠ', ColorUtil.getColor(214,200,42));
        RANK_COLORS.put((int) 'ꔲ', ColorUtil.getColor(121,81,202));
        RANK_COLORS.put((int) 'ꔶ', ColorUtil.getColor(202,130,60));
        RANK_COLORS.put((int) 'ꕄ', ColorUtil.getColor(202,60,60));
        RANK_COLORS.put((int) 'ꕖ', ColorUtil.getColor(68,65,66));
        RANK_COLORS.put((int) 'ꕈ', ColorUtil.getColor(127,214,86));
        RANK_COLORS.put((int) 'ꕀ', ColorUtil.getColor(92,120,7));
        RANK_COLORS.put((int) 'ꕒ', ColorUtil.getColor(120,120,120));
        RANK_COLORS.put((int) 'ꔉ', ColorUtil.getColor(214,200,42));
        RANK_COLORS.put((int) 'ꔓ', ColorUtil.getColor(100,113,251));
        RANK_COLORS.put((int) 'ꔗ', ColorUtil.getColor(100,113,251));
        RANK_COLORS.put((int) 'ꔡ', ColorUtil.getColor(121,81,202));
        RANK_COLORS.put((int) 'ꔥ', ColorUtil.getColor(100,113,251));
        RANK_COLORS.put((int) 'ꔩ', ColorUtil.getColor(121,81,202));
        RANK_COLORS.put((int) 'ꔳ', ColorUtil.getColor(64,151,214));

        RANK_COLORS.put((int) 'ꔷ', ColorUtil.getColor(202,60,60));
        RANK_COLORS.put((int) 'ꔁ', ColorUtil.getColor(121,81,202));
        RANK_COLORS.put((int) 'ꔅ', ColorUtil.getColor(255, 255, 255));
        RANK_COLORS.put((int) 'ꕁ', ColorUtil.getColor(214,200,42));
    }

    public static String replaceCodePoint(int codePoint) {
        return REPLACEMENTS.get(codePoint);
    }

    public static int getGradientColorForReplacement(int codePoint, int charIndex, int totalChars, float alpha, int currentColor) {
        boolean isRank = false;
        for (int rank : RANKS) {
            if (rank == codePoint) {
                isRank = true;
                break;
            }
        }

        if (isRank) {
            Integer baseColor = RANK_COLORS.get(codePoint);
            int endColor = ColorUtil.darken(baseColor, 0.8f);
            float ratio = (float) charIndex / (totalChars - 1);

            int interpolatedColor = ColorUtil.interpolate(endColor, baseColor, ratio);
            return ColorUtil.applyOpacity(interpolatedColor, alpha);
        } else {
            return ColorUtil.applyOpacity(currentColor, alpha);
        }
    }
}