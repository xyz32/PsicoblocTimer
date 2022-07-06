package com.adamratana.pshicotimer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.adamratana.pshicotimer.network.TcpClient;

public class MainActivity extends AppCompatActivity implements TcpClient.OnMessageReceived {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		initSensors();

		findViewById(R.id.triggerButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				
				try {
					Thread.sleep(1000);

					generateTone(880, 200, 1).play();
					Thread.sleep(800);

					generateTone(880, 200, 1).play();
					Thread.sleep(800);

					generateTone(1760, 100, 1).play();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
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

	private AudioTrack generateTone(double freqHz, int durationMs, double volume)
	{
		if (volume > 1 || volume < 0){
			volume = 1; //will make sure it isn't too loud
		}
		int rate =
				AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

		int count = (int)(rate * 2.0 * (durationMs / 1000.0)) & ~1;
		short[] samples = new short[count];
		for(int i = 0; i < count; i += 2){
			short sample = (short)(volume * Math.sin(2 * Math.PI * i / (rate / freqHz)) * 0x7FFF);
			samples[i + 0] = sample;
			samples[i + 1] = sample;
		}

		AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
		track.write(samples, 0, count);
		return track;
	}
}