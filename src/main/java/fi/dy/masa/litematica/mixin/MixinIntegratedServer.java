package fi.dy.masa.litematica.mixin;

import java.net.Proxy;
import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.command.Commands;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.storage.SaveFormat.LevelSave;
import fi.dy.masa.litematica.scheduler.TaskScheduler;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer
{
    private MixinIntegratedServer(LevelSave session, Proxy proxy, DataFixer dataFixer, Commands commandManager,
            MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository,
            PlayerProfileCache userCache, IChunkStatusListenerFactory worldGenerationProgressListenerFactory)
    {
        super(session, proxy, dataFixer, commandManager, minecraftSessionService, gameProfileRepository, userCache,
                worldGenerationProgressListenerFactory);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void onPostTick(BooleanSupplier supplier, CallbackInfo ci)
    {
        TaskScheduler.getInstanceServer().runTasks();
    }
}
