package ru.simsonic.rscMessages;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.MetricsLite;
import ru.simsonic.rscMessages.Data.RowList;
import ru.simsonic.rscMessages.Data.RowMessage;
import ru.simsonic.rscMinecraftLibrary.Bukkit.CommandAnswerException;
import ru.simsonic.rscMinecraftLibrary.Bukkit.GenericChatCodes;
import ru.simsonic.rscMinecraftLibrary.Bukkit.Tools;

public final class BukkitPluginMain extends JavaPlugin implements Listener
{
	private   final static String chatPrefix = "{_DC}[rscm] {_LS}";
	public    final static Logger consoleLog = Bukkit.getLogger();
	protected final Database database = new Database();
	protected final Commands commands = new Commands(this);
	protected final Fetcher  fetcher  = new Fetcher(this);
	protected final SendRawMessage sendRaw = new SendRawMessage(this);
	protected final HashMap<String, RowList> lists = new HashMap<>();
	protected final HashSet<Player> newbies = new HashSet<>();
	protected int autoFetchInterval = 20 * 600;
	private MetricsLite metrics;
	@Override
	public void onLoad()
	{
		saveDefaultConfig();
		consoleLog.log(Level.INFO, "[rscm] rscMessages has been loaded.");
	}
	@Override
	public void onEnable()
	{
		// Update config
		reloadConfig();
		Phrases.extractTranslations(this.getDataFolder());
		boolean updateV2V3 = false;
		boolean updateV3V4 = false;
		switch(getConfig().getInt("internal.version", 1))
		{
			case 1:
				getConfig().set("settings.language", "english");
				getConfig().set("internal.version", 2);
			case 2:
				updateV2V3 = true;
				getConfig().set("internal.version", 3);
				saveConfig();
			case 3:
				updateV3V4 = true;
				getConfig().set("settings.add-prefix-to-json", false);
				getConfig().set("internal.version", 4);
				saveConfig();
			case 4:
				getConfig().set("settings.special-list-for-newbies", "");
				getConfig().set("internal.version", 5);
				saveConfig();
			case 5:
				// NEWEST VERSION
				break;
			default:
				// UNSUPPORTED VERSION?
				break;
		}
		// Read settings
		final String language = getConfig().getString("settings.language", "english");
		Phrases.fill(this, language);
		getConfig().set("settings.broadcast-to-console", getConfig().getBoolean("settings.broadcast-to-console", true));
		getConfig().set("settings.use-metrics", getConfig().getBoolean("settings.use-metrics", true));
		autoFetchInterval = 20 * getConfig().getInt("settings.fetch-interval-sec", 600);
		// Minimum is 1 min
		if(autoFetchInterval < 1200)
			autoFetchInterval = 1200;
		// Setup database
		final String hostname = getConfig().getString("settings.connection.hostname", "localhost:3306");
		final String username = getConfig().getString("settings.connection.username", "user");
		final String password = getConfig().getString("settings.connection.password", "pass");
		final String prefixes = getConfig().getString("settings.connection.prefixes", "rscm_");
		getConfig().set("settings.connection.hostname", hostname);
		getConfig().set("settings.connection.username", username);
		getConfig().set("settings.connection.password", password);
		getConfig().set("settings.connection.prefixes", prefixes);
		database.initialize("rscMessages", hostname, username, password, prefixes);
		// Metrics
		if(getConfig().getBoolean("settings.use-metrics", true))
			try
			{
				metrics = new MetricsLite(this);
				metrics.start();
				consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_METRICS.toString());
			} catch(IOException ex) {
				consoleLog.log(Level.INFO, "[rscm] Exception in Metrics:\n{0}", ex);
			}
		// Fetch lists and schedule them
		database.deploy();
		if(updateV2V3)
		{
			database.Update_v2_to_v3();
			consoleLog.log(Level.INFO, "[rscm] Database schema has been updated from v2 to v3");
		}
		if(updateV3V4)
		{
			database.Update_v3_to_v4();
			consoleLog.log(Level.INFO, "[rscm] Database schema has been updated from v3 to v4");
		}
		// The only aim to register Listener is newbies list for now
		if(!"".equals(getNewbiesListName()))
			getServer().getPluginManager().registerEvents(this, this);
		fetcher.startDeamon();
		// Look for ProtocolLib
		sendRaw.onEnable();
		// Done
		consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_ENABLED.toString());
	}
	@Override
	public void onDisable()
	{
		getServer().getScheduler().cancelTasks(this);
		getServer().getServicesManager().unregisterAll(this);
		for(RowList list : lists.values())
			list.messages.clear();
		database.disconnect();
		lists.clear();
		saveConfig();
		metrics = null;
		consoleLog.log(Level.INFO, "[rscm] {0}", Phrases.PLUGIN_DISABLED.toString());
	}
	private String getNewbiesListName()
	{
		final String listName = getConfig().getString("settings.special-list-for-newbies", "");
		if(!"".equals(listName))
			for(String knownList : lists.keySet())
				if(knownList.equalsIgnoreCase(listName))
					return knownList;
		return "";
	}
	private RowList getNewbiesList()
	{
		final String listName = getConfig().getString("settings.special-list-for-newbies", "");
		if(!"".equals(listName))
			for(Map.Entry<String, RowList> entry : lists.entrySet())
				if(entry.getKey().equalsIgnoreCase(listName))
					return entry.getValue();
		return null;
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		final Player player = event.getPlayer();
		if(player.hasPlayedBefore())
			newbies.remove(player);
		else
			newbies.add(player);
	}
	protected void scheduleBroadcastTasks()
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
	protected void broadcastList(RowList list)
	{
		final RowMessage message = list.getNextMessage(getServer().getWorlds().get(0).getTime());
		if(message != null)
			broadcastMessage(message);
	}
	protected void broadcastMessage(RowMessage message)
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
			final boolean bpa = player.hasPermission("rscm.receive.*");
			final boolean bpl = player.hasPermission("rscm.receive." + message.rowList.name.toLowerCase());
			final boolean bpn = listForNewbies && newbies.contains(player);
			if(bpa || bpl || bpn)
			{
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
		if(getConfig().getBoolean("settings.broadcast-to-console", true))
			consoleLog.log(Level.INFO, "[rscm] {0} ''{1}'' ({2}):\n{3}", new Object[]
			{
				Phrases.ACTION_BROADCAST.toString(),
				message.rowList.name,
				counter,
				text
			});
		message.lastBroadcast = this.getServer().getWorlds().get(0).getTime();
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
				sender.sendMessage(GenericChatCodes.processStringStatic(chatPrefix + answer));
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
					info_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
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
					edit_id = Integer.parseInt(args[1]);
					edit_text = GenericChatCodes.glue(Arrays.copyOfRange(args, 2, args.length), " ");
				} catch(NumberFormatException ex) {
					edit_text = GenericChatCodes.glue(Arrays.copyOfRange(args, 1, args.length), " ");
				}
				commands.edit(sender, args[0], edit_id, edit_text);
				return;
			case "r":
			case "remove":
				int remove_id = -1;
				try
				{
					remove_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
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
					set_id = Integer.parseInt(args[1]);
					set_option = args[2];
					set_value = GenericChatCodes.glue(Arrays.copyOfRange(args, 3, args.length), " ");
				} catch(NumberFormatException ex) {
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
					broadcast_id = Integer.parseInt(args[1]);
				} catch(NumberFormatException ex) {
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
		}
		throw new CommandAnswerException(Phrases.ACTION_WRONGCMD.toString());
	}
}
