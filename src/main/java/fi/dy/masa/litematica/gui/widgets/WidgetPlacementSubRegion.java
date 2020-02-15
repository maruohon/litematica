package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.gui.LitematicaGuiIcons;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetPlacementSubRegion extends WidgetListEntryBase<SubRegionPlacement>
{
    private final SchematicPlacement schematicPlacement;
    private final WidgetListPlacementSubRegions parent;
    private final SubRegionPlacement placement;
    private final boolean isOdd;
    private int buttonsStartX;

    public WidgetPlacementSubRegion(int x, int y, int width, int height, boolean isOdd,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement, int listIndex,
            WidgetListPlacementSubRegions parent)
    {
        super(x, y, width, height, placement, listIndex);

        this.parent = parent;
        this.schematicPlacement = schematicPlacement;
        this.placement = placement;
        this.isOdd = isOdd;

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        posX = this.createButtonOnOff(posX, posY, this.placement.isEnabled(), WidgetSchematicPlacementEntry.ButtonListener.ButtonType.TOGGLE_ENABLED);
        posX = this.createButtonGeneric(posX, posY, WidgetSchematicPlacementEntry.ButtonListener.ButtonType.CONFIGURE);

        this.buttonsStartX = posX;
    }

    private int createButtonGeneric(int xRight, int y, WidgetSchematicPlacementEntry.ButtonListener.ButtonType type)
    {
        String label = StringUtils.translate(type.getTranslationKey());
        int len = this.getStringWidth(label) + 10;
        xRight -= len;
        this.addButton(new ButtonGeneric(xRight, y, len, 20, label), new ButtonListener(type, this));

        return xRight - 2;
    }

    private int createButtonOnOff(int xRight, int y, boolean isCurrentlyOn, WidgetSchematicPlacementEntry.ButtonListener.ButtonType type)
    {
        ButtonOnOff button = new ButtonOnOff(xRight, y, -1, true, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));

        return xRight - button.getWidth() - 2;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId, boolean selected)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean placementSelected = this.schematicPlacement.getSelectedSubRegionPlacement() == this.placement;
        int x = this.getX();
        int y = this.getY();
        int z = this.getZLevel();
        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered and the selected entry
        if (placementSelected || (isActiveGui && this.getId() == hoveredWidgetId))
        {
            RenderUtils.drawRect(x, y, width, height, 0xA0707070, z);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(x, y, width, height, 0xA0101010, z);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(x, y, width, height, 0xA0303030, z);
        }

        if (placementSelected)
        {
            RenderUtils.drawOutline(x, y, width, height, 1, 0xFFE0E0E0, z + 1);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        this.drawString(x + 20, y + 7, 0xFFFFFFFF, pre + name);

        IGuiIcon icon;

        if (this.schematicPlacement.getSchematic().getFile() != null)
        {
            icon = this.schematicPlacement.getSchematic().getType().getIcon();
        }
        else
        {
            icon = LitematicaGuiIcons.SCHEMATIC_TYPE_MEMORY;
        }

        icon.renderAt(x + 2, y + 5, z, false, false);

        if (this.placement.isRegionPlacementModifiedFromDefault())
        {
            icon = LitematicaGuiIcons.NOTICE_EXCLAMATION_11;
            icon.renderAt(this.buttonsStartX - icon.getWidth() - 2, y + 6, z, false, false);
        }

        super.render(mouseX, mouseY, isActiveGui, hoveredWidgetId, placementSelected);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        ISchematic schematic = this.schematicPlacement.getSchematic();
        File schematicFile = schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getName() : StringUtils.translate("litematica.gui.label.schematic_placement.hover.in_memory");
        int x = this.getX();
        int y = this.getY();
        int z = this.getZLevel() + 1;
        int height = this.getHeight();

        if (this.placement.isRegionPlacementModifiedFromDefault() &&
            GuiBase.isMouseOver(mouseX, mouseY, x + this.buttonsStartX - 25, y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_sub_region_modified");
            RenderUtils.drawHoverText(mouseX, mouseY, z, ImmutableList.of(str));
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, x, y, this.buttonsStartX - 14, height))
        {
            List<String> text = new ArrayList<>();
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_name", schematic.getMetadata().getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));

            BlockPos o = this.placement.getPos();
            o = PositionUtils.getTransformedBlockPos(o, this.schematicPlacement.getMirror(), this.schematicPlacement.getRotation());
            o = o.add(this.schematicPlacement.getOrigin());
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.origin", o.getX(), o.getY(), o.getZ()));

            ISchematicRegion region = schematic.getSchematicRegion(this.placement.getName());
            Vec3i size = region != null ? region.getSize() : null;

            if (size != null)
            {
                text.add(StringUtils.translate("litematica.gui.label.placement_sub.hover.region_size", size.getX(), size.getY(), size.getZ()));
            }

            RenderUtils.drawHoverText(mouseX, mouseY, z, text);
        }
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final WidgetSchematicPlacementEntry.ButtonListener.ButtonType type;
        private final WidgetPlacementSubRegion widget;

        public ButtonListener(WidgetSchematicPlacementEntry.ButtonListener.ButtonType type, WidgetPlacementSubRegion widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == WidgetSchematicPlacementEntry.ButtonListener.ButtonType.CONFIGURE)
            {
                GuiSubRegionConfiguration gui = new GuiSubRegionConfiguration(this.widget.schematicPlacement, this.widget.placement);
                gui.setParent(this.widget.parent.getParentGui());
                GuiBase.openGui(gui);
            }
            else if (this.type == WidgetSchematicPlacementEntry.ButtonListener.ButtonType.TOGGLE_ENABLED)
            {
                DataManager.getSchematicPlacementManager().toggleSubRegionEnabled(
                        this.widget.schematicPlacement, this.widget.placement.getName(), this.widget.parent.getParentGui());
                this.widget.parent.refreshEntries();
            }
        }
    }
}
