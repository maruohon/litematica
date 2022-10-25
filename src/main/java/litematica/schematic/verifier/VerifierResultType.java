package litematica.schematic.verifier;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;

import malilib.config.option.DualColorConfig;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import litematica.config.Configs;

public enum VerifierResultType
{
    CORRECT_STATE ("litematica.name.schematic_verifier.correct_state", Configs.Colors.VERIFIER_CORRECT),
    EXTRA         ("litematica.name.schematic_verifier.extra",         Configs.Colors.VERIFIER_EXTRA),
    MISSING       ("litematica.name.schematic_verifier.missing",       Configs.Colors.VERIFIER_MISSING),
    WRONG_BLOCK   ("litematica.name.schematic_verifier.wrong_blocks",  Configs.Colors.VERIFIER_WRONG_BLOCK),
    WRONG_STATE   ("litematica.name.schematic_verifier.wrong_state",   Configs.Colors.VERIFIER_WRONG_STATE);

    public static final ImmutableList<VerifierResultType> INCORRECT_TYPES = ImmutableList.of(
            VerifierResultType.WRONG_BLOCK,
            VerifierResultType.WRONG_STATE,
            VerifierResultType.MISSING,
            VerifierResultType.EXTRA
    );

    public static final ImmutableList<VerifierResultType> SELECTABLE_CATEGORIES = ImmutableList.of(
            VerifierResultType.WRONG_BLOCK,
            VerifierResultType.WRONG_STATE,
            VerifierResultType.MISSING,
            VerifierResultType.EXTRA,
            VerifierResultType.CORRECT_STATE
    );

    private final String translationKey;
    private final DualColorConfig colorConfig;

    VerifierResultType(String translationKey, DualColorConfig colorConfig)
    {
        this.translationKey = translationKey;
        this.colorConfig = colorConfig;
    }

    public Color4f getOverlayColor()
    {
        return this.colorConfig.getFirstColor();
    }

    public int getTextColor()
    {
        return this.colorConfig.getSecondColorInt();
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
            if (expectedState != SchematicVerifier.AIR)
            {
                if (foundState == SchematicVerifier.AIR)
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
