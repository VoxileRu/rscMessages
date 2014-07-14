package ru.simsonic.rscMessages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mcstats.MetricsLite;
import ru.simsonic.utilities.CommandAnswerException;
import ru.simsonic.utilities.LanguageUtility;

public final class Plugin extends JavaPlugin
{
	private static final Logger consoleLog = Logger.getLogger("Minecraft");
	private static final String chatPrefix = "{GRAY}[rscm] {MAGENTA}";
	protected final Database connection = new Database(this);
	protected final Commands commands = new Commands(this);
	private MetricsLite metrics;
	final HashMap<String, RowList> lists = new HashMap<>();
	private int autoFetchInterval = 20 * 600;
	@Override
	public void onLoad()
	{
		saveDefaultConfig();
		switch(getConfig().getInt("internal.version", 1))
		{
			case 1:
				// NEWEST VERSION
				break;
			default:
				// UNSUPPORTED VERSION?
				break;
		}
		consoleLog.log(Level.INFO, "[rscm] rscMessages has been loaded.");
	}
	@Override
	public void onEnable()
	{
		// Read settings 
		reloadConfig();
		getConfig().set("settings.broadcast-to-console", getConfig().getBoolean("settings.broadcast-to-console", true));
		getConfig().set("settings.use-metrics", getConfig().getBoolean("settings.use-metrics", true));
		autoFetchInterval = 20 * getConfig().getInt("settings.fetch-interval-sec", 600);
		// Setup connection
		final String hostname = getConfig().getString("settings.connection.hostname", "localhost:3306");
		final String username = getConfig().getString("settings.connection.username", "user");
		final String password = getConfig().getString("settings.connection.password", "pass");
		final String prefixes = getConfig().getString("settings.connection.prefixes", "rscm_");
		getConfig().set("settings.connection.hostname", hostname);
		getConfig().set("settings.connection.username", username);
		getConfig().set("settings.connection.password", password);
		getConfig().set("settings.connection.prefixes", prefixes);
		connection.Initialize("rscMessages", hostname, username, password, prefixes);
		// Metrics
		if(getConfig().getBoolean("settings.use-metrics", true))
			try
			{
				metrics = new MetricsLite(this);
				metrics.start();
				consoleLog.info("[rscm] Metrics enabled.");
			} catch(IOException ex) {
				consoleLog.log(Level.INFO, "[rscm][Metrics] Exception:\n{0}", ex.getLocalizedMessage());
			}
		// Fetch lists and schedule them
		connection.StartAndDeploy();
		fetchAndSchedule();
		// Done
		consoleLog.log(Level.INFO, "[rscm] rscMessages has been successfully enabled.");
	}
	@Override
	public void onDisable()
	{
		getServer().getScheduler().cancelTasks(this);
		for(RowList list : lists.values())
			list.messages.clear();
		lists.clear();
		saveConfig();
		metrics = null;
		consoleLog.info("[rscm] rscMessages has been disabled.");
	}
	private void scheduleBroadcastTasks()
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
	protected void fetchAndSchedule()
	{
		final BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.cancelTasks(this);
		for(RowList list : lists.values())
			list.messages.clear();
		lists.clear();
		lists.putAll(connection.fetch());
		getServer().getConsoleSender().sendMessage("[rscm] Message lists have been fetched from database.");
		scheduleBroadcastTasks();
		scheduler.scheduleSyncDelayedTask(this, new Runnable()
		{
			@Override
			public void run()
			{
				fetchAndSchedule();
			}
		}, autoFetchInterval);
	}
	protected void broadcastList(RowList list)
	{
		final RowMessage message = list.getNextMessage(getServer().getWorlds().get(0).getTime());
		if(message != null)
			broadcastMessage(message);
	}
	protected void broadcastMessage(RowMessage message)
	{
		message.lastBroadcast = this.getServer().getWorlds().get(0).getTime();
		final String text = LanguageUtility.processStringStatic(message.rowList.prefix + message.text);
		for(Player player : getServer().getOnlinePlayers())
		{
			final boolean bpa = player.hasPermission("rscm.receive.*");
			final boolean bpl = player.hasPermission("rscm.receive." + message.rowList.name.toLowerCase());
			if(bpa || bpl)
				player.sendMessage(text);
		}
		if(getConfig().getBoolean("settings.broadcast-to-console", true))
			getServer().getConsoleSender().sendMessage("[rscm] Broadcasting '" + message.rowList.name + "': " + text);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		try
		{
			switch(label.toLowerCase())
			{
				case "rscm":
					execute(sender, args);
					break;
			}
		} catch(CommandAnswerException ex) {
			for(String answer : ex.getMessageArray())
				sender.sendMessage(LanguageUtility.processStringStatic(chatPrefix + answer));
		}
		return true;
	}
	private void execute(CommandSender sender, String[] args) throws CommandAnswerException
	{
		if(args.length == 0)
			throw new CommandAnswerException("{MAGENTA}rscMessages {GRAY}" + getDescription().getVersion() + "{MAGENTA} by SimSonic.");
		final ArrayList<String> result = new ArrayList<>();
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
				commands.add(sender, args[0], LanguageUtility.glue(Arrays.copyOfRange(args, 1, args.length), " "));
				return;
			case "e":
			case "edit":
				int edit_id = -1;
				String edit_text;
				try
				{
					edit_id = Integer.parseInt(args[1]);
					edit_text = LanguageUtility.glue(Arrays.copyOfRange(args, 2, args.length), " ");
				} catch(NumberFormatException ex) {
					edit_text = LanguageUtility.glue(Arrays.copyOfRange(args, 1, args.length), " ");
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
					set_value = LanguageUtility.glue(Arrays.copyOfRange(args, 3, args.length), " ");
				} catch(NumberFormatException ex) {
					set_option = args[1];
					set_value = LanguageUtility.glue(Arrays.copyOfRange(args, 2, args.length), " ");
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
				// PAGE 3
				if("3".equals(args[0]) && sender.hasPermission("rscm.admin"))
				{
					throw new CommandAnswerException(new String[]
					{
						"Empty page. Sorry.",
					});
				}
				// PAGE 2
				if("2".equals(args[0]))
				{
					throw new CommandAnswerException(new String[]
					{
						"Available options for lists:",
						"{YELLOW}enabled {MAGENTA}(only {GOLD}true{MAGENTA} means true, otherwise false)",
						"{YELLOW}random {MAGENTA}(only {GOLD}true{MAGENTA} means true, otherwise false)",
						"{YELLOW}delay {MAGENTA}(in seconds)",
						"{YELLOW}prefix {MAGENTA}(don't enter <text> to clear)",
						"Available options for messages:",
						"{YELLOW}enabled {MAGENTA}(only {GOLD}true{MAGENTA} means true, otherwise false)",
					});
				}
				// PAGE 1
				throw new CommandAnswerException(new String[]
				{
					"Usage:",
					"{YELLOW}/rscm list [list]",
					"{YELLOW}/rscm broadcast <list> [id]",
					"{YELLOW}/rscm add <list> [text]",
					"{YELLOW}/rscm edit <list> <id> <text>",
					"{YELLOW}/rscm remove <list> [id]",
					"{YELLOW}/rscm set <list> [id] <option> [value]",
					"{YELLOW}/rscm help [1|2|3]",
					"{YELLOW}/rscm reload",
				});
			case "reload":
				if(sender.hasPermission("rscm.admin"))
				{
					reloadConfig();
					getPluginLoader().disablePlugin(this);
					getPluginLoader().enablePlugin(this);
					getServer().getConsoleSender().sendMessage("[rscm] rscMessages has been reloaded.");
				}
				return;
		}
		throw new CommandAnswerException(result);
	}
}