package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkSectionPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.world.ChunkManagerSchematic;

@Mixin(net.minecraft.client.network.ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void onChunkData(net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket packetIn, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(packetIn.getX(), packetIn.getZ());
        }
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void onChunkDelta(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            ChunkSectionPos pos = ((IMixinChunkDeltaUpdateS2CPacket) packet).litematica_getSection();
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void onChunkUnload(net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false &&
            ((Object) this instanceof ChunkManagerSchematic) == false)
        {
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.getX(), packet.getZ());
        }
    }
}
