package ru.simsonic.rscMessages.API;

import java.util.ArrayList;
import org.bukkit.Sound;

public class RowList
{
	// Database
	public int     id;
	public String  name;
	public boolean enabled;
	public boolean random;
	public int     delay_sec;
	public String  prefix;
	public Sound   sound;
	// Internal
	public transient ArrayList<RowMessage> messages = new ArrayList<>();
}
