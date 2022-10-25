package litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;

@Mixin(WorldClient.class)
public interface IMixinWorldClient
{
    @Accessor
    void setClientChunkProvider(ChunkProviderClient provider);
}
