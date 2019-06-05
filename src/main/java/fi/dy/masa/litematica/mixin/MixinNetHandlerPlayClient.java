package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.ChunkPos;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient
{
    @Inject(method = "handleChunkData", at = @At("RETURN"))
    private void onChunkData(SPacketChunkData packetIn, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(new ChunkPos(packetIn.getChunkX(), packetIn.getChunkZ()));
        }
    }
}
