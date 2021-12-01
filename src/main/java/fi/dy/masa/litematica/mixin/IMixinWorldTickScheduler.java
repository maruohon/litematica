package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.WorldTickScheduler;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

@Mixin(WorldTickScheduler.class)
public interface IMixinWorldTickScheduler<T>
{
    @Accessor("chunkTickSchedulers")
    Long2ObjectMap<ChunkTickScheduler<T>> litematica_getChunkTickSchedulers();
}
