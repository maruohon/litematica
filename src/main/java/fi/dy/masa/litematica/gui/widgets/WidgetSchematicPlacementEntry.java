package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiSchematicPlacementsList;
import fi.dy.masa.litematica.gui.LitematicaIcons;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.icon.Icon;
import fi.dy.masa.malilib.render.message.MessageType;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseListEntryWidget;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.message.MessageHelpers;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicPlacementEntry extends BaseListEntryWidget<SchematicPlacementUnloaded>
{
    private final SchematicPlacementManager manager;
    private final GuiSchematicPlacementsList gui;
    private final WidgetListSchematicPlacements listWidget;
    private final SchematicPlacementUnloaded placement;
    @Nullable private final SchematicPlacement loadedPlacement;
    private final boolean isOdd;
    private int buttonsStartX;

    public WidgetSchematicPlacementEntry(int x, int y, int width, int height, boolean isOdd,
            SchematicPlacementUnloaded placement, int listIndex, WidgetListSchematicPlacements listWidget, GuiSchematicPlacementsList gui)
    {
        super(x, y, width, height, placement, listIndex);

        this.gui = gui;
        this.listWidget = listWidget;
        this.placement = placement;
        this.loadedPlacement = placement.isLoaded() ? (SchematicPlacement) placement : null;
        this.isOdd = isOdd;
        this.manager = DataManager.getSchematicPlacementManager();

        int posX = x + width - 2;
        int posY = y + 1;

        // Note: These are placed from right to left

        if (this.useIconButtons())
        {
            posX = this.createButtonIconOnly(posX, posY, ButtonListener.ButtonType.REMOVE);
            posX = this.createButtonIconOnly(posX, posY, ButtonListener.ButtonType.SAVE);
            posX = this.createButtonIconOnly(posX, posY, ButtonListener.ButtonType.DUPLICATE);
            posX = this.createButtonOnOff(posX, posY, this.placement.isEnabled(), ButtonListener.ButtonType.TOGGLE_ENABLED);
            posX = this.createButtonIconOnly(posX, posY, ButtonListener.ButtonType.CONFIGURE);
        }
        else
        {
            posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.REMOVE);
            posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.SAVE);
            posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.DUPLICATE);
            posX = this.createButtonOnOff(posX, posY, this.placement.isEnabled(), ButtonListener.ButtonType.TOGGLE_ENABLED);
            posX = this.createButtonGeneric(posX, posY, ButtonListener.ButtonType.CONFIGURE);
        }

        this.buttonsStartX = posX;
    }

    private boolean useIconButtons()
    {
        return Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.getBooleanValue();
    }

    private int createButtonGeneric(int xRight, int y, ButtonListener.ButtonType type)
    {
        GenericButton button = new GenericButton(xRight, y, -1, true, type.getDisplayName());
        button.addHoverString(type.getHoverKey());

        return this.addButton(button, new ButtonListener(type, this)).getX() - 1;
    }

    private int createButtonIconOnly(int xRight, int y, ButtonListener.ButtonType type)
    {
        Icon icon = type.getIcon();
        GenericButton button;
        int size = 20;

        if (icon != null)
        {
            button = new GenericButton(xRight - size, y, size, size, "", icon, type.getHoverKey());
        }
        else
        {
            button = new GenericButton(xRight, y, -1, true, type.getDisplayName());
            button.addHoverString(type.getHoverKey());
        }

        return this.addButton(button, new ButtonListener(type, this)).getX() - 1;
    }

    private int createButtonOnOff(int xRight, int y, boolean isCurrentlyOn, ButtonListener.ButtonType type)
    {
        String key = this.useIconButtons() ? "%s" : type.getTranslationKey();
        OnOffButton button = new OnOffButton(xRight, y, -1, true, key, isCurrentlyOn);
        button.addHoverString(type.getHoverKey());

        return this.addButton(button, new ButtonListener(type, this)).getX() - 1;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, boolean hovered)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean placementSelected = this.manager.getSelectedSchematicPlacement() == this.placement;
        int x = this.getX();
        int y = this.getY();
        int z = this.getZLevel();
        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered and the selected entry
        if (placementSelected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0707070, z);
        }
        else if (this.isOdd)
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0101010, z);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0303030, z);
        }

        if (placementSelected)
        {
            RenderUtils.renderOutline(x, y, width, height, 1, 0xFFE0E0E0, z);
        }

        String name = this.placement.getName();
        String pre = this.placement.isEnabled() ? BaseScreen.TXT_GREEN : BaseScreen.TXT_RED;
        this.drawString(x + 20, y + 7, 0xFFFFFFFF, pre + name);

        Icon icon;

        if (this.loadedPlacement != null)
        {
            if (this.loadedPlacement.getSchematicFile() != null)
            {
                icon = this.loadedPlacement.getSchematic().getType().getIcon();
            }
            else
            {
                icon = LitematicaIcons.SCHEMATIC_TYPE_MEMORY;
            }

            icon.renderAt(x + 2, y + 5, z + 0.1f, false, false);
        }

        if (this.placement.isRegionPlacementModified())
        {
            icon = LitematicaIcons.NOTICE_EXCLAMATION_11;
            icon.renderAt(this.buttonsStartX - 13, y + 6, z + 0.1f, false, false);
        }

        if (this.placement.isLocked())
        {
            icon = LitematicaIcons.LOCK_LOCKED;
            icon.renderAt(this.buttonsStartX - 26, y + 6, z + 0.1f, false, false);
        }

        super.render(mouseX, mouseY, isActiveGui, placementSelected);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        int x = this.getX();
        int y = this.getY();
        int z = this.getZLevel() + 1;
        int height = this.getHeight();

        if (this.placement.isLocked() &&
            BaseScreen.isMouseOver(mouseX, mouseY, x + this.buttonsStartX - 38, y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_locked");
            RenderUtils.renderHoverText(mouseX, mouseY, z, ImmutableList.of(str));
        }
        else if (this.placement.isRegionPlacementModified() &&
                 BaseScreen.isMouseOver(mouseX, mouseY, x + this.buttonsStartX - 25, y + 6, 11, 11))
        {
            String str = StringUtils.translate("litematica.hud.schematic_placement.hover_info.placement_modified");
            RenderUtils.renderHoverText(mouseX, mouseY, z, ImmutableList.of(str));
        }
        else if (BaseScreen.isMouseOver(mouseX, mouseY, x, y, this.buttonsStartX - 18, height))
        {
            File schematicFile = this.placement.getSchematicFile();
            SchematicMetadata metadata = this.loadedPlacement != null ? this.loadedPlacement.getSchematic().getMetadata() : null;
            String fileName = schematicFile != null ? schematicFile.getName() : StringUtils.translate("litematica.gui.label.schematic_placement.hover.in_memory");
            List<String> text = new ArrayList<>();
            boolean saved = this.placement.isSavedToFile();

            if (metadata != null)
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_name", metadata.getName()));
            }

            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.is_loaded", MessageHelpers.getYesNoColored(this.placement.isLoaded(), false)));

            // Get a cached value, to not query and read the file every rendered frame...
            if (saved && this.gui.getCachedWasModifiedSinceSaved(this.placement))
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.is_saved_to_file_modified"));
            }
            else
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.is_saved_to_file_not_modified", MessageHelpers.getYesNoColored(saved, false)));
            }

            BlockPos o = this.placement.getOrigin();
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.origin", o.getX(), o.getY(), o.getZ()));

            if (metadata != null)
            {
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.sub_region_count", this.loadedPlacement.getSubRegionCount()));

                Vec3i size = metadata.getEnclosingSize();
                text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.enclosing_size", size.getX(), size.getY(), size.getZ()));
            }

            RenderUtils.renderHoverText(mouseX, mouseY, z, text);
        }

        super.postRenderHovered(mouseX, mouseY, isActiveGui, hoveredWidgetId);
    }

    static class ButtonListener implements ButtonActionListener
    {
        private final ButtonType type;
        private final WidgetSchematicPlacementEntry widget;

        public ButtonListener(ButtonType type, WidgetSchematicPlacementEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            if (this.type == ButtonType.REMOVE)
            {
                if (this.widget.placement.isLocked() && BaseScreen.isShiftDown() == false)
                {
                    this.widget.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_placements.remove_fail_locked");
                }
                else
                {
                    this.widget.manager.removeSchematicPlacement(this.widget.placement);
                    this.widget.listWidget.refreshEntries();
                }
            }
            else if (this.type == ButtonType.SAVE)
            {
                if (this.widget.placement.saveToFileIfChanged(this.widget.gui) == false)
                {
                    this.widget.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_placements.save_failed");
                }
            }
            else if (this.type == ButtonType.TOGGLE_ENABLED)
            {
                DataManager.getSchematicPlacementManager().toggleEnabled(this.widget.placement);
                this.widget.listWidget.refreshEntries();
            }
            else if (this.type == ButtonType.DUPLICATE)
            {
                DataManager.getSchematicPlacementManager().duplicateSchematicPlacement(this.widget.placement);
                this.widget.listWidget.refreshEntries();
            }
            else if (this.widget.placement.isLoaded())
            {
                if (this.type == ButtonType.CONFIGURE)
                {
                    GuiPlacementConfiguration gui = new GuiPlacementConfiguration((SchematicPlacement) this.widget.placement);
                    gui.setParent(this.widget.gui);
                    BaseScreen.openGui(gui);
                }
            }
        }

        public enum ButtonType
        {
            CONFIGURE       (LitematicaIcons.CONFIGURATION, "litematica.gui.button.schematic_placements.configure", "litematica.gui.hover.schematic_placement.button.configure"),
            DUPLICATE       (LitematicaIcons.DUPLICATE, "litematica.gui.button.schematic_placements.duplicate", "litematica.gui.hover.schematic_placement.button.duplicate"),
            REMOVE          (LitematicaIcons.TRASH_CAN, "litematica.gui.button.schematic_placements.remove", "litematica.gui.hover.schematic_placement.button.remove"),
            SAVE            (LitematicaIcons.SAVE_DISK, "litematica.gui.button.schematic_placements.save", "litematica.gui.hover.schematic_placement.button.save"),
            TOGGLE_ENABLED  (null,                              "litematica.gui.button.schematic_placements.placement_enabled", "litematica.gui.hover.schematic_placement.button.toggle_enabled");

            @Nullable private final Icon icon;
            private final String translationKey;
            private final String hoverKey;

            private ButtonType(@Nullable Icon icon, String translationKey, String hoverKey)
            {
                this.icon = icon;
                this.translationKey = translationKey;
                this.hoverKey = hoverKey;
            }

            @Nullable
            public Icon getIcon()
            {
                return this.icon;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getHoverKey()
            {
                return this.hoverKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
