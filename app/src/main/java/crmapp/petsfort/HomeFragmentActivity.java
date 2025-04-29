package crmapp.petsfort;

import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.view.View;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import androidx.core.widget.NestedScrollView;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.User;


public class HomeFragmentActivity extends Fragment {

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

	private Timer _timer = new Timer();

	private double postion = 0;

	private ArrayList<HashMap<String, String>> listmap;

	private LinearLayout linear1;
	private NestedScrollView linear2;
	private LinearLayout linear5;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private LinearLayout linear8;
	private ImageView imageview1;
	private TextView textview1;
	private ImageView imageview2;
	private ImageView imageview3;
	private LinearLayout linear3;
	private LinearLayout linear26;
	private LinearLayout linear4;
	private LinearLayout linear11,totalCreditsLinear,totalCreditsLinear2;

	private LinearLayout load;
	private LinearLayout sk;
	private LinearLayout linear27;
	private LinearLayout linear30;
	private LinearLayout linear32;
	private LinearLayout linear37;
	private ShimmerFrameLayout linear28;
	private ShimmerFrameLayout linear29;
	private ShimmerFrameLayout linear31,linear319;
	private ShimmerFrameLayout linear33;
	private ShimmerFrameLayout linear34;
	private ShimmerFrameLayout linear35;
	private ShimmerFrameLayout linear36;
	private ShimmerFrameLayout linear38;
	private ShimmerFrameLayout linear39;
	private ShimmerFrameLayout linear40;
	private ShimmerFrameLayout linear41;
	private RelativeLayout linear9;
	private ViewPager viewpager_slider;
	private LinearLayout linear10;
	private RecyclerView recyclerview1;
	private TextView textview2;
	private LinearLayout linear12,totalLinear3;
	private TextView textview3;

	private CardView orderViewCard;


	private TextView tvBalanceLabel, tvBalanceAmount, viewOrder;
	private ProgressBar progressbar1;

	private Intent action = new Intent();
	private TimerTask t;
	private SharedPreferences sp;
	private Intent i = new Intent();
	private TimerTask u;

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.home_fragment, _container, false);
		userId = Business.localDB_SharedPref.getProxyUID(getActivity().getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}

	private void initialize(Bundle _savedInstanceState, View _view) {
		linear1 = _view.findViewById(R.id.linear1);
		linear2 = _view.findViewById(R.id.linear2);
		linear5 = _view.findViewById(R.id.linear5);
		linear6 = _view.findViewById(R.id.linear6);
		linear7 = _view.findViewById(R.id.linear7);
		linear8 = _view.findViewById(R.id.linear8);
		imageview1 = _view.findViewById(R.id.imageview1);
		textview1 = _view.findViewById(R.id.textview1);
		imageview2 = _view.findViewById(R.id.imageview2);
		imageview3 = _view.findViewById(R.id.imageview3);
		linear3 = _view.findViewById(R.id.linear3);
		linear26 = _view.findViewById(R.id.linear26);
		linear4 = _view.findViewById(R.id.linear4);
		linear11 = _view.findViewById(R.id.linear11);
		totalCreditsLinear = _view.findViewById(R.id.totalCreditsLinear);
		totalCreditsLinear2 = _view.findViewById(R.id.totalCreditsLinear2);
		orderViewCard = _view.findViewById(R.id.orderViewCard);

		load = _view.findViewById(R.id.load);
		sk = _view.findViewById(R.id.sk);
		linear27 = _view.findViewById(R.id.linear27);
		linear30 = _view.findViewById(R.id.linear30);
		linear32 = _view.findViewById(R.id.linear32);
		linear37 = _view.findViewById(R.id.linear37);
		linear28 = _view.findViewById(R.id.linear28);
		linear29 = _view.findViewById(R.id.linear29);
		linear31 = _view.findViewById(R.id.linear31);
		linear319 = _view.findViewById(R.id.linear319);
		linear33 = _view.findViewById(R.id.linear33);
		linear34 = _view.findViewById(R.id.linear34);
		linear35 = _view.findViewById(R.id.linear35);
		linear36 = _view.findViewById(R.id.linear36);
		linear38 = _view.findViewById(R.id.linear38);
		linear39 = _view.findViewById(R.id.linear39);
		linear40 = _view.findViewById(R.id.linear40);
		linear41 = _view.findViewById(R.id.linear41);
		linear9 = _view.findViewById(R.id.linear9);
		viewpager_slider = _view.findViewById(R.id.viewpager_slider);
		linear10 = _view.findViewById(R.id.linear10);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		textview2 = _view.findViewById(R.id.textview2);
		linear12 = _view.findViewById(R.id.linear12);
		totalLinear3 = _view.findViewById(R.id.totalLinear3);
		textview3 = _view.findViewById(R.id.textview3);


		totalLinear3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				i.setClass(getContext().getApplicationContext(), ProfileActivity.class);
				startActivity(i);
			}
		});


		tvBalanceLabel = _view.findViewById(R.id.tvBalanceLabel);
		tvBalanceAmount = _view.findViewById(R.id.tvBalanceAmount);
		viewOrder = _view.findViewById(R.id.viewOrder);

		progressbar1 = _view.findViewById(R.id.progressbar1);
		sp = getContext().getSharedPreferences("sp", Activity.MODE_PRIVATE);

		viewpager_slider.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int _position) {
				postion = _position;
				recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));
				recyclerview1.smoothScrollToPosition((int)_position);
			}

			@Override
			public void onPageScrollStateChanged(int _scrollState) {

			}
		});

