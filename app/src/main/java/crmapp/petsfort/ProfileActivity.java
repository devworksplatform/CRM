package crmapp.petsfort;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.Category;
import crmapp.petsfort.JLogics.Models.User;

public class ProfileActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();


	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		setContentView(R.layout.profile);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}


	ImageView img2;
	LinearLayout rootLinear,totalCreditsLinear,usernameEmailLinear,addressLinear,linear5;
	TextView tvBalanceAmount, tvBalanceLabel;
	TextView usernameEmail, usernameEmailLabel;
	TextView address, addressLabel;
	TextView heading;

	private void initialize(Bundle _savedInstanceState) {
		img2 = findViewById(R.id.img2);
		rootLinear = findViewById(R.id.rootLinear);
		totalCreditsLinear = findViewById(R.id.totalCreditsLinear);
		usernameEmailLinear = findViewById(R.id.usernameEmailLinear);
		addressLinear = findViewById(R.id.addressLinear);
		linear5 = findViewById(R.id.linear5);

		heading = findViewById(R.id.heading);
		tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
		tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
		usernameEmail = findViewById(R.id.usernameEmail);
		usernameEmailLabel = findViewById(R.id.usernameEmailLabel);
		address = findViewById(R.id.address);
		addressLabel = findViewById(R.id.addressLabel);

		final Typeface normalTypeface = Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf");
		final Typeface boldTypeface = Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf");

		heading.setTypeface(boldTypeface, 0);
		tvBalanceLabel.setTypeface(boldTypeface, 0);
		tvBalanceAmount.setTypeface(normalTypeface, 0);
		usernameEmailLabel.setTypeface(boldTypeface, 0);
		usernameEmail.setTypeface(normalTypeface, 0);
		addressLabel.setTypeface(boldTypeface, 0);
		address.setTypeface(normalTypeface, 0);


		linear5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}
	
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);


		Business.UserDataApiClient.getUserDataCallApi(userId, new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
			@Override
			public void onReceived(Business.UserDataApiClient.UserDataApiResponse _data) {
				if(_data.getStatusCode() == 200 && _data.getUser() != null) {
					User userInfo = _data.getUser();
					String name = userInfo.name;
					String email = userInfo.email;

					JHelpers.TransitionManager(rootLinear, 600);
					usernameEmail.setText(name.concat("\n").concat(email));

					String addressStr = "";
					addressStr = userInfo.address;
					address.setText(addressStr);

					updateCredits(userInfo);
				} else {
					JHelpers.TransitionManager(rootLinear, 600);
					usernameEmail.setText("Unknown User");
					updateCredits(null);
				}
			}
		});
	}





	public void updateCredits(User user) {
		double credits = 0;
		String creditsStr = "No Credits Left";

		if (user != null) {
			credits = user.credits;
		}


		if(credits > 0) {
			double finalCredits = credits;
			if (credits<1) {
				tvBalanceAmount.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(finalCredits)));
			} else {
				int timeMs = 2000;

				if(credits<30) {
					timeMs = 500;
				}

				JHelpers.JValueAnimator.animate(0,(int)credits,timeMs, new Callbacker.onAnimateUpdate(){
					@Override
					public void onUpdate(int _value) {
						tvBalanceAmount.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(_value)));
					}
					@Override
					public void onEnd() {
						tvBalanceAmount.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(finalCredits)));
					}
				});
			}
		} else {
			tvBalanceAmount.setText(creditsStr);
		}
	}


}
