package ru.simsonic.rscMessages;

import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.command.CommandSender;
import ru.simsonic.rscMessages.Data.RowList;
import ru.simsonic.rscMessages.Data.RowMessage;
import ru.simsonic.rscUtilityLibrary.Bukkit.Commands.CommandAnswerException;

public class Commands
{
	private final BukkitPluginMain plugin;
	Commands(BukkitPluginMain plugin)
	{
		this.plugin = plugin;
	}
	// rscm list [list]
	void list(CommandSender sender, String list) throws CommandAnswerException
	{
		final ArrayList<String> answers = new ArrayList<>();
		if(list == null)
		{
			// Enum lists
			answers.add(Phrases.ACTION_KNOWNLISTS.toString());
			final ArrayList<String> keys = new ArrayList<>(plugin.lists.keySet());
			Collections.sort(keys);
			for(String key : keys)
				if(viewPermission(sender, key))
				{
					RowList row = plugin.lists.get(key);
					answers.add((row.enabled ? "{_LG}" : "{_LR}") + row.name + " {GRAY}(" + row.messages.size() + ")");
				}
			throw new CommandAnswerException(answers);
		}
		// Enum messages of list
		if(!viewPermission(sender, list))
			notEnoughPermissions();
		final RowList row = getList(list);
		answers.add(Phrases.ACTION_KNOWNMSGS.toString());
		for(RowMessage message : row.messages)
			answers.add((message.enabled ? "{_LG}#" : "{_LR}#") + message.id + "{_R}: " + row.prefix + message.text);
		throw new CommandAnswerException(answers);
	}
	// rscm info <list> [#]
	void info(CommandSender sender, String list, int id) throws CommandAnswerException
	{
		if(list == null)
			throw new CommandAnswerException(Phrases.ACTION_UNSPECLIST.toString());
		final RowList row = getList(list);
		if(!editPermission(sender, list))
			notEnoughPermissions();
		final ArrayList<String> answers = new ArrayList<>();
		if(id <= 0)
		{
			// Show list info
			answers.add("{_LS}" + Phrases.PROPS_LISTPROPS.toString() + " {_LC}" + row.name + "{_LS}:");
			answers.add("{_LB}Enabled: "    + (row.enabled ? "{_LG}true" : "{_LR}false"));
			answers.add("{_LB}Random: "     + (row.random  ? "{_LG}true" : "{_LR}false"));
			answers.add("{_LB}Delay: {_LG}" + row.delay_sec + " sec");
			answers.add("{_LB}Prefix: {_R}" + row.prefix);
			int on = 0, off = 0;
			for(RowMessage msg : row.messages)
				if(msg.enabled)
					on += 1;
				else
					off += 1;
			answers.add("{_LB}" + Phrases.PROPS_MSGCOUNT.toString() + ": {_LP}" + row.messages.size() + "{_LB}"
				+ " ({_LG}" + on + "{_LB}/{_LR}" + off + "{_LB}).");
		} else {
			// Show message info
			final RowMessage msg = getMessage(row, id);
			answers.add("{_LS}" + Phrases.PROPS_MSGPROPS.toString() + " #{_LC}" + id + "{_LS} of list {_LC}" + row.name + "{_LS}:");
			answers.add("{_LB}Enabled: " + (msg.enabled ? "{_LG}true" : "{_LR}false"));
			answers.add("{_LB}" + Phrases.PROPS_MSGTEXT.toString() + "{_NL}{_R}" + row.prefix + msg.text);
		}
		throw new CommandAnswerException(answers);
	}
	// rscm add <list> <text>
	void add(CommandSender sender, String list, String text) throws CommandAnswerException
	{
		if(list == null)
			throw new CommandAnswerException(Phrases.ACTION_UNSPECLIST.toString());
		if(text == null || "".equals(text))
		{
			// Create new list
			if(!setupPermission(sender, list))
				notEnoughPermissions();
			plugin.database.addList(list);
		} else {
			// Add message to the list
			final RowList row = getList(list);
			if(!editPermission(sender, list))
				notEnoughPermissions();
			plugin.database.addMessage(row.name, text);
		}
		plugin.fetcher.startDeamon();
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	// rscm edit <list> <#> <new text>
	void edit(CommandSender sender, String list, int id, String text) throws CommandAnswerException
	{
		final RowList row = getList(list);
		if(!editPermission(sender, list))
			notEnoughPermissions();
		final RowMessage message = getMessage(row, id);
		if(text == null || "".equals(text))
			throw new CommandAnswerException(Phrases.ACTION_UNSPECTEXT.toString());
		// Update message text
		plugin.database.editMessage(message.id, text);
		plugin.fetcher.startDeamon();
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	// rscm remove <list> [#]
	void remove(CommandSender sender, String list, int id) throws CommandAnswerException
	{
		final RowList row = getList(list);
		if(id <= 0)
		{
			// Remove whole list
			if(!setupPermission(sender, list))
				notEnoughPermissions();
			plugin.database.removeList(row.name);
		} else {
			// Remove single message
			if(!editPermission(sender, list))
				notEnoughPermissions();
			final RowMessage message = getMessage(row, id);
			plugin.database.removeMessage(message.id);
		}
		plugin.fetcher.startDeamon();
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	// rscm set <list> <option> [#] <value>
	void set(CommandSender sender, String list, int id, String option, String value) throws CommandAnswerException
	{
		final RowList row = getList(list);
		if(option == null)
			option = "";
		if(id > 0)
		{
			if(!editPermission(sender, list))
				notEnoughPermissions();
			final RowMessage message = getMessage(row, id);
			switch(option.toLowerCase())
			{
				case "enabled":
					plugin.database.setMessageEnabled(message.id, parseBoolean(value));
					break;
				default:
					throw new CommandAnswerException(Phrases.PROPS_MSGVALID.toString());
			}
		} else {
			if(!setupPermission(sender, list))
				notEnoughPermissions();
			switch(option.toLowerCase())
			{
				case "enabled":
					plugin.database.setListEnabled(list, parseBoolean(value));
					break;
				case "random":
					plugin.database.setListRandom(list, parseBoolean(value));
					break;
				case "delay":
					plugin.database.setListDelay(list, parseInteger(value));
					break;
				case "prefix":
					plugin.database.setListPrefix(list, value);
					break;
				default:
					throw new CommandAnswerException(Phrases.PROPS_LISTVALID.toString());
			}
		}
		plugin.fetcher.startDeamon();
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	private boolean parseBoolean(String value) throws CommandAnswerException
	{
		switch(value.toLowerCase())
		{
			case "enable":
			case "true":
			case "yes":
			case "on":
				return true;
			case "disable":
			case "false":
			case "no":
			case "off":
				return false;
			case "":
			default:
				throw new CommandAnswerException(Phrases.ACTION_INCORRECT_V.toString());
		}
	}
	private int parseInteger(String value) throws CommandAnswerException
	{
		try
		{
			return Integer.parseInt(value);
		} catch(IllegalArgumentException ex) {
		}
		throw new CommandAnswerException(Phrases.ACTION_INCORRECT_V.toString());
	}
	// rscm broadcast <list> [#]
	void broadcast(CommandSender sender, String list, int id) throws CommandAnswerException
	{
		if(list == null)
			throw new CommandAnswerException(Phrases.ACTION_UNSPECLIST.toString());
		list = list.toLowerCase();
		if(!setupPermission(sender, list))
			notEnoughPermissions();
		final RowList row = plugin.lists.get(list);
		if(row == null)
			throw new CommandAnswerException(Phrases.ACTION_NOSUCHLIST.toString());
		if(id >= 0)
		{
			for(RowMessage message : row.messages)
				if(message.id == id)
				{
					plugin.broadcastMessage(message);
					throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
				}
			throw new CommandAnswerException(Phrases.ACTION_NOSUCHMSGID.toString());
		}
		plugin.broadcastList(row);
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	private boolean viewPermission(CommandSender sender, String list)
	{
		list = list.toLowerCase();
		if(sender.hasPermission("rscm.receive." + list))
			return true;
		if(sender.hasPermission("rscm.edit." + list))
			return true;
		if(sender.hasPermission("rscm.setup." + list))
			return true;
		return sender.hasPermission("rscm.admin");
	}
	private boolean editPermission(CommandSender sender, String list)
	{
		list = list.toLowerCase();
		if(sender.hasPermission("rscm.edit." + list))
			return true;
		if(sender.hasPermission("rscm.setup." + list))
			return true;
		return sender.hasPermission("rscm.admin");
	}
	private boolean setupPermission(CommandSender sender, String list)
	{
		list = list.toLowerCase();
		if(sender.hasPermission("rscm.setup." + list))
			return true;
		return sender.hasPermission("rscm.admin");
	}
	private void notEnoughPermissions() throws CommandAnswerException
	{
		throw new CommandAnswerException(Phrases.ACTION_NOPERMS.toString());
	}
	private RowList getList(String list) throws CommandAnswerException
	{
		if(list == null || "".equals(list))
			throw new CommandAnswerException(Phrases.ACTION_UNSPECLIST.toString());
		final RowList result = plugin.lists.get(list.toLowerCase());
		if(result == null)
			throw new CommandAnswerException(Phrases.ACTION_NOSUCHLIST.toString());
		return result;
	}
	private RowMessage getMessage(RowList list, int id) throws CommandAnswerException
	{
		if(id <= 0)
			throw new CommandAnswerException(Phrases.ACTION_UNSPECMSGID.toString());
		for(RowMessage message : list.messages)
			if(message.id == id)
				return message;
		throw new CommandAnswerException(Phrases.ACTION_NOSUCHMSGID.toString());
	}
}
