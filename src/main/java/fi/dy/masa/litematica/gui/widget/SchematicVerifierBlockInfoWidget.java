package fi.dy.masa.litematica.gui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import malilib.gui.widget.BaseModelWidget;
import malilib.gui.widget.BlockModelWidget;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.ItemStackWidget;
import malilib.gui.widget.LabelWidget;
import malilib.render.text.StyledTextLine;
import malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.verifier.VerifierResultType;

public class SchematicVerifierBlockInfoWidget extends ContainerWidget
{
    protected final IBlockState expectedState;
    protected final IBlockState foundState;
    protected final LabelWidget expectedDisplayNameLabel;
    protected final LabelWidget expectedPropertiesLabel;
    protected final LabelWidget expectedRegistryNameLabel;
    protected final LabelWidget expectedTitleLabel;
    protected final LabelWidget foundDisplayNameLabel;
    protected final LabelWidget foundPropertiesLabel;
    protected final LabelWidget foundRegistryNameLabel;
    protected final LabelWidget foundTitleLabel;
    protected final BaseModelWidget expectedModelWidget;
    protected final BaseModelWidget foundModelWidget;
    protected final int expectedColumnWidth;

    public SchematicVerifierBlockInfoWidget(VerifierResultType type,
                                            IBlockState expectedState,
                                            IBlockState foundState)
    {
        super(200, 100);

        ItemStack expectedStack = MaterialCache.getInstance().getItemForDisplayNameForState(expectedState);
        ItemStack foundStack = MaterialCache.getInstance().getItemForDisplayNameForState(foundState);

        this.expectedState = expectedState;
        this.foundState = foundState;

        this.expectedTitleLabel = new LabelWidget("litematica.label.schematic_verifier.column.expected");
        this.foundTitleLabel = new LabelWidget("litematica.label.schematic_verifier.column.found");

        int color = 0xFFE0E0E0;
        this.expectedDisplayNameLabel = new LabelWidget(color, expectedStack.getDisplayName());
        this.foundDisplayNameLabel = new LabelWidget(color, foundStack.getDisplayName());

        color = 0xFF8080FF;
        this.expectedRegistryNameLabel = new LabelWidget(color, BlockUtils.getBlockRegistryName(expectedState));
        this.foundRegistryNameLabel = new LabelWidget(color, BlockUtils.getBlockRegistryName(foundState));
        color = 0xFF909090;
        this.expectedPropertiesLabel = new LabelWidget(-1, -1, color);
        this.foundPropertiesLabel = new LabelWidget(-1, -1, color);

        if (Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue())
        {
            this.expectedModelWidget = new BlockModelWidget(expectedState);
            this.foundModelWidget = new BlockModelWidget(foundState);
        }
        else
        {
            this.expectedModelWidget = new ItemStackWidget(expectedStack);
            this.foundModelWidget = new ItemStackWidget(foundStack);
        }

        color = 0xFF303030;
        this.expectedModelWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, color);
        this.foundModelWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, color);

        if (type == VerifierResultType.WRONG_BLOCK || type == VerifierResultType.EXTRA)
        {
            this.foundRegistryNameLabel.setNormalTextColor(type.getTextColor());
        }
        else if (type == VerifierResultType.MISSING)
        {
            this.expectedRegistryNameLabel.setNormalTextColor(type.getTextColor());
        }

        if (type == VerifierResultType.WRONG_STATE)
        {
            this.foundPropertiesLabel.setLabelStyledTextLines(getFoundBlockStateProperties(type, expectedState, foundState));
        }
        else
        {
            this.foundPropertiesLabel.setLabelStyledTextLines(BlockUtils.getBlockStatePropertyStyledTextLines(foundState, " = "));
        }

        this.expectedPropertiesLabel.setLabelStyledTextLines(BlockUtils.getBlockStatePropertyStyledTextLines(expectedState, " = "));

        this.expectedColumnWidth = Math.max(getMaxWidthFrom(this.expectedTitleLabel, this.expectedRegistryNameLabel, this.expectedPropertiesLabel), this.expectedDisplayNameLabel.getWidth() + 22);
        int foundColumnWidth = Math.max(getMaxWidthFrom(this.foundTitleLabel, this.foundRegistryNameLabel, this.foundPropertiesLabel), this.foundDisplayNameLabel.getWidth() + 22);
        int height = getMaxHeightFrom(this.expectedPropertiesLabel, this.foundPropertiesLabel) + 58;

        this.setWidth(this.expectedColumnWidth + foundColumnWidth + 40);
        this.setHeight(height);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xFF000000);
        this.getBorderRenderer().getNormalSettings().setEnabled(true);

        this.reAddSubWidgets();
        this.updateSubWidgetPositions();
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.expectedTitleLabel);
        this.addWidget(this.foundTitleLabel);
        this.addWidget(this.expectedDisplayNameLabel);
        this.addWidget(this.foundDisplayNameLabel);
        this.addWidget(this.expectedRegistryNameLabel);
        this.addWidget(this.foundRegistryNameLabel);
        this.addWidget(this.expectedModelWidget);
        this.addWidget(this.foundModelWidget);

        if (this.expectedState.getProperties().isEmpty() == false)
        {
            this.addWidget(this.expectedPropertiesLabel);
        }

        if (this.foundState.getProperties().isEmpty() == false)
        {
            this.addWidget(this.foundPropertiesLabel);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        int x = 8;
        this.expectedTitleLabel.setPosition(x, 6);
        this.expectedModelWidget.setPosition(x, 18);
        this.expectedDisplayNameLabel.setPosition(x + 20, 22);
        this.expectedRegistryNameLabel.setPosition(x, 40);
        this.expectedPropertiesLabel.setPosition(x, 52);

        x = this.expectedColumnWidth + 22;
        this.foundTitleLabel.setPosition(x, 6);
        this.foundModelWidget.setPosition(x, 18);
        this.foundDisplayNameLabel.setPosition(x + 20, 22);
        this.foundRegistryNameLabel.setPosition(x, 40);
        this.foundPropertiesLabel.setPosition(x, 52);
    }

    public static List<StyledTextLine> getFoundBlockStateProperties(VerifierResultType type,
                                                                    IBlockState expectedState,
                                                                    IBlockState foundState)
    {
        Collection<IProperty<?>> properties = foundState.getPropertyKeys();

        if (properties.size() > 0)
        {
            List<StyledTextLine> lines = new ArrayList<>();

            try
            {
                for (IProperty<?> prop : properties)
                {
                    Comparable<?> val = foundState.getValue(prop);
                    String key;

                    if (type == VerifierResultType.WRONG_STATE &&
                        expectedState.getPropertyKeys().contains(prop) &&
                        expectedState.getValue(prop).equals(val) == false)
                    {
                        if (prop instanceof PropertyBool)
                        {
                            key = val.equals(Boolean.TRUE) ? "litematica.label.schematic_verifier.modified_block_state_property.boolean.true" :
                                                             "litematica.label.schematic_verifier.modified_block_state_property.boolean.false";
                        }
                        else if (prop instanceof PropertyDirection)
                        {
                            key = "litematica.label.schematic_verifier.modified_block_state_property.direction";
                        }
                        else if (prop instanceof PropertyEnum)
                        {
                            key = "litematica.label.schematic_verifier.modified_block_state_property.enum";
                        }
                        else if (prop instanceof PropertyInteger)
                        {
                            key = "litematica.label.schematic_verifier.modified_block_state_property.integer";
                        }
                        else
                        {
                            key = "litematica.label.schematic_verifier.modified_block_state_property.generic";
                        }

                        lines.add(StyledTextLine.translate(key, prop.getName(), val.toString()));
                    }
                    else
                    {
                        if (prop instanceof PropertyBool)
                        {
                            key = val.equals(Boolean.TRUE) ? "malilib.label.block_state_properties.boolean.true" :
                                          "malilib.label.block_state_properties.boolean.false";
                        }
                        else if (prop instanceof PropertyDirection)
                        {
                            key = "malilib.label.block_state_properties.direction";
                        }
                        else if (prop instanceof PropertyEnum)
                        {
                            key = "malilib.label.block_state_properties.enum";
                        }
                        else if (prop instanceof PropertyInteger)
                        {
                            key = "malilib.label.block_state_properties.integer";
                        }
                        else
                        {
                            key = "malilib.label.block_state_properties.generic";
                        }

                        lines.add(StyledTextLine.translate(key, prop.getName(), " = ", val.toString()));
                    }
                }
            }
            catch (Exception ignore) {}

            return lines;
        }

        return Collections.emptyList();
    }
}
