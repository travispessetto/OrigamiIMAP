package com.pessetto.imap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IMAPCommandHandler 
{
	private IMAPState state;
	private AuthenticateState authenticationState;
	private String currentTag;
	public IMAPCommandHandler()
	{
		state = IMAPState.NOT_AUTHENTICATED;
		authenticationState = AuthenticateState.NOT_STARTED;
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
			String response = "* CAPABILITY IMAP4rev1 STARTLS AUTH=PLAIN"+Configuration.CRLF;
			response += tag + " OK CAPABILITY completed" + Configuration.CRLF;
			return response;
		}
		if(command.contains("logout"))
		{
			String response = "* BYE IMAP4rev1 Server logging out" + Configuration.CRLF;
			response += tag + " OK LOGOUT completed"+Configuration.CRLF;
			return response;
		}
		return tag+" BAD" + Configuration.CRLF;
	}
	
	private String GetTag(String cmd)
	{
		currentTag = cmd.split("\\s")[0];
		return currentTag;
	}
}
