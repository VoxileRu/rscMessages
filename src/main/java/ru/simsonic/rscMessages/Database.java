package ru.simsonic.rscMessages;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import ru.simsonic.utilities.ConnectionMySQL;

public class Database extends ConnectionMySQL
{
	private final Plugin plugin;
	Database(Plugin plugin)
	{
		this.plugin = plugin;
	}
	public void StartAndDeploy()
	{
		if(isConnected())
			executeUpdate(loadResourceSQLT("Deploy"));
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
				list.id = rsLists.getInt("id");
				list.name = rsLists.getString("name");
				list.enabled = rsLists.getBoolean("enabled");
				list.random = rsLists.getBoolean("random");
				list.delay_sec = rsLists.getInt("delay_sec");
				list.prefix = rsLists.getString("prefix");
				if(list.prefix == null)
					list.prefix = "";
				result.put(list.name.toLowerCase(), list);
			}
			rsLists.close();
		} catch(SQLException ex) {
			consoleLog.log(Level.WARNING, "[rscm] Exception in fetch(1): {0}", ex);
		}
		try(final ResultSet rsMessages = executeQuery("SELECT * FROM `{DATABASE}`.`{PREFIX}messages` ORDER BY `id` ASC;"))
		{
			while(rsMessages.next())
			{
				final RowMessage msg = new RowMessage();
				msg.id = rsMessages.getInt("id");
				msg.enabled = rsMessages.getBoolean("enabled");
				msg.list = rsMessages.getString("list");
				msg.text = rsMessages.getString("text");
				final RowList list = result.get(msg.list.toLowerCase());
				if(list != null)
				{
					msg.rowList = list;
					list.messages.add(msg);
				}
			}
			rsMessages.close();
		} catch(SQLException ex) {
			consoleLog.log(Level.WARNING, "[rscm] Exception in fetch(2): {0}", ex);
		}
		return result;
	}
}