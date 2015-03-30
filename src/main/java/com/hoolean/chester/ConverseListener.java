package com.hoolean.chester;

import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler;

import java.io.*;

public class ConverseListener
{
	/**
	 * A unicode character used to split nicks so as not to ping users.
	 *
	 * TODO: make the usage of this character configurable
	 */
	private static final char NICK_SPLITTER = (char) 0x200b;

	/**
	 * The instance of MegaHal to teach and use for generating responses.
	 */
	private final MegaHal megaHal;

	/**
	 * The file to save sentences to, allowing permanence in the learning of MegaHal.
	 */
	private final File brainFile;

	/**
	 * Create an instance of ConverseListener with the desired instance of MegaHal to teach and use for generating
	 * responses.
	 *
	 * @param megaHal The instance of MegaHal to teach and use for generating responses
	 */
	public ConverseListener(MegaHal megaHal, File brainFile)
	{
		this.megaHal = megaHal;
		this.brainFile = brainFile;
	}

	/**
	 * Listens out for when a User speaks on a channel, in order to learn from speech and reply when mentioned.
	 *
	 * @param event The details of the event of the User speaking on the Channel
	 */
	@Handler
	public void onMessage(ChannelMessageEvent event)
	{
		String message = event.getMessage();

		// if should prompt a reply
		if (isMessagePinging(message, event.getClient().getNick()))
		{
			// use a builder, as it is likely faster for quick String manipulation
			StringBuilder fixedBuilder = new StringBuilder();
			fixedBuilder.append(message);

			// while the bot's nick appears at some index in the message...
			int index;
			while ((index = fixedBuilder.toString().toLowerCase().indexOf(event.getClient().getNick().toLowerCase())) != -1)
			{
				// ...remove it
				// TODO: review the practise of removing it, there may be a better alternative
				fixedBuilder.delete(index, index + event.getClient().getNick().length());
			}

			// calculate the best reply to the message with MegaHal
			String reply = this.megaHal.getBestMessageFromMessage(fixedBuilder.toString());

			// send the reply
			event.getClient().sendMessage(event.getChannel(), removePings(reply, event.getChannel()));
		}
		else // if should learn from message
		{
			this.megaHal.addMessage(message);

			try
			{
				// store the message to a File
				PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(this.brainFile)));
				printWriter.println(message);
			}
			catch (IOException e)
			{
				System.err.println("Could not append message to a file.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Whether or not a message contains an instance of the User's nick that is likely to ping them.
	 *
	 * @param message The message that may contain the User's nick
	 * @param nick The User's nick
	 * @return True if the message does ping them, else false
	 */
	private static boolean isMessagePinging(String message, String nick)
	{
		// make both Strings lowercase, as we wish the contains to be case-insensitive
		return message.toLowerCase().contains(nick.toLowerCase());
	}

	/**
	 * Splits all instances of every nick from a channel-user in a message with a unicode character, so as not to ping
	 * them if their nick is mentioned in a message.
	 *
	 * The character is invisible on most clients, so their should be no visible effect.
	 *
	 * @param message The message to remove pings from
	 * @param channel The channel to source users and their nicks from
	 * @return The message with all nicks split
	 */
	private static String removePings(String message, Channel channel)
	{
		// using a StringBuilder will probably make String manipulation faster
		StringBuilder fixedBuilder = new StringBuilder();
		fixedBuilder.append(message);

		// for each nick in the channel
		for (String nick : channel.getNames())
		{
			// while the nick is in the message at some index
			int index;
			while ((index = fixedBuilder.toString().toLowerCase().indexOf(nick.toLowerCase())) != -1)
			{
				// split it with the unicode character
				// e.g. Chester -> C<CHARACTER>hester
				fixedBuilder.insert(index + 1, NICK_SPLITTER);
			}
		}

		// return the modified result
		return fixedBuilder.toString();
	}
}
