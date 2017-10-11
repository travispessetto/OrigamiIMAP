package com.pessetto.imap;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class IMAPConnectionHandler implements Runnable{

	private Socket ImapSocket;
	private int threadNo;
	public IMAPConnectionHandler(Socket imapSocket)
	{
		ImapSocket = imapSocket;
		threadNo = 1 + (int)(Math.random() * ((100 - 1) + 1));
	}
	
	@Override
	public void run() 
	{
		try
		{
			IMAPCommandHandler commandHandler = new IMAPCommandHandler();
			DataOutputStream outToClient = new DataOutputStream(ImapSocket.getOutputStream());
			Scanner inFromClient = new Scanner(ImapSocket.getInputStream());
			inFromClient.useDelimiter(Configuration.CRLF);
			String welcome = "* OK Origami IMAPrev1 server ready"+Configuration.CRLF;
			outToClient.writeBytes(welcome);
			String cmd = "";
			while(!Thread.currentThread().isInterrupted() && (cmd = GetFullCmd(inFromClient)) != "QUIT")
			{
				String response = commandHandler.GetResponseForClient(cmd);
				outToClient.writeBytes(response);
				System.out.println("Client("+threadNo+")> " + cmd);
				System.out.print("Server("+threadNo+")> " + response);
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
		
	}
	
	private static String GetFullCmd(Scanner inFromClient)
	{
		String raw = "QUIT";
		if(inFromClient.hasNextLine())
		{
			raw = inFromClient.nextLine();
		}
		return raw;
	}

}
