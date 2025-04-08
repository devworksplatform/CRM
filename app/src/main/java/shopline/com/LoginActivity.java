package shopline.com;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;

import shopline.com.JLogics.Business;
import shopline.com.JLogics.Callbacker;


public class LoginActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private boolean b = false;
	private HashMap<String, Object> user = new HashMap<>();
	private HashMap<String, Object> usernamess = new HashMap<>();
	private boolean verified = false;
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
	
	private RequestNetwork requestNetwork;
	private RequestNetwork.RequestListener _requestNetwork_request_listener;
	private FirebaseAuth auth;
	private OnCompleteListener<AuthResult> _auth_create_user_listener;
	private OnCompleteListener<AuthResult> _auth_sign_in_listener;
	private OnCompleteListener<Void> _auth_reset_password_listener;
	private OnCompleteListener<Void> auth_updateEmailListener;
	private OnCompleteListener<Void> auth_updatePasswordListener;
	private OnCompleteListener<Void> auth_emailVerificationSentListener;
	private OnCompleteListener<Void> auth_deleteUserListener;
	private OnCompleteListener<Void> auth_updateProfileListener;
	private OnCompleteListener<AuthResult> auth_phoneAuthListener;
	private OnCompleteListener<AuthResult> auth_googleSignInListener;
	
	private DatabaseReference users = _firebase.getReference("users");
	private ChildEventListener _users_child_listener;
	private AlertDialog.Builder network_dialog;
	private SharedPreferences userss;
	private Intent intent = new Intent();
	private AlertDialog.Builder verification;
	private TimerTask timer;
	private Calendar Cal = Calendar.getInstance();

	private Business Bus;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		Bus = new Business(this);
		setContentView(R.layout.login);
		initialize(_savedInstanceState);
