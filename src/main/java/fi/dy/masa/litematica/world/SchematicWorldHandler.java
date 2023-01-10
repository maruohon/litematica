package fi.dy.masa.litematica.world;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.Difficulty;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;

public class SchematicWorldHandler
{
    public static final SchematicWorldHandler INSTANCE = new SchematicWorldHandler(LitematicaRenderer.getInstance()::getWorldRenderer);

    protected final Supplier<WorldRendererSchematic> rendererSupplier;
    @Nullable protected WorldSchematic world;

    // The supplier can return null, but it can't be null itself!
    public SchematicWorldHandler(Supplier<WorldRendererSchematic> rendererSupplier)
    {
        this.rendererSupplier = rendererSupplier;
    }

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return INSTANCE.getWorld();
    }

    @Nullable
    public WorldSchematic getWorld()
    {
        if (this.world == null)
        {
            this.world = createSchematicWorld(this.rendererSupplier.get());
        }

        return this.world;
    }

    public static WorldSchematic createSchematicWorld(@Nullable WorldRendererSchematic worldRenderer)
    {
        if (MinecraftClient.getInstance().world == null)
        {
            return null;
        }

        ClientWorld.Properties levelInfo = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
        RegistryEntry<DimensionType> entry = BuiltinRegistries.DIMENSION_TYPE.entryOf(DimensionTypes.OVERWORLD);
        return new WorldSchematic(levelInfo, entry, MinecraftClient.getInstance()::getProfiler, worldRenderer);
    }

    public void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            Litematica.debugLog("Removing the schematic world...");
            this.world = null;
        }
        else
        {
            Litematica.debugLog("(Re-)creating the schematic world...");
            @Nullable WorldRendererSchematic worldRenderer = this.world != null ? this.world.worldRenderer : LitematicaRenderer.getInstance().getWorldRenderer();
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            this.world = createSchematicWorld(worldRenderer);
            Litematica.debugLog("Schematic world (re-)created: {}", this.world);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(this.world);
    }
}
