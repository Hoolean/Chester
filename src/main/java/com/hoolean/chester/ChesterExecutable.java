package com.hoolean.chester;

import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.ClientBuilder;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;

public class ChesterExecutable
{
	// keys and default values for the config
	private static final String PROPERTY_KEY_NICK = "name";
	private static final String PROPERTY_DEFAULT_NICK = "Chester";

	private static final String PROPERTY_KEY_USER = "user";
	private static final String PROPERTY_DEFAULT_USER = "Chester";

	private static final String PROPERTY_KEY_SERVER = "server";
	private static final String PROPERTY_DEFAULT_SERVER = "irc.esper.net:5555";

	private static final String PROPERTY_KEY_CHANNELS = "channels";
	private static final String PROPERTY_DEFAULT_CHANNELS = "#drtshock, #hawkfalcon";

	private static final String DEFAULT_BRAIN = "Hello World\nCan I have some coffee?\nPlease slap me";

	/**
	 * Starts a Chester IRC client with the settings found in config.properties.
	 *
	 * @param args The command line arguments called with it
	 */
	public static void main(String[] args)
	{
		// TODO: support all properties

		Properties properties = getProperties();
		ClientBuilder clientBuilder = new ClientBuilder();

		if (properties.stringPropertyNames().contains(PROPERTY_KEY_NICK))
		{
			clientBuilder.nick(properties.getProperty(PROPERTY_KEY_NICK));
		}
		else
		{
			clientBuilder.nick(PROPERTY_DEFAULT_NICK);
		}

		if (properties.stringPropertyNames().contains(PROPERTY_KEY_USER))
		{
			clientBuilder.user(properties.getProperty(PROPERTY_KEY_USER));
		}
		else
		{
			clientBuilder.user(PROPERTY_DEFAULT_USER);
		}

		String serverString;
		if (properties.stringPropertyNames().contains(PROPERTY_KEY_SERVER))
		{
			serverString = properties.getProperty(PROPERTY_KEY_SERVER);
		}
		else
		{
			serverString = PROPERTY_DEFAULT_SERVER;
		}

		String[] serverParts = serverString.split(":");
		clientBuilder.server(serverParts[0]);
		try
		{
			clientBuilder.server(Integer.parseInt(serverParts[1]));
		}
		catch (NumberFormatException e)
		{
			System.err.println(String.format("'%s' is not a valid server port number. The default port will be used.", serverParts[1]));
		}

		Client client = clientBuilder.build();

		String channelString;
		if (properties.stringPropertyNames().contains(PROPERTY_KEY_CHANNELS))
		{
			channelString = properties.getProperty(PROPERTY_KEY_CHANNELS);
		}
		else
		{
			channelString = PROPERTY_DEFAULT_CHANNELS;
		}

		for (String channel : channelString.split(","))
		{
			client.addChannel(channel.trim());
		}

		// TODO: consider changing Consumer to lambda

		// log any Exceptions
		client.setExceptionListener(new Consumer<Exception>()
		{
			@Override
			public void accept(Exception e)
			{
				e.printStackTrace();
			}
		});

		// create an instance of MegaHal to learn and reply
		MegaHal hal = new MegaHal();

		// the defaul brain file
		File brainFile = new File("brain.txt");
		Scanner brainScanner;
		try
		{
			brainScanner = new Scanner(brainFile);
		}
		catch (FileNotFoundException e)
		{
			brainScanner = new Scanner(DEFAULT_BRAIN);
		}

		// load each sentence
		while (brainScanner.hasNextLine())
		{
			hal.addMessage(brainScanner.nextLine());
		}

		// register a Listener to cause Chester to learn and reply to messages in channels he joins
		client.getEventManager().registerEventListener(new ConverseListener(hal, brainFile));
	}

	/**
	 * Gets the properties at config.properties else creates the file with default value.
	 *
	 * @return The Properties found, else the default Properties
	 */
	private static Properties getProperties()
	{
		// TODO: remove unnecessary timestamp
		// TODO: have values unescaped

		// the default config location
		File configFile = new File("config.properties");
		Properties properties = new Properties();
		boolean createDefault = false;
		try
		{
			properties.load(new FileInputStream(configFile));
		}
		catch (FileNotFoundException e)
		{
			System.err.println(String.format("Config file does not exist at '%s'; attempting to create it.", configFile.getAbsolutePath()));
			createDefault = true;
		}
		catch (IOException e)
		{
			System.err.println(String.format("Failed to read config file at '%s'.", configFile.getAbsolutePath()));
			createDefault = true;
		}

		if (createDefault)
		{
			properties.setProperty(PROPERTY_KEY_NICK, PROPERTY_DEFAULT_NICK);
			properties.setProperty(PROPERTY_KEY_USER, PROPERTY_DEFAULT_USER);
			properties.setProperty(PROPERTY_KEY_SERVER, PROPERTY_DEFAULT_SERVER);
			properties.setProperty(PROPERTY_KEY_CHANNELS, PROPERTY_DEFAULT_CHANNELS);

			try
			{
				properties.store(new FileOutputStream(configFile), null);
			}
			catch (IOException e)
			{
				System.err.println("There was an issue when creating a default config file.");
			}

			return properties;
		}

		return properties;
	}
}
