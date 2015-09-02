package ru.simsonic.rscMessages;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import java.util.logging.Level;

public final class SendRawMessage
{
	private final Plugin plugin;
	private boolean isProtocollibFound = false;
	public SendRawMessage(Plugin plugin)
	{
		this.plugin = plugin;
	}
	public void onEnable()
	{
		final Plugin protocolLib = plugin.getServer().getPluginManager().getPlugin("ProtocolLib");
		isProtocollibFound = (protocolLib != null && protocolLib.isEnabled());
		BukkitPluginMain.consoleLog.info(isProtocollibFound
			? Phrases.PROTOCOLLIB_YES.toString()
			: Phrases.PROTOCOLLIB_NO.toString());
	}
	public boolean sendRawMessage(Player player, String jsonMessage)
	{
		if(isProtocollibFound)
		{
			try
			{
				final PacketContainer chat = new PacketContainer(PacketType.Play.Server.CHAT);
				chat.getChatComponents().write(0, WrappedChatComponent.fromJson(jsonMessage));
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, chat);
				return true;
			} catch(FieldAccessException | InvocationTargetException ex) {
				BukkitPluginMain.consoleLog.log(Level.WARNING, "[rscm] ProtocolLib using exception:\n", ex);
			}
		}
		return false;
	}
}
