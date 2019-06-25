package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.materials.MaterialListAreaAnalyzer;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.data.DataDump;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldInteger;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetInfoIcon;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList>
                             implements ICompletionListener
{
    private final MaterialListBase materialList;

    public GuiMaterialList(MaterialListBase materialList)
    {
        super(10, 44);

        this.materialList = materialList;
        this.materialList.setCompletionListener(this);
        this.title = this.materialList.getTitle();
        this.useTitleHierarchy = false;

        MaterialListUtils.updateAvailableCounts(this.materialList.getMaterialsAll(), this.mc.player);
        WidgetMaterialListEntry.setMaxNameLength(materialList.getMaterialsAll(), materialList.getMultiplier());

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

        boolean isNarrow = this.width < this.getElementTotalWidth();

        int x = 12;
        int y = 24;
        int buttonWidth;
        String label;
        ButtonGeneric button;

        String str = StringUtils.translate("litematica.gui.label.material_list.multiplier");
        int w = this.getStringWidth(str);
        this.addLabel(this.width - w - 56, y + 5, w, 12, 0xFFFFFFFF, str);

        GuiTextFieldInteger tf = new GuiTextFieldInteger(this.width - 52, y + 2, 40, 16, this.textRenderer);
        tf.setText(String.valueOf(this.materialList.getMultiplier()));
        MultiplierListener listener = new MultiplierListener(this.materialList, this);
        this.addTextField(tf, listener);

        this.addWidget(new WidgetInfoIcon(this.width - 23, 10, Icons.INFO_11, "litematica.info.material_list"));

        int gap = 1;
        x += this.createButton(x, y, -1, ButtonListener.Type.REFRESH_LIST) + gap;

        if (this.materialList.supportsRenderLayers())
        {
            x += this.createButton(x, y, -1, ButtonListener.Type.LIST_TYPE) + gap;
        }

        x += this.createButtonOnOff(x, y, -1, this.materialList.getHideAvailable(), ButtonListener.Type.HIDE_AVAILABLE) + gap;
        x += this.createButtonOnOff(x, y, -1, this.materialList.getHudRenderer().getShouldRenderCustom(), ButtonListener.Type.TOGGLE_INFO_HUD) + gap;

        if (isNarrow)
        {
            x = 12;
            y = this.height - 22;
        }

        x += this.createButton(x, y, -1, ButtonListener.Type.CLEAR_IGNORED) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.CLEAR_CACHE) + gap;
        x += this.createButton(x, y, -1, ButtonListener.Type.WRITE_TO_FILE) + gap;
        y += 22;

        y = this.height - 36;
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = StringUtils.translate(type.getLabelKey());
        buttonWidth = this.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        // Progress: Done xx % / Missing xx % / Wrong xx %
        long total = this.materialList.getCountTotal();
        long missing = this.materialList.getCountMissing() - this.materialList.getCountMismatched();
        long mismatch = this.materialList.getCountMismatched();

        if (total != 0 && (this.materialList instanceof MaterialListAreaAnalyzer) == false)
        {
            double pctDone = ((double) (total - (missing + mismatch)) / (double) total) * 100;
            double pctMissing = ((double) missing / (double) total) * 100;
            double pctMismatch = ((double) mismatch / (double) total) * 100;
            String strp;
            String strt = StringUtils.translate("litematica.gui.label.material_list.total", total);

            if (missing == 0 && mismatch == 0)
            {
                strp = StringUtils.translate("litematica.gui.label.material_list.progress.done", String.format("%.0f %%%%", pctDone));
            }
            else
            {
                String str1 = StringUtils.translate("litematica.gui.label.material_list.progress.done", String.format("%.1f %%%%", pctDone));
                String str2 = StringUtils.translate("litematica.gui.label.material_list.progress.missing", String.format("%.1f %%%%", pctMissing));
                String str3 = StringUtils.translate("litematica.gui.label.material_list.progress.mismatch", String.format("%.1f %%%%", pctMismatch));
                strp = String.format("%s / %s / %s", str1, str2, str3);
            }

            str = strt + " / " + StringUtils.translate("litematica.gui.label.material_list.progress", strp);
            w = this.getStringWidth(str);
            this.addLabel(12, this.height - 36, w, 12, 0xFFFFFFFF, str);
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
        else
        {
            label = type.getDisplayName();
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);

        if (type == ButtonListener.Type.CLEAR_CACHE)
        {
            button.setHoverStrings("litematica.gui.button.hover.material_list.clear_cache");
        }
        else if (type == ButtonListener.Type.WRITE_TO_FILE)
        {
            button.setHoverStrings("litematica.gui.button.hover.material_list.write_hold_shift_for_csv");
        }

        this.addButton(button, listener);

        return button.getWidth();
    }

    private int getElementTotalWidth()
    {
        int width = 0;

        width += this.getStringWidth(ButtonListener.Type.REFRESH_LIST.getDisplayName());
        width += this.getStringWidth(ButtonListener.Type.LIST_TYPE.getDisplayName(this.materialList.getMaterialListType().getDisplayName()));
        width += this.getStringWidth(ButtonListener.Type.CLEAR_IGNORED.getDisplayName());
        width += this.getStringWidth(ButtonListener.Type.CLEAR_CACHE.getDisplayName());
        width += this.getStringWidth(ButtonListener.Type.WRITE_TO_FILE.getDisplayName());
        width += (new ButtonOnOff(0, 0, -1, false, ButtonListener.Type.HIDE_AVAILABLE.getTranslationKey(), false)).getWidth();
        width += (new ButtonOnOff(0, 0, -1, false, ButtonListener.Type.TOGGLE_INFO_HUD.getTranslationKey(), false)).getWidth();
        width += this.getStringWidth(StringUtils.translate("litematica.gui.label.material_list.multiplier"));
        width += 130;

        return width;
    }

    private int createButtonOnOff(int x, int y, int width, boolean isCurrentlyOn, ButtonListener.Type type)
    {
        ButtonOnOff button = new ButtonOnOff(x, y, width, false, type.getTranslationKey(), isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));
        return button.getWidth();
    }

    public MaterialListBase getMaterialList()
    {
        return this.materialList;
    }

    @Override
    public void onTaskCompleted()
    {
        // re-create the list widgets when a material list task finishes
        if (GuiUtils.getCurrentScreen() == this)
        {
            WidgetMaterialListEntry.setMaxNameLength(this.materialList.getMaterialsAll(), this.materialList.getMultiplier());
            this.initGui();
        }
    }

    @Override
    protected WidgetListMaterialList createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialList(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiMaterialList parent;
        private final Type type;

        public ButtonListener(Type type, GuiMaterialList parent)
        {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            MaterialListBase materialList = this.parent.materialList;

            switch (this.type)
            {
                case REFRESH_LIST:
                    materialList.reCreateMaterialList();
                    break;

                case LIST_TYPE:
                    BlockInfoListType type = materialList.getMaterialListType();
                    materialList.setMaterialListType((BlockInfoListType) type.cycle(mouseButton == 0));
                    materialList.reCreateMaterialList();
                    break;

                case HIDE_AVAILABLE:
                    materialList.setHideAvailable(! materialList.getHideAvailable());
                    materialList.refreshPreFilteredList();
                    materialList.recreateFilteredList();
                    break;

                case TOGGLE_INFO_HUD:
                    MaterialListHudRenderer renderer = materialList.getHudRenderer();
                    renderer.toggleShouldRender();

                    if (materialList.getHudRenderer().getShouldRenderCustom())
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
                    this.parent.addMessage(MessageType.SUCCESS, 3000, "litematica.message.material_list.material_cache_cleared");
                    break;

                case WRITE_TO_FILE:
                    File dir = new File(FileUtils.getConfigDirectory(), Reference.MOD_ID);
                    boolean csv = GuiBase.isShiftDown();
                    String ext = csv ? ".csv" : ".txt";
                    File file = DataDump.dumpDataToFile(dir, "material_list", ext, this.getMaterialListDump(materialList, csv).getLines());

                    if (file != null)
                    {
                        String key = "litematica.message.material_list_written_to_file";
                        this.parent.addMessage(MessageType.SUCCESS, key, file.getName());
                        StringUtils.sendOpenFileChatMessage(this.parent.mc.player, key, file);
                    }
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        private DataDump getMaterialListDump(MaterialListBase materialList, boolean csv)
        {
            DataDump dump = new DataDump(4, csv ? DataDump.Format.CSV : DataDump.Format.ASCII);
            int multiplier = materialList.getMultiplier();

            ArrayList<MaterialListEntry> list = new ArrayList<>();
            list.addAll(materialList.getMaterialsFiltered(false));
            Collections.sort(list, new MaterialListSorter(materialList));

            for (MaterialListEntry entry : list)
            {
                int total = entry.getCountTotal() * multiplier;
                int missing = multiplier > 1 ? total : entry.getCountMissing();
                int available = entry.getCountAvailable();
                dump.addData(entry.getStack().getName().getString(), String.valueOf(total), String.valueOf(missing), String.valueOf(available));
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
            WRITE_TO_FILE       ("litematica.gui.button.material_list.write_to_file");

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
                return StringUtils.translate(this.translationKey, args);
            }
        }
    }

    private static class MultiplierListener implements ITextFieldListener<GuiTextFieldInteger>
    {
        private final MaterialListBase materialList;
        private final GuiMaterialList gui;

        private MultiplierListener(MaterialListBase materialList, GuiMaterialList gui)
        {
            this.materialList = materialList;
            this.gui = gui;
        }

        @Override
        public boolean onTextChange(GuiTextFieldInteger textField)
        {
            try
            {
                int multiplier = Integer.parseInt(textField.getText());

                if (multiplier != this.materialList.getMultiplier())
                {
                    this.materialList.setMultiplier(multiplier);
                    this.gui.getListWidget().refreshEntries();
                    return true;
                }
            }
            catch (Exception e)
            {
                this.materialList.setMultiplier(1);
                this.gui.getListWidget().refreshEntries();
            }

            return false;
        }
    }
}
