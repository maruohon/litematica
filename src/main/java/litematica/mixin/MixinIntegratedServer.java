package litematica.mixin;

import java.io.File;
import java.net.Proxy;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;

import litematica.scheduler.TaskScheduler;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer
{
    private MixinIntegratedServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn,
            YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn,
            GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn)
    {
        super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;tick()V", shift = Shift.AFTER))
    private void onPostTick(CallbackInfo ci)
    {
        TaskScheduler.getInstanceServer().runTasks();
    }
}
