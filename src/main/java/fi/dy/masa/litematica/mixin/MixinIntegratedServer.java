package fi.dy.masa.litematica.mixin;

import java.io.File;
import java.net.Proxy;
import java.util.function.BooleanSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer
{
    public MixinIntegratedServer(File anvilFileIn, Proxy serverProxyIn, com.mojang.datafixers.DataFixer dataFixerIn,
            Commands commandManagerIn, YggdrasilAuthenticationService authServiceIn,
            MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn,
            PlayerProfileCache profileCacheIn)
    {
        super(anvilFileIn, serverProxyIn, dataFixerIn, commandManagerIn, authServiceIn, sessionServiceIn, profileRepoIn,
                profileCacheIn);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = Shift.AFTER,
            target = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void onPostTick(BooleanSupplier supplier, CallbackInfo ci)
    {
        TaskScheduler.getInstanceServer().runTasks();
    }
}
