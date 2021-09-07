package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk
{
    @Redirect(method = "setBlockState",
                slice = @Slice(from = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/chunk/ChunkSection;getBlockState(III)" +
                                          "Lnet/minecraft/block/BlockState;")),
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z", ordinal = 0))
    private boolean litematica_redirectIsRemote(World world)
    {
        return WorldUtils.shouldPreventBlockUpdates(world) || world.isClient;
    }
}
