package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.world.ChunkProviderSchematic;

@Mixin(net.minecraft.client.network.play.ClientPlayNetHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "handleChunkData", at = @At("RETURN"))
    private void handleChunkData(net.minecraft.network.play.server.SChunkDataPacket packetIn, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(packetIn.getX(), packetIn.getZ());
        }
    }

    @Inject(method = "processChunkUnload", at = @At("RETURN"))
    private void processChunkUnload(net.minecraft.network.play.server.SUnloadChunkPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false &&
            ((Object) this instanceof ChunkProviderSchematic) == false)
        {
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.getX(), packet.getZ());
        }
    }
}
