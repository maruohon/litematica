package fi.dy.masa.litematica.config.interfaces;

import fi.dy.masa.litematica.config.FeatureToggle;

public interface IFeatureCallback
{
    /**
     * Called when (= after) the feature's value is changed.
     * @param feature
     */
    void onValueChange(FeatureToggle feature);
}
