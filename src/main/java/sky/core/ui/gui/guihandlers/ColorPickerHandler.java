package sky.core.ui.gui.guihandlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.elements.Element;
import sky.core.modules.api.elements.ModuleElement;
import sky.core.modules.api.elements.impl.ColorElement;
import sky.core.modules.api.elements.impl.ColorPickerElement;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.gui.Panel;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.utils.math.MathUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ColorPickerHandler {
    private final List<Panel> panels;

    public ColorPickerHandler(List<Panel> panels) {
        this.panels = panels;
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY) {
        for (Panel panel : panels) {
            if (isPanelHidden(panel)) continue;

            if (panel instanceof ThemeEditor themeEditor) {
                for (ColorElement ce : themeEditor.getColorPickers()) {
                    ColorPickerElement picker = ce.getColorPicker();
                    if (picker.isColorPickMode() || picker.getOpenAnimation().isAlive()) {
                        picker.setX(ce.getX() + 110);
                        picker.setY(ce.getY());
                        picker.render(matrixStack, mouseX, mouseY);
                    }
                }
            } else {
                for (ModuleElement module : panel.getModules()) {
                    for (Element elem : module.getElements()) {
                        if (elem instanceof ColorElement ce) {
                            ColorPickerElement picker = ce.getColorPicker();
                            if (picker.isColorPickMode() || picker.getOpenAnimation().isAlive()) {
                                picker.setX(ce.getX() + ce.getWidth() - 7);
                                picker.setY(ce.getY() - 3);
                                picker.render(matrixStack, mouseX, mouseY);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean handleMouseClicked(float mouseX, float mouseY, int button) {
        boolean isColorPickerOpen = false;
        ColorPickerElement activeEyedropperPicker = null;

        for (Panel panel : panels) {
            if (isPanelHidden(panel)) continue;

            if (panel instanceof ThemeEditor themeEditor) {
                for (ColorElement ce : themeEditor.getColorPickers()) {
                    ColorPickerElement picker = ce.getColorPicker();
                    if (picker.isColorPickMode()) {
                        isColorPickerOpen = true;
                        if (picker.isEyeDropperActive()) {
                            activeEyedropperPicker = picker;
                        }
                    }
                }
            } else {
                for (ModuleElement module : panel.getModules()) {
                    for (Element elem : module.getElements()) {
                        if (elem instanceof ColorElement ce) {
                            ColorPickerElement picker = ce.getColorPicker();
                            if (picker.isColorPickMode()) {
                                isColorPickerOpen = true;
                                if (picker.isEyeDropperActive()) {
                                    activeEyedropperPicker = picker;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeEyedropperPicker != null) {
            activeEyedropperPicker.mouseClicked(mouseX, mouseY);
            return true;
        }

        for (Panel panel : panels) {
            if (isPanelHidden(panel)) continue;

            if (panel instanceof ThemeEditor themeEditor) {
                for (ColorElement ce : themeEditor.getColorPickers()) {
                    ColorPickerElement picker = ce.getColorPicker();
                    if (picker.isColorPickMode()) {
                        float pickerWidth = 93 + (picker.getSetting().alphaBar ? 0f : -7f);
                        float pickerHeight = 100f + (picker.getSetting().alphaBar ? 10f : -0f);
                        if (MathUtil.isHovered(mouseX, mouseY, picker.getX() + 7, picker.getY(), pickerWidth, pickerHeight)) {
                            picker.mouseClicked(mouseX, mouseY);
                            return true;
                        }
                    }
                    float effectiveY = ce.getY();
                    float scissorTop = themeEditor.getPickersScissorTop();
                    float scissorBottom = themeEditor.getPickersScissorBottom();
                    if (ce.isVisible() && button == 0
                            && (themeEditor.getCurrentPreset() == null || themeEditor.getCurrentPreset().isCustom())
                            && MathUtil.isHovered(mouseX, mouseY, ce.getX() + 2.5f, ce.getY() - 1.5f, ce.getWidth() - 5.5f, ce.getHeight())
                            && effectiveY < scissorBottom && effectiveY + ce.getHeight() > scissorTop) {
                        for (ColorElement otherCe : themeEditor.getColorPickers()) {
                            if (otherCe != ce && otherCe.getColorPicker().isColorPickMode()) {
                                otherCe.getColorPicker().onClose();
                            }
                        }
                        picker.setColorPickMode(!picker.isColorPickMode());
                        return true;
                    }
                }
            } else {
                for (ModuleElement module : panel.getModules()) {
                    for (Element elem : module.getElements()) {
                        if (elem instanceof ColorElement ce) {
                            ColorPickerElement picker = ce.getColorPicker();
                            if (picker.isColorPickMode()) {
                                float pickerWidth = 93 + (picker.getSetting().alphaBar ? 0f : -7f);
                                float pickerHeight = 100f + (picker.getSetting().alphaBar ? 10f : -0f);
                                if (MathUtil.isHovered(mouseX, mouseY, picker.getX() + 7, picker.getY() - 2, pickerWidth, pickerHeight + 1f)) {
                                    picker.mouseClicked(mouseX, mouseY);
                                    return true;
                                }
                            }
                            float scissorTop = panel.getY() + 28;
                            float scissorBottom = panel.getY() + 28 + panel.getHeight() - 35;
                            float effectiveY = ce.getY();
                            if (ce.isVisible() && button == 0 && module.isOpen()
                                    && MathUtil.isHovered(mouseX, mouseY, ce.getX(), ce.getY() - 1, ce.getWidth(), ce.getHeight() - 7)
                                    && effectiveY >= scissorTop && effectiveY + ce.getHeight() <= scissorBottom) {
                                for (Panel otherPanel : panels) {
                                    if (isPanelHidden(otherPanel)) continue;
                                    for (ModuleElement otherModule : otherPanel.getModules()) {
                                        for (Element otherElem : otherModule.getElements()) {
                                            if (otherElem instanceof ColorElement otherCe && otherCe != ce && otherCe.getColorPicker().isColorPickMode()) {
                                                otherCe.getColorPicker().onClose();
                                            }
                                        }
                                    }
                                }
                                picker.setColorPickMode(!picker.isColorPickMode());
                                return true;
                            }
                        }
                    }
                }
            }
        }

        if (isColorPickerOpen && (button == GLFW.GLFW_MOUSE_BUTTON_2 || button == GLFW.GLFW_MOUSE_BUTTON_3)) {
            closeAllPickers();
            return true;
        }

        if (isColorPickerOpen && button == 0) {
            closeAllPickers();
            return true;
        }

        return false;
    }

    public void closeAllPickers() {
        for (Panel panel : panels) {
            if (isPanelHidden(panel)) continue;

            if (panel instanceof ThemeEditor themeEditor) {
                for (ColorElement ce : themeEditor.getColorPickers()) {
                    ColorPickerElement picker = ce.getColorPicker();
                    if (picker.isColorPickMode()) {
                        picker.onClose();
                    }
                }
            } else {
                for (ModuleElement module : panel.getModules()) {
                    for (Element elem : module.getElements()) {
                        if (elem instanceof ColorElement ce) {
                            ColorPickerElement picker = ce.getColorPicker();
                            if (picker.isColorPickMode()) {
                                picker.onClose();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean hasOpenColorPicker(Panel panel) {
        if (panel instanceof ThemeEditor themeEditor) {
            for (ColorElement ce : themeEditor.getColorPickers()) {
                if (ce.getColorPicker().isColorPickMode()) {
                    return true;
                }
            }
        } else {
            for (ModuleElement module : panel.getModules()) {
                for (Element elem : module.getElements()) {
                    if (elem instanceof ColorElement ce && ce.getColorPicker().isColorPickMode()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPanelHidden(Panel panel) {
        return (panel instanceof ThemeEditor && !Interface.themeeditor.get());
    }
}