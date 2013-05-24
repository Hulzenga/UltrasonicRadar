package com.hulzenga.ultrasonicradar;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hulzenga.ultrasonicradar.bluetooth.ConnectThread;
import com.hulzenga.ultrasonicradar.bluetooth.ConnectedThread;
import com.hulzenga.ultrasonicradar.bluetooth.Messages;
import com.hulzenga.ultrasonicradar.util.Converter;

public class RadarActivity extends Activity {

	private static final String TAG = "RADAR_ACTIVITY";
	
	//status constants	
	private static final char STATUS_CENTER = 'c';
	private static final char STATUS_MANUAL = 'm';
	private static final char STATUS_SWEEP = 'w';
	
	private static final int MIN_STEP_INTERVAL = 1;
	private static final int MAX_STEP_INTERVAL = 16;
	private static final int MIN_SAMPLE_INTERVAL = 0;
	private static final int MAX_SAMPLE_INTERVAL = 200;
	
	private static final int REQUEST_ENABLE_BT = 1;
	
	//status objects
	private int mStepInterval = 8;
	private int mSampleInterval = 40;
	
	//control objects
	private Handler mHandler;
	private Context mContext;
	private ArrayBlockingQueue<String> messageStrings;
	private boolean connected = false;
	private boolean radarViewExpanded = false;
	
	//worker threads and associated handler(s)
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private Handler connectedHandler;
	
	//Bluetooth objects
	private BluetoothAdapter mBluetoothAdapter;
	private Set<BluetoothDevice> mBluetoothDevices;
	private ArrayAdapter<String> mArrayAdapter;
	private BluetoothDevice mBlueToothDevice;
	private BluetoothSocket mSocket;
	
	//UI objects
	private LinearLayout mLeftLayout;
	private LinearLayout mRightLayout;
	private RadarView mRadarView;
	private SeekBar mSweepBar;
	private SeekBar mDistanceBar;
	private IntensityView mIntensityView;
	private RadioGroup controlGroup;
	private TextView commView;	
	private Spinner mBluetoothSpinner;	
	private Button connectButton;
	private Button leftButton;
	private Button rightButton;
	private RadioButton centerControlButton;
	private RadioButton manualControlButton;
	private RadioButton sweepControlButton;
	private Button stepMinButton;
	private TextView stepText;
	private Button stepPlusButton;
	private Button sampleMinButton;
	private TextView sampleText;
	private Button samplePlusButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		mContext = this;
		setContentView(R.layout.activity_radar);

		mBluetoothSpinner = (Spinner) findViewById(R.id.pairedDevicesSpinner);
		commView = (TextView) findViewById(R.id.commView);
		
		int capacity = 10;
		messageStrings = new ArrayBlockingQueue<String>(capacity);
		for (int i = 0; i < capacity; i++) {
			messageStrings.add("");
		}
		
		//link all the UI elements to the appropriate code
		mLeftLayout = (LinearLayout) findViewById(R.id.leftLayout);
		mRightLayout = (LinearLayout) findViewById(R.id.rightLayout);
		mRadarView = (RadarView) findViewById(R.id.radarView);
		mSweepBar = (SeekBar) findViewById(R.id.sweepBar);
		mDistanceBar = (SeekBar) findViewById(R.id.distanceBar);
		mIntensityView = (IntensityView) findViewById(R.id.intensityView1);
		controlGroup = (RadioGroup) findViewById(R.id.controlGroup);
		connectButton = (Button) findViewById(R.id.connectButton);
		leftButton = (Button) findViewById(R.id.leftButton);
		rightButton = (Button) findViewById(R.id.rightButton); 
		centerControlButton = (RadioButton)findViewById(R.id.centerControlButton);
		manualControlButton = (RadioButton)findViewById(R.id.manualControlButton);
		sweepControlButton = (RadioButton)findViewById(R.id.sweepControlButton);		
		stepMinButton = (Button) findViewById(R.id.stepMinButton);
		stepText = (TextView) findViewById(R.id.stepText);
		stepPlusButton = (Button) findViewById(R.id.stepPlusButton);
		sampleMinButton = (Button) findViewById(R.id.sampleMinButton);
		sampleText = (TextView) findViewById(R.id.sampleText);
		samplePlusButton = (Button) findViewById(R.id.samplePlusButton);
		
