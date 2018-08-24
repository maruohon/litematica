package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.interfaces.IRegionRenderCacheBuilder;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayType;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;

@Mixin(RegionRenderCacheBuilder.class)
public class MixinRegionRenderCacheBuilder implements IRegionRenderCacheBuilder
{
    private BufferBuilder[] overlayBufferBuilders;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(CallbackInfo ci)
    {
        this.overlayBufferBuilders = new BufferBuilder[OverlayType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    @Override
    public BufferBuilder getOverlayBuffer(OverlayType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }
}
