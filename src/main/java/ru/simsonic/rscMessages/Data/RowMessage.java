package ru.simsonic.rscMessages.Data;

public class RowMessage
{
	// Database
	public int     id;
	public String  list;
	public boolean enabled;
	public String  text;
	// Internal
	public RowList rowList;
	public long    lastBroadcast;
}
