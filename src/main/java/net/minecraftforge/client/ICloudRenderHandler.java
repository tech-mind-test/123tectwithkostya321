package net.minecraftforge.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

@FunctionalInterface
public interface ICloudRenderHandler
{
    void render(int var1, float var2, MatrixStack var3, World var4, Minecraft var5, double var6, double var8, double var10);
}
