package fi.dy.masa.litematica.scheduler;

import java.util.ArrayList;
import java.util.List;

public class TaskScheduler
{
    private static final TaskScheduler INSTANCE = new TaskScheduler();
    private List<ITask> tasks = new ArrayList<>();
    private List<ITask> tasksToAdd = new ArrayList<>();

    private TaskScheduler()
    {
    }

    public static TaskScheduler getInstance()
    {
        return INSTANCE;
    }

    public void scheduleTask(ITask task, int interval)
    {
        task.createTimer(interval);
        this.tasksToAdd.add(task);
    }

    public void runTasks()
    {
        for (int i = 0; i < this.tasks.size(); ++i)
        {
            boolean finished = false;
            ITask task = this.tasks.get(i);

            if (task.shouldRemove())
            {
                finished = true;
            }
            else if (task.canExecute() && task.getTimer().tick())
            {
                finished = task.execute();
            }

            if (finished)
            {
                task.stop();
                this.tasks.remove(i);
                --i;
            }
        }

        this.addNewTasks();
    }

    private void addNewTasks()
    {
        for (int i = 0; i < this.tasksToAdd.size(); ++i)
        {
            ITask task = this.tasksToAdd.get(i);
            task.init();
            this.tasks.add(task);
        }

        this.tasksToAdd.clear();
    }

    public boolean hasTasks()
    {
        return this.tasks.isEmpty() == false || this.tasksToAdd.isEmpty() == false;
    }

    public boolean hasTask(Class <? extends ITask> clazz)
    {
        for (ITask task : this.tasks)
        {
            if (clazz.equals(task.getClass()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean removeTask(Class <? extends ITask> clazz)
    {
        boolean removed = false;

        for (int i = 0; i < this.tasks.size(); ++i)
        {
            ITask task = this.tasks.get(i);

            if (clazz.equals(task.getClass()))
            {
                task.stop();
                this.tasks.remove(i);
                removed = true;
                --i;
            }
        }

        return removed;
    }

    public void clearTasks()
    {
        for (int i = 0; i < this.tasks.size(); ++i)
        {
            ITask task = this.tasks.get(i);
            task.stop();
        }

        this.tasks.clear();
    }
}