//		FirebaseApp.initializeApp(this);
		initializeLogic();
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
		requestNetwork = new RequestNetwork(this);
		auth = FirebaseAuth.getInstance();
		network_dialog = new AlertDialog.Builder(this);
		userss = getSharedPreferences("users", Activity.MODE_PRIVATE);
		verification = new AlertDialog.Builder(this);
		
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
//							Bus.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString(), new Callbacker.Auth(){
//								@Override
//								public void onSuccess(SupaManager.User user) {
//									_Custom_Loading(false);
//									runOnUiThread(new Runnable() {
//										@Override
//										public void run() {
//											SketchwareUtil.showMessage(getApplicationContext(), "success");
//										}
//									});
//								}
//
//								@Override
//								public void onError(String error) {
//									_Custom_Loading(false);
//									runOnUiThread(new Runnable() {
//										@Override
//										public void run() {
//											SketchwareUtil.showMessage(getApplicationContext(), "error "+error);
//										}
//									});
//
//								}
//							});
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

		signup_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (SketchwareUtil.isConnected(getApplicationContext())) {
					if (username.getText().toString().equals("")) {
						((EditText)username).setError("Enter name");
					}
					else {
						if (username.getText().toString().length() < 4) {
							((EditText)username).setError("Min. 4 Latters");
						}
						else {
							if (email.getText().toString().equals("")) {
								((EditText)email).setError("Enter email");
							}
							else {
								if (password.getText().toString().equals("")) {
									((EditText)password).setError("Enter password");
								}
								else {
									if (!email.getText().toString().contains("gmail.com")) {
										((EditText)email).setError("Enter valid email");
									}
									else {
										if (password.getText().toString().equals("123456")) {
											((EditText)password).setError("Secure Password");
										}
										else {
											if (username.getText().toString().contains("gmail.com")) {
												((EditText)username).setError("Enter Proper Name");
											}
											else {
												if (username.getText().toString().contains("Official") || username.getText().toString().contains("official")) {
													((EditText)username).setError("Can't Add Official In Name");
												}
												else {
													auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(LoginActivity.this, _auth_create_user_listener);
												}
											}
										}
									}
								}
							}
						}
					}
				}
				else {
					SketchwareUtil.showMessage(getApplicationContext(), "No Internet Connection");
				}
			}
		});
		
		password_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (SketchwareUtil.isConnected(getApplicationContext())) {
					if (email.getText().toString().equals("")) {
						((EditText)email).setError("Enter Your Email");
					}
					else {
						if (email.getText().toString().contains("gmail.com")) {
							_Custom_Loading(true);
							auth.sendPasswordResetEmail(email.getText().toString()).addOnCompleteListener(_auth_reset_password_listener);
						}
						else {
							((EditText)email).setError("Please Enter Your Valid Email.");
						}
					}
				}
				else {
					SketchwareUtil.showMessage(getApplicationContext(), "No Internet Connection");
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
		

		
		_users_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {
				
			}
			
			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);
				
			}
			
			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();
				
			}
		};
		users.addChildEventListener(_users_child_listener);
		
		auth_updateEmailListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				
			}
		};
		
		auth_updatePasswordListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				
			}
		};
		
		auth_emailVerificationSentListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				
			}
		};
		
		auth_deleteUserListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				
			}
		};
		
		auth_phoneAuthListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
				
			}
		};
		
		auth_updateProfileListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				
			}
		};
		
		auth_googleSignInListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";
				
			}
		};
		
		_auth_create_user_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				if (_success) {
					user = new HashMap<>();
					user.put("name", username.getText().toString());
					user.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
					user.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
					user.put("password", password.getText().toString());
//					user.put("icon", "https://firebasestorage.googleapis.com/v0/b/hahah-6f6f5.appspot.com/o/st2%2Fuser.png?alt=media&token=b3e4d18e-0629-4ed8-b8ab-1089dcff702e");
					user.put("block", "false");
					user.put("verified", "false");
					user.put("registration_date", new SimpleDateFormat("dd-MM-yyyy").format(Cal.getTime()));
					users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(user);
					userss.edit().putString("name", username.getText().toString()).commit();
					userss.edit().putString("email", FirebaseAuth.getInstance().getCurrentUser().getEmail()).commit();
					userss.edit().putString("password", password.getText().toString()).commit();
					auth.getCurrentUser().sendEmailVerification() .addOnCompleteListener(new OnCompleteListener<Void>() {
						@Override
						public void onComplete(Task<Void> task) {
							if (task.isSuccessful()) {
								showMessage("Verification Link has been sent to your email"); } else {
								showMessage ("Verification Link could not be sent.");}
						} });
					timer = new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									_Custom_Loading(false);
									_TransitionManager(linear1, 200);
									FirebaseAuth.getInstance().signOut();
									username.setText("");
									email.setText("");
									password.setText("");
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
						}
					};
					_timer.schedule(timer, (int)(2000));
				}
				else {
					_Custom_Loading(false);
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
			}
		};
		
		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				if (_success) {
					verified = auth.getCurrentUser().isEmailVerified();
					//powered by ashishtechnozone
					//code for verification status checking
					
					if (verified) {
						userss.edit().putString("email", email.getText().toString()).commit();
						userss.edit().putString("password", password.getText().toString()).commit();
						userss.edit().putString("uid", FirebaseAuth.getInstance().getCurrentUser().getUid()).commit();
						user.put("password", password.getText().toString());
						users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(user);
						SketchwareUtil.showMessage(getApplicationContext(), "Login Successful.");
						intent.setClass(getApplicationContext(), PrincipalActivity.class);
						startActivity(intent);
						overridePendingTransition(android.
						R.anim.fade_in,
						android.R.anim.fade_out);
						finish();
					}
					else {
						_Custom_Loading(false);
						verification.setTitle("Email not Verified !");
						verification.setMessage("Please Verify Your Email. Verification Link Has Been Sent To Your Email or Check SPAM Folder !");
						verification.setCancelable(false);
						verification.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface _dialog, int _which) {
								FirebaseAuth.getInstance().signOut();
							}
						});
						verification.setNegativeButton("Send Link again", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface _dialog, int _which) {
								auth.getCurrentUser().sendEmailVerification() .addOnCompleteListener(new OnCompleteListener<Void>() {
									@Override
									public void onComplete(Task<Void> task) {
										if (task.isSuccessful()) {
											showMessage("Verification Link has been sent to your email !"); } else {
											showMessage ("Verification Link could not be sent !");}
									} });
								FirebaseAuth.getInstance().signOut();
							}
						});
						verification.create().show();
					}
				}
				else {
					_Custom_Loading(false);
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
			}
		};
		
		_auth_reset_password_listener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				timer = new TimerTask() {
					@Override
					public void run() {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								_Custom_Loading(false);
								if (_success) {
									SketchwareUtil.showMessage(getApplicationContext(), "Reset link Has Been Sent To Your Email.");
								}
								else {
									SketchwareUtil.showMessage(getApplicationContext(), "Please Try After Sometimes.");
								}
							}
						});
					}
				};
				_timer.schedule(timer, (int)(3000));
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
//		requestNetwork.startRequestNetwork(RequestNetworkController.GET, "https://www.google.com", "Sketch Store Yt", _requestNetwork_request_listener);
		username.setSingleLine(true);
		email.setSingleLine(true);
		password.setSingleLine(true);
		
		password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
		_round_corner_and_ripple(login_btn, 20, 10, "#4b69ff", true);
		_round_corner_and_ripple(signup_btn, 20, 10, "#4b69ff", true);
		_round_corner_and_ripple(password_btn, 20, 10, "#4b69ff", true);
		imageview1.setColorFilter(0xFF3B3E3E, PorterDuff.Mode.MULTIPLY);
		title.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
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
