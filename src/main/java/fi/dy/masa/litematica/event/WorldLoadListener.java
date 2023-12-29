package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.UnknownCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import fi.dy.masa.litematica.Reference;
import io.netty.buffer.Unpooled;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Save the settings before the integrated server gets shut down
        if (worldBefore != null)
        {
            DataManager.save();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        SchematicWorldHandler.INSTANCE.recreateSchematicWorld(worldAfter == null);

        if (worldAfter != null)
        {
            DataManager.load();
            SchematicConversionMaps.computeMaps();
        }
        else
        {
            DataManager.clear();
        }

        // Send hello message
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler != null)
        {
            Identifier identifier = new Identifier(Reference.MOD_ID, "hello");
            networkHandler.sendPacket(new CustomPayloadC2SPacket(new UnknownCustomPayload(identifier)));
        }
    }
}
