package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import com.mumfrey.liteloader.core.LiteLoader;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.button.ButtonOnOff;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetInfoIcon;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList>
{
    private final MaterialListBase materialList;
    private int id;

    public GuiMaterialList(MaterialListBase materialList)
    {
        super(10, 44);

        this.materialList = materialList;
        this.title = this.materialList.getTitle();
        this.useTitleHierarchy = false;

        Minecraft mc = Minecraft.getMinecraft();

        MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), mc.player);
        WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), mc);

        // Remember the last opened material list, for the hotkey
        if (DataManager.getMaterialList() == null)
        {
            DataManager.setMaterialList(materialList);
        }
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = 24;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        String str = I18n.format("litematica.gui.label.material_list.multiplier", this.materialList.getMultiplier());
        int w = this.fontRenderer.getStringWidth(str) + 6;
        this.addLabel(this.width - w - 6, y + 4, w, 12, 0xFFFFFFFF, str);
        this.createButton(this.width - w - 26, y + 2, -1, ButtonListener.Type.CHANGE_MULTIPLIER);

        this.addWidget(new WidgetInfoIcon(this.width - 23, 12, this.zLevel, Icons.INFO_11, "litematica.info.material_list"));

        int gap = 2;
        x += this.createButton(x, y, -1, ButtonListener.Type.REFRESH_LIST) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.LIST_TYPE) + gap;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHideAvailable(), ButtonListener.Type.HIDE_AVAILABLE) + gap;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHudRenderer().getShouldRender(), ButtonListener.Type.TOGGLE_INFO_HUD) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.CLEAR_IGNORED) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.CLEAR_CACHE) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.WRITE_TO_FILE) + gap;
        y += 22;

        y = this.height - 36;
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        // Progress: Done xx % / Missing xx % / Wrong xx %
        long total = this.materialList.getCountTotal();
        long missing = this.materialList.getCountMissing() - this.materialList.getCountMismatched();
        long mismatch = this.materialList.getCountMismatched();

        if (total != 0)
        {
            double pctDone = ((double) (total - (missing + mismatch)) / (double) total) * 100;
            double pctMissing = ((double) missing / (double) total) * 100;
            double pctMismatch = ((double) mismatch / (double) total) * 100;
            String strp;

            if (missing == 0 && mismatch == 0)
            {
                strp = I18n.format("litematica.gui.label.material_list.progress.done", String.format("%.0f %%%%", pctDone));
            }
            else
            {
                String str1 = I18n.format("litematica.gui.label.material_list.progress.done", String.format("%.1f %%%%", pctDone));
                String str2 = I18n.format("litematica.gui.label.material_list.progress.missing", String.format("%.1f %%%%", pctMissing));
                String str3 = I18n.format("litematica.gui.label.material_list.progress.mismatch", String.format("%.1f %%%%", pctMismatch));
                strp = String.format("%s / %s / %s", str1, str2, str3);
            }

            str = I18n.format("litematica.gui.label.material_list.progress", strp);
            w = this.fontRenderer.getStringWidth(str);
            this.addLabel(12, this.height - 30, w, 12, 0xFFFFFFFF, str);
        }
    }

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        String label = "";

        if (type == ButtonListener.Type.LIST_TYPE)
        {
            label = type.getDisplayName(this.materialList.getMaterialListType().getDisplayName());
        }
        else if (type == ButtonListener.Type.CHANGE_MULTIPLIER)
        {
            String hover = I18n.format("litematica.gui.button.hover.plus_minus_tip");
            ButtonGeneric button = new ButtonGeneric(0, x, y, Icons.BUTTON_PLUS_MINUS_16, hover);
            this.addButton(button, listener);
            return button.getButtonWidth();
        }
        else
        {
            label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);

        if (type == ButtonListener.Type.CLEAR_CACHE)
        {
            button.setHoverStrings("litematica.gui.button.hover.material_list.clear_cache");
        }

        this.addButton(button, listener);

        return width;
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        ButtonOnOff button = ButtonOnOff.create(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));
        return button.getButtonWidth();
    }

    public MaterialListBase getMaterialList()
    {
        return this.materialList;
    }

    @Override
    protected WidgetListMaterialList createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialList(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMaterialList parent;
        private final Type type;

        public ButtonListener(Type type, GuiMaterialList parent)
        {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            MaterialListBase materialList = this.parent.materialList;

            switch (this.type)
            {
                case REFRESH_LIST:
                    materialList.recreateMaterialList();
                    WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), this.parent.mc);
                    break;

                case LIST_TYPE:
                    BlockInfoListType type = materialList.getMaterialListType();
                    materialList.setMaterialListType((BlockInfoListType) type.cycle(mouseButton == 0));
                    materialList.recreateMaterialList();
                    break;

                case HIDE_AVAILABLE:
                    materialList.setHideAvailable(! materialList.getHideAvailable());
                    materialList.refreshPreFilteredList();
                    materialList.recreateFilteredList();
                    break;

                case TOGGLE_INFO_HUD:
                    MaterialListHudRenderer renderer = materialList.getHudRenderer();
                    renderer.toggleShouldRender();

                    if (materialList.getHudRenderer().getShouldRender())
                    {
                        InfoHud.getInstance().addInfoHudRenderer(renderer, true);
                    }
                    else
                    {
                        InfoHud.getInstance().removeInfoHudRenderersOfType(renderer.getClass(), true);
                    }

                    break;

                case CLEAR_IGNORED:
                    materialList.clearIgnored();
                    break;

                case CLEAR_CACHE:
                    MaterialCache.getInstance().clearCache();
                    this.parent.addGuiMessage(MessageType.SUCCESS, "litematica.message.material_list.material_cache_cleared", 3000);
                    break;

                case WRITE_TO_FILE:
                    File dir = new File(LiteLoader.getCommonConfigFolder(), Reference.MOD_ID);
                    File file = DataDump.dumpDataToFile(dir, "material_list", this.getMaterialListDump(materialList).getLines());

                    if (file != null)
                    {
                        String key = "litematica.message.material_list_written_to_file";
                        this.parent.addMessage(MessageType.SUCCESS, key, file.getName());
                        StringUtils.sendOpenFileChatMessage(this.parent.mc.player, key, file);
                    }
                    break;

                case CHANGE_MULTIPLIER:
                {
                    int amount = mouseButton == 1 ? -1 : 1;
                    if (GuiScreen.isShiftKeyDown()) { amount *= 8; }
                    if (GuiScreen.isAltKeyDown()) { amount *= 4; }
                    materialList.setMultiplier(materialList.getMultiplier() + amount);
                    break;
                }
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        private DataDump getMaterialListDump(MaterialListBase materialList)
        {
            DataDump dump = new DataDump(4, DataDump.Format.ASCII);
            int multiplier = materialList.getMultiplier();

            ArrayList<MaterialListEntry> list = new ArrayList<>();
            list.addAll(materialList.getMaterialsFiltered(false));
            Collections.sort(list, new MaterialListSorter(materialList));

            for (MaterialListEntry entry : list)
            {
                int total = entry.getCountTotal() * multiplier;
                int missing = entry.getCountMissing();
                int available = entry.getCountAvailable();
                dump.addData(entry.getStack().getDisplayName(), String.valueOf(total), String.valueOf(missing), String.valueOf(available));
            }

            String titleTotal = multiplier > 1 ? String.format("Total (x%d)", multiplier) : "Total";
            dump.addTitle("Item", titleTotal, "Missing", "Available");
            dump.addHeader(materialList.getTitle());
            dump.setColumnProperties(1, DataDump.Alignment.RIGHT, true); // total
            dump.setColumnProperties(2, DataDump.Alignment.RIGHT, true); // missing
            dump.setColumnProperties(3, DataDump.Alignment.RIGHT, true); // available
            dump.setSort(false);
            dump.setUseColumnSeparator(true);

            return dump;
        }

        public enum Type
        {
            REFRESH_LIST        ("litematica.gui.button.material_list.refresh_list"),
            LIST_TYPE           ("litematica.gui.button.material_list.list_type"),
            HIDE_AVAILABLE      ("litematica.gui.button.material_list.hide_available"),
            TOGGLE_INFO_HUD     ("litematica.gui.button.material_list.toggle_info_hud"),
            CLEAR_IGNORED       ("litematica.gui.button.material_list.clear_ignored"),
            CLEAR_CACHE         ("litematica.gui.button.material_list.clear_cache"),
            WRITE_TO_FILE       ("litematica.gui.button.material_list.write_to_file"),
            CHANGE_MULTIPLIER   ("");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
        }
    }
}
