package net.ess3.xmpp;

import static net.ess3.I18n._;
import net.ess3.api.ICommandHandler;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.ess3.commands.EssentialsCommandHandler;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class EssentialsXMPP extends JavaPlugin implements IEssentialsXMPP
{
	private static final Logger LOGGER = Logger.getLogger("Minecraft");
	private static EssentialsXMPP instance = null;
	private transient UserManager users;
	private transient XMPPManager xmpp;
	private transient IEssentials ess;
	private transient ICommandHandler commandHandler;

	public static IEssentialsXMPP getInstance()
	{
		return instance;
	}

	@Override
	public void onEnable()
	{
		instance = this;

		final PluginManager pluginManager = getServer().getPluginManager();
		ess = (IEssentials)pluginManager.getPlugin("Essentials-3");
		if (!this.getDescription().getVersion().equals(ess.getDescription().getVersion()))
		{
			LOGGER.log(Level.WARNING, _("versionMismatchAll"));
		}
		if (!ess.isEnabled())
		{
			this.setEnabled(false);
			return;
		}

		final EssentialsXMPPPlayerListener playerListener = new EssentialsXMPPPlayerListener(ess);
		pluginManager.registerEvents(playerListener, this);

		users = new UserManager(this.getDataFolder());
		xmpp = new XMPPManager(this);

		ess.addReloadListener(users);
		ess.addReloadListener(xmpp);

		commandHandler = new EssentialsCommandHandler(EssentialsXMPP.class.getClassLoader(), "net.ess3.xmpp.Command", "essentials.", ess);
	}

	@Override
	public void onDisable()
	{
		if (xmpp != null)
		{
			xmpp.disconnect();
		}
		instance = null;
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args)
	{
		return commandHandler.handleCommand(sender, command, commandLabel, args);
	}

	@Override
	public void setAddress(final Player user, final String address)
	{
		final String username = user.getName().toLowerCase(Locale.ENGLISH);
		instance.users.setAddress(username, address);
	}

	@Override
	public String getAddress(final String name)
	{
		return instance.users.getAddress(name);
	}

	@Override
	public IUser getUserByAddress(final String address)
	{
		String username = instance.users.getUserByAddress(address);
		return username == null ? null : ess.getUser(username);
	}

	@Override
	public boolean toggleSpy(final Player user)
	{
		final String username = user.getName().toLowerCase(Locale.ENGLISH);
		final boolean spy = !instance.users.isSpy(username);
		instance.users.setSpy(username, spy);
		return spy;
	}

	@Override
	public String getAddress(final Player user)
	{
		return instance.users.getAddress(user.getName());
	}

	@Override
	public boolean sendMessage(final Player user, final String message)
	{
		return instance.xmpp.sendMessage(instance.users.getAddress(user.getName()), message);
	}

	@Override
	public boolean sendMessage(final String address, final String message)
	{
		return instance.xmpp.sendMessage(address, message);
	}

	@Override
	public List<String> getSpyUsers()
	{
		return instance.users.getSpyUsers();
	}

	@Override
	public void broadcastMessage(final IUser sender, final String message, final String xmppAddress)
	{
		ess.broadcastMessage(sender, message);
		try
		{
			for (String address : getSpyUsers())
			{
				if (!address.equalsIgnoreCase(xmppAddress))
				{
					sendMessage(address, message);
				}
			}
		}
		catch (Exception ex)
		{
			// Ignore exceptions
		}
	}
}
