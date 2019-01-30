package fi.dy.masa.litematica.scheduler;

public interface ITask
{
    /**
     * Initialize the task. Called when the task is added to the task list.
     */
    public void init();

    /**
     * Return whether this task can be executed.
     * @return true if the task can be executed
     */
    public boolean canExecute();

    /**
     * Execute the task. Return true to indicate that this task has finished.
     * @return true to indicate the task has finished and can be removed
     */
    public boolean execute();

    /**
     * Returns true if this task should be removed
     * @return
     */
    public boolean shouldRemove();

    /**
     * Stop the task. This is also called when the tasks are removed.
     */
    public void stop();

    /**
     * Returns the task's timer
     * @return
     */
    public TaskTimer getTimer();

    /**
     * Creates a new timer for the task, with the given execution interval in game ticks
     * @param interval
     */
    public void createTimer(int interval);
}
