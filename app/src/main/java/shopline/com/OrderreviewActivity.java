package shopline.com;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

import shopline.com.JLogics.Business;
import shopline.com.JLogics.Callbacker;
import shopline.com.JLogics.JHelpers;

public class OrderreviewActivity extends AppCompatActivity {

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
	SharedPreferences localDB;
	HashMap<String,Object> cartData;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.orderreview);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}


	private LinearLayout lback,confirmOrderLinear,progressBarLinear,linear2,linear33,linear5;
	private TextView subtotalLabelTextView,subTotalTextView;
	private TextView gstTotalLabelTextView,gstTotalTextView;
	private TextView totalNoDiscountLabelTextView,totalNoDiscountTextView;
	private TextView discountTotalLabelTextView,discountTotalTextView;
	private TextView grandTotalLabelTextView,grandTotalTextView;
	private TextView creditLabelTextView,creditTextView,textviewConfirmLabel,textview1;
	private EditText addressEditText,notesEditText;
	DecimalFormat df = new DecimalFormat("#.###");


	private void initialize(Bundle _savedInstanceState) {
		localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);

		lback = (LinearLayout) findViewById(R.id.linear5111);
		confirmOrderLinear = (LinearLayout) findViewById(R.id.confirmOrderLinear);
		progressBarLinear = (LinearLayout) findViewById(R.id.progressBarLinear);
		linear5 = (LinearLayout) findViewById(R.id.linear5);
		linear2 = (LinearLayout) findViewById(R.id.linear2);
		linear33 = (LinearLayout) findViewById(R.id.linear33);

		addressEditText = (EditText) findViewById(R.id.edittext1);
		notesEditText = (EditText) findViewById(R.id.edittext3);

		textview1 = (TextView) findViewById(R.id.textview1);
		subtotalLabelTextView = (TextView) findViewById(R.id.subtotalLabelTextView);
		subTotalTextView = (TextView) findViewById(R.id.subTotalTextView);
		gstTotalLabelTextView = (TextView) findViewById(R.id.gstTotalLabelTextView);
		gstTotalTextView = (TextView) findViewById(R.id.gstTotalTextView);
		totalNoDiscountLabelTextView = (TextView) findViewById(R.id.totalNoDiscountLabelTextView);
		totalNoDiscountTextView = (TextView) findViewById(R.id.totalNoDiscountTextView);
		discountTotalLabelTextView = (TextView) findViewById(R.id.discountTotalLabelTextView);
		discountTotalTextView = (TextView) findViewById(R.id.discountTotalTextView);
		grandTotalLabelTextView = (TextView) findViewById(R.id.grandTotalLabelTextView);
		grandTotalTextView = (TextView) findViewById(R.id.grandTotalTextView);
		creditLabelTextView = (TextView) findViewById(R.id.creditLabelTextView);
		creditTextView = (TextView) findViewById(R.id.creditTextView);
		textviewConfirmLabel = (TextView) findViewById(R.id.textviewConfirmLabel);


		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		addressEditText.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		notesEditText.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		subtotalLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		subTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		gstTotalLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		gstTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		totalNoDiscountLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		totalNoDiscountTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		discountTotalLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		discountTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		grandTotalLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		grandTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		creditLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		creditTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		textviewConfirmLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

	}

	private void initializeLogic() {
		lback.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		Business.BulkDetailsApiClient bulkDetailsApiClient = new Business.BulkDetailsApiClient();

		progressBarLinear.setVisibility(View.VISIBLE);
		linear2.setVisibility(View.GONE);
		linear33.setVisibility(View.GONE);

		cartData = Business.localDB_SharedPref.getCart(localDB);

		JHelpers.runAfterDelay(this, 500, new Callbacker.Timer(){
			@Override
			public void onEnd() {

				bulkDetailsApiClient.callApi(cartData,
						new Callbacker.ApiResponseWaiters.BulkDetailsApiCallback() {
							@Override
							public void onReceived(Business.BulkDetailsApiClient.BulkDetailsApiResponse response) {

								boolean isEmpty = (response.getStatusCode() != 200);

								if (isEmpty) {
									JHelpers.TransitionManager(linear5, 300);
									progressBarLinear.setVisibility(View.VISIBLE);
									linear2.setVisibility(View.GONE);
									linear33.setVisibility(View.GONE);
									subTotalTextView.setText("₹ 0.00");
									gstTotalTextView.setText("₹ 0.00");
									totalNoDiscountTextView.setText("₹ 0.00");
									discountTotalTextView.setText("- ₹ 0.00");
									grandTotalTextView.setText("₹ 0.00");

								} else {
									Business.BulkDetailsApiClient.CostDetails costDetails = response.getCostDetails();
									startIntroAnimation(costDetails);
								}
							}
						});
			}
		});



		Business.OrderCheckoutApiClient orderCheckoutApiClient = new Business.OrderCheckoutApiClient();

		confirmOrderLinear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				orderCheckoutApiClient.callApi(userId, cartData, new Callbacker.ApiResponseWaiters.OrderCheckoutApiCallback(){

					@Override
					public void onReceived(Business.OrderCheckoutApiClient.OrderCheckoutApiResponse response) {
						if(response.getStatusCode() == 200) {
							finish();
						} else {

						}
					}
				});
			}
		});
	}

	public void startIntroAnimation(Business.BulkDetailsApiClient.CostDetails costDetails) {
		JHelpers.runAfterDelay(OrderreviewActivity.this, 400, new Callbacker.Timer(){
			@Override
			public void onEnd() {
				JHelpers.TransitionManager(linear5, 400);
				progressBarLinear.setVisibility(View.GONE);
				linear2.setVisibility(View.VISIBLE);
				linear33.setVisibility(View.VISIBLE);

				JHelpers.runAfterDelay(OrderreviewActivity.this, 400, new Callbacker.Timer() {
					@Override
					public void onEnd() {
						JHelpers.TransitionManager(linear5, 400);
						subTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotalRate())));
						gstTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotalGst())));
						totalNoDiscountTextView.setText("₹ ".concat(df.format(costDetails.getTotalRate()+costDetails.getTotalGst())));
						discountTotalTextView.setText("- ₹ ".concat(String.valueOf(costDetails.getTotalDiscount())));
						grandTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
						creditTextView.setText(String.valueOf(costDetails.getTotalDiscount()));
					}
				});
			}
		});
	}



}
