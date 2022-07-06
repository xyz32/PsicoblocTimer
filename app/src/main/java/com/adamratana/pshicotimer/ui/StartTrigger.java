package com.adamratana.pshicotimer.ui;

import android.view.View;

import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.adamratana.pshicotimer.R;

public class StartTrigger extends ControlState {

	public StartTrigger(View mainContainer) {
		super(mainContainer);
		findViewById(R.id.startLayout).setVisibility(View.VISIBLE);
		slider.close();
		slider.setLockMode(SlidingPaneLayout.LOCK_MODE_LOCKED);
	}

	@Override
	public void terminate() {
		findViewById(R.id.startLayout).setVisibility(View.GONE);
	}
}
