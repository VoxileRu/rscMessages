package ru.simsonic.rscMessages;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
		BukkitPluginMain.consoleLog.log(Level.INFO, "[rscm] {0}", isProtocollibFound
			? Phrases.PROTOCOLLIB_YES.toString()
			: Phrases.PROTOCOLLIB_NO.toString());
	}
	public boolean sendRawMessage(Player player, String jsonMessage)
	{
		if(isProtocollibFound)
		{
			try
			{
				final com.comphenix.protocol.events.PacketContainer chat = new com.comphenix.protocol.events.PacketContainer(
					com.comphenix.protocol.PacketType.Play.Server.CHAT);
				chat.getChatComponents().write(0,
					com.comphenix.protocol.wrappers.WrappedChatComponent.fromJson(jsonMessage));
				com.comphenix.protocol.ProtocolLibrary.getProtocolManager().sendServerPacket(player, chat);
				return true;
			} catch(Exception ex) {
				BukkitPluginMain.consoleLog.log(Level.WARNING, "[rscm] ProtocolLib using exception:\n", ex);
			}
		}
		return false;
	}
}
