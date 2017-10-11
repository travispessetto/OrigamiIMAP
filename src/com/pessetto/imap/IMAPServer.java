package com.pessetto.imap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IMAPServer {

	private int Port;
	private ServerSocket imapSocket;
	public IMAPServer(int port)
	{
		Port = port;
	}
	
	public static void main(String args[]) throws Exception
	{
		int bindPort = 143;
		if(args.length == 1)
		{
			System.out.println("Setting port to " + args[0]);
			bindPort = Integer.parseInt(args[0]);
		}
		else
		{
			System.out.println("Default to " + bindPort);
		}
		IMAPServer server = new IMAPServer(bindPort);
		server.start();
	}
	
	public void start() throws IOException
	{
		ExecutorService threadPool = Executors.newWorkStealingPool();
		InetSocketAddress bindAddress = new InetSocketAddress(Port);
		imapSocket = new ServerSocket();
		imapSocket.setReuseAddress(true);
		imapSocket.bind(bindAddress);
		while(!Thread.interrupted()  || !Thread.currentThread().isInterrupted())
		{
			System.out.println("IMAP Awaiting Connection");
			Socket connectionSocket = imapSocket.accept();
			IMAPConnectionHandler handler = new IMAPConnectionHandler(connectionSocket);
			threadPool.submit(handler);
		}
	}

}
