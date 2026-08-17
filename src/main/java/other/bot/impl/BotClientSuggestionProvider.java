/* Decompiler 100ms, total 536ms, lines 133 */
package other.bot.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.network.play.client.CTabCompletePacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.World;
import other.bot.connection.BotClientPlayNetHandler;
import other.bot.player.BotPlayer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class BotClientSuggestionProvider implements ISuggestionProvider {
    private final BotClientPlayNetHandler connection;
    private final Minecraft mc;
    private int currentTransaction = -1;
    private CompletableFuture<Suggestions> future;

    public BotClientSuggestionProvider(BotClientPlayNetHandler p_i49558_1_, Minecraft p_i49558_2_) {
        this.connection = p_i49558_1_;
        this.mc = p_i49558_2_;
    }

    public Collection<String> getPlayerNames() {
        List<String> list = Lists.newArrayList();
        Iterator var2 = this.connection.getPlayerInfoMap().iterator();

        while(var2.hasNext()) {
            NetworkPlayerInfo networkplayerinfo = (NetworkPlayerInfo)var2.next();
            list.add(networkplayerinfo.getGameProfile().getName());
        }

        return list;
    }

    public Collection<String> getTargetedEntity() {
        return (Collection)(this.mc.objectMouseOver != null && this.mc.objectMouseOver.getType() == Type.ENTITY ? Collections.singleton(((EntityRayTraceResult)this.mc.objectMouseOver).getEntity().getCachedUniqueIdString()) : Collections.emptyList());
    }

    public Collection<String> getTeamNames() {
        return this.connection.getWorld().getScoreboard().getTeamNames();
    }

    public Collection<ResourceLocation> getSoundResourceLocations() {
        return this.mc.getSoundHandler().getAvailableSounds();
    }

    public Stream<ResourceLocation> getRecipeResourceLocations() {
        return this.connection.getRecipeManager().getKeys();
    }

    public boolean hasPermissionLevel(int level) {
        BotPlayer clientplayerentity = this.mc.getBotPlayer();
        return clientplayerentity != null ? clientplayerentity.hasPermissionLevel(level) : level == 0;
    }

    public CompletableFuture<Suggestions> getSuggestionsFromServer(CommandContext<ISuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
        if (this.future != null) {
            this.future.cancel(false);
        }

        this.future = new CompletableFuture();
        int i = ++this.currentTransaction;
        this.connection.sendPacket(new CTabCompletePacket(i, context.getInput()));
        return this.future;
    }

    private static String formatDouble(double p_209001_0_) {
        return String.format(Locale.ROOT, "%.2f", p_209001_0_);
    }

    private static String formatInt(int p_209002_0_) {
        return Integer.toString(p_209002_0_);
    }

    public Collection<Coordinates> func_217294_q() {
        RayTraceResult raytraceresult = this.mc.objectMouseOver;
        if (raytraceresult != null && raytraceresult.getType() == Type.BLOCK) {
            BlockPos blockpos = ((BlockRayTraceResult)raytraceresult).getPos();
            return Collections.singleton(new Coordinates(formatInt(blockpos.getX()), formatInt(blockpos.getY()), formatInt(blockpos.getZ())));
        } else {
            return ISuggestionProvider.super.func_217294_q();
        }
    }

    public Collection<Coordinates> func_217293_r() {
        RayTraceResult raytraceresult = this.mc.objectMouseOver;
        if (raytraceresult != null && raytraceresult.getType() == Type.BLOCK) {
            Vector3d vector3d = raytraceresult.getHitVec();
            return Collections.singleton(new Coordinates(formatDouble(vector3d.x), formatDouble(vector3d.y), formatDouble(vector3d.z)));
        } else {
            return ISuggestionProvider.super.func_217293_r();
        }
    }

    public Set<RegistryKey<World>> func_230390_p_() {
        return this.connection.func_239164_m_();
    }

    public DynamicRegistries func_241861_q() {
        return this.connection.func_239165_n_();
    }

    public void handleResponse(int transaction, Suggestions result) {
        if (transaction == this.currentTransaction) {
            this.future.complete(result);
            this.future = null;
            this.currentTransaction = -1;
        }

    }
}