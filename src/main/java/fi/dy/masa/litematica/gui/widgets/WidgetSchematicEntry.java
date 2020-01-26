package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiSchematicSaveConvert;
import fi.dy.masa.litematica.gui.LitematicaGuiIcons;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicEntry extends WidgetListEntryBase<ISchematic>
{
    private final WidgetListLoadedSchematics parent;
    private final ISchematic schematic;
    private final int typeIconX;
    private final int typeIconY;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSchematicEntry(int x, int y, int width, int height, boolean isOdd,
            ISchematic schematic, int listIndex, WidgetListLoadedSchematics parent)
    {
        super(x, y, width, height, schematic, listIndex);

        this.parent = parent;
        this.schematic = schematic;
        this.isOdd = isOdd;
        y += 1;

        int posX = x + width - 2;

        // Note: These are placed from right to left

        if (this.useIconButtons())
        {
            posX -= this.createButtonIconOnly(posX, y, ButtonListener.Type.UNLOAD);
            posX -= this.createButtonIconOnly(posX, y, ButtonListener.Type.RELOAD);
            posX -= this.createButtonIconOnly(posX, y, ButtonListener.Type.SAVE_TO_FILE);
            posX -= this.createButtonIconOnly(posX, y, ButtonListener.Type.CREATE_PLACEMENT);
        }
        else
        {
            posX -= this.addButton(posX, y, ButtonListener.Type.UNLOAD);
            posX -= this.addButton(posX, y, ButtonListener.Type.RELOAD);
            posX -= this.addButton(posX, y, ButtonListener.Type.SAVE_TO_FILE);
            posX -= this.addButton(posX, y, ButtonListener.Type.CREATE_PLACEMENT);
        }

        this.buttonsStartX = posX;
        this.typeIconX = this.x + 2;
        this.typeIconY = y + 4;
    }

    private boolean useIconButtons()
    {
        return Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.getBooleanValue();
    }

    private int addButton(int x, int y, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        ButtonGeneric button = new ButtonGeneric(x, y, -1, true, type.getDisplayName());

        if (type.getHoverKey() != null)
        {
            button.setHoverStrings(type.getHoverKey());
        }

        this.addButton(button, listener);

        return button.getWidth() + 1;
    }

    private int createButtonIconOnly(int xRight, int y, ButtonListener.Type type)
    {
        IGuiIcon icon = type.getIcon();
        ButtonGeneric button;
        int size = 20;

        if (icon != null)
        {
            button = new ButtonGeneric(xRight - size, y, size, size, "", icon, type.getHoverKey());
        }
        else
        {
            button = new ButtonGeneric(xRight, y, -1, true, type.getDisplayName(), type.getHoverKey());
        }

        String hover = type.getHoverKey();

        if (org.apache.commons.lang3.StringUtils.isBlank(hover) == false)
        {
            button.setHoverStrings(hover);
        }

        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth() + 1;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0x50FFFFFF);
        }

        boolean modified = this.schematic.getMetadata().wasModifiedSinceSaved();
        String schematicName = this.schematic.getMetadata().getName();
        int color = modified ? 0xFFFF9010 : 0xFFFFFFFF;
        this.drawString(this.x + 20, this.y + 7, color, schematicName);

        GlStateManager.disableBlend();

        IGuiIcon icon;

        if (this.schematic.getFile() != null)
        {
            icon = this.schematic.getType().getIcon();
        }
        else
        {
            icon = LitematicaGuiIcons.SCHEMATIC_TYPE_MEMORY;
        }

        icon.renderAt(this.typeIconX, this.typeIconY, this.zLevel, false, false);

        if (modified)
        {
            LitematicaGuiIcons.NOTICE_EXCLAMATION_11.renderAt(this.buttonsStartX - 13, this.y + 6, this.zLevel, false, false);
        }

        this.drawSubWidgets(mouseX, mouseY);

        RenderUtils.disableItemLighting();
        GlStateManager.disableLighting();
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        if (this.schematic.getMetadata().wasModifiedSinceSaved() &&
            GuiBase.isMouseOver(mouseX, mouseY, this.buttonsStartX - 13, this.y + 6, 11, 11))
        {
            String str = WidgetFileBrowserBase.DATE_FORMAT.format(new Date(this.schematic.getMetadata().getTimeModified()));
            RenderUtils.drawHoverText(mouseX, mouseY, ImmutableList.of(StringUtils.translate("litematica.gui.label.loaded_schematic.modified_on", str)));
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 12, this.height))
        {
            List<String> lines = new ArrayList<>();
            File schematicFile = this.schematic.getFile();
            String fileName = schematicFile != null ? schematicFile.getName() : StringUtils.translate("litematica.gui.label.schematic_placement.hover.in_memory");

            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_name", this.schematic.getMetadata().getName()));
            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));
            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_type", this.schematic.getType().getDisplayName()));

            RenderUtils.drawHoverText(mouseX, mouseY, lines);
        }

        RenderUtils.color(1f, 1f, 1f, 1f);

        super.postRenderHovered(mouseX, mouseY, selected);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final WidgetSchematicEntry widget;

        public ButtonListener(Type type, WidgetSchematicEntry widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            if (this.type == Type.CREATE_PLACEMENT)
            {
                BlockPos pos = new BlockPos(this.widget.mc.player.getPositionVector());
                ISchematic entry = this.widget.schematic;
                String name = entry.getMetadata().getName();
                boolean enabled = GuiBase.isShiftDown() == false;

                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                SchematicPlacement placement = SchematicPlacement.createFor(entry, pos, name, enabled, enabled);
                manager.addSchematicPlacement(placement, true);
                manager.setSelectedSchematicPlacement(placement);
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                ISchematic schematic = this.widget.schematic;
                String name = schematic.getFile() != null ? schematic.getFile().getName() : schematic.getMetadata().getName();

                if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue() && schematic.getFile() == null)
                {
                    name = FileUtils.generateSimpleSafeFileName(name);
                }

                GuiSchematicSaveConvert gui = new GuiSchematicSaveConvert(schematic, name);
                gui.setUpdatePlacementsOption(true);
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }
            else if (this.type == Type.RELOAD)
            {
                this.widget.schematic.readFromFile();
                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                manager.getAllPlacementsOfSchematic(this.widget.schematic).forEach((placement) -> { manager.markChunksForRebuild(placement); } );
            }
            else if (this.type == Type.UNLOAD)
            {
                SchematicHolder.getInstance().removeSchematic(this.widget.schematic);
                this.widget.parent.refreshEntries();
            }
        }

        public enum Type
        {
            CREATE_PLACEMENT    (LitematicaGuiIcons.PLACEMENT,  "litematica.gui.button.create_placement",   "litematica.gui.button.hover.schematic_list.create_new_placement_shift_to_disable"),
            RELOAD              (LitematicaGuiIcons.RELOAD,     "litematica.gui.button.reload",             "litematica.gui.button.hover.schematic_list.reload_schematic"),
            SAVE_TO_FILE        (LitematicaGuiIcons.SAVE_DISK,  "litematica.gui.button.save_to_file",       "litematica.gui.button.hover.schematic_list.save_to_file"),
            UNLOAD              (LitematicaGuiIcons.TRASH_CAN,  "litematica.gui.button.unload",             "litematica.gui.button.hover.schematic_list.unload");

            private final IGuiIcon icon;
            private final String translationKey;
            @Nullable private final String hoverKey;

            private Type(IGuiIcon icon, String translationKey, @Nullable String hoverKey)
            {
                this.icon = icon;
                this.translationKey = translationKey;
                this.hoverKey = hoverKey;
            }

            public IGuiIcon getIcon()
            {
                return this.icon;
            }

            @Nullable
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
