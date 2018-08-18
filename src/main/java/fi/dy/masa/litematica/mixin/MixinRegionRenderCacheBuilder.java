package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;

@Mixin(RegionRenderCacheBuilder.class)
public class MixinRegionRenderCacheBuilder implements IRegionRenderCacheBuilder
{
    private BufferBuilder[] overlayBufferBuilders;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(CallbackInfo ci)
    {
        this.overlayBufferBuilders = new BufferBuilder[] { new BufferBuilder(262144), new BufferBuilder(262144) };
    }

    @Override
    public BufferBuilder getOverlayBuffer(boolean outlineBuffer)
    {
        return this.overlayBufferBuilders[outlineBuffer ? 0 : 1];
    }
}
