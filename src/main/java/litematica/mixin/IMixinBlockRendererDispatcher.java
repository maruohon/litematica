package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;

@Mixin(BlockRendererDispatcher.class)
public interface IMixinBlockRendererDispatcher
{
    @Accessor
    BlockFluidRenderer getFluidRenderer();
}
