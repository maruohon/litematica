package fi.dy.masa.litematica.schematic.verifier;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.option.ColorConfig;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Color4f;

public enum VerifierResultType
{
    ALL           ("litematica.name.schematic_verifier.all",           Configs.Colors.VERIFIER_CORRECT), // color not used
    CORRECT_STATE ("litematica.name.schematic_verifier.correct_state", Configs.Colors.VERIFIER_CORRECT),
    EXTRA         ("litematica.name.schematic_verifier.extra",         Configs.Colors.VERIFIER_EXTRA),
    MISSING       ("litematica.name.schematic_verifier.missing",       Configs.Colors.VERIFIER_MISSING),
    WRONG_BLOCK   ("litematica.name.schematic_verifier.wrong_blocks",  Configs.Colors.VERIFIER_WRONG_BLOCK),
    WRONG_STATE   ("litematica.name.schematic_verifier.wrong_state",   Configs.Colors.VERIFIER_WRONG_STATE);

    public static final ImmutableList<VerifierResultType> INCORRECT_TYPES = ImmutableList.of(
            VerifierResultType.WRONG_BLOCK,
            VerifierResultType.WRONG_STATE,
            VerifierResultType.MISSING,
            VerifierResultType.EXTRA);

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private final String translationKey;
    private final ColorConfig colorConfig;

    VerifierResultType(String translationKey, ColorConfig colorConfig)
    {
        this.translationKey = translationKey;
        this.colorConfig = colorConfig;
    }

    public Color4f getOverlayColor()
    {
        return this.colorConfig.getColor();
    }

    // TODO add a separate config
    public int getTextColor()
    {
        return this.colorConfig.getColor().intValue | 0xFF000000;
    }

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    public String getCategoryWidgetTranslationKey()
    {
        return this.translationKey + ".widget";
    }

    public static VerifierResultType from(IBlockState expectedState, IBlockState foundState)
    {
        VerifierResultType type = null;

        if (foundState != expectedState)
        {
            if (expectedState != AIR)
            {
                if (foundState == AIR)
                {
                    type = VerifierResultType.MISSING;
                }
                else if (expectedState.getBlock() != foundState.getBlock())
                {
                    type = VerifierResultType.WRONG_BLOCK;
                }
                else
                {
                    type = VerifierResultType.WRONG_STATE;
                }
            }
            else if (Configs.Visuals.IGNORE_EXISTING_FLUIDS.getBooleanValue() == false ||
                             foundState.getMaterial().isLiquid() == false)
            {
                type = VerifierResultType.EXTRA;
            }
        }

        if (type == null)
        {
            type = VerifierResultType.CORRECT_STATE;
        }

        return type;
    }
}
