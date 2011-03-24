package com.github.hborders.mobiletwitter;

import android.app.Activity;
import android.os.Bundle;

import static com.github.hborders.mobiletwitter.MobileTwitterApplication.getMobileTwitterApplication;

public class LoadingActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loading_activity_layout);

		getMobileTwitterApplication(this).load();
	}
}
