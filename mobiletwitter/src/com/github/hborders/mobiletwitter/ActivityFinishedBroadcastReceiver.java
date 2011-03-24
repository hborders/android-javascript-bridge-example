package com.github.hborders.mobiletwitter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActivityFinishedBroadcastReceiver extends BroadcastReceiver {
	private final Activity activity;

	public ActivityFinishedBroadcastReceiver(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		activity.finish();
	}
}
