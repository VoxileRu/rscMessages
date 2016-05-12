package ru.simsonic.rscMessages.API;

import java.util.ArrayList;

public class RowList
{
	// Database
	public int     id;
	public String  name;
	public boolean enabled;
	public boolean random;
	public int     delay_sec;
	public String  prefix;
	public String  sound;
	// Internal
	public transient ArrayList<RowMessage> messages = new ArrayList<>();
}
