package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicEntry extends WidgetListEntryBase<LitematicaSchematic>
{
    private final WidgetListLoadedSchematics parent;
    private final LitematicaSchematic schematic;
    private final int typeIconX;
    private final int typeIconY;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSchematicEntry(int x, int y, int width, int height, boolean isOdd,
            LitematicaSchematic schematic, int listIndex, WidgetListLoadedSchematics parent)
    {
        super(x, y, width, height, schematic, listIndex);

        this.parent = parent;
        this.schematic = schematic;
        this.isOdd = isOdd;
        y += 1;

        int posX = x + width;

        posX -= this.addButton(posX, y, ButtonListener.Type.UNLOAD);
        posX -= this.addButton(posX, y, ButtonListener.Type.RELOAD);
        posX -= this.addButton(posX, y, ButtonListener.Type.SAVE_TO_FILE);
        posX -= this.addButton(posX, y, ButtonListener.Type.CREATE_PLACEMENT);

        this.buttonsStartX = posX;
        this.typeIconX = this.x + 2;
        this.typeIconY = y + 4;
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

        return button.getWidth() + 2;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
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
        this.drawString(this.x + 20, this.y + 7, color, schematicName, drawContext);

        RenderUtils.color(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        File schematicFile = this.schematic.getFile();
        String fileName = schematicFile != null ? schematicFile.getName() : null;
        this.parent.bindTexture(Icons.TEXTURE);

        Icons icon;

        if (fileName != null)
        {
            icon = Icons.SCHEMATIC_TYPE_FILE;
        }
        else
        {
            icon = Icons.SCHEMATIC_TYPE_MEMORY;
        }

        icon.renderAt(this.typeIconX, this.typeIconY, this.zLevel, false, false);

        if (modified)
        {
            Icons.NOTICE_EXCLAMATION_11.renderAt(this.buttonsStartX - 13, this.y + 6, this.zLevel, false, false);
        }

        this.drawSubWidgets(mouseX, mouseY, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        if (this.schematic.getMetadata().wasModifiedSinceSaved() &&
            GuiBase.isMouseOver(mouseX, mouseY, this.buttonsStartX - 13, this.y + 6, 11, 11))
        {
            String str = WidgetFileBrowserBase.DATE_FORMAT.format(new Date(this.schematic.getMetadata().getTimeModified()));
            List<String> strs = ImmutableList.of(StringUtils.translate("litematica.gui.label.loaded_schematic.modified_on", str));
            RenderUtils.drawHoverText(mouseX, mouseY, strs, drawContext);
        }
        else if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - 12, this.height))
        {
            List<String> lines = new ArrayList<>();
            File schematicFile = this.schematic.getFile();
            String fileName = schematicFile != null ? schematicFile.getName() : null;

            if (fileName != null)
            {
                lines.add(fileName);
            }
            else
            {
                lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.in_memory"));
            }

            RenderUtils.drawHoverText(mouseX, mouseY, lines, drawContext);
        }

        RenderUtils.color(1f, 1f, 1f, 1f);

        super.postRenderHovered(mouseX, mouseY, selected, drawContext);
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
                BlockPos pos = BlockPos.ofFloored(this.widget.mc.player.getPos());
                LitematicaSchematic entry = this.widget.schematic;
                String name = entry.getMetadata().getName();
                boolean enabled = GuiBase.isShiftDown() == false;

                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                SchematicPlacement placement = SchematicPlacement.createFor(entry, pos, name, enabled, enabled);
                manager.addSchematicPlacement(placement, true);
                manager.setSelectedSchematicPlacement(placement);
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                LitematicaSchematic entry = this.widget.schematic;
                GuiSchematicSave gui = new GuiSchematicSave(entry);
                gui.setParent(GuiUtils.getCurrentScreen());
                GuiBase.openGui(gui);
            }
            else if (this.type == Type.RELOAD)
            {
                this.widget.schematic.readFromFile();
                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                manager.getAllPlacementsOfSchematic(this.widget.schematic).forEach(manager::markChunksForRebuild);
            }
            else if (this.type == Type.UNLOAD)
            {
                SchematicHolder.getInstance().removeSchematic(this.widget.schematic);
                this.widget.parent.refreshEntries();
            }
        }

        public enum Type
        {
            CREATE_PLACEMENT    ("litematica.gui.button.create_placement"),
            RELOAD              ("litematica.gui.button.reload", "litematica.gui.button.hover.schematic_list.reload_schematic"),
            SAVE_TO_FILE        ("litematica.gui.button.save_to_file"),
            UNLOAD              ("litematica.gui.button.unload");

            private final String translationKey;
            @Nullable private final String hoverKey;

            Type(String translationKey)
            {
                this(translationKey, null);
            }

            Type(String translationKey, @Nullable String hoverKey)
            {
                this.translationKey = translationKey;
                this.hoverKey = hoverKey;
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
