package litematica.scheduler.tasks;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.StringUtils;
import litematica.render.infohud.InfoHud;
import litematica.schematic.ISchematic;
import litematica.schematic.SchematicMetadata;

/**
 * This task doesn't actually do/run anything on its own.
 * It gets triggered by a hotkey, which takes the first matching
 * task from the task manager, and triggers it, which then marks
 * the task as completed, and it will get removed after that.
 * This is to both allow removing the tasks via the Task Manager menu,
 * and also to allow multiple preview tasks to get started at once,
 * which will then get triggered one by one in order.
 */
public class SetSchematicPreviewTask extends TaskBase
{
    protected final ISchematic schematic;

    public SetSchematicPreviewTask(ISchematic schematic)
    {
        this.schematic = schematic;
        this.name = StringUtils.translate("litematica.label.task.set_schematic_preview",
                                          schematic.getMetadata().getName());
        this.infoHudLines.add(this.name);
        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute()
    {
        return this.finished;
    }

    public void setPreview()
    {
        try
        {
            Minecraft mc = Minecraft.getMinecraft();
            BufferedImage screenshot = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());

            int x = screenshot.getWidth() >= screenshot.getHeight() ? (screenshot.getWidth() - screenshot.getHeight()) / 2 : 0;
            int y = screenshot.getHeight() >= screenshot.getWidth() ? (screenshot.getHeight() - screenshot.getWidth()) / 2 : 0;
            int longerSide = Math.min(screenshot.getWidth(), screenshot.getHeight());
            //System.out.printf("w: %d, h: %d, x: %d, y: %d\n", screenshot.getWidth(), screenshot.getHeight(), x, y);

            int previewDimensions = 140;
            double scaleFactor = (double) previewDimensions / longerSide;
            BufferedImage scaled = new BufferedImage(previewDimensions, previewDimensions, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(scaleFactor, scaleFactor);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

            Graphics2D graphics = scaled.createGraphics();
            graphics.drawImage(screenshot.getSubimage(x, y, longerSide, longerSide), scaleOp, 0, 0);

            int[] pixels = scaled.getRGB(0, 0, previewDimensions, previewDimensions, null, 0, scaled.getWidth());

            SchematicMetadata meta = this.schematic.getMetadata();
            meta.setPreviewImagePixelData(pixels);
            meta.setTimeModifiedToNowIfNotRecentlyCreated();

            this.schematic.writeToFile(this.schematic.getFile(), true);

            MessageDispatcher.success("litematica.message.info.schematic_manager.set_preview.success", meta.getName());
        }
        catch (Exception e)
        {
            MessageDispatcher.error().console(e).translate("litematica.message.error.schematic_preview_set_failed",
                                                           this.schematic.getMetadata().getName());
        }

        this.finished = true;
    }
}
