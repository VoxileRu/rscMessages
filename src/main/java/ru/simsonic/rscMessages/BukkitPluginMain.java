package ru.simsonic.rscMessages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
	protected final BukkitUpdater  updating   = new BukkitUpdater(this, Settings.UPDATER_URL, Settings.CHAT_PREFIX);
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
					execute(sender, args);
					break;
			}
		} catch(CommandAnswerException ex) {
			for(String answer : ex.getMessageArray())
				sender.sendMessage(GenericChatCodes.processStringStatic(Settings.CHAT_PREFIX + answer));
		}
		return true;
	}
	private void execute(CommandSender sender, String[] args) throws CommandAnswerException
	{
		if(args.length == 0)
			throw new CommandAnswerException(Tools.getPluginWelcome(this, null));
		final String command = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, (args.length >= 5) ? args.length : 5);
		switch(command)
		{
			case "l":
			case "list":
				commands.list(sender, args[0]);
				return;
			case "i":
			case "info":
				int info_id = -1;
				try
				{
					info_id = BukkitCommands.parseInteger(args[1]);
				} catch(CommandAnswerException ex) {
					throw ex;
				}
				commands.info(sender, args[0], info_id);
				return;
			case "a":
			case "add":
				commands.add(sender, args[0], GenericChatCodes.glue(Arrays.copyOfRange(args, 1, args.length), " "));
				return;
			case "e":
			case "edit":
				int edit_id = -1;
				String edit_text;
				try
				{
					edit_id = BukkitCommands.parseInteger(args[1]);
					edit_text = GenericChatCodes.glue(Arrays.copyOfRange(args, 2, args.length), " ");
				} catch(CommandAnswerException ex) {
					edit_text = GenericChatCodes.glue(Arrays.copyOfRange(args, 1, args.length), " ");
				}
				commands.edit(sender, args[0], edit_id, edit_text);
				return;
			case "r":
			case "remove":
				int remove_id = -1;
				try
				{
					remove_id = BukkitCommands.parseInteger(args[1]);
				} catch(CommandAnswerException ex) {
					throw ex;
				}
				commands.remove(sender, args[0], remove_id);
				return;
			// rscm set <list> [#] <option> <value>
			case "s":
			case "set":
				int set_id = -1;
				String set_option;
				String set_value;
				try
				{
					set_id = BukkitCommands.parseInteger(args[1]);
					set_option = args[2];
					set_value = GenericChatCodes.glue(Arrays.copyOfRange(args, 3, args.length), " ");
				} catch(CommandAnswerException ex) {
					set_option = args[1];
					set_value = GenericChatCodes.glue(Arrays.copyOfRange(args, 2, args.length), " ");
				}
				commands.set(sender, args[0], set_id, set_option, set_value);
				return;
			case "b":
			case "broadcast":
				int broadcast_id = -1;
				try
				{
					broadcast_id = BukkitCommands.parseInteger(args[1]);
				} catch(CommandAnswerException ex) {
					throw ex;
				}
				// <list> [#]
				commands.broadcast(sender, args[0], broadcast_id);
				return;
			case "h":
			case "help":
				// PAGE 2
				if("2".equals(args[0]))
				{
					throw new CommandAnswerException(new String[]
					{
						Phrases.HELP_OPTIONS_LIST.toString(),
						Phrases.HELP_LIST_ENABLED.toString(),
						Phrases.HELP_LIST_RANDOM.toString(),
						Phrases.HELP_LIST_DELAY.toString(),
						Phrases.HELP_LIST_PREFIX.toString(),
						Phrases.HELP_LIST_SOUND.toString(),
						Phrases.HELP_OPTIONS_MSG.toString(),
						Phrases.HELP_MSG_ENABLED.toString(),
					});
				}
				// PAGE 1
				throw new CommandAnswerException(new String[]
				{
					Phrases.HELP_USAGE.toString(),
					"{YELLOW}/rscm list [list]",
					"{YELLOW}/rscm info <list> [id]",
					"{YELLOW}/rscm broadcast <list> [id]",
					"{YELLOW}/rscm add <list> [text]",
					"{YELLOW}/rscm edit <list> <id> <text>",
					"{YELLOW}/rscm remove <list> [id]",
					"{YELLOW}/rscm set <list> [id] <option> [value]",
					"{YELLOW}/rscm help [1|2]",
					"{YELLOW}/rscm reload",
					"{YELLOW}/rscm update [do]",
				});
			case "reload":
				if(sender.hasPermission("rscm.admin"))
				{
					reloadConfig();
					getPluginLoader().disablePlugin(this);
					getPluginLoader().enablePlugin(this);
					consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_RELOADED.toString());
				}
				return;
			case "update":
				if(sender.hasPermission("rscm.admin"))
				{
					if(args.length > 0 && "do".equals(args[0]))
					{
						updating.doUpdate(sender instanceof Player ? (Player)sender : null);
					} else {
						updating.checkUpdate(sender instanceof Player ? (Player)sender : null);
					}
				}
				return;
		}
		throw new CommandAnswerException(Phrases.ACTION_WRONGCMD.toString());
	}
}
