/*
 * This file is part of ViaMCP - https://github.com/FlorianMichael/ViaMCP
 * Copyright (C) 2020-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mods.viaversion.viamcp.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import mods.viaversion.vialoadingbase.ViaLoadingBase;
import mods.viaversion.viamcp.ViaMCP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.glfw.GLFW;


public class VersionSelectScreen extends TextFieldWidget {
    private ProtocolVersion lastAppliedVersion;

    public VersionSelectScreen(FontRenderer font, int x, int y, int width, int height, ITextComponent title) {
        super(font, x, y, width, height, title);
        setText("-1");
        setTextColor(TextFormatting.BLUE.getColor());
        this.lastAppliedVersion = ProtocolVersion.getProtocol(ViaMCP.NATIVE_VERSION);
    }

    public void setVersion(int protocol) {
        ProtocolVersion version = ProtocolVersion.getProtocol(protocol);
        this.lastAppliedVersion = version;
        if (!getText().trim().equals("-1")) {
            setText(version.getName());
        }
    }

    private void applyClosestVersionFromText() {
        String raw = getText().trim();
        if (raw.equals("-1")) {
            ProtocolVersion target = ProtocolVersion.getProtocol(754);
            if (lastAppliedVersion != target) {
                ViaLoadingBase.getInstance().reload(target);
                lastAppliedVersion = target;
            }
            setTextColor(TextFormatting.BLUE.getColor());
            return;
        }
        ProtocolVersion resolved = ProtocolVersion.getClosest(raw);
        ProtocolVersion target = resolved != null ? resolved : ProtocolVersion.getProtocol(ViaMCP.NATIVE_VERSION);
        setText(target.getName());
        if (lastAppliedVersion != target) {
            ViaLoadingBase.getInstance().reload(target);
            lastAppliedVersion = target;
        }
        setTextColor(TextFormatting.WHITE.getColor());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyClosestVersionFromText();
            this.setFocused2(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasFocused = this.isFocused();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (wasFocused && !this.isFocused()) {
            applyClosestVersionFromText();
        }
        return handled;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        String raw = getText().trim();
        if (raw.equals("-1")) {
            setTextColor(TextFormatting.BLUE.getColor());
            return;
        }
        ProtocolVersion closest = ProtocolVersion.getClosest(raw);
        if (closest == null) {
            setTextColor(TextFormatting.RED.getColor());
            return;
        }

        boolean exactMatch = closest.getName().equalsIgnoreCase(getText().trim());
        if (exactMatch) {
            if (lastAppliedVersion != closest) {
                ViaLoadingBase.getInstance().reload(closest);
                lastAppliedVersion = closest;
            }
            setTextColor(TextFormatting.WHITE.getColor());
        } else {
            setTextColor(TextFormatting.YELLOW.getColor());
        }
    }
}