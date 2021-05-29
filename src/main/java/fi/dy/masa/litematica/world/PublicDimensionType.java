package fi.dy.masa.litematica.world;

import java.util.OptionalLong;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.IBiomeMagnifier;

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
                               IBiomeMagnifier biomeAccessType,
                               ResourceLocation infiniburn,
                               ResourceLocation skyProperties,
                               float ambientLight)
    {
        super(fixedTime, hasSkylight, hasCeiling, ultrawarm, natural, coordinateScale,
              hasEnderDragonFight, piglinSafe, bedWorks, respawnAnchorWorks, hasRaids,
              logicalHeight, biomeAccessType, infiniburn, skyProperties, ambientLight);
    }
}
