package com.pessetto.imap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

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
				if(commandHandler.GetStartTLSRequested())
				{
					PrintDebugMessage(cmd);
					commandHandler.SetStartTLSRequested(false);
					MakeSocketSSL(ImapSocket);
					commandHandler.SetStartTLSActivated(true);
				}
				String[] subCmds = cmd.split(Configuration.CRLF);
				String[] subResponses = response.split(Configuration.CRLF);
				for(String subCmd : subCmds)
				{
					System.out.println("Client("+threadNo+")> " + subCmd);
				}
				for(String subResponse : subResponses)
				{
					System.out.println("Server("+threadNo+")> " + subResponse);
				}
				System.out.println();
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
	
	private void MakeSocketSSL(Socket old) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException, InterruptedException
	{
		PrintDebugMessage("Encrypting Socket");
		KeyStore keyStore  = KeyStore.getInstance(KeyStore.getDefaultType());
		//same as used by SMTP
		InputStream ksIs = IMAPConnectionHandler.class.getClassLoader().getResourceAsStream("keys");
		keyStore.load(ksIs,"password".toCharArray());
		if(ksIs != null)
		{
			ksIs.close();
		}
		
		PrintDebugMessage("Opening keystore");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, "password".toCharArray());
		
		PrintDebugMessage("Opening truststore");
		InputStream trustStoreIs = IMAPConnectionHandler.class.getClassLoader().getResourceAsStream("truststore");
		KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(trustStoreIs, "password".toCharArray());
		TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustFactory.init(trustStore);
		
		//SSLContext sslContext = SSLContext.getDefault();
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		if(keyManagerFactory.getKeyManagers() == null)
		{
			PrintDebugMessage("Key manager array appears to be null");
		}
		else
		{
			PrintDebugMessage(String.format("Key manager array is of size %s",keyManagerFactory.getKeyManagers().length));
		}
		sslContext.init(keyManagerFactory.getKeyManagers(),trustFactory.getTrustManagers(), null);
		
		PrintDebugMessage("Establishing new SSLSocket");
		SSLSocket newSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(old, null,old.getPort(),false);
		newSocket.setEnabledProtocols(newSocket.getSupportedProtocols());
		newSocket.setEnabledCipherSuites(newSocket.getSupportedCipherSuites());
		newSocket.setUseClientMode(false);
		Thread.sleep(1000);
		PrintDebugMessage("Starting Handshake");
		newSocket.startHandshake();
		PrintDebugMessage("Handshake ended");
		if(newSocket.getNeedClientAuth())
		{
			Certificate[] serverCerts = newSocket.getSession().getLocalCertificates();
			PrintDebugMessage("Checking server certificates");
			Date today = new Date();
			for(Certificate cert : serverCerts)
			{
				X509Certificate x509Cert = (X509Certificate) cert;
				Date notAfter = x509Cert.getNotAfter();
				Date notBefore = x509Cert.getNotBefore();
				if(today.after(notAfter))
				{
					PrintDebugMessage("Bad certificate: past valid date.");
				}
				else if(today.before(notBefore))
				{
					PrintDebugMessage("Bad certificate: before valid date");
				}
				else
				{
					PrintDebugMessage("Certifcate appears to have valid date");
				}
			}
		}
		PrintDebugMessage("Setting old socket to new");
		old = newSocket;
		PrintDebugMessage("Socket should be encrypted now");
	}
	
	private void PrintDebugMessage(String msg)
	{
		System.out.println(String.format("Server(%s)[DEBUG]>%s", threadNo,msg));
	}

}
