package com.adamratana.pshicotimer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.adamratana.pshicotimer.network.TcpClient;
import com.adamratana.pshicotimer.ui.ControlState;
import com.adamratana.pshicotimer.ui.StartTrigger;
import com.adamratana.pshicotimer.ui.TimerRunning;

import needle.BackgroundThreadExecutor;
import needle.Needle;

public class MainActivity extends AppCompatActivity implements TcpClient.OnMessageReceived {
	View mainContainer;
	ControlState crState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mainContainer = findViewById(R.id.mainContainer);
		crState = new StartTrigger(mainContainer);

//		initSensors();

		findViewById(R.id.triggerButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				crState.terminate();
				crState = new TimerRunning(mainContainer);
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Looking for sensors");
		builder.setCancelable(false);
		AlertDialog alertDialog = builder.create();

		ViewGroup result = (ViewGroup) getLayoutInflater().inflate(R.layout.fragment_login, alertDialog.getListView(), false);

		alertDialog.setView(result);

		alertDialog.show();
		TcpClient tcp =new TcpClient(this, "phsico-timer.local", 9123);
	}

	@Override
	public void messageReceived(String message) {

	}
}