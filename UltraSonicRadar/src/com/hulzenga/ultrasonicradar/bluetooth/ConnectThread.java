package com.hulzenga.ultrasonicradar.bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectThread extends Thread{

	private final String TAG = "CONNECT_THREAD";
	private Handler postHandler;
	private BluetoothSocket mSocket;
	
	public ConnectThread(BluetoothSocket socket, Handler postHandler) {
		this.postHandler = postHandler;
		mSocket = socket;
	}
	
	@Override
	public void run() {
		
		try {
			mSocket.connect();					
		} catch (IOException e) {				
			try {						
				mSocket.close();
			} catch (IOException e1) {							
				Log.e(TAG, "Failed to close the socket !?");
				e1.printStackTrace();						
			}					
			Log.e(TAG, "Failed to connect");
			e.printStackTrace();
		}
		
		if (mSocket.isConnected()) {
			postHandler.sendEmptyMessage(Messages.CONNECTION_SUCCES);			
		} else {
			postHandler.sendEmptyMessage(Messages.CONNECTION_FAILURE);
		}				
	}
}
