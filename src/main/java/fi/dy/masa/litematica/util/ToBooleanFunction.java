package fi.dy.masa.litematica.util;

@FunctionalInterface
public interface ToBooleanFunction<R>
{
    boolean applyAsBoolean(R value);
}
