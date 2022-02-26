package fi.dy.masa.litematica.gui.widget.list.entry;

import java.util.List;
import java.util.Locale;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.util.FileNameUtils;

public class SchematicEntryWidget extends BaseDataListEntryWidget<ISchematic>
{
    /*
    private final int typeIconX;
    private final int typeIconY;
    private final int buttonsStartX;
    */

    public SchematicEntryWidget(ISchematic schematic, DataListEntryWidgetData constructData)
    {
        super(schematic, constructData);

        // Note: These are placed from right to left

        /*
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
        this.typeIconX = x + 2;
        this.typeIconY = y + 4;
        */
    }

    /*
    private boolean useIconButtons()
    {
        return Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.getBooleanValue();
    }

    private int addButton(int x, int y, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        GenericButton button = new GenericButton(x, y, -1, true, type.getDisplayName());
        button.translateAndAddHoverString(type.getHoverKey());

        this.addButton(button, listener);

        return button.getWidth() + 1;
    }

    private int createButtonIconOnly(int xRight, int y, ButtonListener.Type type)
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
            button.translateAndAddHoverString(type.getHoverKey());
        }

        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth() + 1;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        int x = this.getX();
        int y = this.getY();
        int z = this.getZ();
        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered entry
        if (this.isMouseOver(mouseX, mouseY))
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x70FFFFFF);
        }
        else if (this.isOdd)
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x20FFFFFF);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0x50FFFFFF);
        }

        boolean modified = this.schematic.getMetadata().wasModifiedSinceSaved();
        String schematicName = this.schematic.getMetadata().getName();
        int color = modified ? 0xFFFF9010 : 0xFFFFFFFF;
        this.drawString(x + 20, y + 7, color, schematicName);

        GlStateManager.disableBlend();

        Icon icon;

        if (this.schematic.getFile() != null)
        {
            icon = this.schematic.getType().getIcon();
        }
        else
        {
            icon = LitematicaIcons.SCHEMATIC_TYPE_MEMORY;
        }

        icon.renderAt(this.typeIconX, this.typeIconY, z + 0.1f, false, false);

        if (modified)
        {
            LitematicaIcons.NOTICE_EXCLAMATION_11.renderAt(this.buttonsStartX - 13, y + 6, z + 0.1f, false, false);
        }

        this.renderSubWidgets(mouseX, mouseY, isActiveGui, hoveredWidgetId);

        RenderUtils.disableItemLighting();
        GlStateManager.disableLighting();
        RenderUtils.color(1f, 1f, 1f, 1f);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        int x = this.getX();
        int y = this.getY();
        int z = this.getZ() + 1;
        int height = this.getHeight();

        if (this.schematic.getMetadata().wasModifiedSinceSaved() &&
            BaseScreen.isMouseOver(mouseX, mouseY, this.buttonsStartX - 13, y + 6, 11, 11))
        {
            String str = BaseFileBrowserWidget.DATE_FORMAT.format(new Date(this.schematic.getMetadata().getTimeModified()));
            TextRenderUtils.renderHoverText(mouseX, mouseY, z, ImmutableList.of(StringUtils.translate("litematica.gui.label.loaded_schematic.modified_on", str)));
        }
        else if (BaseScreen.isMouseOver(mouseX, mouseY, x, y, this.buttonsStartX - 12, height))
        {
            List<String> lines = new ArrayList<>();
            File schematicFile = this.schematic.getFile();
            String fileName = schematicFile != null ? schematicFile.getName() : StringUtils.translate("litematica.gui.label.schematic_placement.hover.in_memory");

            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_name", this.schematic.getMetadata().getName()));
            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));
            lines.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_type", this.schematic.getType().getDisplayName()));

            TextRenderUtils.renderHoverText(mouseX, mouseY, z, lines);
        }

        super.postRenderHovered(mouseX, mouseY, isActiveGui, hoveredWidgetId);

        RenderUtils.color(1f, 1f, 1f, 1f);
    }
    */

    public static boolean schematicSearchFilter(ISchematic entry, List<String> searchTerms)
    {
        String fileName = null;

        if (entry.getFile() != null)
        {
            fileName = entry.getFile().getName().toLowerCase(Locale.ROOT);
            fileName = FileNameUtils.getFileNameWithoutExtension(fileName);
        }

        for (String searchTerm : searchTerms)
        {
            if (entry.getMetadata().getName().toLowerCase(Locale.ROOT).contains(searchTerm))
            {
                return true;
            }

            if (fileName != null && fileName.contains(searchTerm))
            {
                return true;
            }
        }

        return false;
    }

    /*
    private static class ButtonListener implements ButtonActionListener
    {
        private final Type type;
        private final SchematicEntryWidget widget;

        public ButtonListener(Type type, SchematicEntryWidget widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            if (this.type == Type.CREATE_PLACEMENT)
            {
                BlockPos pos = new BlockPos(this.widget.mc.player.getPositionVector());
                ISchematic entry = this.widget.schematic;
                String name = entry.getMetadata().getName();
                boolean enabled = BaseScreen.isShiftDown() == false;

                SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                SchematicPlacement placement = SchematicPlacement.createFor(entry, pos, name, enabled);
                manager.addSchematicPlacement(placement, true);
                manager.setSelectedSchematicPlacement(placement);
            }
            else if (this.type == Type.SAVE_TO_FILE)
            {
                ISchematic schematic = this.widget.schematic;
                String name = schematic.getFile() != null ? schematic.getFile().getName() : schematic.getMetadata().getName();

                if (Configs.Generic.GENERATE_LOWERCASE_NAMES.getBooleanValue() && schematic.getFile() == null)
                {
                    name = FileNameUtils.generateSimpleSafeFileName(name);
                }

                GuiSchematicSaveConvert gui = new GuiSchematicSaveConvert(schematic, name);
                gui.setUpdatePlacementsOption(true);
                gui.setParent(GuiUtils.getCurrentScreen());
                BaseScreen.openScreen(gui);
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
            CREATE_PLACEMENT    (LitematicaIcons.PLACEMENT, "litematica.gui.button.create_placement", "litematica.gui.button.hover.schematic_list.create_new_placement_shift_to_disable"),
            RELOAD              (LitematicaIcons.RELOAD, "litematica.gui.button.reload", "litematica.gui.button.hover.schematic_list.reload_schematic"),
            SAVE_TO_FILE        (LitematicaIcons.SAVE_DISK, "litematica.gui.button.save_to_file", "litematica.gui.button.hover.schematic_list.save_to_file"),
            UNLOAD              (LitematicaIcons.TRASH_CAN, "litematica.gui.button.unload", "litematica.gui.button.hover.schematic_list.unload");

            private final Icon icon;
            private final String translationKey;
            @Nullable
            private final String hoverKey;

            private Type(Icon icon, String translationKey, @Nullable String hoverKey)
            {
                this.icon = icon;
                this.translationKey = translationKey;
                this.hoverKey = hoverKey;
            }

            public Icon getIcon()
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
    */
}
