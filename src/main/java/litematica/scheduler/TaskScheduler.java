package litematica.scheduler;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;

public class TaskScheduler
{
    private static final TaskScheduler INSTANCE_CLIENT = new TaskScheduler();
    private static final TaskScheduler INSTANCE_SERVER = new TaskScheduler();

    private final List<ITask> tasks = new ArrayList<>();
    private final List<ITask> tasksToAdd = new ArrayList<>();

    private TaskScheduler()
    {
    }

    public static TaskScheduler getInstanceClient()
    {
        return INSTANCE_CLIENT;
    }

    public static TaskScheduler getInstanceServer()
    {
        return INSTANCE_SERVER;
    }

    public static TaskScheduler getServerInstanceIfExistsOrClient()
    {
        Minecraft mc = Minecraft.getMinecraft();
        // Yes this is actually correct despite the naming - in single player we want to
        // schedule stuff to the integrated server's thread in some cases
        return mc.isSingleplayer() ? INSTANCE_SERVER : INSTANCE_CLIENT;
    }

    public void scheduleTask(ITask task, int interval)
    {
        synchronized (this)
        {
            task.createTimer(interval);
            task.getTimer().setNextDelay(0);
            this.tasksToAdd.add(task);
        }
    }

    public void runTasks()
    {
        synchronized (this)
        {
            if (this.tasks.isEmpty() == false)
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
            }

            if (this.tasksToAdd.isEmpty() == false)
            {
                this.addNewTasks();
            }
        }
    }

    private void addNewTasks()
    {
        for (ITask task : this.tasksToAdd)
        {
            task.init();
            this.tasks.add(task);
        }

        this.tasksToAdd.clear();
    }

    public boolean hasTask(Class <? extends ITask> clazz)
    {
        synchronized (this)
        {
            for (ITask task : this.tasks)
            {
                if (clazz.equals(task.getClass()))
                {
                    return true;
                }
            }

            for (ITask task : this.tasksToAdd)
            {
                if (clazz.equals(task.getClass()))
                {
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    public <T extends ITask> T getFirstTaskOfType(Class <? extends T> clazz)
    {
        synchronized (this)
        {
            for (ITask task : this.tasks)
            {
                if (clazz.isAssignableFrom(task.getClass()))
                {
                    return clazz.cast(task);
                }
            }

            return null;
        }
    }

    /*
    public boolean hasTasks()
    {
        synchronized (this)
        {
            return this.tasks.isEmpty() == false || this.tasksToAdd.isEmpty() == false;
        }
    }

    public <T extends ITask> List<T> getTasksOfType(Class <? extends T> clazz)
    {
        synchronized (this)
        {
            List<T> list = new ArrayList<>();

            for (int i = 0; i < this.tasks.size(); ++i)
            {
                ITask task = this.tasks.get(i);

                if (clazz.isAssignableFrom(task.getClass()))
                {
                    list.add(clazz.cast(task));
                }
            }

            return list;
        }
    }

    public boolean removeTasksOfType(Class <? extends ITask> clazz)
    {
        synchronized (this)
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
    }
    */

    public ImmutableList<ITask> getAllTasks()
    {
        synchronized (this)
        {
            return ImmutableList.copyOf(this.tasks);
        }
    }

    public boolean removeTask(ITask task)
    {
        synchronized (this)
        {
            int index = this.tasks.indexOf(task);

            if (index >= 0)
            {
                task.stop();
                this.tasks.remove(index);
                return true;
            }

            return false;
        }
    }

    public void clearTasks()
    {
        synchronized (this)
        {
            for (ITask task : this.tasks)
            {
                task.stop();
            }

            this.tasks.clear();
        }
    }
}
