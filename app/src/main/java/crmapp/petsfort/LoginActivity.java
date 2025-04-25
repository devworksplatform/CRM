package crmapp.petsfort;

import android.app.*;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

import crmapp.petsfort.JLogics.AppVersionManager;
import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.User;

public class LoginActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private boolean b = false;
	private HashMap<String, Object> usernamess = new HashMap<>();
//	private boolean verified = false;
	private double exist = 0;
	private double n = 0;
	private double not = 0;
	private HashMap<String, Object> map2 = new HashMap<>();
	
	private ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
	
	private ScrollView vscroll1;
	private LinearLayout linear1;
	private TextView title;
	private TextView message;
	private LinearLayout linear2;
	private EditText username;
	private EditText email;
	private LinearLayout linear3;
	private LinearLayout LayoutPassword;
	private LinearLayout linear4;
	private TextView textview_forgot_paasword;
	private LinearLayout linear5;
	private LinearLayout linear11;
	private LinearLayout linear6;
	private TextView login_btn;
	private TextView signup_btn;
	private TextView password_btn;
	private LinearLayout linear10;
	private LinearLayout CreateAccount;
	private LinearLayout loginaccount;
	private EditText password;
	private ImageView imageview1;
	private TextView textview1;
	private TextView textview2;
	private TextView textview4;
	private TextView textview5;
	private TextView textview9;
	private TextView textview10;
	
	private FirebaseAuth auth;
	private OnCompleteListener<AuthResult> _auth_sign_in_listener;
	private SharedPreferences userss;
	private Intent intent = new Intent();




	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.login);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();

		// Call this method to check/request permission
		acquireNotificationPermission();
	}

	private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

	private void acquireNotificationPermission() {
		if (Build.VERSION.SDK_INT >= 33) { // Android 13+
			String permission = getNotificationPermission();
			if (permission != null) {
				if (ContextCompat.checkSelfPermission(this, permission)
						!= PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this,
							new String[]{permission},
							REQUEST_NOTIFICATION_PERMISSION);
				} else {
					// Permission already granted
					onNotificationPermissionGranted();
				}
			}
		} else {
			// No runtime permission required before API 33
			onNotificationPermissionGranted();
		}
	}

	private String getNotificationPermission() {
		try {
			// Dynamically fetch Manifest.permission.POST_NOTIFICATIONS
			return (String) Class.forName("android.Manifest$permission")
					.getDeclaredField("POST_NOTIFICATIONS")
					.get(null);
		} catch (Exception e) {
//			e.printStackTrace(); // Should never happen on API 33+
			return null;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				onNotificationPermissionGranted();
			} else {
				onNotificationPermissionDenied();
			}
		}
	}

	private void onNotificationPermissionGranted() {
		// Do something when permission is granted
	}

	private void onNotificationPermissionDenied() {
		// Show explanation or fallback
	}

	
	private void initialize(Bundle _savedInstanceState) {
		vscroll1 = findViewById(R.id.vscroll1);
		linear1 = findViewById(R.id.linear1);
		title = findViewById(R.id.title);
		message = findViewById(R.id.message);
		linear2 = findViewById(R.id.linear2);
		username = findViewById(R.id.username);
		email = findViewById(R.id.email);
		linear3 = findViewById(R.id.linear3);
		LayoutPassword = findViewById(R.id.LayoutPassword);
		linear4 = findViewById(R.id.linear4);
		textview_forgot_paasword = findViewById(R.id.textview_forgot_paasword);
		linear5 = findViewById(R.id.linear5);
		linear11 = findViewById(R.id.linear11);
		linear6 = findViewById(R.id.linear6);
		login_btn = findViewById(R.id.login_btn);
		signup_btn = findViewById(R.id.signup_btn);
		password_btn = findViewById(R.id.password_btn);
		linear10 = findViewById(R.id.linear10);
		CreateAccount = findViewById(R.id.CreateAccount);
		loginaccount = findViewById(R.id.loginaccount);
		password = findViewById(R.id.password);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		textview2 = findViewById(R.id.textview2);
		textview4 = findViewById(R.id.textview4);
		textview5 = findViewById(R.id.textview5);
		textview9 = findViewById(R.id.textview9);
		textview10 = findViewById(R.id.textview10);
		auth = FirebaseAuth.getInstance();
		userss = getSharedPreferences("logindata", Activity.MODE_PRIVATE);

		textview2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = "https://petsfort.in/privacy_policy"; // Replace with the desired URL
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
			}
		});

		textview_forgot_paasword.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_TransitionManager(linear1, 200);
				login_btn.setVisibility(View.GONE);
				signup_btn.setVisibility(View.GONE);
				password_btn.setVisibility(View.VISIBLE);
				loginaccount.setVisibility(View.VISIBLE);
				CreateAccount.setVisibility(View.GONE);
				username.setVisibility(View.GONE);
				email.setVisibility(View.VISIBLE);
				LayoutPassword.setVisibility(View.GONE);
				textview_forgot_paasword.setVisibility(View.GONE);
				linear11.setVisibility(View.GONE);
				linear6.setVisibility(View.GONE);
			}
		});

		login_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (true) {
					if (email.getText().toString().equals("")) {
						((EditText)email).setError("Enter email");
					}
					else {
						if (password.getText().toString().equals("")) {
							((EditText)password).setError("Enter password");
						}
						else {
							_Custom_Loading(true);
							auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(LoginActivity.this, _auth_sign_in_listener);
						}
					}
				}
				else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							SketchwareUtil.showMessage(getApplicationContext(), "No Internet Connection");
						}
					});
				}
			}
		});

		imageview1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (b) {
					password.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
					b = false;
					imageview1.setImageResource(R.drawable.ic_visibility_grey);
				}
				else {
					password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
					b = true;
					imageview1.setImageResource(R.drawable.ic_visibility_off_grey);
				}
			}
		});
		
		textview5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_TransitionManager(linear1, 200);
				signup_btn.setVisibility(View.VISIBLE);
				password_btn.setVisibility(View.GONE);
				login_btn.setVisibility(View.GONE);
				loginaccount.setVisibility(View.VISIBLE);
				CreateAccount.setVisibility(View.GONE);
				username.setVisibility(View.VISIBLE);
				email.setVisibility(View.VISIBLE);
				LayoutPassword.setVisibility(View.VISIBLE);
				textview_forgot_paasword.setVisibility(View.GONE);
				linear11.setVisibility(View.VISIBLE);
			}
		});
		
		textview10.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_TransitionManager(linear1, 200);
				login_btn.setVisibility(View.VISIBLE);
				signup_btn.setVisibility(View.GONE);
				password_btn.setVisibility(View.GONE);
				loginaccount.setVisibility(View.GONE);
				CreateAccount.setVisibility(View.VISIBLE);
				username.setVisibility(View.GONE);
				email.setVisibility(View.VISIBLE);
				LayoutPassword.setVisibility(View.VISIBLE);
				textview_forgot_paasword.setVisibility(View.VISIBLE);
				linear11.setVisibility(View.VISIBLE);
			}
		});


		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				if (_success) {
					AppVersionManager.setLoginToCurrentVersion(getApplicationContext());
					userss.edit().putString("email", FirebaseAuth.getInstance().getCurrentUser().getEmail()).commit();
					userss.edit().putString("uid", FirebaseAuth.getInstance().getCurrentUser().getUid()).commit();


					Business.UserDataApiClient.getUserDataCallApi(FirebaseAuth.getInstance().getCurrentUser().getUid(), new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
						@Override
						public void onReceived(Business.UserDataApiClient.UserDataApiResponse response) {
                            super.onReceived(response);
							if (response.getStatusCode() == 200) {
								if(response.getUser().isBlocked == 1) {
									FirebaseAuth.getInstance().signOut();
									SketchwareUtil.showMessage(getApplicationContext(), "Your account is blocked, Login Failed");

									intent.setClass(getApplicationContext(), MainActivity.class);
									startActivity(intent);
									overridePendingTransition(android.
													R.anim.fade_in,
											android.R.anim.fade_out);
									finish();
								} else {
									SketchwareUtil.showMessage(getApplicationContext(), "Please Wait a Moment!");
									Business.JFCM.subscribeBasicTopics(FirebaseAuth.getInstance().getCurrentUser().getUid(), User.resolveRoleToString(response.getUser().role), new OnCompleteListener<Void>() {
										@Override
										public void onComplete(@NonNull Task<Void> task) {
											if(task.isSuccessful()) {
												userss.edit().putString("name", response.getUser().name).commit();
												userss.edit().putString("role", response.getUser().role).commit();
												SketchwareUtil.showMessage(getApplicationContext(), "Login Successful.");
											} else {
												try{
													Business.JFCM.unSubscribeAll();
												} catch (Exception ignored) {}
												FirebaseAuth.getInstance().signOut();
												SketchwareUtil.showMessage(getApplicationContext(), "FCM Server Issue, Login Failed.");
											}


											intent.setClass(getApplicationContext(), MainActivity.class);
											startActivity(intent);
											overridePendingTransition(android.
															R.anim.fade_in,
													android.R.anim.fade_out);
											finish();
										}
									});
								}
							} else {
								FirebaseAuth.getInstance().signOut();
								SketchwareUtil.showMessage(getApplicationContext(), "Server Busy, Login Failed.");

								intent.setClass(getApplicationContext(), MainActivity.class);
								startActivity(intent);
								overridePendingTransition(android.
												R.anim.fade_in,
										android.R.anim.fade_out);
								finish();
							}

                        }
					});
				}
				else {
					_Custom_Loading(false);
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
			}
		};
		

	}
	
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);
		_UI();
	}
	
	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;
		
		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration);
		autoTransition.setInterpolator(new android.view.animation.DecelerateInterpolator()); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}
	
	
	public void _UI() {
		username.setSingleLine(true);
		email.setSingleLine(true);
		password.setSingleLine(true);
		
		password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
		_round_corner_and_ripple(login_btn, 20, 10, "#4b69ff", true);
		_round_corner_and_ripple(signup_btn, 20, 10, "#4b69ff", true);
		_round_corner_and_ripple(password_btn, 20, 10, "#4b69ff", true);
		imageview1.setColorFilter(0xFF3B3E3E, PorterDuff.Mode.MULTIPLY);
		title.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		message.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		username.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		email.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview_forgot_paasword.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		login_btn.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		password_btn.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		signup_btn.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 1);
		password.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview4.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		username.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)20, (int)0, 0xFFFFFFFF, 0xFFF6F2F1));
		email.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)20, (int)0, 0xFFFFFFFF, 0xFFF6F2F1));
		LayoutPassword.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)20, (int)0, 0xFFFFFFFF, 0xFFF6F2F1));
		signup_btn.setVisibility(View.GONE);
		password_btn.setVisibility(View.GONE);
		loginaccount.setVisibility(View.GONE);
		username.setVisibility(View.GONE);
		b = true;
	}
	
	
	public void _round_corner_and_ripple(final View _view, final double _radius, final double _shadow, final String _color, final boolean _ripple) {
		if (_ripple) {
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.parseColor(_color));
			gd.setCornerRadius((int)_radius);
			_view.setElevation((int)_shadow);
			android.content.res.ColorStateList clrb = new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{Color.parseColor("#018376")});
			android.graphics.drawable.RippleDrawable ripdrb = new android.graphics.drawable.RippleDrawable(clrb , gd, null);
			_view.setClickable(true);
			_view.setBackground(ripdrb);
		}
		else {
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.parseColor(_color));
			gd.setCornerRadius((int)_radius);
			_view.setBackground(gd);
			_view.setElevation((int)_shadow);
		}
	}
	
	
	public void _Custom_Loading(final boolean _ifShow) {
		if (_ifShow) {
			if (coreprog == null){
				coreprog = new ProgressDialog(this);
				coreprog.setCancelable(false);
				coreprog.setCanceledOnTouchOutside(false);
				
				coreprog.requestWindowFeature(Window.FEATURE_NO_TITLE);  coreprog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
				
			}
			coreprog.setMessage(null);
			coreprog.show();
			View _view = getLayoutInflater().inflate(R.layout.custom_dialog, null);
			LinearLayout linear_base = (LinearLayout) _view.findViewById(R.id.linear_base);
			
			android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
			gd.setColor(Color.TRANSPARENT);
			gd.setCornerRadius(25);
			linear_base.setBackground(gd);
			coreprog.setContentView(_view);
		}
		else {
			if (coreprog != null){
				coreprog.dismiss();
			}
		}
	}
	private ProgressDialog coreprog;
	{
	}
	
	
	@Deprecated
	public void showMessage(String _s) {
		Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
	}
	
	@Deprecated
	public int getLocationX(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[0];
	}
	
	@Deprecated
	public int getLocationY(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[1];
	}
	
	@Deprecated
	public int getRandom(int _min, int _max) {
		Random random = new Random();
		return random.nextInt(_max - _min + 1) + _min;
	}
	
	@Deprecated
	public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
		ArrayList<Double> _result = new ArrayList<Double>();
		SparseBooleanArray _arr = _list.getCheckedItemPositions();
		for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
			if (_arr.valueAt(_iIdx))
			_result.add((double)_arr.keyAt(_iIdx));
		}
		return _result;
	}
	
	@Deprecated
	public float getDip(int _input) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
	}
	
	@Deprecated
	public int getDisplayWidthPixels() {
		return getResources().getDisplayMetrics().widthPixels;
	}
	
	@Deprecated
	public int getDisplayHeightPixels() {
		return getResources().getDisplayMetrics().heightPixels;
	}
}