//		textview3.setOnClickListener(new View.OnClickListener(){
//			@Override
//			public void onClick(View _view){
//				i.setClass(getContext().getApplicationContext(), CategoryActivity.class);
//				startActivity(i);
//			}
//		});

		orderViewCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				i.setClass(getContext().getApplicationContext(), OrderActivity.class);
				startActivity(i);
			}
		});
	}

	private void initializeLogic() {
		listmap = new ArrayList<>();

		recyclerview1.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL, false));
		recyclerview1.setHasFixedSize(true);
		recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));

		final float scaleFactor = 0.99f; viewpager_slider.setPageMargin(-0); viewpager_slider.setOffscreenPageLimit(2); viewpager_slider.setPageTransformer(false, new ViewPager.PageTransformer() { @Override public void transformPage(@NonNull View page1, float position) { page1.setScaleY((1 - Math.abs(position) * (1 - scaleFactor))); page1.setScaleX(scaleFactor + Math.abs(position) * (1 - scaleFactor)); } });
		viewpager_slider.setAdapter(new Viewpager_sliderAdapter(listmap));

		linear26.setVisibility(View.VISIBLE);
		linear4.setVisibility(View.GONE);
		sk.setVisibility(View.GONE);
		linear11.setVisibility(View.GONE);
		totalCreditsLinear.setVisibility(View.GONE);
		totalCreditsLinear2.setVisibility(View.GONE);

		tvBalanceAmount.setText("...");

//		JHelpers.runAfterDelay(getActivity(),100, new Callbacker.Timer(){
//			@Override
//			public void onEnd() {
				_firebase.getReference("datas/announcement/all").addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot _dataSnapshot) {
						if(_dataSnapshot.exists()) {
							for (DataSnapshot child : _dataSnapshot.getChildren()) {
								HashMap<String, String> announcementMap = new HashMap<>();
								String value = String.valueOf(child.getValue());
								announcementMap.put("img", value);
								listmap.add(announcementMap);
							}
							recyclerview1.getAdapter().notifyDataSetChanged();
							viewpager_slider.getAdapter().notifyDataSetChanged();
							viewpager_slider.setCurrentItem(0);
						} else {
							linear9.setVisibility(View.GONE);
						}

						hideLoadingAndStartIntro();
					}

					@Override
					public void onCancelled(DatabaseError _databaseError) { }
				});
//			}
//		});


		linear1.setVisibility(View.GONE);
		load.setVisibility(View.GONE);
		textview2.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 1);
