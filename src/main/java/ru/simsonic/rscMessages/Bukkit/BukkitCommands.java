package ru.simsonic.rscMessages.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.simsonic.rscMessages.API.RowList;
import ru.simsonic.rscMessages.API.RowMessage;
import ru.simsonic.rscMessages.BukkitPluginMain;
import ru.simsonic.rscMessages.Phrases;
import ru.simsonic.rscMinecraftLibrary.Bukkit.CommandAnswerException;
import ru.simsonic.rscMinecraftLibrary.Bukkit.GenericChatCodes;
import ru.simsonic.rscMinecraftLibrary.Bukkit.Tools;

public class BukkitCommands
{
	private final BukkitPluginMain plugin;
	public BukkitCommands(BukkitPluginMain plugin)
	{
		this.plugin = plugin;
	}
	public void execute(CommandSender sender, String[] args) throws CommandAnswerException
	{
		if(args.length == 0)
			throw new CommandAnswerException(Tools.getPluginWelcome(plugin, null));
		final String command = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, (args.length >= 5) ? args.length : 5);
		switch(command)
		{
			case "l":
			case "list":
				list(sender, args[0]);
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
				info(sender, args[0], info_id);
				return;
			case "a":
			case "add":
				add(sender, args[0], GenericChatCodes.glue(Arrays.copyOfRange(args, 1, args.length), " "));
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
				edit(sender, args[0], edit_id, edit_text);
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
				remove(sender, args[0], remove_id);
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
				set(sender, args[0], set_id, set_option, set_value);
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
				broadcast(sender, args[0], broadcast_id);
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
					plugin.reloadConfig();
					plugin.getPluginLoader().disablePlugin(plugin);
					plugin.getPluginLoader().enablePlugin(plugin);
					throw new CommandAnswerException(Phrases.PLUGIN_RELOADED.toString());
				}
				return;
			case "update":
				if(sender.hasPermission("rscm.admin"))
				{
					if(args.length > 0 && "do".equals(args[0]))
						plugin.updating.doUpdate(sender instanceof Player ? (Player)sender : null);
					else
						plugin.updating.checkUpdate(sender instanceof Player ? (Player)sender : null);
				}
				return;
		}
		throw new CommandAnswerException(Phrases.ACTION_WRONGCMD.toString());
	}
	// rscm list [list]
	public void list(CommandSender sender, String list) throws CommandAnswerException
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
	public void info(CommandSender sender, String list, int id) throws CommandAnswerException
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
			answers.add("{_LB}Delay: {_LS}" + row.delay_sec + " sec");
			answers.add("{_LB}Prefix: {_R}{_LS}\"" + row.prefix + "{_R}{_LS}\"");
			answers.add("{_LB}Sound: {_R}"  + (row.sound != null ? row.sound.name() : "no sound"));
			int on = 0, off = 0;
			for(RowMessage msg : row.messages)
				if(msg.enabled)
					on += 1;
				else
					off += 1;
			answers.add("{_LB}" + Phrases.PROPS_MSGCOUNT.toString() + ": {_LS}" + row.messages.size() + "{_LB}"
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
	public void add(CommandSender sender, String list, String text) throws CommandAnswerException
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
	public void edit(CommandSender sender, String list, int id, String text) throws CommandAnswerException
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
	public void remove(CommandSender sender, String list, int id) throws CommandAnswerException
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
	public void set(CommandSender sender, String list, int id, String option, String value) throws CommandAnswerException
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
				case "sound":
					plugin.database.setListSound(list, value);
					break;
				default:
					throw new CommandAnswerException(Phrases.PROPS_LISTVALID.toString());
			}
		}
		plugin.fetcher.startDeamon();
		throw new CommandAnswerException(Phrases.ACTION_DONE.toString());
	}
	public static boolean parseBoolean(String value) throws CommandAnswerException
	{
		switch(value.toLowerCase())
		{
			case "enable":
			case "true":
			case "yes":
			case "on":
			case "1":
				return true;
			case "disable":
			case "false":
			case "no":
			case "off":
			case "0":
				return false;
			case "":
			default:
				throw new CommandAnswerException(Phrases.ACTION_INCORRECT_V.toString());
		}
	}
	public static int parseInteger(String value) throws CommandAnswerException
	{
		if(value == null || "".equals(value))
			return -1;
		try
		{
			value = value.replace("#", "");
			return Integer.parseInt(value);
		} catch(IllegalArgumentException ex) {
		}
		throw new CommandAnswerException(Phrases.ACTION_INCORRECT_V.toString());
	}
	// rscm broadcast <list> [#]
	public void broadcast(CommandSender sender, String list, int id) throws CommandAnswerException
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
		return editPermission(sender, list);
	}
	private boolean editPermission(CommandSender sender, String list)
	{
		list = list.toLowerCase();
		if(sender.hasPermission("rscm.edit." + list))
			return true;
		return setupPermission(sender, list);
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
