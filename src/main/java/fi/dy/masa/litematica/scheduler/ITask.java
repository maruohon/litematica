package fi.dy.masa.litematica.scheduler;

public interface ITask
{
    /**
     * Get the display name for this task, used in the Task Manager GUI
     * @return
     */
    String getDisplayName();

    /**
     * Initialize the task. Called when the task is added to the task list.
     */
    void init();

    /**
     * Return whether this task can be executed.
     * @return true if the task can be executed
     */
    boolean canExecute();

    /**
     * Execute the task. Return true to indicate that this task has finished.
     * @return true to indicate the task has finished and can be removed
     */
    boolean execute();

    /**
     * Returns true if this task should be removed
     * @return
     */
    boolean shouldRemove();

    /**
     * Stop the task. This is also called when the tasks are removed.
     */
    void stop();

    /**
     * Returns the task's timer
     * @return
     */
    TaskTimer getTimer();

    /**
     * Creates a new timer for the task, with the given execution interval in game ticks
     * @param interval
     */
    void createTimer(int interval);
}
