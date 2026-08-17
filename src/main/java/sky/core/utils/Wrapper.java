package sky.core.utils;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;

public interface Wrapper {
    Minecraft mc = Minecraft.getInstance();
    MainWindow mw = mc.getMainWindow();
    BufferBuilder BUFFER = Tessellator.getInstance().getBuffer();
    Tessellator TESSELLATOR = Tessellator.getInstance();
}