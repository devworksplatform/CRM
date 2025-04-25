package crmapp.petsfort;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Typeface;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import crmapp.petsfort.JLogics.AppVersionManager;
import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.HashMap;
import java.util.Timer;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;


public class PrincipalActivity extends AppCompatActivity {
	
	private Timer _timer = new Timer();
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

	private Toolbar _toolbar;
	private AppBarLayout _app_bar;
	private CoordinatorLayout _coordinator;
	private DrawerLayout _drawer;
	private boolean pos = false;
	private double n = 0;
	private String Category = "";
	private HashMap<String, Object> map = new HashMap<>();
	private String st = "";

	private LinearLayout linear1;
	private LinearLayout fragment_frame;
	private BottomNavigationView bottomnavigation1;
//	private LinearLayout linear5;
	private LinearLayout linear6;
	private LinearLayout linear7i;
	private LinearLayout linear8;
//	private ImageView imageview1;
	private TextView textview1;
	private ImageView imageview2;
	private ImageView imageview3;
	private ViewPager viewpager1;
	private LinearLayout rootLinear;
	private LinearLayout _drawer_linear1;
	private LinearLayout _drawer_linear2;
	private LinearLayout _drawer_linear3;
	private LinearLayout _drawer_linear5;
	private LinearLayout _drawer_linear6;
	private LinearLayout _drawer_linear7;
	private TextView _drawer_textview7;
	private LinearLayout _drawer_linear8;
	private LinearLayout _drawer_linear9;
	private LinearLayout _drawer_linear10;
	private TextView _drawer_textview11;
	private LinearLayout _drawer_linear11;
	private LinearLayout _drawer_linear12;
	private ImageView _drawer_circleimageview1;
	private TextView _drawer_textview1;
	private ImageView imageLogoName;
	private ImageView _drawer_imageview1;
	private TextView _drawer_textview2;
	private LinearLayout _drawer_linear4;
	private ImageView _drawer_imageview2;
	private TextView _drawer_textview3;
	private ImageView _drawer_imageview3;
	private TextView _drawer_textview4;
	private Switch _drawer_switch1;
	private ImageView _drawer_imageview4;
	private TextView _drawer_textview5;
	private ImageView _drawer_imageview5;
	private TextView _drawer_textview6;
	private ImageView _drawer_imageview6;
	private TextView _drawer_textview8;
	private ImageView _drawer_imageview7;
	private TextView _drawer_textview9;
	private ImageView _drawer_imageview8;
	private TextView _drawer_textview10;
	private ImageView _drawer_imageview9;
	private TextView _drawer_textview12;
	private ImageView _drawer_imageview10;
	private TextView _drawer_textview13;

	private Intent ii = new Intent();
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		setContentView(R.layout.principal);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();

