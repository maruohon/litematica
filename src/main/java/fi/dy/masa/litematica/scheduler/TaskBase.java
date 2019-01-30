package fi.dy.masa.litematica.scheduler;

public abstract class TaskBase implements ITask
{
    private TaskTimer timer = new TaskTimer(1);

    @Override
    public TaskTimer getTimer()
    {
        return this.timer;
    }

    @Override
    public void createTimer(int interval)
    {
        this.timer = new TaskTimer(interval);
    }

    @Override
    public void init()
    {
    }

    @Override
    public void stop()
    {
    }
}
