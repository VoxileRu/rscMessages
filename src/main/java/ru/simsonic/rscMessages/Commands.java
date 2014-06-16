package ru.simsonic.rscMessages;
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
	void list(CommandSender sender, String list)
	{
		if(list == null)
		{
			for(String known : plugin.lists.keySet())
			{
				if(sender.hasPermission("rscm.edit." + known))
				{
					RowList row = plugin.lists.get(known);
				}
			}
		} else {
		}
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
	void notEnoughPermissions() throws CommandAnswerException
	{
		throw new CommandAnswerException("You have not permission to do that!");
	}
}