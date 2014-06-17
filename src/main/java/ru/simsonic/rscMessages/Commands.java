package ru.simsonic.rscMessages;
import java.util.ArrayList;
import java.util.Collections;
import org.bukkit.command.CommandSender;
import ru.simsonic.utilities.CommandAnswerException;

public class Commands
{
	private final Plugin plugin;
	Commands(Plugin plugin)
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
			answers.add("Known message lists are:");
			final ArrayList<String> keys = new ArrayList<>(plugin.lists.keySet());
			Collections.sort(keys);
			for(String key : keys)
				if(viewPermission(sender, key))
				{
					RowList row = plugin.lists.get(key);
					answers.add((row.enabled ? "{_LG}" : "{_LR}") + key + " {GRAY}(" + row.messages.size() + ")");
				}
			throw new CommandAnswerException(answers);
		}
		// Enum messages of list
		list = list.toLowerCase();
		if(!viewPermission(sender, list))
			notEnoughPermissions();
		answers.add("List messages are:");
		final RowList row = plugin.lists.get(list);
		for(RowMessage message : row.messages)
			answers.add((message.enabled ? "{_LG}#" : "{_LR}#") + message.id + "{_R}: " + row.prefix + message.text);
		throw new CommandAnswerException(answers);
	}
	// rscm add <list> <text>
	void add(CommandSender sender, String list, String text)
	{
		if(list == null || "".equals(list))
		{
		} else {
		}
	}
	// rscm edit <list> <#> <new text>
	void edit(CommandSender sender, String list, int id, String text)
	{
		if(list == null || "".equals(list))
		{
		} else {
		}
	}
	// rscm remove <list> [#]
	void remove(CommandSender sender, String list, int id)
	{
		if(list == null || "".equals(list))
		{
		} else {
		}
	}
	// rscm set <list> <option> [#] <value>
	void set(CommandSender sender, String list, String option, int id, String value) throws CommandAnswerException
	{
		final RowList row = getList(list);
		if(option == null)
			option = "";
		if(id > 0)
		{
			if(!editPermission(sender, list))
				notEnoughPermissions();
			final RowMessage msg = getMessage(row, id);
			switch(option.toLowerCase())
			{
				case "enabled":
					plugin.connection.setMessageEnabled(msg.id, Boolean.parseBoolean(value));
					break;
				default:
					throw new CommandAnswerException("{_LR}Valid option is: {_R}enabled.");
			}
		} else {
			if(!setupPermission(sender, list))
				notEnoughPermissions();
			switch(option.toLowerCase())
			{
				case "enabled":
					plugin.connection.setListEnabled(list, Boolean.parseBoolean(value));
					break;
				case "random":
					plugin.connection.setListRandom(list, Boolean.parseBoolean(value));
					break;
				case "delay":
					plugin.connection.setListDelay(list, Integer.parseInt(value));
					break;
				case "prefix":
					plugin.connection.setListPrefix(list, value);
					break;
				default:
					throw new CommandAnswerException("{_LR}Valid options are: {_R}enabled, random, delay, prefix.");
			}
		}
		throw new CommandAnswerException("{_LR}Done.");
	}
	// rscm broadcast <list> [#]
	void broadcast(CommandSender sender, String list, int id) throws CommandAnswerException
	{
		if(list == null)
			throw new CommandAnswerException("{_LR}List should be specified.");
		list = list.toLowerCase();
		if(!setupPermission(sender, list))
			notEnoughPermissions();
		final RowList row = plugin.lists.get(list);
		if(row == null)
			throw new CommandAnswerException("{_LR}No such list.");
		if(id >= 0)
		{
			for(RowMessage message : row.messages)
				if(message.id == id)
				{
					plugin.broadcastMessage(message);
					return;
				}
			throw new CommandAnswerException("{_LR}No such message id.");
		} else
			plugin.broadcastList(row);
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
		throw new CommandAnswerException("You have not permission to do that!");
	}
	private RowList getList(String list) throws CommandAnswerException
	{
		if(list == null || "".equals(list))
			throw new CommandAnswerException("{_LR}List should be specified.");
		final RowList result = plugin.lists.get(list.toLowerCase());
		if(result == null)
			throw new CommandAnswerException("{_LR}No such list.");
		return result;
	}
	private RowMessage getMessage(RowList list, int id) throws CommandAnswerException
	{
		if(id <= 0)
			throw new CommandAnswerException("{_LR}Message id should be specified.");
		for(RowMessage message : list.messages)
			if(message.id == id)
				return message;
		throw new CommandAnswerException("{_LR}No such message id.");
	}
}