package fi.dy.masa.litematica.mixin;

import javax.annotation.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

@Mixin(Minecraft.class)
public class MixinMinecraft
{
    @Inject(method = "runTickKeyboard", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V"))
    private void onKeyboardInput(CallbackInfo ci)
    {
        if (InputEventHandler.getInstance().onKeyInput())
        {
            ci.cancel();
        }
    }

    @Inject(method = "runTickMouse", cancellable = true,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
    private void onMouseInput(CallbackInfo ci)
    {
        if (InputEventHandler.getInstance().onMouseInput())
        {
            ci.cancel();
        }
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;)V", at = @At("HEAD"))
    private void onLoadWorld(@Nullable WorldClient worldClientIn, CallbackInfo ci)
    {
        // Save the settings before the integrated server gets shut down
        if (worldClientIn == null)
        {
            DataManager.save();
        }
    }
}
