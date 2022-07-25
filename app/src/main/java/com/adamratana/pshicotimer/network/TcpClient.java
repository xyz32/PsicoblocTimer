package com.adamratana.pshicotimer.network;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TcpClient {
	public static final int TIMEOUT_COUNT = 2;
	public static final String TAG = TcpClient.class.getSimpleName();
	private final String serverIp; //server IP address
	private final int serverPort;
	// message to send to the server
	private String mServerMessage;
	// sends message received notifications
	private OnMessageReceived mMessageListener = null;
	// while this is true, the server will continue running
	private boolean mRun = false;
	// used to send messages
	private PrintWriter mBufferOut;
	// used to read messages from the server
	private BufferedReader mBufferIn;
	private ScheduledExecutorService executor;
	int timeoutCounter = TIMEOUT_COUNT;

	/**
	 * Constructor of the class. OnMessagedReceived listens for the messages received from server
	 */
	public TcpClient(OnMessageReceived listener, String serverIp, int serverPort) {
		mMessageListener = listener;
		this.serverIp = serverIp;
		this.serverPort = serverPort;
	}

	/**
	 * Sends the message entered by client to the server
	 *
	 * @param message text entered by client
	 */
	public void sendMessage(final String message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				if (mBufferOut != null) {
					mBufferOut.println(message);
					mBufferOut.flush();
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
	}

	/**
	 * Close the connection and release the members
	 */
	public void stopClient() {

		mRun = false;

		if (mBufferOut != null) {
			mBufferOut.flush();
			mBufferOut.close();
		}

		mMessageListener = null;
		mBufferIn = null;
		mBufferOut = null;
		mServerMessage = null;
	}

	public void run() {

		mRun = true;

		try {
			//here you must put your computer's IP address.
			InetAddress serverAddr = InetAddress.getByName(serverIp);

			Log.d("TCP Client", "C: Connecting...");

			//create a socket to make the connection with the server
			Socket socket = new Socket(serverAddr, serverPort);
			//sends the message to the server
			mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

			//receives the message which the server sends back
			mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			mMessageListener.onConnect();

			try {

				executor = Executors.newSingleThreadScheduledExecutor();
				executor.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						if (timeoutCounter == 0) {
							mRun = false;
							try {
								socket.close();
							} catch (IOException e) {
								if (mMessageListener != null) {
									mMessageListener.onDisconnect();
								}
							}
						}
						timeoutCounter -= 1;
						sendMessage("PING");
					}
				},1000,1000, TimeUnit.MILLISECONDS);
				//in this while the client listens for the messages sent by the server
				while (mRun) {

					mServerMessage = mBufferIn.readLine();

					if (mServerMessage != null && mMessageListener != null) {
						//call the method messageReceived from MyActivity class
						if (mServerMessage.startsWith("PONG")) {
							timeoutCounter = TIMEOUT_COUNT;
						} else {
							mMessageListener.messageReceived(mServerMessage);
						}
					}

				}

				Log.d("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

			} catch (Exception e) {
				Log.e("TCP", "S: Error", e);
			} finally {
				//the socket must be closed. It is not possible to reconnect to this socket
				// after it is closed, which means a new socket instance has to be created.
				socket.close();
				executor.shutdown();
				if (mMessageListener != null) {
					mMessageListener.onDisconnect();
				}
			}

		} catch (Exception e) {
			Log.e("TCP", "C: Error", e);
			if (mMessageListener != null) {
				mMessageListener.onFailed();
			}
		}
	}

	//Declare the interface. The method messageReceived(String message) will must be implemented in the Activity
	//class at on AsyncTask doInBackground
	public interface OnMessageReceived {
		void messageReceived(String message);
		void onConnect();
		void onDisconnect();
		void onFailed();
	}

}