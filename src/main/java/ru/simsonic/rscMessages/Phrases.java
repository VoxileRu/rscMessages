package ru.simsonic.rscMessages;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;

public enum Phrases
{
	PLUGIN_ENABLED    ("generic.enabled"),
	PLUGIN_DISABLED   ("generic.disabled"),
	PLUGIN_METRICS    ("generic.metrics"),
	PLUGIN_RELOADED   ("generic.reloaded"),
	FETCHED           ("generic.fetched"),
	PROPS_LISTPROPS   ("props.listprops"),
	PROPS_MSGCOUNT    ("props.msgcount"),
	PROPS_MSGPROPS    ("props.msgprops"),
	PROPS_LISTVALID   ("props.listvalid"),
	PROPS_MSGVALID    ("props.msgvalid"),
	ACTION_KNOWNLISTS ("actions.knownlists"),
	ACTION_KNOWNMSGS  ("actions.knownmsgs"),
	ACTION_DONE       ("actions.done"),
	ACTION_NOPERMS    ("actions.noperms"),
	ACTION_UNSPECLIST ("actions.unspeclist"),
	ACTION_NOSUCHLIST ("actions.nosuchlist"),
	ACTION_UNSPECMSGID("actions.unspecmsgid"),
	ACTION_NOSUCHMSGID("actions.nosuchmsgid"),
	ACTION_UNSPECTEXT ("actions.unspectext");
	private final String node;
	private String phrase;
	private Phrases(String node)
	{
		this.node = node;
	}
	@Override
	public String toString()
	{
		return phrase;
	}
	public static void fill(BukkitPluginMain plugin, String langName)
	{
		final File langFile = new File(plugin.getDataFolder(), langName + ".yml");
		final YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
		for(Phrases value : Phrases.values())
			value.phrase = langConfig.getString(value.node, value.node);
	}
	public static void extract(BukkitPluginMain plugin, String langName)
	{
		try
		{
			final File langFile = new File(plugin.getDataFolder(), langName + ".yml");
			if(!langFile.isFile())
			{
				final YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
				final InputStream langStream = BukkitPluginMain.class.getResourceAsStream("/languages/" + langName + ".yml");
				YamlConfiguration langSource = YamlConfiguration.loadConfiguration(new InputStreamReader(langStream));
				langConfig.setDefaults(langSource);
				langConfig.save(langFile);
			}
		} catch(IOException ex) {
			BukkitPluginMain.consoleLog.log(Level.WARNING, "Cannot extract language: {0}", langName);
		}
	}
}
