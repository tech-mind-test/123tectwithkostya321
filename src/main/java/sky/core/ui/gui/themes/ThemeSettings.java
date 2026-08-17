package sky.core.ui.gui.themes;

import lombok.Getter;

@Getter
public enum ThemeSettings {
    MAIN("Основной"),
    MODULE_VISUAL("Визуальные модули"),
    TEXT("Текст"),
    TEXT_INACTIVE("Неактивный текст"),
    HEADER("Текст заголовков"),
    PREMIUM("Премиум текст"),
    SLIDER("Слайдер"),
    SLIDER_CIRCLE("Круг слайдера"),
    SLIDER_WINDOW("Окно слайдера"),
    INDICATOR("Индикатор"),
    INDICATOR_INACTIVE("Неактивный индикатор"),
    BUTTON("Кнопка"),
    BUTTON_INACTIVE("Неактивная кнопка"),
    OUTLINE("Обводка"),
    SEPARATOR("Разделитель"),
    SCROLL_BAR("Скролл бар"),
    FIELD("Поле"),
    FIELD_INACTIVE("Неактивное поле"),
    LOGO("Лого"),
    LOGO_TEXT("Текст в лого"),
    TOOLTIP_BG("Фон в подсказках"),
    TOOLTIP("Текст в подсказках"),
    WINDOW_BG("Фон окон"),
    WINDOW_TEXT("Текст окон");

    private final String displayName;

    ThemeSettings(String displayName) {
        this.displayName = displayName;
    }
}
