package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
//import java.util.OptionalLong;
//import net.minecraft.world.biome.IBiomeMagnifier;
import net.minecraft.client.Minecraft;
//import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Difficulty;
import net.minecraft.client.world.ClientWorld.ClientWorldInfo;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
    }

    public static WorldSchematic createSchematicWorld()
    {
        ClientWorldInfo levelInfo = new ClientWorldInfo(Difficulty.PEACEFUL, false, true);
/*        return new WorldSchematic(Minecraft.getInstance().getNetworkHandler(), info, net.minecraft.world.World.END, PublicDimensionType.get_THE_END(), Minecraft.getInstance()::getProfiler);
    }

    private static class PublicDimensionType extends DimensionType
    {
        private PublicDimensionType(OptionalLong fixedTime,
                               boolean hasSkylight,
                               boolean hasCeiling,
                               boolean ultrawarm,
                               boolean natural,
                               double coordinateScale,
                               boolean hasEnderDragonFight,
                               boolean piglinSafe,
                               boolean bedWorks,
                               boolean respawnAnchorWorks,
                               boolean hasRaids,
                               int logicalHeight,
                               IBiomeMagnifier biomeAccessType,
                               ResourceLocation infiniburn,
                               ResourceLocation skyProperties,
                               float ambientLight)
        {
            super(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale,
                  hasEnderDragonFight, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids,
                  logicalHeight, biomeAccessType, infiniburn, skyProperties, ambientLight);
        }
        public static DimensionType get_THE_END()
        {
            return DimensionType.THE_END;
        }*/
        DimensionType dimType = Minecraft.getInstance().world.getRegistryManager().getDimensionTypes().get(DimensionType.THE_END_REGISTRY_KEY);
        return new WorldSchematic(levelInfo, dimType, Minecraft.getInstance()::getProfiler);
    }

    public static void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            world = null;
        }
        else
        {
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            world = createSchematicWorld();
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
