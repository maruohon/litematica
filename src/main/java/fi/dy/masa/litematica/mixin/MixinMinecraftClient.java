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

    @Inject(method = "doItemUse()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getCount()I", ordinal = 0), cancellable = true)
    private void handlePlacementRestriction(CallbackInfo ci)
    {
        if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            if (WorldUtils.handlePlacementRestriction((MinecraftClient)(Object) this))
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }
}
