package litematica.gui.widget.list.entry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import malilib.gui.BaseScreen;
import malilib.gui.TextInputScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.overlay.message.MessageOutput;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import litematica.data.DataManager;
import litematica.gui.AreaSubRegionEditScreen;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.selection.SelectionManager;
import litematica.util.PositionUtils;

public class AreaSubRegionEntryWidget extends BaseDataListEntryWidget<String>
{
    protected final AreaSelection selection;
    protected final SelectionManager manager;
    protected final GenericButton configureButton;
    protected final GenericButton removeButton;
    protected final GenericButton renameButton;
    protected int buttonsStartX;

    public AreaSubRegionEntryWidget(String data,
                                    DataListEntryWidgetData constructData,
                                    AreaSelection selection)
    {
        super(data, constructData);

        this.selection = selection;
        this.manager = DataManager.getSelectionManager();

        int h = constructData.height - 2;
        this.configureButton = GenericButton.create(h, "litematica.button.misc.configure", this::openConfigurationMenu);
        this.removeButton    = GenericButton.create(h, "litematica.button.misc.remove.minus", this::removeRegion);
        this.renameButton    = GenericButton.create(h, "litematica.button.misc.rename", this::renameRegion);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, this.isOdd ? 0x70606060 : 0x70909090);
        this.setText(StyledTextLine.of(data));
        this.addHoverInfo(selection, data);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.configureButton);
        this.addWidget(this.removeButton);
        this.addWidget(this.renameButton);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        this.removeButton.setRight(this.getRight() - 2);
        this.renameButton.setRight(this.removeButton.getX() - 2);
        this.configureButton.setRight(this.renameButton.getX() - 2);
        this.configureButton.centerVerticallyInside(this);
        this.renameButton.centerVerticallyInside(this);
        this.removeButton.centerVerticallyInside(this);

        this.buttonsStartX = this.configureButton.getX();
    }

    @Override
    protected boolean isSelected()
    {
        return this.data.equals(this.selection.getCurrentSubRegionBoxName());
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    protected void addHoverInfo(AreaSelection selection, String regionName)
    {
        List<String> lines = new ArrayList<>();
        SelectionBox box = selection.getSubRegionBox(regionName);

        if (box != null)
        {
            BlockPos pos1 = box.getPos1();
            BlockPos pos2 = box.getPos2();

            if (pos1 != null)
            {
                lines.add(StringUtils.translate("litematica.hover.area_editor.multi_region.sub_region.pos1",
                                                pos1.getX(), pos1.getY(), pos1.getZ()));
            }

            if (pos2 != null)
            {
                lines.add(StringUtils.translate("litematica.hover.area_editor.multi_region.sub_region.pos1",
                                                pos2.getX(), pos2.getY(), pos2.getZ()));
            }

            if (pos1 != null && pos2 != null)
            {
                Vec3i size = PositionUtils.getAreaSizeFromRelativeEndPosition(pos2.subtract(pos1));
                lines.add(StringUtils.translate("litematica.hover.area_editor.multi_region.sub_region.dimensions",
                                               Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ())));
            }

            this.getHoverInfoFactory().addStrings(lines);
        }
    }

    protected void openConfigurationMenu()
    {
        BaseScreen.openScreenWithParent(new AreaSubRegionEditScreen(this.selection));
    }

    protected void removeRegion()
    {
        this.scheduleTask(() -> {
            this.selection.removeSubRegionBox(this.data);
            this.listWidget.refreshEntries();
        });
    }

    protected void renameRegion()
    {
        SelectionBox box = this.selection.getSubRegionBox(this.data);

        if (box != null)
        {
            String title = "litematica.title.screen.area_editor.rename_sub_region";
            String name = box.getName();
            BaseScreen.openPopupScreenWithCurrentScreenAsParent(new TextInputScreen(title, name, this::renameRegionTo));
        }
    }

    protected boolean renameRegionTo(String newName)
    {
        boolean success = this.selection.renameSubRegionBox(this.data, newName, MessageOutput.MESSAGE_OVERLAY);

        if (success)
        {
            this.listWidget.refreshEntries();
        }

        return success;
    }
}
