package net.minecraft.client.audio;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundEvents;

public class UnderwaterAmbientSoundHandler implements IAmbientSoundHandler
{
    private final AbstractClientPlayerEntity player;
    private final SoundHandler soundHandler;
    private int delay = 0;

    public UnderwaterAmbientSoundHandler(AbstractClientPlayerEntity playerIn, SoundHandler soundHandlerIn)
    {
        this.player = playerIn;
        this.soundHandler = soundHandlerIn;
    }

    public void tick()
    {
        --this.delay;

        if (this.delay <= 0 && this.player.canSwim())
        {
            float f = this.player.world.rand.nextFloat();

            if (f < 1.0E-4F)
            {
                this.delay = 0;
                this.soundHandler.play(new UnderwaterAmbientSounds.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE));
            }
            else if (f < 0.001F)
            {
                this.delay = 0;
                this.soundHandler.play(new UnderwaterAmbientSounds.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE));
            }
            else if (f < 0.01F)
            {
                this.delay = 0;
                this.soundHandler.play(new UnderwaterAmbientSounds.SubSound(this.player, SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS));
            }
        }
    }
}
