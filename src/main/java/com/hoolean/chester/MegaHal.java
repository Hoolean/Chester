package com.hoolean.chester;

import java.util.*;

/**
 * An implementation of the MegaHal AI algorithm, with strong influence from the JMegaHal and JSMegaHal implementations.
 */
public class MegaHal
{
	/**
	 * A class to hold groups of String tokens and metadata about the group.
	 */
	private class TokenGroup
	{
		// the String tokens in the group
		private final String[] tokens;

		// a hash code based on the four hash codes of each String; collisions are possible
		private final int hashCode;

		// whether or not the group can start a message
		private boolean canStart = false;
		// whether or not the group can end a message
		private boolean canEnd = false;

		/**
		 * Instantiates a TokenGroup Object with the specified tokens to be in the group. By default, the metadata is
		 * set to suggest that the TokenGroup cannot end or begin a message.
		 *
		 * @param tokens The tokens to populate the TokenGroup, as an Array of the length specified by the Markov length
		 */
		public TokenGroup(String... tokens)
		{
			// store the tokens
			this.tokens = tokens;

			// generate a hash code based on the values of the hashCode() of each of the tokens
			int hashCode = 0;
			for (String token : tokens)
			{
				hashCode += token.hashCode();
			}

			// store this generated hash code
			this.hashCode = hashCode;
		}

		/**
		 * Set whether or not the TokenGroup can start a message.
		 *
		 * @param canStart True if the TokenGroup can start a message, else False
		 */
		public void setCanStart(boolean canStart)
		{
			this.canStart = canStart;
		}

		/**
		 * Set whether or not the TokenGroup can end a message.
		 *
		 * @param canEnd True if the TokenGroup can end a message, else False
		 */
		public void setCanEnd(boolean canEnd)
		{
			this.canEnd = canEnd;
		}

		/**
		 * Gets whether or not the TokenGroup can start a message.
		 *
		 * @return True if the TokenGroup can start a message, else False
		 */
		public boolean canStart()
		{
			return this.canStart;
		}

		/**
		 * Gets whether or not the TokenGroup can end a message.
		 *
		 * @return True if the TokenGroup can end a message, else False
		 */
		public boolean canEnd()
		{
			return this.canEnd;
		}

		/**
		 * Gets the token at a specified index in the TokenGroup.
		 *
		 * @param index The index of the desired token
		 * @return the token at the index specified
		 */
		public String getToken(int index)
		{
			return this.tokens[index];
		}

		/**
		 * Returns the internal primitive array of the contained string tokens as a List Object; useful when an Array is
		 * antiquated or too primitive and hence impractical.
		 *
		 * @return The internal array of tokens as a List
		 */
		public List<String> toList()
		{
			return Arrays.asList(this.tokens);
		}

		@Override
		public int hashCode()
		{
			return this.hashCode;
		}
	}

	/**
	 * The default amount of Tokens in each TokenGroup.
	 */
	public static final int DEFAULT_MARKOV_LENGTH = 4;

	// TODO: review use of HashSet<String>; perhaps an ArrayList<String> would lead to better learning a more frequent

	/**
	 * Stores the TokenGroups that a Token is in. Is useful when randomly selecting an [ideally] appropriate middle
	 * symbol for a message;
	 */
	private final HashMap<String, HashSet<TokenGroup>> tokenMap = new HashMap<String, HashSet<TokenGroup>>();

	/**
	 * Prevents repetition of TokenGroup Objects; when metadata (such as canStart and canEnd) is changed, the change
	 * should be made effective on just the one instance of the Object in existance.
	 *
	 * As the data-type is a HashMap, if one has an identical instance, one can use the Hash to find the one existing
	 * instance that should preferably used to the clone, to ensure that these metadata changes are global to all
	 * similar Tokens.
	 */
	private final HashMap<TokenGroup, TokenGroup> tokenGroupMap = new HashMap<TokenGroup, TokenGroup>();

	/**
	 * Links a TokenGroup to tokens that have occurred directly after it in observed messages.
	 */
	private final HashMap<TokenGroup, HashSet<String>> nextTokenMap = new HashMap<TokenGroup, HashSet<String>>();

	/**
	 * Links a TokenGroup to tokens that have occurred directly before it in observed messages.
	 */
	private final HashMap<TokenGroup, HashSet<String>> previousTokenMap = new HashMap<TokenGroup, HashSet<String>>();

