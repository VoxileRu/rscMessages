package ru.simsonic.rscMessages.Bukkit;

import org.bukkit.configuration.file.FileConfiguration;
import ru.simsonic.rscCommonsLibrary.ConnectionMySQL.ConnectionParams;
import ru.simsonic.rscMessages.API.Settings;
import ru.simsonic.rscMessages.BukkitPluginMain;

public class BukkitSettings implements Settings
{
	private final BukkitPluginMain plugin;
	private final static String DEFAULT_CONNECTION_HOSTNAME = "localhost:3306";
	private final static String DEFAULT_CONNECTION_USERNAME = "user";
	private final static String DEFAULT_CONNECTION_PASSWORD = "pass";
	private final static String DEFAULT_CONNECTION_PREFIXES = "rscm_";
	private final static long   DEFAULT_AUTOFETCH_INTERVAL  = 600;
	private final static String DEFAULT_NEWBIES_LISTNAME    = "newbies";
	private final static long   DEFAULT_NEWBIES_INTERVAL    = 60 * 60 * 24 * 7;
	private boolean updateDB_V2V3 = false;
	private boolean updateDB_V3V4 = false;
	private boolean updateDB_V5V6 = false;
	private long    autofetchInterval = DEFAULT_AUTOFETCH_INTERVAL;
	public BukkitSettings(BukkitPluginMain plugin)
	{
		this.plugin = plugin;
	}
	@Override
	public void onLoad()
	{
		plugin.saveDefaultConfig();
	}
	@Override
	public void onEnable()
	{
		plugin.reloadConfig();
		updateDB_V2V3 = false;
		updateDB_V3V4 = false;
		updateDB_V5V6 = false;
		final FileConfiguration config = plugin.getConfig();
		switch(config.getInt("internal.version", 1))
		{
			/*
			case 0:
				BukkitPluginMain.consoleLog.info("[rscfjd] Filling config.yml with default values...");
				new File(plugin.getDataFolder(), "config.yml").delete();
				plugin.saveDefaultConfig();
				plugin.reloadConfig();
				// config.set("settings.trajectory", Settings.defaultFirstJoinTrajectory);
				config.set("internal.version", 1);
			*/
			case 1:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v1 -> v2).");
				config.set("settings.language", "english");
				config.set("internal.version", 2);
			case 2:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v2 -> v3).");
				updateDB_V2V3 = true;
				config.set("internal.version", 3);
			case 3:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v3 -> v4).");
				updateDB_V3V4 = true;
				config.set("settings.add-prefix-to-json", false);
				config.set("internal.version", 4);
			case 4:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v4 -> v5).");
				config.set("settings.special-list-for-newbies", DEFAULT_NEWBIES_LISTNAME);
				config.set("internal.version", 5);
			case 5:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v5 -> v6).");
				updateDB_V5V6 = true;
				config.set("internal.version", 6);
			case 6:
				BukkitPluginMain.consoleLog.info("[rscfjd] Updating config.yml version (v6 -> v7).");
				config.set("settings.for-newbies.special-list-name",
					config.getString("settings.special-list-for-newbies", DEFAULT_NEWBIES_LISTNAME));
				config.set("settings.for-newbies.interval-sec", DEFAULT_NEWBIES_INTERVAL);
				config.set("settings.special-list-for-newbies", null);
				config.set("internal.version", 7);
			case 7:
				// NEWEST VERSION
				break;
			default:
				// UNSUPPORTED VERSION?
				break;
		}
		// Minimum fetching interval is 1 min
		autofetchInterval = config.getLong("settings.fetch-interval-sec", 0);
		if(autofetchInterval < DEFAULT_AUTOFETCH_INTERVAL)
		{
			autofetchInterval = DEFAULT_AUTOFETCH_INTERVAL;
			config.set("settings.fetch-interval-sec", autofetchInterval);
		}
		config.set("settings.broadcast-to-console", config.getBoolean("settings.broadcast-to-console", true));
		config.set("settings.use-metrics", config.getBoolean("settings.use-metrics", true));
	}
	@Override
	public boolean doUpdateDB_v2v3()
	{
		return updateDB_V2V3;
	}
	@Override
	public boolean doUpdateDB_v3v4()
	{
		return updateDB_V3V4;
	}
	@Override
	public boolean doUpdateDB_v5v6()
	{
		return updateDB_V5V6;
	}
	@Override
	public ConnectionParams getDatabaseCP()
	{
		final ConnectionParams cp = new ConnectionParams();
		cp.nodename = plugin.getName();
		cp.database = plugin.getConfig().getString("settings.connection.hostname", DEFAULT_CONNECTION_HOSTNAME);
		cp.username = plugin.getConfig().getString("settings.connection.username", DEFAULT_CONNECTION_USERNAME);
		cp.password = plugin.getConfig().getString("settings.connection.password", DEFAULT_CONNECTION_PASSWORD);
		cp.prefixes = plugin.getConfig().getString("settings.connection.prefixes", DEFAULT_CONNECTION_PREFIXES);
		plugin.getConfig().set("settings.connection.hostname", cp.database);
		plugin.getConfig().set("settings.connection.username", cp.username);
		plugin.getConfig().set("settings.connection.password", cp.password);
		plugin.getConfig().set("settings.connection.prefixes", cp.prefixes);
		return cp;
	}
	@Override
	public String getLanguage()
	{
		final String language = plugin.getConfig().getString("settings.language", "english");
		plugin.getConfig().set("settings.language", language);
		return language;
	}
	@Override
	public long getAutofetchInterval()
	{
		return 20 * autofetchInterval;
	}
	@Override
	public String getNewbiesListName()
	{
		return plugin.getConfig().getString("settings.for-newbies.special-list-name", DEFAULT_NEWBIES_LISTNAME);
	}
	@Override
	public long getNewbiesInterval()
	{
		return 20 * plugin.getConfig().getLong("settings.for-newbies.interval-sec", DEFAULT_NEWBIES_INTERVAL);
	}
}