		checkForAppUpdate();
	}

	private ActivityResultLauncher<IntentSenderRequest> activityResultLauncher;


	void checkForAppUpdate() {
		try{
			activityResultLauncher = registerForActivityResult(
					new ActivityResultContracts.StartIntentSenderForResult(),
					result -> {
						if (result.getResultCode() == RESULT_OK) {
							Log.d("AppUpdate", "Update flow completed successfully.");
						} else {
							Log.d("AppUpdate", "Update flow canceled or failed.");
						}
					}
			);


			if(AppVersionManager.getAlertFreezeEnableToCurrentVersion(getApplicationContext())) {
				showAlertFreeze();
			}


			AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

			// Returns an intent object that you use to check for an update.
			Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();


			// Checks that the platform will allow the specified type of update.
			appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
				if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {

					AppVersionManager.setAlertFreezeEnableToCurrentVersion(getApplicationContext());
					showAlertFreeze();

					appUpdateManager.startUpdateFlowForResult(
							// Pass the intent that is returned by 'getAppUpdateInfo()'.
							appUpdateInfo,
							// an activity result launcher registered via registerForActivityResult
							activityResultLauncher,
							// Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
							// flexible updates.
							AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
				}
			});
		} catch (Exception e) {
//			StringWriter sw = new StringWriter();
//			PrintWriter pw = new PrintWriter(sw);
//			e.printStackTrace(pw);
//			String stackTrace = sw.toString();
//			String message = e.getMessage();
//
//			TextView textView = new TextView(getApplicationContext());
//			textView.setText(stackTrace);
//			textView.setTextIsSelectable(true);
//			textView.setPadding(32, 32, 32, 32); // Optional: padding for better readability
//			textView.setTextSize(14); // Optional: adjust text size
//
//			AlertDialog.Builder builder = new AlertDialog.Builder(PrincipalActivity.this); // Use Activity context, not Application context
//			builder.setTitle(message);
//			builder.setView(textView);
//			builder.setCancelable(false);
//
//			AlertDialog dialog = builder.create();
//			dialog.show();

		}

	}


	void showAlertFreeze(){
		AlertDialog.Builder builder = new AlertDialog.Builder(PrincipalActivity.this);
		builder.setTitle("Please Update the PetsFort App");
		builder.setMessage("Please Update The PetsFort App in PlayStore to Use");

		// Optional: disable all buttons or don't add them at all
		builder.setCancelable(false);

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void initialize(Bundle _savedInstanceState) {
		_app_bar = findViewById(R.id._app_bar);
		_coordinator = findViewById(R.id._coordinator);
		_toolbar = findViewById(R.id._toolbar);
		setSupportActionBar(_toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				onBackPressed();
			}
		});
		_drawer = findViewById(R.id._drawer);
		ActionBarDrawerToggle _toggle = new ActionBarDrawerToggle(PrincipalActivity.this, _drawer, _toolbar, R.string.app_name, R.string.app_name);
		_drawer.addDrawerListener(_toggle);
		_toggle.syncState();
		
		LinearLayout _nav_view = findViewById(R.id._nav_view);

		rootLinear = findViewById(R.id.rootLinear);
		linear1 = findViewById(R.id.linear1);
		fragment_frame = findViewById(R.id.fragment_frame);
		bottomnavigation1 = findViewById(R.id.bottomnavigation1);
//		linear5 = findViewById(R.id.linear5);
		linear6 = findViewById(R.id.linear6);
		linear7i = findViewById(R.id.linear7i);
		linear8 = findViewById(R.id.linear8);
