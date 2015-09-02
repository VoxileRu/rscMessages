package ru.simsonic.rscMessages;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import ru.simsonic.rscCommonsLibrary.ConnectionMySQL;
import ru.simsonic.rscMessages.Data.RowList;
import ru.simsonic.rscMessages.Data.RowMessage;

public class Database extends ConnectionMySQL
{
	Database()
	{
		super(BukkitPluginMain.consoleLog);
	}
	public void deploy()
	{
		if(isConnected())
			executeUpdateT("Deploy");
	}
	public void cleanup()
	{
		if(isConnected())
			executeUpdateT("Cleanup");
	}
	public void Update_v2_to_v3()
	{
		if(isConnected())
			executeUpdateT("Update_v2_to_v3");
	}
	public void Update_v3_to_v4()
	{
		if(isConnected())
			executeUpdateT("Update_v3_to_v4");
	}
	public Map<String, RowList> fetch()
	{
		final HashMap<String, RowList> result = new HashMap<>();
		if(!isConnected())
			return result;
		try(final ResultSet rsLists = executeQuery("SELECT * FROM `{DATABASE}`.`{PREFIX}lists` ORDER BY `id` ASC;"))
		{
			while(rsLists.next())
			{
				final RowList list = new RowList();
				list.id        = rsLists.getInt("id");
				list.name      = rsLists.getString("name");
				list.enabled   = rsLists.getBoolean("enabled");
				list.random    = rsLists.getBoolean("random");
				list.delay_sec = rsLists.getInt("delay_sec");
				list.prefix    = rsLists.getString("prefix");
				if(list.prefix == null)
					list.prefix = "";
				result.put(list.name.toLowerCase(), list);
			}
			rsLists.close();
		} catch(SQLException ex) {
			logger.log(Level.WARNING, "Exception in fetch(1): {0}", ex);
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
			logger.log(Level.WARNING, "Exception in fetch(2): {0}", ex);
		}
		return result;
	}
	public void addList(String list)
	{
		// This place should include some security filters...
		setupQueryTemplate("{LIST}", list);
		executeUpdate("INSERT IGNORE INTO `{DATABASE}`.`{PREFIX}lists` (`name`) VALUES ('{LIST}');");
	}
	public void addMessage(String list, String message)
	{
		// This place should include some security filters...
		setupQueryTemplate("{LIST}", list);
		setupQueryTemplate("{MESSAGE}", message);
		executeUpdate("INSERT INTO `{DATABASE}`.`{PREFIX}messages` (`list`, `text`) VALUES ('{LIST}', '{MESSAGE}');");
	}
	public void editMessage(int id, String text)
	{
		// This place should include some security filters...
		setupQueryTemplate("{ID}", Integer.toString(id));
		setupQueryTemplate("{TEXT}", text);
		executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}messages` SET `text` = '{TEXT}' WHERE `id` = '{ID}';");
	}
	public void removeList(String list)
	{
		// This place should include some security filters...
		setupQueryTemplate("{LIST}", list);
		executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}messages` WHERE `list` = '{LIST}';");
		executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}lists` WHERE `name` = '{LIST}';");
	}
	public void removeMessage(int id)
	{
		// This place should include some security filters...
		setupQueryTemplate("{ID}", Integer.toString(id));
		executeUpdate("DELETE FROM `{DATABASE}`.`{PREFIX}messages` WHERE `id` = '{ID}';");
	}
	private void setListOption(String list, String option, String value)
	{
		setupQueryTemplate("{LIST}", list);
		setupQueryTemplate("{OPTION}", option);
		setupQueryTemplate("{VALUE}", value);
		executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}lists` SET `{OPTION}` = {VALUE} WHERE `name` = '{LIST}';");
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
		setListOption(list, "prefix", (prefix != null) ? "'" + prefix + "'" : "''");
	}
	private void setMessageOption(int id, String option, String value)
	{
		setupQueryTemplate("{ID}", Integer.toString(id));
		setupQueryTemplate("{OPTION}", option);
		setupQueryTemplate("{VALUE}", value);
		executeUpdate("UPDATE `{DATABASE}`.`{PREFIX}messages` SET `{OPTION}` = {VALUE} WHERE `id` = '{ID}';");
	}
	public void setMessageEnabled(int id, boolean value)
	{
		setMessageOption(id, "enabled", value ? "b'1'" : "b'0'");
	}
}
