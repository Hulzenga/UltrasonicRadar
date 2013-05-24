package com.hulzenga.ultrasonicradar.bluetooth;

import android.os.Bundle;
import android.os.Message;

/**
 * Message builder and definition class
 * @author Jouke Hulzenga
 */
public class Messages {
	
	//defined message constants
	public static final int CONNECTION_SUCCES = 0;
	public static final int CONNECTION_FAILURE = 1;
	public static final int STREAM_FAILURE = 2;
	public static final int CONNECTION_LOST = 3;
	public static final int BREAK_CONNECTION = 4;
	public static final int SEND_MESSAGE = 5;		//key: "data"
	public static final int MESSAGE_RECEIVED = 6;  	//key: "data"
	public static final int CONNECTION_BROKEN = 7;
	

	/**
	 * builds a simple message with an added data bundle
	 * @param what the type of message as defined in {@link Messages}
	 * @param data the message contents
	 * @return
	 */
	public static Message dataMessage(int what, String data) {
		Message msg = new Message();
		msg.what = what;
		Bundle b = new Bundle();
		b.putString("data", data);
		msg.setData(b);
		return msg;
	}
}
