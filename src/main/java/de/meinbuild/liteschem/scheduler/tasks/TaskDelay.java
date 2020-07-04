package de.meinbuild.liteschem.scheduler.tasks;

import java.util.function.BooleanSupplier;
import de.meinbuild.liteschem.scheduler.TaskScheduler;

public class TaskDelay extends TaskBase
{
    protected final TaskScheduler scheduler;
    protected final TaskBase task;
    protected final BooleanSupplier startConditionChecker;
    protected final int interval;

    public TaskDelay(TaskBase task, int interval, TaskScheduler scheduler, BooleanSupplier startConditionChecker)
    {
        this.task = task;
        this.scheduler = scheduler;
        this.interval = interval;
        this.startConditionChecker = startConditionChecker;
    }

    @Override
    public boolean execute()
    {
        if (this.startConditionChecker.getAsBoolean())
        {
            this.scheduler.scheduleTask(this.task, this.interval);
            this.finished = true;
            return true;
        }

        return false;
    }
}
