package fi.dy.masa.litematica.mixin;

import net.minecraft.util.math.BlockBox;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldTickScheduler.class)
public interface IMixinWorldTickScheduler<T>
{
    @Invoker("visitChunks")
    void litematicaVisitChunks(BlockBox box, WorldTickScheduler.ChunkVisitor<T> visitor);
}
