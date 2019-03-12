package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import net.minecraft.server.integrated.IntegratedServer;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer
{
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;tick()V", shift = Shift.AFTER))
    private void onPostTick(CallbackInfo ci)
    {
        TaskScheduler.getInstanceServer().runTasks();
    }
}
