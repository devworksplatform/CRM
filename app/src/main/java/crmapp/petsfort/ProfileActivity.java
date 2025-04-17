package crmapp.petsfort;

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

import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;

public class ProfileActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.profile);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}


	ImageView img1,img2;
	LinearLayout rootLinear,totalCreditsLinear,usernameEmailLinear,addressLinear,linear5;
	TextView tvBalanceAmount, tvBalanceLabel;
	TextView usernameEmail, usernameEmailLabel;
	TextView address, addressLabel;
	TextView heading;

	private void initialize(Bundle _savedInstanceState) {
		img1 = findViewById(R.id.img1);
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

		JHelpers.runAfterDelay(ProfileActivity.this, 300, new Callbacker.Timer(){
			@Override
			public void onEnd() {
				JHelpers.TransitionManager(rootLinear,600);
				img2.setVisibility(View.VISIBLE);

				_firebase.getReference("datas/users/details/".concat(FirebaseAuth.getInstance().getCurrentUser().getUid())).addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot snapshot) {
						if (snapshot.exists()) {
							if (snapshot.hasChild("name") && snapshot.hasChild("email")) {
								String name = snapshot.child("name").getValue(String.class);
								String email = snapshot.child("email").getValue(String.class);

								JHelpers.TransitionManager(rootLinear, 600);
								usernameEmail.setText(name.concat("\n").concat(email));
							} else {
								JHelpers.TransitionManager(rootLinear, 600);
								usernameEmail.setText("Unknown User");
							}

							String addressStr = "";
							if (snapshot.hasChild("address")) {
								addressStr = snapshot.child("address").getValue(String.class);
								address.setText(addressStr);
							}

							updateCredits(snapshot);

						} else {
							JHelpers.TransitionManager(rootLinear, 600);
							usernameEmail.setText("Unknown User");
						}
					}

					@Override
					public void onCancelled(DatabaseError error) {
						// Handle error if needed
					}
				});


			}
		});


	}





	public void updateCredits(DataSnapshot _dataSnapshot) {
		double credits = 0;
		String creditsStr = "No Credits Left";

		if (_dataSnapshot.exists() && _dataSnapshot.hasChild("credits")) {
			creditsStr = _dataSnapshot.child("credits").getValue(String.class);
			try{
				credits = Double.parseDouble(creditsStr);
			} catch (Exception e) {}
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
