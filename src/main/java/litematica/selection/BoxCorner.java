package litematica.selection;

import java.util.function.BiConsumer;
import java.util.function.Function;

import malilib.util.position.BlockPos;

public enum BoxCorner
{
    NONE        ((box, pos) -> {},              box -> null),
    CORNER_1    (CornerDefinedBox::setCorner1,  CornerDefinedBox::getCorner1),
    CORNER_2    (CornerDefinedBox::setCorner2,  CornerDefinedBox::getCorner2);

    public final BiConsumer<CornerDefinedBox, BlockPos> cornerSetter;
    public final Function<CornerDefinedBox, BlockPos> cornerGetter;

    BoxCorner(BiConsumer<CornerDefinedBox, BlockPos> cornerSetter,
              Function<CornerDefinedBox, BlockPos> cornerGetter)
    {
        this.cornerSetter = cornerSetter;
        this.cornerGetter = cornerGetter;
    }
}
