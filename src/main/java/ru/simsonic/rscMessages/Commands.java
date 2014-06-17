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
			answers.add((message.enabled ? "{_LG}on: {_R}" : "{_LR}off: {_R}") + row.prefix + message.text);
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
		if(list == null)
			throw new CommandAnswerException("{RED}List should be specified");
	}
	// rscm broadcast <list> [#]
	void broadcast(CommandSender sender, String list, int id)
	{
		if(list != null)
		{
		} else {
		}
	}
	private boolean viewPermission(CommandSender sender, String list)
	{
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
		if(sender.hasPermission("rscm.edit." + list))
			return true;
		if(sender.hasPermission("rscm.setup." + list))
			return true;
		return sender.hasPermission("rscm.admin");
	}
	private boolean setupPermission(CommandSender sender, String list)
	{
		if(sender.hasPermission("rscm.setup." + list))
			return true;
		return sender.hasPermission("rscm.admin");
	}
	void notEnoughPermissions() throws CommandAnswerException
	{
		throw new CommandAnswerException("You have not permission to do that!");
	}
}