package ru.simsonic.rscMessages;

public class RowMessage
{
	// Database
	int     id;
	String  list;
	boolean enabled;
	String  text;
	// Internal
	RowList rowList;
	long    lastBroadcast;
}