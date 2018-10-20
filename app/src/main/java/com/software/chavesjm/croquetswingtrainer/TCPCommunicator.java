package com.software.chavesjm.croquetswingtrainer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

class TCPCommunicator implements Runnable {

	private Socket m_socket = null;
	private ServerSocket serverSocket = null;
	private BufferedWriter bufferedWriter = null;
	private int m_serverPort;

	TCPCommunicator(int serverPort){
		m_serverPort = serverPort;
	}

	public boolean isConnected(){
		return (m_socket != null && serverSocket != null && !m_socket.isClosed() && m_socket.isConnected());
	}

	public boolean closeConnections() throws IOException {

		m_socket.close();
		serverSocket.close();
		bufferedWriter.close();

		return true;
	}

	public boolean writeToSocket(String value)
	{
		if(bufferedWriter != null){
		try
		{
			bufferedWriter.write(value);
			bufferedWriter.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		}

		return true;

	}

	public void run() {

		try {
			serverSocket = new ServerSocket(m_serverPort);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		while (!Thread.currentThread().isInterrupted()) {

			try {

				m_socket = serverSocket.accept();

				bufferedWriter = new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}