	/**
	 * A Random instance used for generating Random values. Obviously.
	 *
	 * Basically, it's more efficient to store one for each instance than instantiate a new one each time.
	 */
	private final Random random = new Random();

	/**
	 * The Markov length; when tokens are grouped into TokenGroups, the TokenGroups are of this length.
	 */
	private final int markovLength;

	/**
	 * Create a MegaHal instance with the default Markov length of 4.
	 */
	public MegaHal()
	{
		this(DEFAULT_MARKOV_LENGTH);
	}

	/**
	 * Create a MegaHal instance with a specified Markov length;
	 *
	 * @param markovLength The specified Markov length; see variable annotations for a better description.
	 */
	public MegaHal(int markovLength)
	{
		this.markovLength = markovLength;
	}

	/**
	 * Adds the TokenGroups that can be extracted from a message to the Markov chain, effectively allowing the instance
	 * to learn.
	 *
	 * @param message The message to extract TokenGroups from
	 */
	public void addMessage(String message)
	{
		// remove whitespace
		message = message.trim();

		// if message is too small to possibly contain the desired amount of tokens
		if (message.length() < this.markovLength)
			return; // there is nothing we can learn from this

		List<String> messageTokens = this.getTokens(message);

		// if there are not enough tokens to create a token group of the desired size
		if (messageTokens.size() < this.markovLength)
			return; // there is nothing we can learn from this

		/*
		This for-loop serves the purpose of creating overlappng TokenGroup's of the specified markovLength out of the
		list of tokens.

		For example, with the indexes of tokens from the above example and the default length of 4, the result would be
		as so (---- on a line symbolises a group with the tokens directly above included in it):

			0 1 2 3 4 5 6 7 8 9 A B
			|=|=|=| | | | | | | | |
			  |=|=|=| | | | | | | |
			    |=|=|=| | | | | | |
			      |=|=|=| | | | | |
			        |=|=|=| | | | |
			          |=|=|=| | | |
			            |=|=|=| | |
			              |=|=|=| |
			                |=|=|=|

		With groups looking much like:

			{ 0, 1, 2, 3 }
			{ 1, 2, 3, 4 }
			{ 2, 3, 4, 5 }

			... etc ...

			{ 7, 8, 9, A }
			{ 8, 9, A, B }

		... where the index numbers are replaced with the tokens.
		 */
		for (int startingIndex = 0; startingIndex <= messageTokens.size() - this.markovLength; startingIndex++)
		{
			// the tokens that will go in the new TokenGroup Object
			String[] tokenGroupTokens = new String[this.markovLength];

			// iterate through each token for this TokenGroup, starting at startingIndex and ending at
			// startingIndex + markovLength
			for (int i = 0; i < this.markovLength; i++)
			{
				// populate tokenGroupTokens with the token
				tokenGroupTokens[i] = messageTokens.get(startingIndex + i);
			}

			// create a TokenGroup Object from the tokens
			TokenGroup tokenGroup = new TokenGroup(tokenGroupTokens);

			// if there is already an instance of an identical tokenGroup Object
			if (tokenGroupMap.containsKey(tokenGroup))
			{
				// use this instead; if any changes are made to the Object, they should be made to the singular
				// tokenGroup representing these tokens.
				tokenGroup = tokenGroupMap.get(tokenGroup);
			}
			else
			{
				// store this token in the tokenGroup; for an explanation of why this self-referential Map is necessary,
				// see the explanation at the variable's definition
				tokenGroupMap.put(tokenGroup, tokenGroup);
			}

			// if this is the first group
			boolean first = startingIndex == 0;

			// if this is the last group
			boolean last = startingIndex == messageTokens.size() - this.markovLength;

			// the TokenGroup is the first; it can start a message
			if (first)
			{
				// store this property of the TokenGroup
				tokenGroup.setCanStart(true);
			}

			// the TokenGroup is the last; it can end a message
			if (last)
			{
				// store this property of the TokenGroup
				tokenGroup.setCanEnd(true);
			}

			// loop through each token in this group and add a reference from the token to the group it is in
			for (String token : tokenGroupTokens)
			{
				// if this token has no set of references
				if (!this.tokenMap.containsKey(token))
				{
					// create a set of references
					this.tokenMap.put(token, new HashSet<TokenGroup>(1));
				}

				// add this group to the list of references from the token
				this.tokenMap.get(token).add(tokenGroup);
			}

			// if there has been a token previous to this TokenGroup
			if (!first)
			{
				// the last token of the previous TokenGroup, AKA the token before the starting token
				String previousToken = messageTokens.get(startingIndex - 1);

				// if this TokenGroup has no list of previous tokens
				if (!previousTokenMap.containsKey(tokenGroup))
				{
					// create a set of previous tokens
					previousTokenMap.put(tokenGroup, new HashSet<String>(1));
				}

				// add this token to the group of known previous tokens to this TokenGroup
				previousTokenMap.get(tokenGroup).add(previousToken);
			}

			// if there has been a token previous to this TokenGroup
			if (!last)
			{
				// the first token of the next TokenGroup, AKA the token after the last token in this TokenGroup
				String nextToken = messageTokens.get(startingIndex + 1);

				// if this TokenGroup has no list of following tokens
				if (!nextTokenMap.containsKey(tokenGroup))
				{
					// create a set of following tokens
					nextTokenMap.put(tokenGroup, new HashSet<String>(1));
				}

				// add this token to the group of known following tokens to this TokenGroup
				nextTokenMap.get(tokenGroup).add(nextToken);
			}
		}
	}

