package com.github.hborders.mobiletwitter;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import static com.github.hborders.mobiletwitter.MobileTwitterApplication.getMobileTwitterApplication;

public class LoginActivity extends Activity {
	private BroadcastReceiver taskListBroadcastReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity_layout);

		taskListBroadcastReceiver = new ActivityFinishedBroadcastReceiver(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		registerReceiver(taskListBroadcastReceiver, new IntentFilter(
				MobileTwitterApplication.TWEETS_LIST_ACTION));
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		EditText usernameEditText = (EditText) findViewById(R.id.usernameEditText);
		usernameEditText.requestFocus();
	}

	@Override
	protected void onStop() {
		unregisterReceiver(taskListBroadcastReceiver);

		super.onStop();
	}

	public void login(View view) throws IOException {
		EditText usernameEditText = (EditText) findViewById(R.id.usernameEditText);
		EditText passwordEditText = (EditText) findViewById(R.id.passwordEditText);
		String username = usernameEditText.getText().toString();
		String password = passwordEditText.getText().toString();

		getMobileTwitterApplication(this).login(username, password);
	}
}
