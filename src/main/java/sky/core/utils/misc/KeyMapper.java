package sky.core.utils.misc;

import lombok.Getter;
import net.minecraft.client.util.InputMappings;
import org.lwjgl.glfw.GLFW;

import static sky.core.utils.Wrapper.mc;

@Getter
public enum KeyMapper {
    A("A", GLFW.GLFW_KEY_A),
    B("B", GLFW.GLFW_KEY_B),
    C("C", GLFW.GLFW_KEY_C),
    D("D", GLFW.GLFW_KEY_D),
    E("E", GLFW.GLFW_KEY_E),
    F("F", GLFW.GLFW_KEY_F),
    G("G", GLFW.GLFW_KEY_G),
    H("H", GLFW.GLFW_KEY_H),
    I("I", GLFW.GLFW_KEY_I),
    J("J", GLFW.GLFW_KEY_J),
    K("K", GLFW.GLFW_KEY_K),
    L("L", GLFW.GLFW_KEY_L),
    M("M", GLFW.GLFW_KEY_M),
    N("N", GLFW.GLFW_KEY_N),
    O("O", GLFW.GLFW_KEY_O),
    P("P", GLFW.GLFW_KEY_P),
    Q("Q", GLFW.GLFW_KEY_Q),
    R("R", GLFW.GLFW_KEY_R),
    S("S", GLFW.GLFW_KEY_S),
    T("T", GLFW.GLFW_KEY_T),
    U("U", GLFW.GLFW_KEY_U),
    V("V", GLFW.GLFW_KEY_V),
    W("W", GLFW.GLFW_KEY_W),
    X("X", GLFW.GLFW_KEY_X),
    Y("Y", GLFW.GLFW_KEY_Y),
    Z("Z", GLFW.GLFW_KEY_Z),
    ZERO("0", GLFW.GLFW_KEY_0),
    ONE("1", GLFW.GLFW_KEY_1),
    TWO("2", GLFW.GLFW_KEY_2),
    THREE("3", GLFW.GLFW_KEY_3),
    FOUR("4", GLFW.GLFW_KEY_4),
    FIVE("5", GLFW.GLFW_KEY_5),
    SIX("6", GLFW.GLFW_KEY_6),
    SEVEN("7", GLFW.GLFW_KEY_7),
    EIGHT("8", GLFW.GLFW_KEY_8),
    NINE("9", GLFW.GLFW_KEY_9),
    F1("F1", GLFW.GLFW_KEY_F1),
    F2("F2", GLFW.GLFW_KEY_F2),
    F3("F3", GLFW.GLFW_KEY_F3),
    F4("F4", GLFW.GLFW_KEY_F4),
    F5("F5", GLFW.GLFW_KEY_F5),
    F6("F6", GLFW.GLFW_KEY_F6),
    F7("F7", GLFW.GLFW_KEY_F7),
    F8("F8", GLFW.GLFW_KEY_F8),
    F9("F9", GLFW.GLFW_KEY_F9),
    F10("F10", GLFW.GLFW_KEY_F10),
    F11("F11", GLFW.GLFW_KEY_F11),
    F12("F12", GLFW.GLFW_KEY_F12),
    NUM1("NUM1", GLFW.GLFW_KEY_KP_1),
    NUM2("NUM2", GLFW.GLFW_KEY_KP_2),
    NUM3("NUM3", GLFW.GLFW_KEY_KP_3),
    NUM4("NUM4", GLFW.GLFW_KEY_KP_4),
    NUM5("NUM5", GLFW.GLFW_KEY_KP_5),
    NUM6("NUM6", GLFW.GLFW_KEY_KP_6),
    NUM7("NUM7", GLFW.GLFW_KEY_KP_7),
    NUM8("NUM8", GLFW.GLFW_KEY_KP_8),
    NUM9("NUM9", GLFW.GLFW_KEY_KP_9),
    SPACE("SPCE", GLFW.GLFW_KEY_SPACE),
    ENTER("ENTR", GLFW.GLFW_KEY_ENTER),
    ESC("ESC", GLFW.GLFW_KEY_ESCAPE),
    LSHIFT("LSHF", GLFW.GLFW_KEY_LEFT_SHIFT),
    RSHIFT("RSHF", GLFW.GLFW_KEY_RIGHT_SHIFT),
    LCTRL("LCTR", GLFW.GLFW_KEY_LEFT_CONTROL),
    RCTRL("RCTR", GLFW.GLFW_KEY_RIGHT_CONTROL),
    LALT("LALT", GLFW.GLFW_KEY_LEFT_ALT),
    RALT("RALT", GLFW.GLFW_KEY_RIGHT_ALT),
    LSUPER("LSUP", GLFW.GLFW_KEY_LEFT_SUPER),
    RSUPER("RSUP", GLFW.GLFW_KEY_RIGHT_SUPER),
    UP("UP", GLFW.GLFW_KEY_UP),
    DOWN("DOWN", GLFW.GLFW_KEY_DOWN),
    LEFT("LEFT", GLFW.GLFW_KEY_LEFT),
    RIGHT("RIGHT", GLFW.GLFW_KEY_RIGHT),
    BACK("BACK", GLFW.GLFW_KEY_BACKSPACE),
    HOME("HOME", GLFW.GLFW_KEY_HOME),
    INS("INS", GLFW.GLFW_KEY_INSERT),
    DEL("DEL", GLFW.GLFW_KEY_DELETE),
    END("END", GLFW.GLFW_KEY_END),
    PUP("PUP", GLFW.GLFW_KEY_PAGE_UP),
    TAB("TAB", GLFW.GLFW_KEY_TAB),
    PDOWN("PDWN", GLFW.GLFW_KEY_PAGE_DOWN),
    MENU("MENU", GLFW.GLFW_KEY_MENU),
    CAPS("CAPS", GLFW.GLFW_KEY_CAPS_LOCK),
    NUM("NUM", GLFW.GLFW_KEY_NUM_LOCK),
    SCROL("SCRL", GLFW.GLFW_KEY_SCROLL_LOCK),
    KP_DECIMAL("DCML", GLFW.GLFW_KEY_KP_DECIMAL),
    KP_DIVIDE("DVDE", GLFW.GLFW_KEY_KP_DIVIDE),
    KP_MULTIPLY("MULT", GLFW.GLFW_KEY_KP_MULTIPLY),
    KP_SUBTRACT("SUBT", GLFW.GLFW_KEY_KP_SUBTRACT),
    KP_PLUS("PLUS", GLFW.GLFW_KEY_KP_ADD),
    KP_ENTER("ENTR", GLFW.GLFW_KEY_KP_ENTER),
    KP_EQUAL("EQUL", GLFW.GLFW_KEY_KP_EQUAL),
    APOSTROPHE("'", GLFW.GLFW_KEY_APOSTROPHE),
    SLASH("/", GLFW.GLFW_KEY_SLASH),
    MINUS("-", GLFW.GLFW_KEY_MINUS),
    PLUS("+", GLFW.GLFW_KEY_EQUAL),
    BACKSLASH("SLSH", GLFW.GLFW_KEY_BACKSLASH),
    PERIOD(".", GLFW.GLFW_KEY_PERIOD),
    COMMA("COMA", GLFW.GLFW_KEY_COMMA),
    PAUSE("PAUS", GLFW.GLFW_KEY_PAUSE),
    GRAVE("`", 96),
    MOUSE_MIDDLE("MMC", GLFW.GLFW_MOUSE_BUTTON_3),
    MOUSE_BUTTON_4("M3", GLFW.GLFW_MOUSE_BUTTON_4),
    MOUSE_BUTTON_5("M4", GLFW.GLFW_MOUSE_BUTTON_5);

    private final String name;
    private final int keyCode;

    KeyMapper(String name, int keyCode) {
        this.name = name;
        this.keyCode = keyCode;
    }

    public static String getKey(int keyCode) {
        for (KeyMapper key : values()) {
            if (key.keyCode == keyCode) {
                return key.name;
            }
        }
        return "unk";
    }

    public static boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && InputMappings.isKeyDown(mc.getMainWindow().getHandle(), keyCode);
    }

    public static KeyMapper fromName(String name) {
        try {
            return KeyMapper.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}