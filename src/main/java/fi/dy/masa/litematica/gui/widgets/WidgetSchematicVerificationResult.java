package fi.dy.masa.litematica.gui.widgets;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.LitematicaIcons;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.SortCriteria;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.list.entry.SortableListEntryWidget;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.render.TextRenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicVerificationResult extends SortableListEntryWidget<BlockMismatchEntry>
{
    public static final String HEADER_EXPECTED = "litematica.gui.label.schematic_verifier.expected";
    public static final String HEADER_FOUND = "litematica.gui.label.schematic_verifier.found";
    public static final String HEADER_COUNT = "litematica.gui.label.schematic_verifier.count";

    private static int maxNameLengthExpected;
    private static int maxNameLengthFound;
    private static int maxCountLength;

    private final BlockModelShapes blockModelShapes;
    private final GuiSchematicVerifier guiSchematicVerifier;
    private final WidgetListSchematicVerificationResults listWidget;
    private final SchematicVerifier verifier;
    private final BlockMismatchEntry mismatchEntry;
    @Nullable private final String header1;
    @Nullable private final String header2;
    @Nullable private final String header3;
    @Nullable private final BlockMismatchInfo mismatchInfo;
    private final int count;
    private final boolean isOdd;
    @Nullable private final GenericButton buttonIgnore;

    public WidgetSchematicVerificationResult(int x, int y, int width, int height, boolean isOdd,
            WidgetListSchematicVerificationResults listWidget, GuiSchematicVerifier guiSchematicVerifier,
            BlockMismatchEntry entry, int listIndex)
    {
        super(x, y, width, height, entry, listIndex);

        this.columnCount = 3;
        this.blockModelShapes = this.mc.getBlockRendererDispatcher().getBlockModelShapes();
        this.mismatchEntry = entry;
        this.guiSchematicVerifier = guiSchematicVerifier;
        this.listWidget = listWidget;
        this.verifier = guiSchematicVerifier.getPlacement().getSchematicVerifier();
        this.isOdd = isOdd;

        // Main header
        if (entry.header1 != null && entry.header2 != null)
        {
            this.header1 = entry.header1;
            this.header2 = entry.header2;
            this.header3 = BaseScreen.TXT_BOLD + StringUtils.translate(HEADER_COUNT) + BaseScreen.TXT_RST;
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        // Category title
        else if (entry.header1 != null)
        {
            this.header1 = entry.header1;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = null;
            this.count = 0;
            this.buttonIgnore = null;
        }
        // Mismatch entry
        else
        {
            this.header1 = null;
            this.header2 = null;
            this.header3 = null;
            this.mismatchInfo = new BlockMismatchInfo(entry.blockMismatch.stateExpected, entry.blockMismatch.stateFound);
            this.count = entry.blockMismatch.count;

            if (entry.mismatchType != MismatchType.CORRECT_STATE)
            {
                this.buttonIgnore = this.createButton(x + width, y + 1, ButtonListener.ButtonType.IGNORE_MISMATCH);
            }
            else
            {
                this.buttonIgnore = null;
            }
        }
    }

    public static void setMaxNameLengths(List<BlockMismatch> mismatches)
    {
        maxNameLengthExpected = StringUtils.getStringWidth(BaseScreen.TXT_BOLD + StringUtils.translate(HEADER_EXPECTED) + BaseScreen.TXT_RST);
        maxNameLengthFound    = StringUtils.getStringWidth(BaseScreen.TXT_BOLD + StringUtils.translate(HEADER_FOUND) + BaseScreen.TXT_RST);
        maxCountLength = 7 * StringUtils.getStringWidth("8");

        for (BlockMismatch entry : mismatches)
        {
            ItemStack stack = ItemUtils.getItemForState(entry.stateExpected);
            String name = BlockMismatchInfo.getDisplayName(entry.stateExpected, stack);
            maxNameLengthExpected = Math.max(maxNameLengthExpected, StringUtils.getStringWidth(name));

            stack = ItemUtils.getItemForState(entry.stateFound);
            name = BlockMismatchInfo.getDisplayName(entry.stateFound, stack);
            maxNameLengthFound = Math.max(maxNameLengthFound, StringUtils.getStringWidth(name));
        }

        maxCountLength = Math.max(maxCountLength, StringUtils.getStringWidth(BaseScreen.TXT_BOLD + StringUtils.translate(HEADER_COUNT) + BaseScreen.TXT_RST));
    }

    private GenericButton createButton(int x, int y, ButtonListener.ButtonType type)
    {
        GenericButton button = new GenericButton(x, y, -1, true, type.getDisplayName());
        return this.addButton(button, new ButtonListener(type, this.mismatchEntry, this.guiSchematicVerifier));
    }

    @Override
    protected int getCurrentSortColumn()
    {
        return this.verifier.getSortCriteria().ordinal();
    }

    @Override
    protected boolean getSortInReverse()
    {
        return this.verifier.getSortInReverse();
    }

    @Override
    protected int getColumnPosX(int column)
    {
        int x1 = this.getX() + 4;
        int x2 = x1 + maxNameLengthExpected + 40; // including item icon
        int x3 = x2 + maxNameLengthFound + 40;

        switch (column)
        {
            case 0: return x1;
            case 1: return x2;
            case 2: return x3;
            case 3: return x3 + maxCountLength + 20;
            default: return x1;
        }
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        if (this.mismatchEntry.type != BlockMismatchEntry.Type.HEADER)
        {
            return false;
        }

        int column = this.getMouseOverColumn(mouseX, mouseY);

        switch (column)
        {
            case 0:
                this.verifier.setSortCriteria(SortCriteria.NAME_EXPECTED);
                break;
            case 1:
                this.verifier.setSortCriteria(SortCriteria.NAME_FOUND);
                break;
            case 2:
                this.verifier.setSortCriteria(SortCriteria.COUNT);
                break;
            default:
                return false;
        }

        // Re-create the widgets
        this.listWidget.refreshEntries();

        return true;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return (this.buttonIgnore == null || mouseX < this.buttonIgnore.getX()) && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    protected boolean shouldRenderAsSelected()
    {
        if (this.mismatchEntry.type == BlockMismatchEntry.Type.CATEGORY_TITLE)
        {
            return this.verifier.isMismatchCategorySelected(this.mismatchEntry.mismatchType);
        }
        else if (this.mismatchEntry.type == BlockMismatchEntry.Type.DATA)
        {
            return this.verifier.isMismatchEntrySelected(this.mismatchEntry.blockMismatch);
        }

        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId, boolean selected)
    {
        selected = this.shouldRenderAsSelected();

        // Default color for even entries
        int color = 0xA0303030;

        // Draw a lighter background for the hovered and the selected entry
        if (selected)
        {
            color = 0xA0707070;
        }
        else if (isActiveGui && this.getId() == hoveredWidgetId)
        {
            color = 0xA0505050;
        }
        // Draw a slightly darker background for odd entries
        else if (this.isOdd)
        {
            color = 0xA0101010;
        }

        int x = this.getX();
        int y = this.getY();
        int z = this.getZ();
        int width = this.getWidth();
        int height = this.getHeight();

        ShapeRenderUtils.renderRectangle(x, y, z, width, height, color);

        if (selected)
        {
            ShapeRenderUtils.renderOutline(x, y, z, width, height, 1, 0xFFE0E0E0);
        }

        int x1 = this.getColumnPosX(0);
        int x2 = this.getColumnPosX(1);
        int x3 = this.getColumnPosX(2);
        y = this.getY() + 7;
        color = 0xFFFFFFFF;

        if (this.header1 != null && this.header2 != null)
        {
            this.drawString(x1, y, color, this.header1);
            this.drawString(x2, y, color, this.header2);
            this.drawString(x3, y, color, this.header3);

            this.renderColumnHeader(mouseX, mouseY, LitematicaIcons.ARROW_DOWN, LitematicaIcons.ARROW_UP);
        }
        else if (this.header1 != null)
        {
            this.drawString(x + 4, y, color, this.header1);
        }
        else if (this.mismatchInfo != null &&
                (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE ||
                 this.mismatchEntry.blockMismatch.stateExpected.getBlock() != Blocks.AIR)) 
        {
            this.drawString(x1 + 20, y, color, this.mismatchInfo.nameExpected);

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                this.drawString(x2 + 20, y, color, this.mismatchInfo.nameFound);
            }

            this.drawString(x3, y, color, String.valueOf(this.count));

            y = this.getY() + 3;
            ShapeRenderUtils.renderRectangle(x1, y, z, 16, 16, 0x20FFFFFF); // light background for the item

            boolean useBlockModelConfig = Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue();
            boolean hasModelExpected = this.mismatchInfo.stateExpected.getRenderType() == EnumBlockRenderType.MODEL;
            boolean hasModelFound    = this.mismatchInfo.stateFound.getRenderType() == EnumBlockRenderType.MODEL;
            boolean isAirItemExpected = this.mismatchInfo.stackExpected.isEmpty();
            boolean isAirItemFound    = this.mismatchInfo.stackFound.isEmpty();
            boolean useBlockModelExpected = hasModelExpected && (isAirItemExpected || useBlockModelConfig || this.mismatchInfo.stateExpected.getBlock() == Blocks.FLOWER_POT);
            boolean useBlockModelFound    = hasModelFound    && (isAirItemFound    || useBlockModelConfig || this.mismatchInfo.stateFound.getBlock() == Blocks.FLOWER_POT);

            GlStateManager.pushMatrix();
            RenderUtils.enableGuiItemLighting();

            IBakedModel model;
            float origZ = this.mc.getRenderItem().zLevel;
            this.mc.getRenderItem().zLevel = z + 1;

            if (useBlockModelExpected)
            {
                model = this.blockModelShapes.getModelForState(this.mismatchInfo.stateExpected);
                RenderUtils.renderModelInGui(x1, y, z + 1, model, this.mismatchInfo.stateExpected);
            }
            else
            {
                this.mc.getRenderItem().renderItemAndEffectIntoGUI(this.mc.player, this.mismatchInfo.stackExpected, x1, y);
                this.mc.getRenderItem().renderItemOverlayIntoGUI(this.textRenderer, this.mismatchInfo.stackExpected, x1, y, null);
            }

            if (this.mismatchEntry.mismatchType != MismatchType.CORRECT_STATE)
            {
                ShapeRenderUtils.renderRectangle(x2, y, z, 16, 16, 0x20FFFFFF); // light background for the item

                if (useBlockModelFound)
                {
                    model = this.blockModelShapes.getModelForState(this.mismatchInfo.stateFound);
                    RenderUtils.renderModelInGui(x2, y, z + 1, model, this.mismatchInfo.stateFound);
                }
                else
                {
                    this.mc.getRenderItem().renderItemAndEffectIntoGUI(this.mc.player, this.mismatchInfo.stackFound, x2, y);
                    this.mc.getRenderItem().renderItemOverlayIntoGUI(this.textRenderer, this.mismatchInfo.stackFound, x2, y, null);
                }
            }

            this.mc.getRenderItem().zLevel = origZ;

            RenderUtils.disableItemLighting();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        super.render(mouseX, mouseY, isActiveGui, hoveredWidgetId, selected);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        if (this.mismatchInfo != null && this.buttonIgnore != null && mouseX < this.buttonIgnore.getX())
        {
            int x = mouseX + 10;
            int y = mouseY;
            int width = this.mismatchInfo.getTotalWidth();
            int height = this.mismatchInfo.getTotalHeight();

            if (x + width > GuiUtils.getCurrentScreen().width)
            {
                x = mouseX - width - 10;
            }

            if (y + height > GuiUtils.getCurrentScreen().height)
            {
                y = mouseY - height - 2;
            }

            this.mismatchInfo.render(x, y, this.getZ() + 1, this.mc);
        }
    }

    public static class BlockMismatchInfo
    {
        private final IBlockState stateExpected;
        private final IBlockState stateFound;
        private final ItemStack stackExpected;
        private final ItemStack stackFound;
        private final String blockRegistrynameExpected;
        private final String blockRegistrynameFound;
        private final String nameExpected;
        private final String nameFound;
        private final int totalWidth;
        private final int totalHeight;
        private final int columnWidthExpected;

        public BlockMismatchInfo(IBlockState stateExpected, IBlockState stateFound)
        {
            this.stateExpected = stateExpected;
            this.stateFound = stateFound;

            this.stackExpected = ItemUtils.getItemForState(this.stateExpected);
            this.stackFound = ItemUtils.getItemForState(this.stateFound);

            Block blockExpected = this.stateExpected.getBlock();
            Block blockFound = this.stateFound.getBlock();
            ResourceLocation rl1 = Block.REGISTRY.getNameForObject(blockExpected);
            ResourceLocation rl2 = Block.REGISTRY.getNameForObject(blockFound);

            this.blockRegistrynameExpected = rl1 != null ? rl1.toString() : "<null>";
            this.blockRegistrynameFound = rl2 != null ? rl2.toString() : "<null>";

            this.nameExpected = getDisplayName(stateExpected, this.stackExpected);
            this.nameFound =    getDisplayName(stateFound,    this.stackFound);

            List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected, " = ");
            List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound, " = ");

            int w1 = Math.max(StringUtils.getStringWidth(this.nameExpected) + 20, StringUtils.getStringWidth(this.blockRegistrynameExpected));
            int w2 = Math.max(StringUtils.getStringWidth(this.nameFound) + 20, StringUtils.getStringWidth(this.blockRegistrynameFound));
            w1 = Math.max(w1, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsExpected));
            w2 = Math.max(w2, fi.dy.masa.litematica.render.RenderUtils.getMaxStringRenderLength(propsFound));

            this.columnWidthExpected = w1;
            this.totalWidth = this.columnWidthExpected + w2 + 40;
            this.totalHeight = Math.max(propsExpected.size(), propsFound.size()) * (StringUtils.getFontHeight() + 2) + 50;
        }

        public static String getDisplayName(IBlockState state, ItemStack stack)
        {
            Block block = state.getBlock();
            String key = block.getTranslationKey() + ".name";
            String name = StringUtils.translate(key);
            name = key.equals(name) == false ? name : stack.getDisplayName();

            if (block == Blocks.FLOWER_POT && state.getValue(BlockFlowerPot.CONTENTS) != BlockFlowerPot.EnumFlowerType.EMPTY)
            {
                name = ((new ItemStack(Items.FLOWER_POT)).getDisplayName()) + " & " + name;
            }

            return name;
        }

        public int getTotalWidth()
        {
            return this.totalWidth;
        }

        public int getTotalHeight()
        {
            return this.totalHeight;
        }

        public void render(int x, int y, int zLevel, Minecraft mc)
        {
            if (this.stateExpected != null && this.stateFound != null)
            {
                GlStateManager.pushMatrix();

                int x1 = x + 10;
                int x2 = x + this.columnWidthExpected + 30;

                ShapeRenderUtils.renderOutlinedRectangle(x, y, zLevel, this.totalWidth, this.totalHeight, 0xFF000000, BaseScreen.COLOR_HORIZONTAL_BAR);
                ShapeRenderUtils.renderRectangle(x1, y + 16, zLevel, 16, 16, 0x50C0C0C0); // light background for the item
                ShapeRenderUtils.renderRectangle(x2, y + 16, zLevel, 16, 16, 0x50C0C0C0); // light background for the item

                FontRenderer textRenderer = mc.fontRenderer;
                String pre = BaseScreen.TXT_WHITE + BaseScreen.TXT_BOLD;
                String strExpected = pre + StringUtils.translate("litematica.gui.label.schematic_verifier.expected") + BaseScreen.TXT_RST;
                String strFound =    pre + StringUtils.translate("litematica.gui.label.schematic_verifier.found") + BaseScreen.TXT_RST;

                GlStateManager.translate(0f, 0f, zLevel + 0.1f);

                y += 4;
                textRenderer.drawString(strExpected, x1, y, 0xFFFFFFFF);
                textRenderer.drawString(strFound,    x2, y, 0xFFFFFFFF);
                y += 12;

                GlStateManager.disableLighting();
                RenderUtils.enableGuiItemLighting();

                boolean useBlockModelConfig = Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue();
                boolean hasModelExpected = this.stateExpected.getRenderType() == EnumBlockRenderType.MODEL;
                boolean hasModelFound    = this.stateFound.getRenderType() == EnumBlockRenderType.MODEL;
                boolean isAirItemExpected = this.stackExpected.isEmpty();
                boolean isAirItemFound    = this.stackFound.isEmpty();
                boolean useBlockModelExpected = hasModelExpected && (isAirItemExpected || useBlockModelConfig || this.stateExpected.getBlock() == Blocks.FLOWER_POT);
                boolean useBlockModelFound    = hasModelFound    && (isAirItemFound    || useBlockModelConfig || this.stateFound.getBlock() == Blocks.FLOWER_POT);
                BlockModelShapes blockModelShapes = mc.getBlockRendererDispatcher().getBlockModelShapes();

                //mc.getRenderItem().zLevel += 100;

                IBakedModel model;

                if (useBlockModelExpected)
                {
                    model = blockModelShapes.getModelForState(this.stateExpected);
                    RenderUtils.renderModelInGui(x1, y, zLevel + 1, model, this.stateExpected);
                }
                else
                {
                    mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackExpected, x1, y);
                    mc.getRenderItem().renderItemOverlayIntoGUI(textRenderer, this.stackExpected, x1, y, null);
                }

                if (useBlockModelFound)
                {
                    model = blockModelShapes.getModelForState(this.stateFound);
                    RenderUtils.renderModelInGui(x2, y, zLevel + 1, model, this.stateFound);
                }
                else
                {
                    mc.getRenderItem().renderItemAndEffectIntoGUI(mc.player, this.stackFound, x2, y);
                    mc.getRenderItem().renderItemOverlayIntoGUI(textRenderer, this.stackFound, x2, y, null);
                }

                //mc.getRenderItem().zLevel -= 100;

                //GlStateManager.disableBlend();
                RenderUtils.disableItemLighting();

                textRenderer.drawString(this.nameExpected, x1 + 20, y + 4, 0xFFFFFFFF);
                textRenderer.drawString(this.nameFound,    x2 + 20, y + 4, 0xFFFFFFFF);

                y += 20;
                textRenderer.drawString(this.blockRegistrynameExpected, x1, y, 0xFF4060FF);
                textRenderer.drawString(this.blockRegistrynameFound,    x2, y, 0xFF4060FF);
                y += StringUtils.getFontHeight() + 4;

                List<String> propsExpected = BlockUtils.getFormattedBlockStateProperties(this.stateExpected, " = ");
                List<String> propsFound = BlockUtils.getFormattedBlockStateProperties(this.stateFound, " = ");
                TextRenderUtils.renderText(x1, y, 0xFFB0B0B0, propsExpected);
                TextRenderUtils.renderText(x2, y, 0xFFB0B0B0, propsFound);

                GlStateManager.popMatrix();
            }
        }
    }

    private static class ButtonListener implements ButtonActionListener
    {
        private final ButtonType type;
        private final GuiSchematicVerifier guiSchematicVerifier;
        private final BlockMismatchEntry mismatchEntry;

        public ButtonListener(ButtonType type, BlockMismatchEntry mismatchEntry, GuiSchematicVerifier guiSchematicVerifier)
        {
            this.type = type;
            this.mismatchEntry = mismatchEntry;
            this.guiSchematicVerifier = guiSchematicVerifier;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            if (this.type == ButtonType.IGNORE_MISMATCH)
            {
                this.guiSchematicVerifier.getPlacement().getSchematicVerifier().ignoreStateMismatch(this.mismatchEntry.blockMismatch);
                this.guiSchematicVerifier.initGui();
            }
        }

        public enum ButtonType
        {
            IGNORE_MISMATCH ("litematica.gui.button.schematic_verifier.ignore");

            private final String translationKey;

            private ButtonType(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.translationKey);
            }
        }
    }
}
