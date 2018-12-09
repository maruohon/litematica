package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.SubChunkPos;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient
{
    @Inject(method = "handleChunkData", at = @At("RETURN"))
    private void onChunkData(SPacketChunkData packetIn, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue())
        {
            for (int y = 0; y < 16; ++y)
            {
                WorldUtils.markSchematicChunkForRenderUpdate(new SubChunkPos(packetIn.getChunkX(), y, packetIn.getChunkZ()));
            }
        }
    }
}
