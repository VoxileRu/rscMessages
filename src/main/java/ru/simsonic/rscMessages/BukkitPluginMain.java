package ru.simsonic.rscMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.MetricsLite;
import ru.simsonic.rscMessages.API.RowList;
import ru.simsonic.rscMessages.API.RowMessage;
import ru.simsonic.rscMessages.API.Settings;
import ru.simsonic.rscMessages.Bukkit.BukkitCommands;
import ru.simsonic.rscMessages.Bukkit.BukkitSettings;
import ru.simsonic.rscMinecraftLibrary.AutoUpdater.BukkitUpdater;
import ru.simsonic.rscMinecraftLibrary.Bukkit.CommandAnswerException;
import ru.simsonic.rscMinecraftLibrary.Bukkit.GenericChatCodes;
import ru.simsonic.rscMinecraftLibrary.Bukkit.Tools;

public final class BukkitPluginMain extends JavaPlugin
{
	public    final static Logger  consoleLog = Bukkit.getLogger();
	public    final Settings       settings   = new BukkitSettings(this);
	public    final Database       database   = new Database();
	public    final Fetcher        fetcher    = new Fetcher(this);
	public    final BukkitUpdater  updating   = new BukkitUpdater(this, Settings.UPDATER_URL, Settings.CHAT_PREFIX, Settings.UPDATE_CMD);
	protected final BukkitCommands commands   = new BukkitCommands(this);
	protected final SendRawMessage sendRaw    = new SendRawMessage(this);
	public    final HashMap<String, RowList> lists = new HashMap<>();
	private   final static Random  rnd = new Random();
	private MetricsLite metrics;
	@Override
	public void onLoad()
	{
		settings.onLoad();
		consoleLog.log(Level.INFO, "[rscm] rscMessages has been loaded.");
	}
	@Override
	public void onEnable()
	{
		// Read settings and localization strings
		settings.onEnable();
		updating.onEnable();
		Phrases.extractTranslations(getDataFolder());
		Phrases.fill(this, settings.getLanguage());
		// Setup database
		database.initialize(settings.getDatabaseCP());
		database.deploy();
		if(settings.doUpdateDB_v2v3())
		{
			database.update_v2_to_v3();
			consoleLog.log(Level.INFO, "[rscm] Database schema has been updated to v3");
		}
		if(settings.doUpdateDB_v3v4())
		{
			database.update_v3_to_v4();
			consoleLog.log(Level.INFO, "[rscm] Database schema has been updated to v4");
		}
		if(settings.doUpdateDB_v5v6())
		{
			database.update_v5_to_v6();
			consoleLog.log(Level.INFO, "[rscm] Database schema has been updated to v6");
		}
		// Apply all configuration changes
		saveConfig();
		reloadConfig();
		// Metrics
		if(settings.getUseMetrics())
			try
			{
				metrics = new MetricsLite(this);
				metrics.start();
				consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_METRICS.toString());
			} catch(IOException ex) {
				consoleLog.log(Level.INFO, "[rscm] Exception in Metrics:\n{0}", ex);
			}
		// Fetch latest lists and messages from database
		fetcher.startDeamon();
		// Looking for other useful plugins: ProtocolLib, etc.
		sendRaw.onEnable();
		// Done
		for(Player online : Tools.getOnlinePlayers())
			if(online.hasPermission("rscm.admin"))
				updating.onAdminJoin(online, false);
		consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_ENABLED.toString());
	}
	@Override
	public void onDisable()
	{
		getServer().getScheduler().cancelTasks(this);
		getServer().getServicesManager().unregisterAll(this);
		database.disconnect();
		for(RowList list : lists.values())
			list.messages.clear();
		lists.clear();
		metrics = null;
		consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_DISABLED.toString());
	}
	private String getNewbiesListName()
	{
		final String listName = settings.getNewbiesListName();
		if(!"".equals(listName))
			for(String list : lists.keySet())
				if(list.equalsIgnoreCase(listName))
					return list;
		return "";
	}
	public void scheduleBroadcastTasks()
	{
		final BukkitScheduler scheduler = getServer().getScheduler();
		for(final RowList list : lists.values())
		{
			final int delay_ticks = 20 * list.delay_sec;
			scheduler.scheduleSyncRepeatingTask(this, new Runnable()
			{
				@Override
				public void run()
				{
					if(list.enabled)
						broadcastList(list);
				}
			}, delay_ticks, delay_ticks);
		}
	}
	public void broadcastMessage(RowMessage message)
	{
		final Plugin  placeholder     = getServer().getPluginManager().getPlugin("PlaceholderAPI");
		final boolean usePlaceholders = (placeholder != null && placeholder.isEnabled());
		final boolean jsonPrefixes    = getConfig().getBoolean("settings.add-prefix-to-json", false);
		final boolean listForNewbies  = message.rowList.name.equals(getNewbiesListName());
		final String  text            = GenericChatCodes.processStringStatic(
			((message.isJson && !jsonPrefixes) ? "" : message.rowList.prefix) + message.text);
		int counter = 0;
		for(Player player : Tools.getOnlinePlayers())
		{
			final long    ppt = (System.currentTimeMillis() - player.getFirstPlayed()) / 1000L;
			final boolean bpn = listForNewbies && !player.hasPermission("rscm.admin") && (ppt < settings.getNewbiesInterval());
			final boolean bpa = player.hasPermission("rscm.receive.*");
			final boolean bpl = player.hasPermission("rscm.receive." + message.rowList.name.toLowerCase());
			if(bpn || bpa || bpl)
			{
				// Play sound
				if(message.rowList.sound != null)
					player.playSound(player.getLocation(), message.rowList.sound, 1.0f, 1.0f);
				// Send message
				final String targetedText = usePlaceholders
					? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text)
					: text;
				if(message.isJson)
				{
					if(sendRaw.sendRawMessage(player, targetedText) == false)
						player.sendMessage(targetedText);
				} else
					player.sendMessage(targetedText);
				counter += 1;
			}
		}
		if(settings.getBroadcastToConsole())
			consoleLog.log(Level.INFO, "[rscm] {0} ''{1}'' ({2}):\n{3}", new Object[]
			{
				Phrases.ACTION_BROADCAST.toString(),
				message.rowList.name,
				counter,
				text
			});
		message.lastBroadcast = this.getServer().getWorlds().get(0).getTime();
	}
	public void broadcastList(RowList list)
	{
		final RowMessage message = getNextMessage(list);
		if(message != null)
			broadcastMessage(message);
	}
	private RowMessage getNextMessage(RowList rowList)
	{
		if(rowList.messages.isEmpty())
			return null;
		if(rowList.random)
		{
			final ArrayList<RowMessage> veryOldMessages = new ArrayList<>();
			final ArrayList<RowMessage> enabledMessages = new ArrayList<>();
			final long currentTime  = getServer().getWorlds().get(0).getTime();
			final long veryLongTime = 3 * rowList.messages.size() * 20 * rowList.delay_sec;
			for(RowMessage msg : rowList.messages)
				if(msg.enabled)
				{
					if(msg.lastBroadcast == 0 || (currentTime - msg.lastBroadcast) > veryLongTime)
						veryOldMessages.add(msg);
					enabledMessages.add(msg);
				}
			ArrayList<RowMessage> selectFrom = veryOldMessages.isEmpty() ? enabledMessages : veryOldMessages;
			if(selectFrom.isEmpty())
				return null;
			return selectFrom.get(rnd.nextInt(selectFrom.size()));
		}
		RowMessage largestTime = rowList.messages.get(0);
		for(RowMessage msg : rowList.messages)
		{
			if(msg.enabled == false)
				continue;
			if(msg.lastBroadcast == 0)
				return msg;
			if(msg.lastBroadcast < largestTime.lastBroadcast)
				largestTime = msg;
		}
		return largestTime.enabled ? largestTime : null;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		try
		{
			switch(command.getName().toLowerCase())
			{
				case "rscm":
					commands.execute(sender, args);
					break;
			}
		} catch(CommandAnswerException ex) {
			for(String answer : ex.getMessageArray())
				sender.sendMessage(GenericChatCodes.processStringStatic(Settings.CHAT_PREFIX + answer));
		}
		return true;
	}
}
