package crmapp.petsfort;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

import crmapp.petsfort.JLogics.AppVersionManager;
import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;

public class MainActivity extends AppCompatActivity {
	private SharedPreferences userss;

	private Timer _timer = new Timer();

	private LinearLayout linear1,rootLinear;
	private ImageView imageview13,imageview2;
	
	private Intent tela = new Intent();
	private TimerTask timer;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		rootLinear = findViewById(R.id.rootLinear);
		linear1 = findViewById(R.id.linear1);
//		imageview1 = findViewById(R.id.imageview1);
		imageview2 = findViewById(R.id.imageview2);

//		imageview2.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);

		userss = getSharedPreferences("logindata", Activity.MODE_PRIVATE);

	}
	
	private void initializeLogic() {
		//		imageview1.setVisibility(View.VISIBLE);
		imageview2.setVisibility(View.GONE);


		if(!AppVersionManager.isLoggedIn(getApplicationContext())) {
			if(FirebaseAuth.getInstance().getCurrentUser() != null) {
				Toast.makeText(this, "Welcome To PetsFort", Toast.LENGTH_SHORT).show();
//				Toast.makeText(this, "Sign In Again To Use New Features", Toast.LENGTH_SHORT).show();
//				SharedPreferences localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);
//				Business.localDB_SharedPref.clearCart(localDB);
//				try{
//					Business.JFCM.unSubscribeAll();
//				} catch (Exception ignored) {}
//				FirebaseAuth.getInstance().signOut();

				startInit();
			} else {
				startInit();
			}
		} else {
			startInit();
		}


	}


	void startInit() {
		JHelpers.runAfterDelay(MainActivity.this,500, new Callbacker.Timer(){
			@Override
			public void onEnd() {
				JHelpers.TransitionManager(rootLinear,1000);
//				imageview1.setVisibility(View.VISIBLE);
				imageview2.setVisibility(View.VISIBLE);


				if ((FirebaseAuth.getInstance().getCurrentUser() != null)) {
					timer = new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									String userRole = userss.getString("role", null);
									if (userRole == null) {
										FirebaseAuth.getInstance().signOut();
										tela.setClass(getApplicationContext(), LoginActivity.class);
										startActivity(tela);
										finish();
									} else {
										if (userRole.equals("2") || userRole.equals("4")) {
											tela.setClass(getApplicationContext(), ChooseUserActivity.class);
										} else {
											tela.setClass(getApplicationContext(), PrincipalActivity.class);
										}
										startActivity(tela);
										finish();
									}
								}
							});
						}
					};
					_timer.schedule(timer, (int)(1700));
				}
				else {
					timer = new TimerTask() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									tela.setClass(getApplicationContext(), LoginActivity.class);
									startActivity(tela);
									finish();
								}
							});
						}
					};
					_timer.schedule(timer, (int)(2000));
				}
			}
		});

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
