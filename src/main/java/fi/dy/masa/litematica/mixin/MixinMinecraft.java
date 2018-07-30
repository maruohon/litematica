package fi.dy.masa.litematica.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

@Mixin(Minecraft.class)
public class MixinMinecraft
{
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void onLoadWorldPre(@Nullable WorldClient worldClientIn, String loadingMessage, CallbackInfo ci)
    {
        // Save the settings before the integrated server gets shut down
        if (Minecraft.getMinecraft().world != null)
        {
            DataManager.save();
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("RETURN"))
    private void onLoadWorldPost(@Nullable WorldClient worldClientIn, String loadingMessage, CallbackInfo ci)
    {
        SchematicWorldHandler.getInstance().recreateSchematicWorld(worldClientIn == null);

        if (worldClientIn != null)
        {
            DataManager.load();
        }
        else
        {
            SchematicHolder.getInstance().clearLoadedSchematics();
        }
    }

    @Inject(method = "runTick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }

    @Inject(method = "runTick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getSystemTime()J"))
    private void onRunTickEnd(CallbackInfo ci)
    {
        DataManager.runTasks();
    }
}
