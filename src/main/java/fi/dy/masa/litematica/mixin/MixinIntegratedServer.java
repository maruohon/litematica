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
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.UserCache;
import net.minecraft.world.level.storage.LevelStorage.Session;
import fi.dy.masa.litematica.scheduler.TaskScheduler;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer
{
    private MixinIntegratedServer(Session session, Proxy proxy, DataFixer dataFixer, CommandManager commandManager,
            MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository,
            UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory)
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
