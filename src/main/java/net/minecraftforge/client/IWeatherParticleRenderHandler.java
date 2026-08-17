package net.minecraftforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

@FunctionalInterface
public interface IWeatherParticleRenderHandler
{
    void render(int var1, World var2, Minecraft var3, ActiveRenderInfo var4);
}
