package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void litematica_onUpdateChunk(ChunkDataS2CPacket packet, CallbackInfo ci)
    {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onUpdateChunk({}, {})", chunkX, chunkZ);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(chunkX, chunkZ);
        }

        DataManager.getSchematicPlacementManager().onClientChunkLoad(chunkX, chunkZ);
        // TODO verifier updates?
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void litematica_onChunkUnload(UnloadChunkS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onChunkUnload({}, {})", packet.getX(), packet.getZ());
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.getX(), packet.getZ());
        }
    }

    @Inject(method = "onGameMessage", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"))
    private void litematica_onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }
}
