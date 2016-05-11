package ru.simsonic.rscMessages.API;

public class RowMessage
{
	// Database
	public int     id;
	public String  list;
	public boolean enabled;
	public String  text;
	public boolean isJson;
	// Internal
	public transient RowList rowList;
	public transient long    lastBroadcast;
}