//		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		imageLogoName = findViewById(R.id.imageLogoName);
		imageview2 = findViewById(R.id.imageview2);
		imageview3 = findViewById(R.id.imageview3);
		viewpager1 = findViewById(R.id.viewpager1);

		final LinearLayout _drawer_rootLinear = _nav_view.findViewById(R.id.rootLinear);
		_drawer_linear1 = _nav_view.findViewById(R.id.linear1);
		_drawer_linear2 = _nav_view.findViewById(R.id.linear2);
		_drawer_linear3 = _nav_view.findViewById(R.id.linear3);
		_drawer_linear5 = _nav_view.findViewById(R.id.linear5);
		_drawer_linear6 = _nav_view.findViewById(R.id.linear6);
		_drawer_linear7 = _nav_view.findViewById(R.id.linear7);
		_drawer_textview7 = _nav_view.findViewById(R.id.textview7);
		_drawer_linear8 = _nav_view.findViewById(R.id.linear8);
		_drawer_linear9 = _nav_view.findViewById(R.id.linear9);
		_drawer_linear10 = _nav_view.findViewById(R.id.linear10);
		_drawer_textview11 = _nav_view.findViewById(R.id.textview11);
		_drawer_linear11 = _nav_view.findViewById(R.id.linear11);
		_drawer_linear12 = _nav_view.findViewById(R.id.linear12);
		_drawer_circleimageview1 = _nav_view.findViewById(R.id.circleimageview1);
		_drawer_textview1 = _nav_view.findViewById(R.id.textview1);
		_drawer_imageview1 = _nav_view.findViewById(R.id.imageview1);
		_drawer_textview2 = _nav_view.findViewById(R.id.textview2);
		_drawer_linear4 = _nav_view.findViewById(R.id.linear4);
		_drawer_imageview2 = _nav_view.findViewById(R.id.imageview2);
		_drawer_textview3 = _nav_view.findViewById(R.id.textview3);
		_drawer_imageview3 = _nav_view.findViewById(R.id.imageview3);
		_drawer_textview4 = _nav_view.findViewById(R.id.textview4);
		_drawer_switch1 = _nav_view.findViewById(R.id.switch1);
		_drawer_imageview4 = _nav_view.findViewById(R.id.imageview4);
		_drawer_textview5 = _nav_view.findViewById(R.id.textview5);
		_drawer_imageview5 = _nav_view.findViewById(R.id.imageview5);
		_drawer_textview6 = _nav_view.findViewById(R.id.textview6);
		_drawer_imageview6 = _nav_view.findViewById(R.id.imageview6);
		_drawer_textview8 = _nav_view.findViewById(R.id.textview8);
		_drawer_imageview7 = _nav_view.findViewById(R.id.imageview7);
		_drawer_textview9 = _nav_view.findViewById(R.id.textview9);
		_drawer_imageview8 = _nav_view.findViewById(R.id.imageview8);
		_drawer_textview10 = _nav_view.findViewById(R.id.textview10);
		_drawer_imageview9 = _nav_view.findViewById(R.id.imageview9);
		_drawer_textview12 = _nav_view.findViewById(R.id.textview12);
		_drawer_imageview10 = _nav_view.findViewById(R.id.imageview10);
		_drawer_textview13 = _nav_view.findViewById(R.id.textview13);


		_drawer_textview12.setText("v" + AppVersionManager.getAppVersion(PrincipalActivity.this));


		final Typeface normalTypeface = Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf");
		final Typeface boldTypeface = Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf");

		_drawer_textview1.setTypeface(boldTypeface, 0);
		_drawer_textview2.setTypeface(boldTypeface, 0);
		_drawer_textview11.setTypeface(boldTypeface, 0);
		_drawer_textview13.setTypeface(normalTypeface, 0);
		_drawer_textview3.setTypeface(normalTypeface, 0);
		_drawer_textview6.setTypeface(normalTypeface, 0);
		_drawer_textview4.setTypeface(normalTypeface, 0);
		_drawer_textview5.setTypeface(normalTypeface, 0);
		_drawer_textview12.setTypeface(normalTypeface, 0);

		textview1.setVisibility(View.GONE);
		imageLogoName.setVisibility(View.VISIBLE);

		bottomnavigation1.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				final int _itemId = item.getItemId();
				viewpager1.setCurrentItem((int)_itemId);
				if (_itemId == 0) {
					JHelpers.TransitionManager(rootLinear, 400);
					textview1.setVisibility(View.GONE);
					imageLogoName.setVisibility(View.VISIBLE);
				}
//				if (_itemId == 1) {
//					textview1.setText("Category");
//				}
				if (_itemId == 1) {
					JHelpers.TransitionManager(rootLinear, 400);
					textview1.setVisibility(View.VISIBLE);
					imageLogoName.setVisibility(View.GONE);
					textview1.setText("Cart Products");
				}
				return true;
			}
		});
		
		linear7i.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				Intent intent = new Intent();
				intent.putExtra("category", "");
				intent.setClass(getApplicationContext(), SearchActivity.class);
				startActivity(intent);
			}
		});

		linear8.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_drawer.openDrawer(Gravity.RIGHT);
			}
		});
		_drawer_rootLinear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_drawer.closeDrawer(Gravity.RIGHT);
			}
		});
		_drawer_linear1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Pass
			}
		});
		
		viewpager1.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {
				
			}
			
			@Override
			public void onPageSelected(int _position) {
				bottomnavigation1.getMenu().getItem(_position).setChecked(true);
				if (_position == 0) {
					JHelpers.TransitionManager(rootLinear, 400);
					textview1.setVisibility(View.GONE);
					imageLogoName.setVisibility(View.VISIBLE);
				}
//				if (_position == 1) {
//					textview1.setText("Category");
//				}
				if (_position == 1) {
					JHelpers.TransitionManager(rootLinear, 400);
					textview1.setVisibility(View.VISIBLE);
					imageLogoName.setVisibility(View.GONE);
					textview1.setText("Cart Products");
				}
			}
			
			@Override
			public void onPageScrollStateChanged(int _scrollState) {
				
			}
		});

		Business.UserDataApiClient.getUserDataCallApi(userId, new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
			@Override
			public void onReceived(Business.UserDataApiClient.UserDataApiResponse _data) {
				if(_data.getStatusCode() == 200 && _data.getUser() != null) {
					_drawer_textview1.setText(_data.getUser().name);
				} else {
					_drawer_textview1.setText("Unknown User");
				}
			}
		});


