package fi.dy.masa.litematica.world;

import java.util.OptionalLong;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeAccessType;
import net.minecraft.world.dimension.DimensionType;

public class PublicDimensionType extends DimensionType
{
    public PublicDimensionType(OptionalLong fixedTime,
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
                               BiomeAccessType biomeAccessType,
                               Identifier infiniburn,
                               Identifier skyProperties,
                               float ambientLight)
    {
        super(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale,
              hasEnderDragonFight, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids,
              logicalHeight, biomeAccessType, infiniburn, skyProperties, ambientLight);
    }
}
