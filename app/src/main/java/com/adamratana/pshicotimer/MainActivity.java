package com.adamratana.pshicotimer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.adamratana.pshicotimer.network.TcpClient;

public class MainActivity extends AppCompatActivity implements TcpClient.OnMessageReceived {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		initSensors();

		final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
		tg.startTone(ToneGenerator.TONE_PROP_BEEP);
	}

	private void initSensors() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Looking for sensors");
		builder.setCancelable(false);
		AlertDialog alertDialog = builder.create();

		ViewGroup result = (ViewGroup) getLayoutInflater().inflate(R.layout.frame_login, alertDialog.getListView(), false);

		alertDialog.setView(result);

		alertDialog.show();
		TcpClient tcp =new TcpClient(this, "phsico-timer.local", 9123);
	}

	@Override
	public void messageReceived(String message) {

	}
}