	/**
	 * Get a message based on the Markov chain.
	 *
	 * @return A hopefully human-like message, assuming the storing of values previously, else an empty String.
	 */
	public String getMessage()
	{
		return this.getBestMessageFromToken(null);
	}

	/**
	 * Get a message based on the Markov chain and a message to reply to; only one token in the message will be used and
	 * will be selected entirely at Random.
	 *
	 * TODO: decide whether the Token chosen at random can be punctuation
	 *
	 * @param message The message to use to influence the message returned.
	 * @return A hopefully human-like message, assuming the storing of values previously, else an empty String.
	 */
	public String getBestMessageFromMessage(String message)
	{
		// all of the tokens in the provided message
		List<String> messageTokens = this.getTokens(message);

		// return a reply based on a Random token in the message
		return this.getBestMessageFromToken(messageTokens.get(this.random.nextInt(messageTokens.size())));
	}

	/**
	 * Get a message based on the Markov chain and a token to reply to.
	 *
	 * @param token The token to use to influence the message returned.
	 * @return A hopefully human-like message, assuming the storing of values previously, else an empty String.
	 */
	public String getBestMessageFromToken(String token)
	{
		// the list of tokens that will be concatenated at the end of the method to produce the best response message
		List<String> messageTokens = new LinkedList<String>();

		// a list of potential TokenGroups to be at the centre of the message
		List<TokenGroup> potentialMiddleGroups;

		// if there is a reference to every TokenGroup the token is in...
		if (this.tokenMap.containsKey(token))
		{
			// ...use this as the Random pool
			potentialMiddleGroups = new ArrayList<TokenGroup>(this.tokenMap.get(token));
		}
		else
		{
			// ...else use all TokenGroups
			potentialMiddleGroups = new ArrayList<TokenGroup>(this.tokenGroupMap.keySet());
		}

		// if there are no potential TokenGroups...
		if (potentialMiddleGroups.size() == 0)
		{
			// return an empty String; nothing else can be done if the Markov chain is empty
			return "";
		}

		// randomly select a middle TokenGroup from the pool
		TokenGroup middleTokenGroup = potentialMiddleGroups.get(random.nextInt(potentialMiddleGroups.size()));

		// add all of the middle tokens to the resulting message's tokens
		messageTokens.addAll(middleTokenGroup.toList());

		/*
		This while-loop iterates through TokenGroups sourced from the previous TokenGroup iterated over
		(or the middle TokenGroup if in its first iteration) until it comes across one that is a possible ending
		TokenGroup. At this point, it stops.
		 */
		TokenGroup iteratingTokenGroup = middleTokenGroup;
		while (!iteratingTokenGroup.canEnd())
		{
			// a List of tokens learnt from observed messages to come after the current TokenGroup
			List<String> potentialNextTokens = new ArrayList<String>(this.nextTokenMap.get(iteratingTokenGroup));

			// a random Token from this List
			String nextToken = potentialNextTokens.get(random.nextInt(potentialNextTokens.size()));

			// add this randomly selected Token to the tokens to be in the message so far (at the end)
			messageTokens.add(nextToken);

			/*
			The TokenGroup that will be used to find the next token. It is the final three tokens of the current
			TokenGroup with the new nextToken added at the end.

			It can be visualised like so on this loop's first iteration (after this it continues, still with the last
			elements of the list):

				messageTokens:
					0 1 2 3
					|-----|		<-- tokens used to search for this nextToken

				messageTokens after nextToken added:
					0 1 2 3 4

				tokens used to search for the next nextToken:
					0 1 2 3 4
					  |-----|	<-- tokens used to search for the next nextToken
			 */
			List<String> searchTokens = new ArrayList<String>();
			searchTokens.addAll(iteratingTokenGroup.toList().subList(1, iteratingTokenGroup.toList().size()));
			searchTokens.add(nextToken);
			iteratingTokenGroup = this.tokenGroupMap.get(new TokenGroup(searchTokens.toArray(new String[searchTokens.size()])));
		}

		// intialise the token group being iterated over to the middleGroup once more to begin searching for preceding
		// tokens
		iteratingTokenGroup = middleTokenGroup;

		/*
		This while-loop iterates through TokenGroups sourced from the previous TokenGroup iterated over
		(or the middle TokenGroup if in its first iteration) until it comes across one that is a possible ending
		TokenGroup. At this point, it stops.
		 */
		while (!iteratingTokenGroup.canStart())
		{
			// a List of tokens learnt from observed messages to come before the current TokenGroup
			ArrayList<String> potentialPreviousTokens = new ArrayList<String>(this.previousTokenMap.get(iteratingTokenGroup));

			// a random Token from this List
			String previousToken = potentialPreviousTokens.get(random.nextInt(potentialPreviousTokens.size()));

			// add this randomly selected Token to the tokens to be in the message so far (at the end)
			messageTokens.add(previousToken);

			/*
			The TokenGroup that will be used to find the next token. It is the first three tokens of the current
			TokenGroup with the new previousToken added at the beginning.

			It can be visualised like so on this loop's first iteration (after this it continues, still with the last
			elements of the list):

				messageTokens:
					  4 5 6 7 8 9 A B C (etc)
					  |-----|		<-- tokens used to search for this nextToken

				messageTokens after previousToken added:
					3 4 5 7 8 9 A B C (etc)

				tokens used to search for the next previousToken:
					3 4 5 6 7 8
					|-----|	<-- tokens used to search for the next nextToken
			 */
			List<String> searchTokens = new ArrayList<String>();
			searchTokens.add(previousToken);
			searchTokens.addAll(iteratingTokenGroup.toList().subList(0, iteratingTokenGroup.toList().size() - 1));
			iteratingTokenGroup = this.tokenGroupMap.get(new TokenGroup(searchTokens.toArray(new String[searchTokens.size()])));
			messageTokens.add(0, previousToken);
		}

		// join all messageTokens into a message
		return String.join("", messageTokens);
	}