		initBlueTooth();
		initButtonListeners();
		initHandler();
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
	}
	
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            mArrayAdapter.add(device.getName() + "\n" + device.getAddress());	            
	        }
	    }
	};
	
	public void initBlueTooth() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "This device doesn't support Bluetooth");
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
		}
		
		mBluetoothDevices = mBluetoothAdapter.getBondedDevices();
		
		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		
		for (BluetoothDevice device: mBluetoothDevices) {
			mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		}
		
		
		mBluetoothSpinner.setAdapter(mArrayAdapter);
		
		mBluetoothAdapter.startDiscovery();
	}
	
	public void initButtonListeners() {
		
		mRadarView.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				clear();
			}
		});
		mSweepBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int i = seekBar.getProgress();
				mRadarView.adjustBuffer(i, mStepInterval);
				sendLine("S," + String.valueOf((int) (i*5.7)));
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mRadarView.setSweepAngle(progress);				
			}
		});
		mDistanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int i = seekBar.getProgress();
				sendLine("d," + String.valueOf((i+1)*40));
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mRadarView.setMaxDistance((progress+1)*40);
				
			}
		});
		mIntensityView.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				clear();
			}
		});
		connectButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!connected) {
					createSocket();
					mConnectThread = new ConnectThread(mSocket, mHandler);
					mConnectThread.start();										
				} else{
					disconnect();
				}
			}
		});
		leftButton.setOnTouchListener(new OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {					
					leftButton.setPressed(true);
					sendLine("l");
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					leftButton.setPressed(false);
					sendLine("s");
				}
				return true;
			}
		});	
		rightButton.setOnTouchListener(new OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					rightButton.setPressed(true);
					sendLine("r");
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					rightButton.setPressed(false);
					sendLine("s");
				}
				return true;
			}
		});
		centerControlButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				setStatus(STATUS_CENTER);
				sendLine("c");
			}
		});
		manualControlButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				setStatus(STATUS_MANUAL);
				sendLine("m");
			}
		});
		sweepControlButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				setStatus(STATUS_SWEEP);
				sendLine("w");
			}
		});	
		stepMinButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mStepInterval != MIN_STEP_INTERVAL) {
					setStepInterval(mStepInterval-1);
					mRadarView.adjustBuffer(mSweepBar.getProgress(), mStepInterval);
					sendLine("p," + String.valueOf(mStepInterval));
				}				
			}
		});
		stepPlusButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mStepInterval != MAX_STEP_INTERVAL) {
					mRadarView.adjustBuffer(mSweepBar.getProgress(), mStepInterval);
					setStepInterval(mStepInterval+1);
					sendLine("p," + String.valueOf(mStepInterval));
				}
			}
		});
		sampleMinButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mSampleInterval != MIN_SAMPLE_INTERVAL) {
					setSampleInterval(mSampleInterval-10);
					sendLine("a," + String.valueOf(mSampleInterval));
				}				
			}
		});
		samplePlusButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mSampleInterval != MAX_SAMPLE_INTERVAL) {
					setSampleInterval(mSampleInterval+10);
					sendLine("a," + String.valueOf(mSampleInterval));
				}				
			}
		});		
	}
	
	public void initHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {				
				switch (msg.what) {
				case Messages.CONNECTION_SUCCES:
					connected = true;
					connectButton.setText("disconnect");
					Toast.makeText(mContext, "Succesfully Connected", Toast.LENGTH_SHORT).show();
					mConnectedThread = new ConnectedThread(mSocket, mHandler);
					connectedHandler = mConnectedThread.getHandler();
					mConnectedThread.start();
					sendLine("t");
					break;
				case Messages.CONNECTION_FAILURE:
					Toast.makeText(mContext, "Failed to Connect", Toast.LENGTH_LONG).show();
					break;
				case Messages.MESSAGE_RECEIVED:
					processMessage(msg.getData().getString("data"));
					break;
				case Messages.CONNECTION_BROKEN:
					connected = false;
					connectButton.setText("connect");
					Toast.makeText(mContext, "Disconnected", Toast.LENGTH_SHORT).show();						
					break;
				}				
			}			
		};
	}
	
	public void disconnect() {
		try {
			if (mConnectedThread.isAlive()) {							
				connectedHandler.sendEmptyMessage(Messages.BREAK_CONNECTION);
			} else {
				mSocket.close();
				connected = false;
				Toast.makeText(mContext, "Disconnected", Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			Log.e(TAG, "Failed to close socket");
			e.printStackTrace();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
	}
	
	@Override
	protected void onPause() {
		if (connected) {
			disconnect();
		}
		super.onPause();
	}
	
		@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

		//TODO: move this to the connectThread, it really has no business being in here
	public void createSocket() {
		
		String selectedDevice = mArrayAdapter.getItem(mBluetoothSpinner.getSelectedItemPosition());
		String MAC = (selectedDevice.split("\n"))[1];
		
		mBlueToothDevice = mBluetoothAdapter.getRemoteDevice(MAC);//pairedDevices.iterator().next();						
		
		final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //standard SPP UUID
		
		try {					
			mSocket = mBlueToothDevice.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			Log.e(TAG, "Failed to create Socket");
			e.printStackTrace();
		}
		
		mBluetoothAdapter.cancelDiscovery();
	}
	
	public void setStatus(char status) {
		switch(status) {
		case STATUS_CENTER:
			controlGroup.check(R.id.centerControlButton);
			leftButton.setEnabled(true);
			rightButton.setEnabled(true);
			break;
		case STATUS_MANUAL:
			controlGroup.check(R.id.manualControlButton);
			leftButton.setEnabled(true);
			rightButton.setEnabled(true);
			break;
		case STATUS_SWEEP:
			controlGroup.check(R.id.sweepControlButton);
			leftButton.setEnabled(false);
			rightButton.setEnabled(false);
			break;
		}
	}
	
	public void clear() {
		mRadarView.flush();
		mIntensityView.flush();
	}
	
	
 	public void addLine(String line) {		
 		StringBuilder sb = new StringBuilder();
		try {
			messageStrings.take();
			messageStrings.put(line);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (String s : messageStrings) {
			sb.append(s);
			sb.append("\n");
		}		
		commView.setText(sb);				
	}
 	 	
 	public void sendLine(String s) {
 		if(connected) { 			
 			connectedHandler.sendMessage(Messages.dataMessage(Messages.SEND_MESSAGE, s));
 			addLine("Tx: " + s);
 		}
 	}

	public void setSweep(int deviceSweep) {
		int sweepAngle = Converter.angleToDegree(deviceSweep); 				
		mSweepBar.setProgress(sweepAngle);
		mRadarView.setSweepAngle(sweepAngle);
		mRadarView.adjustBuffer(mSweepBar.getProgress(), mStepInterval);
	}

 	public void setDistance(int distance) {
 		mDistanceBar.setProgress(distance/40 - 1);
 		mRadarView.setMaxDistance(distance);
 		mRadarView.adjustBuffer(mSweepBar.getProgress(), mStepInterval);
 	} 	

	private void setStepInterval(int stepInterval) {
		mStepInterval = stepInterval;
		stepText.setText(String.valueOf(mStepInterval));
	}
	
	private void setSampleInterval(int sampleInterval) {
		mSampleInterval = sampleInterval;
		sampleText.setText(String.valueOf(mSampleInterval));
	}

 	public void processMessage(String msg) {
 		addLine("Rx: " + msg);
 		String[] split = msg.split(",");
 		
 		try {
 			if (split[0].length() > 0) {
 	 			switch(split[0].charAt(0)) {
 	 	 		case 'M':
 	 				mRadarView.addMeasurement(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
 	 	 			mIntensityView.addIntensity(Float.parseFloat(split[4]));
 	 				break;			
 	 			case 'c':
 	 				setStatus('c');
 	 				break;
 	 			case 'm':
 	 				setStatus('m');
 	 				break; 	 				
 	 			case 'w':
 	 				setStatus('w');
 	 				break;
 	 			case 'W':
 	 				setSweep(Integer.parseInt(split[1]));
 	 				break;
 	 			case 'L':
 	 				mIntensityView.setLevel(Float.parseFloat(split[1]));
 	 				break;
 	 			case 'T':
 	 				mIntensityView.addTriggerPoint();
 	 				break;
 	 			case 'C':
 	 				clear();
 	 				break;
 	 			case 'd':
 	 				setDistance(Integer.parseInt(split[1]));
 	 				break;
 	 			case 'p':
 	 				setStepInterval(Integer.parseInt(split[1]));
 	 				break;
 	 			case 'a':
 	 				setSampleInterval(Integer.parseInt(split[1]));
 	 				break;
 	 			}
 	 		}
 		} catch (Exception e) {
 			Log.e(TAG, "something went wrong parsing the message");
 			e.printStackTrace();
 		}
 		 	
 	}
}

