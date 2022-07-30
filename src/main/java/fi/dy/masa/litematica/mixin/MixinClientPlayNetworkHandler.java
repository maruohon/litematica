package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "updateChunk", at = @At("HEAD"))
    private void litematica_onUpdateChunk(int x, int z, LightData lightData, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(x, z);
        }

        DataManager.getSchematicPlacementManager().onClientChunkLoad(x, z);
        // TODO verifier updates?
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void onChunkUnload(UnloadChunkS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.getX(), packet.getZ());
        }
    }

    @Inject(method = "onGameMessage", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }
}
