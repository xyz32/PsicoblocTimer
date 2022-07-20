package com.adamratana.pshicotimer.ui;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.adamratana.pshicotimer.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import needle.BackgroundThreadExecutor;
import needle.Needle;

public class TimerRunning extends ControlState {
	private final static String DISPLAY_TIME_FORMAT = "mm:ss.SSS";
	private final TextView statusText;
	private final SimpleDateFormat formatter;
	private final TextView leftAthleteText;
	private final TextView rightAthleteText;
	private boolean timerRunning;
	private long leftRunTime = 0;
	private long rightRunTime = 0;
	private AudioTrack audioLow;
	private AudioTrack audioHigh;
	private final ArrayAdapter<String> leftHistoryList;
	private final ArrayAdapter<String> rightHistoryList;

	BackgroundThreadExecutor EXECUTOR = Needle.onBackgroundThread()
			.withTaskType("generic")
			.withThreadPoolSize(1);
	private ScheduledExecutorService executor;
	private long startTime;

	public TimerRunning(View mainContainer, ArrayAdapter<String> leftHistoryList, ArrayAdapter<String> rightHistoryList) {
		super(mainContainer);

		this.leftHistoryList = leftHistoryList;
		this.rightHistoryList = rightHistoryList;

		audioLow = generateTone(880, 200, 1);
		audioHigh = generateTone(1760, 100, 1);

		timerRunning = false;

		formatter = new SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.US);
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		findViewById(R.id.runningLayout).setVisibility(View.VISIBLE);
		statusText = (TextView) findViewById(R.id.textStatus);
		leftAthleteText = (TextView) findViewById(R.id.textLeftAthleteCurrent);
		rightAthleteText = (TextView) findViewById(R.id.textRightAthleteCurrent);
		slider.setLockMode(SlidingPaneLayout.LOCK_MODE_UNLOCKED);

		EXECUTOR.execute(new Runnable() {
			@Override
			public void run() {
				try {
					updateStatus("3");
					Thread.sleep(1000);

					updateStatus("2");

					audioLow.play();
					Thread.sleep(800);
					audioLow.stop();

					updateStatus("1");
					audioLow.play();
					Thread.sleep(800);
					audioLow.stop();

					updateStatus("GO!");
					startTime = System.currentTimeMillis();

					audioHigh.play();
					timerRunning = true;

					executor = Executors.newSingleThreadScheduledExecutor();
					executor.scheduleAtFixedRate(new Runnable() {
						@Override
						public void run() {
							if (!timerRunning) {
								return;
							}

							long currentTime = System.currentTimeMillis() - startTime;
							if ((currentTime / 1000) % 2 == 0) {
								updateStatus("GO!");
							} else {
								updateStatus("");
							}

							updateTimers(currentTime);
						}
					}, 0, 53, TimeUnit.MILLISECONDS); //use a prime number to avoid creating patterns
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void terminate() {
		timerRunning = false;

		audioLow.release();
		audioHigh.release();

		leftHistoryList.insert(leftAthleteText.getText().toString(), 0);
		leftHistoryList.notifyDataSetChanged();
		rightHistoryList.insert(rightAthleteText.getText().toString(),0);
		rightHistoryList.notifyDataSetChanged();
		((TextView)findViewById(R.id.textLeftOrder)).setText("");
		((TextView)findViewById(R.id.textRightOrder)).setText("");

		leftRunTime = 0;
		rightRunTime = 0;
		updateTimers(0);
		findViewById(R.id.runningLayout).setVisibility(View.GONE);
	}

	private void updateStatus(String status) {
		Needle.onMainThread().execute(new Runnable() {
			@Override
			public void run() {
				statusText.setText(status);
			}
		});
	}

	private void updateTimers(long timer) {
		String text = formatter.format(new Date(timer));
		Needle.onMainThread().execute(new Runnable() {
			@Override
			public void run() {
				if (leftRunTime == 0) {
					leftAthleteText.setText(text);
				} else {
					leftAthleteText.setText(formatter.format(new Date(leftRunTime)));
				}

				if (rightRunTime == 0) {
					rightAthleteText.setText(text);
				} else {
					rightAthleteText.setText(formatter.format(new Date(rightRunTime)));
				}
			}
		});
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

	public void onButtonEvent(String message) {
		if (!timerRunning) {
			return;
		}

		if (message.equals("LEFT") && leftRunTime == 0) {
			leftRunTime = System.currentTimeMillis() - startTime;
			if (rightRunTime == 0) {
				updateUIElement(R.id.textLeftOrder, "1st");
			} else {
				updateUIElement(R.id.textLeftOrder, "2nd");
			}
		}

		if (message.equals("RIGHT") && rightRunTime == 0) {
			rightRunTime = System.currentTimeMillis() - startTime;
			if (leftRunTime == 0) {
				updateUIElement(R.id.textRightOrder, "1st");
			} else {
				updateUIElement(R.id.textRightOrder, "2nd");
			}
		}

		if (leftRunTime != 0 && rightRunTime != 0) {
			timingRunDone();
		}
	}

	private void timingRunDone() {
		updateStatus("Done.");
		timerRunning = false;
		executor.shutdown();
		updateTimers(System.currentTimeMillis() - startTime); //make sure all timers are refreshed.
	}

	private void updateUIElement(int id, String text) {
		Needle.onMainThread().execute(new Runnable() {
			@Override
			public void run() {
				((TextView) findViewById(id)).setText(text);
			}
		});
	}
}
