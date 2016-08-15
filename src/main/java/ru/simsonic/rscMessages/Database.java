package ru.simsonic.rscMessages;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Sound;
import ru.simsonic.rscCommonsLibrary.ConnectionMySQL;
import ru.simsonic.rscMessages.API.RowList;
import ru.simsonic.rscMessages.API.RowMessage;

public class Database extends ConnectionMySQL
{
	public void deploy()
	{
		try
		{
			if(isConnected())
				executeUpdateT("Deploy");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void cleanup()
	{
		try
		{
			if(isConnected())
				executeUpdateT("Cleanup");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void update_v2_to_v3()
	{
		try
		{
			if(isConnected())
				executeUpdateT("Update_v2_to_v3");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void update_v3_to_v4()
	{
		try
		{
			if(isConnected())
				executeUpdateT("Update_v3_to_v4");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void update_v5_to_v6()
	{
		try
		{
			if(isConnected())
				executeUpdateT("Update_v5_to_v6");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public Map<String, RowList> fetch()
	{
		final HashMap<String, RowList> result = new HashMap<>();
		try
		{
			if(!isConnected())
				return result;
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
			return Collections.EMPTY_MAP;
		}
		try(final ResultSet rsLists = executeQuery("SELECT * FROM `{DATABASE}`.`{PREFIX}lists` ORDER BY `id` ASC;"))
		{
			while(rsLists.next())
			{
				final RowList list = new RowList();
				list.id        = rsLists.getInt    ("id");
				list.name      = rsLists.getString ("name");
				list.enabled   = rsLists.getBoolean("enabled");
				list.random    = rsLists.getBoolean("random");
				list.delay_sec = rsLists.getInt    ("delay_sec");
				list.prefix    = rsLists.getString ("prefix");
				if(list.prefix == null)
					list.prefix = "";
				list.sound     = null;
				final String soundName = rsLists.getString("sound");
				if(soundName != null && !"".equals(soundName))
					for(Sound sound : Sound.values())
						if(sound.name().equalsIgnoreCase(soundName.trim()))
						{
							list.sound = sound;
							break;
						}
				result.put(list.name.toLowerCase(), list);
			}
			rsLists.close();
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
		try(final ResultSet rsMessages = executeQuery("SELECT * FROM `{DATABASE}`.`{PREFIX}messages` ORDER BY `id` ASC;"))
		{
			while(rsMessages.next())
			{
				final RowMessage msg = new RowMessage();
				msg.id      = rsMessages.getInt("id");
				msg.enabled = rsMessages.getBoolean("enabled");
				msg.list    = rsMessages.getString("list");
				msg.text    = rsMessages.getString("text");
				msg.isJson  = rsMessages.getBoolean("json");
				final RowList list = result.get(msg.list.toLowerCase());
				if(list != null)
				{
					msg.rowList = list;
					list.messages.add(msg);
				}
			}
			rsMessages.close();
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
		return result;
	}
	public void addList(String list)
	{
		try
		{
			// This place should include some security filters...
			setupQueryTemplate("{LIST}", list);
			executeUpdate("INSERT IGNORE INTO `{DATABASE}`.`{PREFIX}lists` (`name`) VALUES ('{LIST}');");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void addMessage(String list, String message)
	{
		try
		{
			// This place should include some security filters...
			setupQueryTemplate("{LIST}", list);
			setupQueryTemplate("{MESSAGE}", message);
			executeUpdate("INSERT INTO `{DATABASE}`.`{PREFIX}messages` (`list`, `text`) VALUES ('{LIST}', '{MESSAGE}');");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void editMessage(int id, String text)
	{
		try
		{
			// This place should include some security filters...
			setupQueryTemplate("{ID}", Integer.toString(id));
			setupQueryTemplate("{TEXT}", text);
			executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}messages` SET `text` = '{TEXT}' WHERE `id` = '{ID}';");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void removeList(String list)
	{
		try
		{
			// This place should include some security filters...
			setupQueryTemplate("{LIST}", list);
			executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}messages` WHERE `list` = '{LIST}';");
			executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}lists` WHERE `name` = '{LIST}';");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void removeMessage(int id)
	{
		try
		{
			// This place should include some security filters...
			setupQueryTemplate("{ID}", Integer.toString(id));
			executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}messages` WHERE `id` = '{ID}';");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	private void setListOption(String list, String option, String value)
	{
		try
		{
			setupQueryTemplate("{LIST}", list);
			setupQueryTemplate("{OPTION}", option);
			setupQueryTemplate("{VALUE}", value);
			executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}lists` SET `{OPTION}` = {VALUE} WHERE `name` = '{LIST}';");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void setListEnabled(String list, boolean value)
	{
		setListOption(list, "enabled", value ? "b'1'" : "b'0'");
	}
	public void setListRandom(String list, boolean value)
	{
		setListOption(list, "random", value ? "b'1'" : "b'0'");
	}
	public void setListDelay(String list, int delay)
	{
		setListOption(list, "delay_sec", "'" + delay + "'");
	}
	public void setListPrefix(String list, String prefix)
	{
		setListOption(list, "prefix", (prefix != null ? "'" + prefix + "'" : "''"));
	}
	public void setListSound(String list, String sound)
	{
		setListOption(list, "sound", (sound != null ? "'" + sound + "'" : "''"));
	}
	private void setMessageOption(int id, String option, String value)
	{
		try
		{
			setupQueryTemplate("{ID}", Integer.toString(id));
			setupQueryTemplate("{OPTION}", option);
			setupQueryTemplate("{VALUE}", value);
			executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}messages` SET `{OPTION}` = {VALUE} WHERE `id` = '{ID}';");
		} catch(SQLException ex) {
			BukkitPluginMain.consoleLog.warning(ex.toString());
		}
	}
	public void setMessageEnabled(int id, boolean value)
	{
		setMessageOption(id, "enabled", value ? "b'1'" : "b'0'");
	}
}
