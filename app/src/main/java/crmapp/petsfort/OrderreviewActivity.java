package crmapp.petsfort;

import android.app.*;
import android.content.*;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.*;
import android.view.View;
import android.view.View.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.User;

public class OrderreviewActivity extends AppCompatActivity {

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
	SharedPreferences localDB;
	HashMap<String,Object> cartData;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.orderreview);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}


	private CardView confirmOrderLinearCard;
	private LinearLayout lback,confirmOrderLinear,progressBarLinear,linear2,linear33,linear5,rootLinear;
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

		rootLinear = (LinearLayout) findViewById(R.id.rootLinear);
		lback = (LinearLayout) findViewById(R.id.linear5111);
		confirmOrderLinear = (LinearLayout) findViewById(R.id.confirmOrderLinear);
		confirmOrderLinearCard = (CardView) findViewById(R.id.confirmOrderLinearCard);
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

	Double credits = 0.0;
	String creditse = "";
	boolean isCreditsLoaded = false;
	Business.BulkDetailsApiClient.CostDetails costDetails;
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);


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

				Business.UserDataApiClient.getUserDataCallApi(userId, new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
					@Override
					public void onReceived(Business.UserDataApiClient.UserDataApiResponse _data) {
						String creditsStr = "0";
						String addressStr = "";
						if(_data.getStatusCode() == 200 && _data.getUser() != null) {
							creditsStr = String.valueOf(_data.getUser().credits);
							credits = _data.getUser().credits;
							creditse = _data.getUser().creditse;
							addressStr = _data.getUser().address;
						}


						isCreditsLoaded = true;
						JHelpers.TransitionManager(rootLinear, 300);
						addressEditText.setText(addressStr);
						addressEditText.setEnabled(false);
						creditTextView.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(credits)));
					}
				});

