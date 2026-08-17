package sky.core.utils.render.font;

public class Fonts {
    public static final String FONT_DIR = "/assets/minecraft/SkyCore/fonts/";
    private static volatile boolean initialized = false;

    public static volatile StyledFont[] sf_medium = new StyledFont[25];
    public static volatile StyledFont[] sf_regular = new StyledFont[34];
    public static volatile StyledFont[] sf_semibold = new StyledFont[37];
    public static volatile StyledFont[] sf_bold = new StyledFont[25];
    public static volatile StyledFont[] icons = new StyledFont[41];
    public static volatile StyledFont[] lox = new StyledFont[20];
    public static volatile StyledFont[] waypoint_icons = new StyledFont[37];
    public static volatile StyledFont[] divine_icons = new StyledFont[20];
    public static volatile StyledFont[] divine = new StyledFont[20];
    public static volatile StyledFont[] skycore = new StyledFont[81];
    public static volatile StyledFont[] sfregular = new StyledFont[25];
    public static volatile StyledFont[] mototanya = new StyledFont[17];
    public static volatile StyledFont[] krisa = new StyledFont[17];

    public static synchronized void init() {
        if (initialized) return;
        fill(sf_medium, "sf_medium.ttf", 0);
        fill(sf_bold, "sf_bold.ttf", 0);
        fill(sf_regular, "sf_regular.ttf", -1.01f);
        fill(sf_semibold, "sf_semibold.ttf", 0);
        fill(icons, "icons.ttf", 0);
        fill(lox, "lox.ttf", 0);
        fill(divine_icons, "divine_icons.ttf", 0);
        fill(divine, "divine.ttf", 0);
        fill(waypoint_icons, "waypoint_icons.ttf", 0);
        fill(sfregular, "sfregular.ttf", 0);
        fill(mototanya, "mototanya.ttf", 0);
        fill(krisa, "icon.ttf", 0);
        initSkycoreSizes();
        initialized = true;
    }


    private static void initSkycoreSizes() {
        for (int size : new int[]{10, 12, 13, 14, 80}) {
            skycore[size] = new StyledFont("skycore.ttf", size, 0, true);
        }
    }

    private static void fill(StyledFont[] target, String fontFileName, float spacing) {
        for (int i = 1; i < target.length; i++) {
            target[i] = new StyledFont(fontFileName, i, spacing, true);
        }
    }
}
