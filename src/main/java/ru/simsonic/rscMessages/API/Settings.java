package ru.simsonic.rscMessages.API;

import ru.simsonic.rscCommonsLibrary.ConnectionMySQL.ConnectionParams;

public interface Settings
{
	public static final String UPDATER_URL = "http://simsonic.github.io/rscMessages/latest.json";
	public static final String CHAT_PREFIX = "{_DC}[rscm] {_LS}";
	
	public abstract void onLoad();
	public abstract void onEnable();
	
	public abstract boolean doUpdateDB_v2v3();
	public abstract boolean doUpdateDB_v3v4();
	public abstract boolean doUpdateDB_v5v6();
	public abstract ConnectionParams getDatabaseCP();
	
	public abstract String  getLanguage();
	public abstract long    getAutofetchInterval();
	public abstract String  getNewbiesListName();
	public abstract long    getNewbiesInterval();
	public abstract boolean getBroadcastToConsole();
	public abstract boolean getUseMetrics();
}
