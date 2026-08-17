package sky.core.utils.client;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyStorage {

    private static final Map<Integer, String> KEY_NAMES = new HashMap<>() {{
        put(GLFW.GLFW_KEY_SPACE, "Space");
        put(GLFW.GLFW_KEY_APOSTROPHE, "'");
        put(GLFW.GLFW_KEY_COMMA, ",");
        put(GLFW.GLFW_KEY_MINUS, "-");
        put(GLFW.GLFW_KEY_PERIOD, ".");
        put(GLFW.GLFW_KEY_SLASH, "/");
        put(GLFW.GLFW_KEY_0, "0");
        put(GLFW.GLFW_KEY_1, "1");
        put(GLFW.GLFW_KEY_2, "2");
        put(GLFW.GLFW_KEY_3, "3");
        put(GLFW.GLFW_KEY_4, "4");
        put(GLFW.GLFW_KEY_5, "5");
        put(GLFW.GLFW_KEY_6, "6");
        put(GLFW.GLFW_KEY_7, "7");
        put(GLFW.GLFW_KEY_8, "8");
        put(GLFW.GLFW_KEY_9, "9");
        put(GLFW.GLFW_KEY_SEMICOLON, ";");
        put(GLFW.GLFW_KEY_EQUAL, "=");
        put(GLFW.GLFW_KEY_A, "A");
        put(GLFW.GLFW_KEY_B, "B");
        put(GLFW.GLFW_KEY_C, "C");
        put(GLFW.GLFW_KEY_D, "D");
        put(GLFW.GLFW_KEY_E, "E");
        put(GLFW.GLFW_KEY_F, "F");
        put(GLFW.GLFW_KEY_G, "G");
        put(GLFW.GLFW_KEY_H, "H");
        put(GLFW.GLFW_KEY_I, "I");
        put(GLFW.GLFW_KEY_J, "J");
        put(GLFW.GLFW_KEY_K, "K");
        put(GLFW.GLFW_KEY_L, "L");
        put(GLFW.GLFW_KEY_M, "M");
        put(GLFW.GLFW_KEY_N, "N");
        put(GLFW.GLFW_KEY_O, "O");
        put(GLFW.GLFW_KEY_P, "P");
        put(GLFW.GLFW_KEY_Q, "Q");
        put(GLFW.GLFW_KEY_R, "R");
        put(GLFW.GLFW_KEY_S, "S");
        put(GLFW.GLFW_KEY_T, "T");
        put(GLFW.GLFW_KEY_U, "U");
        put(GLFW.GLFW_KEY_V, "V");
        put(GLFW.GLFW_KEY_W, "W");
        put(GLFW.GLFW_KEY_X, "X");
        put(GLFW.GLFW_KEY_Y, "Y");
        put(GLFW.GLFW_KEY_Z, "Z");
        put(GLFW.GLFW_KEY_LEFT_BRACKET, "[");
        put(GLFW.GLFW_KEY_BACKSLASH, "\\");
        put(GLFW.GLFW_KEY_RIGHT_BRACKET, "]");
        put(GLFW.GLFW_KEY_GRAVE_ACCENT, "`");
        put(GLFW.GLFW_KEY_ESCAPE, "Esc");
        put(GLFW.GLFW_KEY_ENTER, "Enter");
        put(GLFW.GLFW_KEY_TAB, "Tab");
        put(GLFW.GLFW_KEY_BACKSPACE, "Back");
        put(GLFW.GLFW_KEY_INSERT, "Ins");
        put(GLFW.GLFW_KEY_DELETE, "Del");
        put(GLFW.GLFW_KEY_RIGHT, "Right");
        put(GLFW.GLFW_KEY_LEFT, "Left");
        put(GLFW.GLFW_KEY_DOWN, "Down");
        put(GLFW.GLFW_KEY_UP, "Up");
        put(GLFW.GLFW_KEY_PAGE_UP, "PgUp");
        put(GLFW.GLFW_KEY_PAGE_DOWN, "PgDn");
        put(GLFW.GLFW_KEY_HOME, "Home");
        put(GLFW.GLFW_KEY_END, "End");
        put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps");
        put(GLFW.GLFW_KEY_SCROLL_LOCK, "Scroll");
        put(GLFW.GLFW_KEY_NUM_LOCK, "Num");
        put(GLFW.GLFW_KEY_PRINT_SCREEN, "Print");
        put(GLFW.GLFW_KEY_PAUSE, "Pause");
        put(GLFW.GLFW_KEY_F1, "F1");
        put(GLFW.GLFW_KEY_F2, "F2");
        put(GLFW.GLFW_KEY_F3, "F3");
        put(GLFW.GLFW_KEY_F4, "F4");
        put(GLFW.GLFW_KEY_F5, "F5");
        put(GLFW.GLFW_KEY_F6, "F6");
        put(GLFW.GLFW_KEY_F7, "F7");
        put(GLFW.GLFW_KEY_F8, "F8");
        put(GLFW.GLFW_KEY_F9, "F9");
        put(GLFW.GLFW_KEY_F10, "F10");
        put(GLFW.GLFW_KEY_F11, "F11");
        put(GLFW.GLFW_KEY_F12, "F12");
        put(GLFW.GLFW_KEY_KP_0, "Num0");
        put(GLFW.GLFW_KEY_KP_1, "Num1");
        put(GLFW.GLFW_KEY_KP_2, "Num2");
        put(GLFW.GLFW_KEY_KP_3, "Num3");
        put(GLFW.GLFW_KEY_KP_4, "Num4");
        put(GLFW.GLFW_KEY_KP_5, "Num5");
        put(GLFW.GLFW_KEY_KP_6, "Num6");
        put(GLFW.GLFW_KEY_KP_7, "Num7");
        put(GLFW.GLFW_KEY_KP_8, "Num8");
        put(GLFW.GLFW_KEY_KP_9, "Num9");
        put(GLFW.GLFW_KEY_KP_DECIMAL, "Num.");
        put(GLFW.GLFW_KEY_KP_DIVIDE, "Num/");
        put(GLFW.GLFW_KEY_KP_MULTIPLY, "Num*");
        put(GLFW.GLFW_KEY_KP_SUBTRACT, "Num-");
        put(GLFW.GLFW_KEY_KP_ADD, "Num+");
        put(GLFW.GLFW_KEY_KP_ENTER, "NumEnter");
        put(GLFW.GLFW_KEY_LEFT_SHIFT, "LShift");
        put(GLFW.GLFW_KEY_LEFT_CONTROL, "LCtrl");
        put(GLFW.GLFW_KEY_LEFT_ALT, "LAlt");
        put(GLFW.GLFW_KEY_LEFT_SUPER, "LWin");
        put(GLFW.GLFW_KEY_RIGHT_SHIFT, "RShift");
        put(GLFW.GLFW_KEY_RIGHT_CONTROL, "RCtrl");
        put(GLFW.GLFW_KEY_RIGHT_ALT, "RAlt");
        put(GLFW.GLFW_KEY_RIGHT_SUPER, "RWin");
        put(GLFW.GLFW_KEY_MENU, "Menu");
    }};

    public static String getKey(int keyCode) {
        if (keyCode == -1 || keyCode == 0 || keyCode == -100) {
            return "None";
        }

        if (keyCode >= 0 && keyCode <= 8) {
            return switch (keyCode) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "MB" + keyCode;
            };
        }

        String keyName = KEY_NAMES.get(keyCode);
        if (keyName != null) {
            return keyName;
        }

        return "Key" + keyCode;
    }
}