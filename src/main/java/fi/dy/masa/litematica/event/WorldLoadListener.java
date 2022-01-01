package fi.dy.masa.litematica.event;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;

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
        SchematicWorldHandler.recreateSchematicWorld(worldAfter == null);

        if (worldAfter != null)
        {
            DataManager.load();
            SchematicConversionMaps.computeMaps();
        }
        else
        {
            DataManager.clear();
        }
    }
}
