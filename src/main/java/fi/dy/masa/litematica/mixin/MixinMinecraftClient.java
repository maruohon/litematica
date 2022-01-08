package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable>
{
    public MixinMinecraftClient(String string_1)
    {
        super(string_1);
    }

    @Inject(method = "doItemUse()V", at = @At("TAIL"))
    private void onRightClickMouseTail(CallbackInfo ci)
    {
        if (WorldUtils.shouldDoEasyPlaceActions())
        {
            WorldUtils.onRightClickTail((MinecraftClient)(Object) this);
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }
}
