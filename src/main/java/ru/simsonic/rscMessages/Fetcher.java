package ru.simsonic.rscMessages;

import java.util.Map;
import java.util.logging.Level;
import org.bukkit.scheduler.BukkitScheduler;
import ru.simsonic.rscCommonsLibrary.RestartableThread;
import ru.simsonic.rscMessages.API.RowList;

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
		// Disable expired messages
		plugin.database.cleanup();
		// Retrieve messages
		final Map<String, RowList> newData = plugin.database.fetch();
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
				}, plugin.settings.getAutofetchInterval());
			}
		});
	}
}
