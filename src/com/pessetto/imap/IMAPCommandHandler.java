package com.pessetto.imap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMAPCommandHandler 
{
	private IMAPState state;
	private AuthenticateState authenticationState;
	private String currentTag;
	private boolean starttlsRequested;
	private boolean starttlsActivated;
	public IMAPCommandHandler()
	{
		state = IMAPState.NOT_AUTHENTICATED;
		authenticationState = AuthenticateState.NOT_STARTED;
		starttlsRequested = false;
		starttlsActivated = false;
	}
	
	public String GetResponseForClient(String command)
	{
		// things that must use previous tag
		if(authenticationState == AuthenticateState.CHALLENGE)
		{
			authenticationState = AuthenticateState.FINISHED;
			state = IMAPState.AUTHENTICATED;
			String response = currentTag + " OK Authentication Complete" + Configuration.CRLF;
			return response;
		}
		String tag = GetTag(command);
		command = command.toLowerCase();
		if(state == IMAPState.NOT_AUTHENTICATED)
		{
			// Not authenticated commands
		}
		if(state == IMAPState.AUTHENTICATED)
		{
			if(command.contains("status"))
			{
				Pattern statusPattern = Pattern.compile("([^\\s\\(\\)\\\"]+)");
				Matcher matcher = statusPattern.matcher(command);
				matcher.find(2);
				matcher.find();
				String box = matcher.group();
				String response = "* STATUS " + box + " (";
				boolean first = true;
				while(matcher.find())
				{
					if(!first)
					{
						response += " ";
					}
					else
					{
						first = false;
					}
					String item = matcher.group().toLowerCase();
					if(item.contains("uidnext"))
					{
						// TODO: Generate an actual unique id
						int uidnext = 0;
						response += "UIDNEXT "+uidnext;
					}
					else if(item.contains("messages"))
					{
						// TODO: implement message count
						int messageCount = 0;
						response += "MESSAGES " + messageCount;
					}
					else if(item.contains("unseen"))
					{
						// TODO: implement unseen message count
						int unseenCount = 0;
						response += "UNSEEN " + unseenCount;
					}
					else if(item.contains("recent"))
					{
						// TODO: implement recent count
						int recentCount = 0;
						response += "RECENT " + recentCount;
					}
				}
				response += ")"+Configuration.CRLF+tag+" OK STATUS completed";
				response += Configuration.CRLF;
				return response;
			}
			else if(command.contains("select"))
			{
				// Placeholder
				String response = "* 1 EXISTS"+Configuration.CRLF;
				response += "* 0 RECENT"+Configuration.CRLF;
				response += "* OK [UNSEEN 0]"+Configuration.CRLF;
				response += "* OK [UIDNEXT 2] Predicted next UID"+Configuration.CRLF;
				response += String.format("%s OK [READ-WRITE] SELECT complete" + Configuration.CRLF, tag);
				return response;
			}
			else if(command.contains("uid"))
			{
				if(command.contains("fetch"))
				{
					String[] commandParts = command.split(" ");
					String rangeStr = commandParts[3];
					String[] ranges = rangeStr.split(":");
					long minRange = Long.parseLong(ranges[0]);
					long maxRange = 0;
					if(ranges[1].contains("*"))
					{
						// This will be set to last UID in future because it should get all
						maxRange = minRange;
					}
					else
					{
						maxRange = Long.parseLong(ranges[1]);
					}
					// This will need to be updated to show all messages unseen headers
					String response = "* 1 FETCH (FLAGS (\\Seen UID 1))" + Configuration.CRLF;
					response += String.format("%s OK UID FETCH completed", tag);
					return response;
				}
			}
		}
		if(state == IMAPState.SELECTED)
		{
			
		}
		if(state == IMAPState.LOGOUT)
		{
			
		}
		// valid in any state so keep outside (noop, logout, and capability)
		if(command.contains("logout"))
		{
			state = IMAPState.LOGOUT;
		}
		if(command.contains("authenticate"))
		{
			authenticationState = AuthenticateState.CHALLENGE;
			String response = "+ Ready for authentication" + Configuration.CRLF;
			return response;
		}
		if(command.contains("capability"))
		{
			String encryptionOptions = " STARTTLS ";
			if(starttlsActivated)
			{
				encryptionOptions = "";
			}
			String response = String.format("* CAPABILITY IMAP4rev1 %s AUTH=PLAIN"+Configuration.CRLF,encryptionOptions);
			response += tag + " OK CAPABILITY completed" + Configuration.CRLF;
			return response;
		}
		if(command.contains("logout"))
		{
			String response = "* BYE IMAP4rev1 Server logging out" + Configuration.CRLF;
			response += tag + " OK LOGOUT completed"+Configuration.CRLF;
			return response;
		}
		if(command.contains("noop"))
		{
			String response = tag + " OK NOOP completed" + Configuration.CRLF;
			return response;
		}
		if(command.contains("starttls"))
		{
			String response = String.format("%s OK Begin TLS negotiation now%s", tag,Configuration.CRLF);
			starttlsRequested = true;
			return response;
		}
		return tag+" BAD" + Configuration.CRLF;
	}
	
	public boolean GetStartTLSRequested()
	{
		return starttlsRequested;
	}
	
	public void SetStartTLSActivated(boolean value)
	{
		starttlsActivated = value;
	}
	
	public void SetStartTLSRequested(boolean value)
	{
		starttlsRequested = value;
	}
	
	private String GetTag(String cmd)
	{
		currentTag = cmd.split("\\s")[0];
		return currentTag;
	}
}
