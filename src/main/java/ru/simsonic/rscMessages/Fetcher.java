package ru.simsonic.rscMessages;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitScheduler;
import ru.simsonic.rscMessages.Data.RowList;
import ru.simsonic.rscUtilityLibrary.RestartableThread;

public class Fetcher extends RestartableThread
{
	private final BukkitPluginMain plugin;
	Fetcher(BukkitPluginMain plugin)
	{
		this.plugin = plugin;
	}
	@Override
	public void run()
	{
		// Potentially long work
		final Map<String, RowList> newData = plugin.connection.fetch();
		final BukkitScheduler scheduler = plugin.getServer().getScheduler();
		scheduler.runTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				scheduler.cancelTasks(plugin);
				for(RowList list : plugin.lists.values())
					list.messages.clear();
				plugin.lists.clear();
				plugin.lists.putAll(newData);
				BukkitPluginMain.consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.DATA_FETCHED.toString());
				plugin.scheduleBroadcastTasks();
				scheduler.scheduleSyncDelayedTask(plugin, new Runnable()
				{
					@Override
					public void run()
					{
						plugin.fetcher.startDeamon();
					}
				}, plugin.autoFetchInterval);
			}
		});
	}
}