//		textview3.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);


		tvBalanceLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
		tvBalanceAmount.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
		viewOrder.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);


		linear28.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear29.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear31.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)25, 0xFFBDBDBD));
		linear319.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)25, 0xFFBDBDBD));
		linear33.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear34.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear35.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear36.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear38.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear39.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear40.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));
		linear41.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFBDBDBD));

		Fragment fragment = new Frag1FragmentActivity();
		getChildFragmentManager()
				.beginTransaction()
				.replace(R.id.sk, fragment)
				.commit();

	}


	public void hideLoadingAndStartIntro() {
		JHelpers.runAfterDelay(getActivity(), 100, new Callbacker.Timer(){
			@Override
			public void onEnd() {
				JHelpers.TransitionManager(linear3, 400);
				linear26.setVisibility(View.GONE);
				linear4.setVisibility(View.VISIBLE);

				JHelpers.runAfterDelay(getActivity(), 400, new Callbacker.Timer(){
					@Override
					public void onEnd() {
						float translationY = TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_DIP, 35, getContext().getResources().getDisplayMetrics());
						float translationYfrom = TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_DIP, -6, getContext().getResources().getDisplayMetrics());

						JHelpers.TransitionManager(linear3, 400);
						totalCreditsLinear.setVisibility(View.VISIBLE);
						totalCreditsLinear2.setVisibility(View.VISIBLE);

						JHelpers.JValueAnimator.animate((int) translationYfrom, (int) translationY, 2000, new Callbacker.onAnimateUpdate(){
							@Override
							public void onUpdate(int _value) {
								totalCreditsLinear2.setTranslationY(_value);
							}

							@Override
							public void onEnd() {
								totalCreditsLinear2.setTranslationY(translationY);
							}
						});

						JHelpers.runAfterDelay(getActivity(), 400, new Callbacker.Timer(){
							@Override
							public void onEnd() {
								JHelpers.TransitionManager(linear3, 400);
								linear11.setVisibility(View.VISIBLE);
								sk.setVisibility(View.VISIBLE);
								loadCredits();
							}
						});
					}
				});
			}
		});

	}

	public void _rippleRoundStroke(final View _view, final String _focus, final String _pressed, final double _round, final double _stroke, final String _strokeclr) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(Color.parseColor(_focus));
		GG.setCornerRadius((float)_round);
		GG.setStroke((int) _stroke,
		Color.parseColor("#" + _strokeclr.replace("#", "")));
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ Color.parseColor(_pressed)}), GG, null);
		_view.setBackground(RE);
	}



	public void loadCredits() {

		JHelpers.runAfterDelay(getActivity(),400,new Callbacker.Timer(){
			@Override
			public void onEnd() {

				Business.UserDataApiClient.getUserDataCallApi(userId, new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
					@Override
					public void onReceived(Business.UserDataApiClient.UserDataApiResponse _data) {
						updateCredits(_data.getUser());
					}
				});

//				_firebase.getReference("datas/users/details/".concat(userId)).addListenerForSingleValueEvent(new ValueEventListener() {
//
//					@Override
//					public void onDataChange(DataSnapshot _dataSnapshot) {
//						updateCredits(_dataSnapshot);
//					}
//
//					@Override
//					public void onCancelled(DatabaseError _databaseError) { }
//				});
			}
		});

	}


	public void updateCredits(User user) {
		try {
			String dateStr = user.creditse;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			LocalDate expiryDate = LocalDate.parse(dateStr, formatter);

			LocalDate today = LocalDate.now();

			long daysToExpire = ChronoUnit.DAYS.between(today, expiryDate);

			if (daysToExpire > 0) {
				tvBalanceLabel.setText(tvBalanceLabel.getText().toString() + " Expire : " + daysToExpire + " days (" + dateStr + ")");
			} else if (daysToExpire == 0) {
				tvBalanceLabel.setText(tvBalanceLabel.getText().toString() + " Expiring Today (" + dateStr + ")");
			} else {
				tvBalanceLabel.setText(tvBalanceLabel.getText().toString() + " Expired " + Math.abs(daysToExpire) + " days ago.");
			}
		} catch(Exception e) {
//			tvBalanceLabel.setText("No Credits Left");
		}


		double credits = 0;
		String creditsStr = "No Credits Left";

		if (user != null){
			creditsStr = String.valueOf(user.credits);
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



	public class Viewpager_sliderAdapter extends PagerAdapter {

		Context _context;
		ArrayList<HashMap<String, String>> _data;

		public Viewpager_sliderAdapter(Context _ctx, ArrayList<HashMap<String, String>> _arr) {
			_context = _ctx;
			_data = _arr;
		}

		public Viewpager_sliderAdapter(ArrayList<HashMap<String, String>> _arr) {
			_context = getContext().getApplicationContext();
			_data = _arr;
		}

		@Override
		public int getCount() {
			return _data.size();
		}

		@Override
		public boolean isViewFromObject(View _view, Object _object) {
			return _view == _object;
		}

		@Override
		public void destroyItem(ViewGroup _container, int _position, Object _object) {
			_container.removeView((View) _object);
		}

		@Override
		public int getItemPosition(Object _object) {
			return super.getItemPosition(_object);
		}

		@Override
		public CharSequence getPageTitle(int pos) {
			// Use the Activity Event (onTabLayoutNewTabAdded) in order to use this method
			return "page " + String.valueOf(pos);
		}

		@Override
		public Object instantiateItem(ViewGroup _container,  final int _position) {
			View _view = LayoutInflater.from(_context).inflate(R.layout.slider, _container, false);

			final androidx.cardview.widget.CardView cardview1 = _view.findViewById(R.id.cardview1);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);

			Glide.with(getContext().getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("img").toString())).into(imageview1);
			cardview1.setRadius((float)15);
			cardview1.setCardElevation((float)5);

			_container.addView(_view);
			return _view;
		}
	}

	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {

		ArrayList<HashMap<String, String>> _data;

		public Recyclerview1Adapter(ArrayList<HashMap<String, String>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.dots, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final LinearLayout linear1 = _view.findViewById(R.id.linear1);

			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			_view.setLayoutParams(_lp);
			_rippleRoundStroke(linear1, "#FFFFFF", "#FFFFFF", 555, 0, "#FFFFFF");
			if (postion == _position) {
				_rippleRoundStroke(linear1, "#FFFFFF", "#FFFFFF", 555, 0, "#FFFFFF");
				linear1.setAlpha((float)(1.0d));
			}
			else {
				_rippleRoundStroke(linear1, "#FFFFFF", "#FFFFFF", 555, 0, "#FFFFFF");
				linear1.setAlpha((float)(0.4d));
			}
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
