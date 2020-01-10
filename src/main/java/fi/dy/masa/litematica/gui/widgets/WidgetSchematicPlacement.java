package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.LitematicaGuiIcons;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.Messages;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicPlacement extends WidgetListEntryBase<SchematicPlacementUnloaded>
{
    private final SchematicPlacementManager manager;
    private final WidgetListSchematicPlacements parent;
    private final SchematicPlacementUnloaded placement;
    @Nullable private final SchematicPlacement loadedPlacement;
    private final boolean isOdd;
    private int buttonsStartX;

    public WidgetSchematicPlacement(int x, int y, int width, int height, boolean isOdd,
            SchematicPlacementUnloaded placement, int listIndex, WidgetListSchematicPlacements parent)
    {
        super(x, y, width, height, placement, listIndex);

        this.parent = parent;
        this.placement = placement;
        this.loadedPlacement = placement.isLoaded() ? (SchematicPlacement) placement : null;
        this.isOdd = isOdd;
        this.manager = DataManager.getSchematicPlacementManager();

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.REMOVE);
        posX = this.createButtonOnOff(posX, posY, this.placement.isEnabled(), ButtonListener.ButtonType.TOGGLE_ENABLED);
        posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.CONFIGURE);

        this.buttonsStartX = posX;
    }

    private int createButtonGeneric(int xRight, int y, ButtonListener.ButtonType type)
    {
        return this.addButton(new ButtonGeneric(xRight, y, -1, true, type.getDisplayName()), new ButtonListener(type, this)).getX() - 1;
    }

    private int createButtonOnOff(int xRight, int y, boolean isCurrentlyOn, ButtonListener.ButtonType type)
    {
        ButtonOnOff button = new ButtonOnOff(xRight, y, -1, true, type.getTranslationKey(), isCurrentlyOn);
        return this.addButton(button, new ButtonListener(type, this)).getX() - 2;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean placementSelected = this.manager.getSelectedSchematicPlacement() == this.placement;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || placementSelected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (placementSelected)
        {
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? GuiBase.TXT_GREEN : GuiBase.TXT_RED;
        this.drawString(this.x + 20, this.y + 7, 0xFFFFFFFF, pre + name);

        IGuiIcon icon;

        if (this.loadedPlacement != null)
        {
            if (this.loadedPlacement.getSchematicFile() != null)
            {
                icon = this.loadedPlacement.getSchematic().getType().getIcon();
            }
            else
            {
                icon = LitematicaGuiIcons.SCHEMATIC_TYPE_MEMORY;
            }

            icon.renderAt(this.x + 2, this.y + 5, this.zLevel, false, false);
        }

        if (this.placement.isRegionPlacementModified())
        {
            icon = LitematicaGuiIcons.NOTICE_EXCLAMATION_11;
            icon.renderAt(this.buttonsStartX - 13, this.y + 6, this.zLevel, false, false);
        }

        if (this.placement.isLocked())
        {
            icon = LitematicaGuiIcons.LOCK_LOCKED;
            icon.renderAt(this.buttonsStartX - 26, this.y + 6, this.zLevel, false, false);
        }

        super.render(mouseX, mouseY, placementSelected);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        if (this.placement.isLocked() &&
            GuiBase.isMouseOver(mouseX, mouseY, this.x + this.buttonsStartX - 38, this.y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_locked");
            RenderUtils.drawHoverText(mouseX, mouseY, ImmutableList.of(str));
        }
        else if (this.placement.isRegionPlacementModified() &&
                 GuiBase.isMouseOver(mouseX, mouseY, this.x + this.buttonsStartX - 25, this.y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_modified");
            RenderUtils.drawHoverText(mouseX, mouseY, ImmutableList.of(str));
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 18, this.height))
        {
            File schematicFile = this.placement.getSchematicFile();
            SchematicMetadata metadata = this.loadedPlacement != null ? this.loadedPlacement.getSchematic().getMetadata() : null;
            String fileName = schematicFile != null ? schematicFile.getName() : StringUtils.translate("litematica.gui.label.schematic_placement.hover.in_memory");
            List<String> text = new ArrayList<>();

            if (metadata != null)
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_name", metadata.getName()));
            }

            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.is_loaded", Messages.getYesNoColored(this.placement.isLoaded(), false)));

            BlockPos o = this.placement.getOrigin();
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.origin", o.getX(), o.getY(), o.getZ()));

            if (metadata != null)
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.sub_region_count", this.loadedPlacement.getSubRegionCount()));

                Vec3i size = metadata.getEnclosingSize();
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.enclosing_size", size.getX(), size.getY(), size.getZ()));
            }

            RenderUtils.drawHoverText(mouseX, mouseY, text);
        }
    }

    static class ButtonListener implements IButtonActionListener
    {
        private final ButtonType type;
        private final WidgetSchematicPlacement widget;

        public ButtonListener(ButtonType type, WidgetSchematicPlacement widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == ButtonType.REMOVE)
            {
                if (this.widget.placement.isLocked() && GuiBase.isShiftDown() == false)
                {
                    this.widget.parent.getParentGui().addMessage(MessageType.ERROR, "litematica.error.schematic_placements.remove_fail_locked");
                }
                else
                {
                    this.widget.manager.removeSchematicPlacement(this.widget.placement);
                    this.widget.parent.refreshEntries();
                }
            }
            else if (this.type == ButtonType.TOGGLE_ENABLED)
            {
                DataManager.getSchematicPlacementManager().toggleEnabled(this.widget.placement);
                this.widget.parent.refreshEntries();
            }
            else if (this.widget.placement.isLoaded())
            {
                if (this.type == ButtonType.CONFIGURE)
                {
                    GuiPlacementConfiguration gui = new GuiPlacementConfiguration((SchematicPlacement) this.widget.placement);
                    gui.setParent(this.widget.parent.getParentGui());
                    GuiBase.openGui(gui);
                }
            }
        }

        public enum ButtonType
        {
            CONFIGURE       ("litematica.gui.button.schematic_placements.configure"),
            REMOVE          ("litematica.gui.button.schematic_placements.remove"),
            TOGGLE_ENABLED  ("litematica.gui.button.schematic_placements.placement_enabled");

            private final String translationKey;

            private ButtonType(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
