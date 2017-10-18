package com.pessetto.imap;

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
			String response = "* CAPABILITY IMAPrev1 STARTLS AUTH=PLAIN LOGINDISABLED"+Configuration.CRLF;
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
