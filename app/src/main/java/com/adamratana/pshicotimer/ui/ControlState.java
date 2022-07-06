package com.adamratana.pshicotimer.ui;

import android.view.View;

import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.adamratana.pshicotimer.R;

public abstract class ControlState {
	protected final View container;
	protected final SlidingPaneLayout slider;

	protected ControlState(View container) {
		this.container = container;
		slider = (SlidingPaneLayout) findViewById(R.id.slidingPane);
	}

	public abstract void terminate();

	protected View findViewById(int id) {
		return container.findViewById(id);
	}
}
