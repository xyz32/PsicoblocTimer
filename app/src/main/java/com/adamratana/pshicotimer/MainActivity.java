package com.adamratana.pshicotimer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.adamratana.pshicotimer.network.TcpClient;
import com.adamratana.pshicotimer.ui.ControlState;
import com.adamratana.pshicotimer.ui.StartTrigger;
import com.adamratana.pshicotimer.ui.TimerRunning;

import java.util.ArrayList;
import java.util.List;

import needle.BackgroundThreadExecutor;
import needle.Needle;

public class MainActivity extends AppCompatActivity implements TcpClient.OnMessageReceived {
	View mainContainer;
	ControlState crState;
	final Handler handler = new Handler();

	BackgroundThreadExecutor NETWORK_EXECUTOR = Needle.onBackgroundThread()
			.withTaskType("network")
			.withThreadPoolSize(1);
	private TcpClient tcp;
	private AlertDialog alertDialog;
	private final List<String> leftHistoryList = new ArrayList<>();
	private final List<String> rightHistoryList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ArrayAdapter<String> leftHistoryAdapter = new ArrayAdapter<>(this, R.layout.list_history_item, leftHistoryList);
		ArrayAdapter<String> rightHistoryAdapter = new ArrayAdapter<>(this, R.layout.list_history_item, rightHistoryList);

		((ListView)findViewById(R.id.listLeftAthletePrev)).setAdapter(leftHistoryAdapter);
		((ListView)findViewById(R.id.listRightAthletePrev)).setAdapter(rightHistoryAdapter);

		mainContainer = findViewById(R.id.mainContainer);
		crState = new StartTrigger(mainContainer);

		initSensors();

		findViewById(R.id.triggerButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				crState.terminate();
				crState = new TimerRunning(mainContainer, leftHistoryAdapter, rightHistoryAdapter);
			}
		});

		findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				crState.terminate();
				crState = new StartTrigger(mainContainer);
			}
		});
	}

	private void initSensors() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Looking for sensors");
		builder.setCancelable(false);
		alertDialog = builder.create();

		ViewGroup result = (ViewGroup) getLayoutInflater().inflate(R.layout.fragment_login, alertDialog.getListView(), false);

		alertDialog.setView(result);

		alertDialog.show();

		connectToSensor();
	}

	private void connectToSensor() {
		NETWORK_EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				tcp = new TcpClient(MainActivity.this, "phsico-timer.local", 8123);
				tcp.run();
			}
		});
	}

	private void disconnectFromSensor() {
		if (tcp != null) {
			tcp.stopClient();
		}

	}

	@Override
	public void messageReceived(String message) {
		System.out.println(message);
		switch (message.trim()) {
			case "SUCCESS":
				Needle.onMainThread().execute(new Runnable() {
					@Override
					public void run() {
						alertDialog.hide();
					}
				});
			case "LEFT":
			case "RIGHT":
				if (crState instanceof TimerRunning) {
					((TimerRunning) crState).onButtonEvent(message);
				}
		}
	}

	@Override
	public void onConnect() {
		tcp.sendMessage("1234");
	}

	@Override
	public void onDisconnect() {
		initSensors();
	}

	@Override
	public void onFailed() {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				connectToSensor();
			}
		}, 1000);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		disconnectFromSensor();
		super.onDestroy();
	}
}