	/**
	 * Extract all Tokens from a message, where a token is a non-interrupted sequence or either alphanumeric or
	 * punctuation characters, continuing for as long as possible.
	 *
	 * See annotation of method for a better explanation.
	 *
	 * @param message The message to extract tokens from.
	 * @return The tokens from the message as a List of Strings
	 */
	private List<String> getTokens(String message)
	{
		// a list of tokens found in the message, both alphanumerical and grammatical
		List<String> messageTokens = new ArrayList<String>();
		StringBuilder buffer = new StringBuilder(); // the progress through the token being iterated over
		boolean punctuation = true; // whether or not the current token being iterated over is punctuation

		/*
		This for-loop serves the purpose of splitting a message up into tokens of both punctuation. For example, where
		a single-underline is used to show an alphanumeric token and a double-underline is used to show an punctuation
		one, observe how the below message would be split into tokens:

			Chester is the best bot... ever!
			-------=--=---=----=---====----=

		...resulting in the tokens:

			"Chester", " ", "is", " ", "the", " ", "best", " ", "bot", "... ", "ever", "!"

		 */
		for (char character : message.toCharArray())
		{
			// if the character is alphanumeric when the previous character's token was punctuation, or vice-versa
			if ((Character.isAlphabetic(character) || Character.isDigit(character)) == punctuation)
			{
				// show this change of state of the token
				punctuation = !punctuation;

				// if the previous character was existent (it may not be if this is the first iteration)
				if (buffer.length() > 0)
				{
					// add this token to the list of tokens in this message
					messageTokens.add(buffer.toString());

					// create a blank new buffer
					buffer = new StringBuilder();
				}
			}

			// add this character to the buffer for this token
			buffer.append(character);
		}

		// tidy up the final token and do what the loop does at the end of every other token, minus the creation of a
		// new blank buffer
		if (buffer.length() > 0)
		{
			messageTokens.add(buffer.toString());
		}

		return messageTokens;
	}
}
