package ru.simsonic.rscMessages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import org.bukkit.configuration.file.YamlConfiguration;

public enum Phrases
{
	PLUGIN_ENABLED    ("generic.enabled"),
	PLUGIN_DISABLED   ("generic.disabled"),
	PLUGIN_METRICS    ("generic.metrics"),
	PLUGIN_RELOADED   ("generic.reloaded"),
	PROTOCOLLIB_YES   ("generic.plib-y"),
	PROTOCOLLIB_NO    ("generic.plib-n"),
	DATA_FETCHED      ("generic.fetched"),
	PROPS_LISTPROPS   ("props.listprops"),
	PROPS_MSGCOUNT    ("props.msgcount"),
	PROPS_MSGPROPS    ("props.msgprops"),
	PROPS_LISTVALID   ("props.listvalid"),
	PROPS_MSGVALID    ("props.msgvalid"),
	PROPS_MSGTEXT     ("props.msgtext"),
	ACTION_BROADCAST  ("actions.broadcasting"),
	ACTION_KNOWNLISTS ("actions.knownlists"),
	ACTION_KNOWNMSGS  ("actions.knownmsgs"),
	ACTION_INCORRECT_V("actions.incorrect"),
	ACTION_DONE       ("actions.done"),
	ACTION_NOPERMS    ("actions.noperms"),
	ACTION_UNSPECLIST ("actions.unspeclist"),
	ACTION_NOSUCHLIST ("actions.nosuchlist"),
	ACTION_UNSPECMSGID("actions.unspecmsgid"),
	ACTION_NOSUCHMSGID("actions.nosuchmsgid"),
	ACTION_UNSPECTEXT ("actions.unspectext"),
	ACTION_WRONGCMD   ("actions.wrongcmd"),
	HELP_USAGE        ("help.usage"),
	HELP_OPTIONS_LIST ("help.options-list"),
	HELP_OPTIONS_MSG  ("help.options-msg"),
	HELP_LIST_ENABLED ("help.list-enabled"),
	HELP_LIST_RANDOM  ("help.list-random"),
	HELP_LIST_DELAY   ("help.list-delay"),
	HELP_LIST_PREFIX  ("help.list-prefix"),
	HELP_MSG_ENABLED  ("help.msg-enabled"),
	;
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
	public static void extractTranslations(File workingDir)
	{
		extractTranslation(workingDir, "english");
		extractTranslation(workingDir, "russian");
	}
	private static void extractTranslation(File workingDir, String langName)
	{
		try
		{
			final File langFile = new File(workingDir, langName + ".yml");
			if(langFile.isFile())
				langFile.delete();
			final FileChannel fileChannel = new FileOutputStream(langFile).getChannel();
			fileChannel.force(true);
			final InputStream langStream = BukkitPluginMain.class.getResourceAsStream("/languages/" + langName + ".yml");
			fileChannel.transferFrom(Channels.newChannel(langStream), 0, Long.MAX_VALUE);
		} catch(IOException ex) {
		}
	}
}