//		_firebase.getReference("datas/users/details/".concat(userId)).addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(DataSnapshot snapshot) {
//				if (snapshot.exists()) {
//					if (snapshot.hasChild("name")) {
//						String name = snapshot.child("name").getValue(String.class);
//						_drawer_textview1.setText(name);
//					} else {
//						_drawer_textview1.setText("Unknown User");
//					}
//				} else {
//					_drawer_textview1.setText("Unknown User");
//				}
//			}
//
//			@Override
//			public void onCancelled(DatabaseError error) {
//				// Handle error if needed
//			}
//		});


		_drawer_linear1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				
			}
		});
		
		_drawer_linear7.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_drawer.closeDrawer(Gravity.RIGHT);
				ii.setClass(getApplicationContext(), OrderActivity.class);
				startActivity(ii);
			}
		});

		_drawer_linear4.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_drawer.closeDrawer(Gravity.RIGHT);
				if(viewpager1.getCurrentItem() != 0) {
					viewpager1.setCurrentItem(0);
				}
			}
		});

		_drawer_linear12.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				_drawer.closeDrawer(Gravity.RIGHT);
				ii.setClass(getApplicationContext(), ProfileActivity.class);
				startActivity(ii);
			}
		});

		_drawer_imageview1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if(Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), "no_proxy").equals("no_proxy")) {
					Business.JFCM.unSubscribeAll();
					Business.localDB_SharedPref.clearCart(getSharedPreferences("localDB", Activity.MODE_PRIVATE));
					FirebaseAuth.getInstance().signOut();

					ii.setClass(getApplicationContext(), MainActivity.class);
					startActivity(ii);
					finish();
				} else {
					Business.localDB_SharedPref.clearCart(getSharedPreferences("localDB", Activity.MODE_PRIVATE));
					finish();
				}
			}
		});
	}
	
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);
		_DG_DrawerTransparent();
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _v) {
				_drawer.openDrawer(Gravity.RIGHT);
				SketchwareUtil.hideKeyboard(getApplicationContext());
			}
		});
		LinearLayout _nav_view = (LinearLayout) findViewById(R.id._nav_view);
		androidx.drawerlayout.widget.DrawerLayout
		.LayoutParams lp = new androidx.drawerlayout.widget.DrawerLayout
		.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		lp.gravity=Gravity.RIGHT;
		_nav_view.setLayoutParams(lp);
		//mahdi_313
		getSupportActionBar().hide();
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 1);
		bottomnavigation1.getMenu().add(0, 0, 0, "Home").setIcon(R.drawable.home);
//		bottomnavigation1.getMenu().add(0, 1, 0, "").setIcon(R.drawable.categories2);
		bottomnavigation1.getMenu().add(0, 1, 0, "Cart").setIcon(R.drawable.trolley);
		viewpager1.setAdapter(new MyFragmentAdapter(getSupportFragmentManager()));
	}
	public class MyFragmentAdapter extends androidx.fragment.app.FragmentStatePagerAdapter {
				
				public MyFragmentAdapter(FragmentManager manager) {
						super(manager);
				}

				
				@Override
				public int getCount() {
						return 2;
				}
				@Override
				public Fragment getItem(int _position) {
						if (_position == 0) {
					return new HomeFragmentActivity();
			}
//			if (_position == 1) {
//					return new CategoryFragmentActivity();
//			}
			if (_position == 1) {
					return new CartFragmentActivity();
			}
						return null;
				}
		}
	{
	}
	
	@Override
	public void onBackPressed() {
		if (_drawer.isDrawerOpen(GravityCompat.START)) {
			_drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}
	public void _DG_DrawerTransparent() {
		final LinearLayout _nav_view = (LinearLayout) findViewById(R.id._nav_view); _nav_view.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
		//mahdi_313
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
