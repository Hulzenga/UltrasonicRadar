package com.hulzenga.ultrasonicradar.bluetooth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hulzenga.ultrasonicradar.bluetooth.Messages;

public class ConnectedThread extends Thread {

	private final String TAG = "CONNECTED_THREAD";
	
	private Handler postHandler;
	private Handler mHandler;
	
	private BluetoothSocket mSocket;	
	private boolean stop = false;	
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	private BufferedReader mReader;
	private BufferedWriter mWriter;
	
	
	public ConnectedThread(BluetoothSocket socket, Handler postHandler) {
		this.mSocket = socket;
		this.postHandler = postHandler;
		initHandler();
	}	
	
	public void initHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case Messages.BREAK_CONNECTION:
					stop = true;
					break;
				case Messages.SEND_MESSAGE:
					try {						
						mWriter.write(msg.getData().getString("data") + "\n");
						mWriter.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}				
			}			
		};
	}
		
	public void run() {		
		try {
			mInputStream = mSocket.getInputStream();
			mReader = new BufferedReader(new InputStreamReader(mInputStream));
			mOutputStream = mSocket.getOutputStream();
			mWriter = new BufferedWriter(new OutputStreamWriter(mOutputStream));
		} catch (IOException e) {
			Log.e(TAG, "Failed to acquire connection streams");
			e.printStackTrace();
			postHandler.sendEmptyMessage(Messages.STREAM_FAILURE);
		}
		
		if(mInputStream != null && mOutputStream != null) {

			String line = null;			
			while(!stop) {					
				try {
					if (mReader.ready()) {
						line = mReader.readLine();
					}					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
							
				if (line != null) {
					postHandler.sendMessage(Messages.dataMessage(Messages.MESSAGE_RECEIVED, line));
					line = null;
				}
				
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//finish up by closing the streams and the socket
		try {
			mReader.close();
			mWriter.close();
			mSocket.close();			
			postHandler.sendEmptyMessage(Messages.CONNECTION_BROKEN);
			
		} catch (IOException e1) {							
			Log.e(TAG, "Failed to close the socket");
		}
	}
	
	public Handler getHandler() {
		return mHandler;
	}
}
