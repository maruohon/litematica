package fi.dy.masa.litematica.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("HEAD"))
    private void onLoadWorldPre(@Nullable WorldClient worldClientIn, GuiScreen loadingScreen, CallbackInfo ci)
    {
        // Save the settings before the integrated server gets shut down
        if (Minecraft.getInstance().world != null)
        {
            DataManager.save();
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("RETURN"))
    private void onLoadWorldPost(@Nullable WorldClient worldClientIn, GuiScreen loadingScreen, CallbackInfo ci)
    {
        SchematicWorldHandler.recreateSchematicWorld(worldClientIn == null);

        if (worldClientIn != null)
        {
            DataManager.load();
        }
        else
        {
            SchematicHolder.getInstance().clearLoadedSchematics();
            InfoHud.getInstance().reset(); // remove the line providers and clear the data
            DataManager.removeSchematicVerificationTask();
        }
    }

    @Inject(method = "runTick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }

    @Inject(method = "runTick()V", at = @At("RETURN"))
    private void onRunTickEnd(CallbackInfo ci)
    {
        DataManager.runTasks();
    }
}
