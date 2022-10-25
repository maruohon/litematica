package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import litematica.util.WorldUtils;

@Mixin(Chunk.class)
public abstract class MixinChunk
{
    @Redirect(method = "setBlockState",
              at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isRemote:Z"))
    private boolean redirectIsRemote(World world)
    {
        return WorldUtils.shouldPreventBlockUpdates(world) ? true : world.isRemote;
    }
}