//				_firebase.getReference("datas/users/details/".concat(userId)).addListenerForSingleValueEvent(new ValueEventListener() {
//					@Override
//					public void onDataChange(DataSnapshot _dataSnapshot) {
//						String creditsStr = "0";
//						if (_dataSnapshot.exists() && _dataSnapshot.hasChild("credits")) {
//							creditsStr = _dataSnapshot.child("credits").getValue(String.class);
//							try{
//								credits = Double.parseDouble(creditsStr);
//							} catch (Exception e) {}
//						}
//
//						String addressStr = "";
//						if (_dataSnapshot.exists() && _dataSnapshot.hasChild("address")) {
//							addressStr = _dataSnapshot.child("address").getValue(String.class);
//							addressEditText.setText(addressStr);
//						}
//
//						isCreditsLoaded = true;
//						JHelpers.TransitionManager(rootLinear, 300);
//						creditTextView.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(credits)));
//
//					}
//
//					@Override
//					public void onCancelled(DatabaseError _databaseError) { }
//				});

				bulkDetailsApiClient.callApi(cartData,
						new Callbacker.ApiResponseWaiters.BulkDetailsApiCallback() {
							@Override
							public void onReceived(Business.BulkDetailsApiClient.BulkDetailsApiResponse response) {

								boolean isEmpty = (response.getStatusCode() != 200);

								if (isEmpty) {
									JHelpers.TransitionManager(rootLinear, 1000);
									progressBarLinear.setVisibility(View.VISIBLE);
									linear2.setVisibility(View.GONE);
									linear33.setVisibility(View.GONE);
									subTotalTextView.setText("₹ 0.00");
									gstTotalTextView.setText("₹ 0.00");
									totalNoDiscountTextView.setText("₹ 0.00");
									discountTotalTextView.setText("- ₹ 0.00");
									grandTotalTextView.setText("₹ 0.00");
								} else {
									costDetails = response.getCostDetails();
									startIntroAnimation(costDetails);
								}
							}
						});
			}
		});



		Business.OrderCheckoutApiClient orderCheckoutApiClient = new Business.OrderCheckoutApiClient();

		confirmOrderLinearCard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmOrderLinearCard.setEnabled(false);
				if(!isCreditsLoaded) {
					Toast.makeText(OrderreviewActivity.this, "Loading Credits, Please Try Agin!", Toast.LENGTH_SHORT).show();
					confirmOrderLinearCard.setEnabled(true);
					return;
				}

				if(credits <= 0 || (credits<costDetails.getTotal())) {
					new AlertDialog.Builder(OrderreviewActivity.this).setTitle("Insufficient Credits ₹")
							.setMessage("Contact our admin to recharge the credits to make orders. try again after credits added to you. current balance ₹ ".concat(String.valueOf(credits)).concat(". Required for the order is ₹ ").concat(String.valueOf(costDetails.getTotal())))
							.setCancelable(false)
							.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									finish();
								}
							}).show();
					Toast.makeText(OrderreviewActivity.this, "Insufficient Credits", Toast.LENGTH_SHORT).show();
					return;
				}

				try {
					String dateStr = creditse;
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
					LocalDate expiryDate = LocalDate.parse(dateStr, formatter);

					LocalDate today = LocalDate.now();

					long daysToExpire = ChronoUnit.DAYS.between(today, expiryDate);

					if (daysToExpire < 0) {
						dateStr = "Expired " + Math.abs(daysToExpire) + " days ago.";
						new AlertDialog.Builder(OrderreviewActivity.this).setTitle("Credits " + credits + "₹ "+ dateStr)
								.setMessage("Contact our admin to increase the expiry date to use the credits to make orders. try again after credits expiry date is increased to you. current balance ₹ ".concat(String.valueOf(credits)))
								.setCancelable(false)
								.setPositiveButton("OK", new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										finish();
									}
								}).show();
						Toast.makeText(OrderreviewActivity.this, "Insufficient Credits", Toast.LENGTH_SHORT).show();
						return;
					}
				} catch(Exception e) {
					//			tvBalanceLabel.setText("No Credits Left");
				}

				ProgressDialog progressDialog = new ProgressDialog(OrderreviewActivity.this);
				progressDialog.setMessage("Processing Order...");
				progressDialog.setIndeterminate(true); // Indeterminate for loading animation
				progressDialog.setCancelable(false); // Make it non-cancelable
				progressDialog.show();


				HashMap<String,String> otherData = new HashMap<String,String>();
				otherData.put("address",addressEditText.getText().toString());
				otherData.put("notes",notesEditText.getText().toString());
				cartData.put("otherData",otherData);

				orderCheckoutApiClient.callApi(userId, cartData, new Callbacker.ApiResponseWaiters.OrderCheckoutApiCallback(){
					@Override
					public void onReceived(Business.OrderCheckoutApiClient.OrderCheckoutApiResponse response) {
						if(response.getStatusCode() == 200) {
							progressDialog.hide();
							new AlertDialog.Builder(OrderreviewActivity.this).setTitle("Order Confirmed")
									.setMessage("Order Has been created successfully, Thank you for shopping with us.")
									.setCancelable(false)
									.setPositiveButton("OK", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											Business.localDB_SharedPref.clearCart(localDB);
											Intent intent = new Intent();
											intent.setClass(OrderreviewActivity.this, MainActivity.class);
											startActivity(intent);
											finishAffinity();
										}
									}).show();
						} else {
							if(response.getErrorMessage().startsWith("OutOfStock")) {
								String[] tuple = response.getErrorMessage().split(",");
								Integer product_available_stock = Integer.parseInt(tuple[1]);
								String product_id = tuple[2];
								String product_name = tuple[3];

								HashMap<String,Object> map = new HashMap<>();
								map.put("count", product_available_stock);
								Business.localDB_SharedPref.updateCartProduct(localDB,product_id,map);

								progressDialog.hide();
								new AlertDialog.Builder(OrderreviewActivity.this).setTitle("Out Of Stock!!!")
										.setMessage("Order was failed to confirm, There is not enough stock for "+product_name+". Only "+product_available_stock+ " Left, please try again to confirm order.")
										.setCancelable(false)
										.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.dismiss();
												confirmOrderLinearCard.setEnabled(true);
												finish();
											}
										}).show();
							} else {
								progressDialog.hide();
								new AlertDialog.Builder(OrderreviewActivity.this).setTitle("Order Failed!!!")
										.setMessage("Order was failed due to "+response.getErrorMessage()+", Please try again or Contact our admin team about the issue.")
										.setCancelable(false)
										.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												dialog.dismiss();
												confirmOrderLinearCard.setEnabled(true);
											}
										}).show();
							}


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
				JHelpers.TransitionManager(rootLinear, 800);
				progressBarLinear.setVisibility(View.GONE);
				linear2.setVisibility(View.VISIBLE);
				linear33.setVisibility(View.VISIBLE);

				JHelpers.runAfterDelay(OrderreviewActivity.this, 800, new Callbacker.Timer() {
					@Override
					public void onEnd() {
						JHelpers.TransitionManager(rootLinear, 800);
						subTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotalRate())));
						gstTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotalGst())));
						totalNoDiscountTextView.setText("₹ ".concat(df.format(costDetails.getTotalMrp())));
						discountTotalTextView.setText("- ₹ ".concat(String.valueOf(costDetails.getTotalDiscount())));
						grandTotalTextView.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
					}
				});
			}
		});
	}



}
