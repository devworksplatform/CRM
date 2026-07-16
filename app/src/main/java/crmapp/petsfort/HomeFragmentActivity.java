package crmapp.petsfort;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
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

import com.airbnb.lottie.LottieAnimationView;
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import androidx.core.widget.NestedScrollView;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.Product;
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
		extras(_view);
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

		final float scaleFactor = 0.985f;
		viewpager_slider.setClipToPadding(false);
		viewpager_slider.setPageMargin((int) (8 * getResources().getDisplayMetrics().density));
		viewpager_slider.setOffscreenPageLimit(2);
		viewpager_slider.setPageTransformer(false, new ViewPager.PageTransformer() { @Override public void transformPage(@NonNull View page1, float position) { page1.setScaleY((1 - Math.abs(position) * (1 - scaleFactor))); page1.setScaleX(scaleFactor + Math.abs(position) * (1 - scaleFactor)); } });
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
								if (child.child("type").exists()) {
									announcementMap.put("img", String.valueOf(child.child("img").getValue() == null ? "" : child.child("img").getValue()));
									announcementMap.put("title", String.valueOf(child.child("title").getValue() == null ? "" : child.child("title").getValue()));
									announcementMap.put("subtitle", String.valueOf(child.child("subtitle").getValue() == null ? "" : child.child("subtitle").getValue()));
									announcementMap.put("product_id", String.valueOf(child.child("product_id").getValue() == null ? "" : child.child("product_id").getValue()));
									announcementMap.put("type", String.valueOf(child.child("type").getValue() == null ? "" : child.child("type").getValue()));
									announcementMap.put("group_id", String.valueOf(child.child("group_id").getValue() == null ? "" : child.child("group_id").getValue()));
								} else {
									announcementMap.put("img", String.valueOf(child.getValue()));
								}
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

//		Fragment fragment = new Frag1FragmentActivity();
//		getChildFragmentManager()
//				.beginTransaction()
//				.replace(R.id.sk, fragment)
//				.commit();

	}




	int n=0;
	void extras(View _view) {
		LottieAnimationView catVisit =_view.findViewById(R.id.cat_visit);

		catVisit.setProgress(0f); // 0f = start frame
		catVisit.setSpeed(1f);  // Normal speed
		catVisit.playAnimation();
		catVisit.addAnimatorListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(@NonNull Animator animation) {

			}

			@Override
			public void onAnimationEnd(@NonNull Animator animation) {
				if(n==0){
					catVisit.setProgress(1f);
					catVisit.setSpeed(-1f);
					catVisit.playAnimation();
					n+=1;
				} else {
					_view.findViewById(R.id.cat_visitLinear).setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationCancel(@NonNull Animator animation) {

			}

			@Override
			public void onAnimationRepeat(@NonNull Animator animation) {

			}
		});
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

		Fragment fragment = new Frag1FragmentActivity();
		getChildFragmentManager()
				.beginTransaction()
				.replace(R.id.sk, fragment)
				.commit();

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
		public float getPageWidth(int position) {
			return _data.size() > 1 ? 0.92f : 1f;
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
			final LinearLayout offerContainer = _view.findViewById(R.id.offerBannerTextContainer);
			final TextView offerTitle = _view.findViewById(R.id.offerBannerTitle);
			final TextView offerSubtitle = _view.findViewById(R.id.offerBannerSubtitle);
			final TextView offerBadge = _view.findViewById(R.id.offerBannerBadge);
			final TextView offerAction = _view.findViewById(R.id.offerBannerAction);
			final ImageView offerImage = _view.findViewById(R.id.offerBannerImage);
			final ImageView announcementIcon = _view.findViewById(R.id.offerAnnouncementIcon);
			final ImageView celebrationIcon = _view.findViewById(R.id.offerCelebrationIcon);
			final FrameLayout confettiOverlay = _view.findViewById(R.id.offerConfettiOverlay);

			String imageUrl = _data.get((int)_position).get("img");
			String title = _data.get((int)_position).get("title");
			String groupId = _data.get((int)_position).get("group_id");
			if (title != null && !title.isEmpty()) {
				imageview1.setVisibility(View.GONE);
				offerContainer.setVisibility(View.VISIBLE);
				announcementIcon.setVisibility(View.VISIBLE);
				celebrationIcon.setVisibility(View.VISIBLE);
				confettiOverlay.setVisibility(View.VISIBLE);
				offerTitle.setText(title);
				offerSubtitle.setText(_data.get((int)_position).get("subtitle"));
				offerBadge.setText(groupId != null && !groupId.isEmpty() ? "GROUP OFFER" : "PRODUCT OFFER");
				offerAction.setText(groupId != null && !groupId.isEmpty() ? "View products  →" : "View product  →");
				if (imageUrl != null && !imageUrl.isEmpty()) {
					Glide.with(getContext().getApplicationContext()).load(Uri.parse(imageUrl)).placeholder(R.drawable.default_image).centerCrop().into(offerImage);
				} else {
					offerImage.setImageResource(R.drawable.default_image);
				}
				announcementIcon.setAlpha(0f);
				announcementIcon.setTranslationX(-70f);
				announcementIcon.setRotation(-18f);
				announcementIcon.animate().alpha(1f).translationX(0f).rotation(0f).setDuration(480)
						.setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();
				celebrationIcon.setAlpha(0f);
				celebrationIcon.setTranslationX(55f);
				celebrationIcon.setTranslationY(-20f);
				celebrationIcon.setRotation(-66f);
				celebrationIcon.animate().alpha(1f).translationX(0f).translationY(0f).rotation(-90f).setStartDelay(100).setDuration(430)
						.setInterpolator(new android.view.animation.OvershootInterpolator(1.6f)).start();
				confettiOverlay.setAlpha(0f);
				confettiOverlay.setScaleX(0.72f);
				confettiOverlay.setScaleY(0.72f);
				confettiOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(80).setDuration(520)
						.setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();
				startLoopingOfferAnimations(_view, announcementIcon, celebrationIcon, confettiOverlay);
			} else {
				offerContainer.setVisibility(View.GONE);
				announcementIcon.setVisibility(View.GONE);
				celebrationIcon.setVisibility(View.GONE);
				confettiOverlay.setVisibility(View.GONE);
				imageview1.setVisibility(View.VISIBLE);
				if (imageUrl != null && !imageUrl.isEmpty()) {
					Glide.with(getContext().getApplicationContext()).load(Uri.parse(imageUrl)).placeholder(R.drawable.default_image).centerCrop().into(imageview1);
				}
			}
			cardview1.setRadius((float)18);
			cardview1.setCardElevation((float)2);
			cardview1.setForeground(null);
			String productId = _data.get((int)_position).get("product_id");
			boolean hasOfferTarget = (groupId != null && !groupId.isEmpty()) || (productId != null && !productId.isEmpty());
			if (hasOfferTarget) {
				cardview1.setOnTouchListener((view, event) -> {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						view.animate().scaleX(0.985f).scaleY(0.985f).setDuration(80).start();
					} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
						view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
					}
					return false;
				});
			}
			if (groupId != null && !groupId.isEmpty()) {
				cardview1.setClickable(true);
				cardview1.setOnClickListener(v -> openOfferGroup(groupId, title));
			} else if (productId != null && !productId.isEmpty()) {
				cardview1.setClickable(true);
				cardview1.setOnClickListener(v -> openOfferProduct(productId));
			}

			_container.addView(_view);
			return _view;
		}
	}

	private void startLoopingOfferAnimations(View bannerView, ImageView announcementIcon,
			ImageView celebrationIcon, FrameLayout confettiOverlay) {
		ObjectAnimator announcementPulse = ObjectAnimator.ofPropertyValuesHolder(
				announcementIcon,
				PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f),
				PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f),
				PropertyValuesHolder.ofFloat(View.ROTATION, 0f, -7f, 5f, 0f));
		announcementPulse.setDuration(1050);
		announcementPulse.setRepeatCount(ValueAnimator.INFINITE);
		announcementPulse.setRepeatMode(ValueAnimator.RESTART);

		ObjectAnimator popperBounce = ObjectAnimator.ofPropertyValuesHolder(
				celebrationIcon,
				PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -9f, 0f),
				PropertyValuesHolder.ofFloat(View.ROTATION, -90f, -82f, -94f, -90f),
				PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
				PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f));
		popperBounce.setDuration(1200);
		popperBounce.setStartDelay(150);
		popperBounce.setRepeatCount(ValueAnimator.INFINITE);
		popperBounce.setRepeatMode(ValueAnimator.RESTART);

		ArrayList<Animator> animationList = new ArrayList<>();
		animationList.add(announcementPulse);
		animationList.add(popperBounce);
		createIndependentConfetti(confettiOverlay, animationList);
		final Animator[] animations = animationList.toArray(new Animator[0]);
		final boolean[] entranceComplete = {false};
		Runnable startAnimations = () -> {
			if (!bannerView.isAttachedToWindow()) return;
			for (Animator animation : animations) {
				if (!animation.isRunning()) animation.start();
			}
		};
		bannerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) {
				if (entranceComplete[0]) startAnimations.run();
			}

			@Override
			public void onViewDetachedFromWindow(View v) {
				for (Animator animation : animations) animation.cancel();
			}
		});
		bannerView.postDelayed(() -> {
			entranceComplete[0] = true;
			startAnimations.run();
		}, 650);
	}

	private void createIndependentConfetti(FrameLayout layer, ArrayList<Animator> animations) {
		layer.removeAllViews();
		final float density = getResources().getDisplayMetrics().density;
		final int[] colors = {0xFFFDE047, 0xFFFFFFFF, 0xFFFB7185, 0xFF67E8F9, 0xFFA7F3D0, 0xFFC4B5FD};
		Random random = new Random(7419);

		for (int i = 0; i < 22; i++) {
			View paper = new View(getContext());
			int width = Math.round((3 + random.nextInt(4)) * density);
			int height = Math.round((6 + random.nextInt(7)) * density);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
			layer.addView(paper, params);
			paper.setAlpha(0f);

			GradientDrawable shape = new GradientDrawable();
			shape.setColor(colors[random.nextInt(colors.length)]);
			shape.setCornerRadius((random.nextBoolean() ? 1.5f : 5f) * density);
			paper.setBackground(shape);

			final float horizontalVelocity = -(70 + random.nextInt(245)) * density;
			final float verticalVelocity = -(45 + random.nextInt(145)) * density;
			final float gravity = (170 + random.nextInt(150)) * density;
			final float curve = (random.nextFloat() * 32f - 16f) * density;
			final float spin = (random.nextBoolean() ? 1f : -1f) * (380 + random.nextInt(1080));
			final float startRotation = random.nextInt(180);
			final float originYOffset = (8 + random.nextInt(28)) * density;

			ValueAnimator flight = ValueAnimator.ofFloat(0f, 1f);
			flight.setDuration(1050 + random.nextInt(1050));
			flight.setStartDelay(random.nextInt(950));
			flight.setRepeatCount(ValueAnimator.INFINITE);
			flight.setRepeatMode(ValueAnimator.RESTART);
			flight.setInterpolator(new android.view.animation.LinearInterpolator());
			flight.addUpdateListener(animation -> {
				float time = (float) animation.getAnimatedValue();
				float originX = Math.max(0f, layer.getWidth() - 18f * density);
				float x = originX + horizontalVelocity * time + curve * time * time;
				float y = originYOffset + verticalVelocity * time + gravity * time * time;
				paper.setX(x);
				paper.setY(y);
				paper.setRotation(startRotation + spin * time);
				paper.setRotationX((spin * 0.7f) * time);
				paper.setScaleX(0.55f + 0.45f * Math.abs((float) Math.cos(time * Math.PI * (2.5f + Math.abs(spin) / 700f))));
				float alpha = time < 0.08f ? time / 0.08f : (time > 0.72f ? (1f - time) / 0.28f : 1f);
				paper.setAlpha(Math.max(0f, Math.min(1f, alpha)));
			});
			animations.add(flight);
		}
	}

	private void openOfferGroup(String groupId, String title) {
		Intent intent = new Intent(getContext(), SearchActivity.class);
		intent.putExtra("offer_group_id", groupId);
		intent.putExtra("offer_group_title", title == null ? "Group offer" : title);
		startActivity(intent);
		requireActivity().overridePendingTransition(R.anim.offer_page_enter, R.anim.offer_page_exit);
	}

	private void openOfferProduct(String productId) {
		HashMap<String, String> filter = new HashMap<>();
		filter.put("field", "product_id");
		filter.put("operator", "eq");
		filter.put("value", productId);
		ArrayList<HashMap<String, String>> filters = new ArrayList<>();
		filters.add(filter);
		HashMap<String, Object> request = new HashMap<>();
		request.put("filters", filters);
		request.put("limit", 1);
		request.put("offset", 0);

		new Business.QueryApiClient().callApi(request, new Callbacker.ApiResponseWaiters.QueryApiCallback() {
			@Override
			public void onReceived(Business.QueryApiClient.QueryApiResponse response) {
				if (!isAdded()) return;
				if (response.getStatusCode() == 200 && !response.getProducts().isEmpty()) {
					Product offerProduct = response.getProducts().get(0);
					Intent intent = new Intent(getContext(), ProductviewActivity.class);
					intent.putExtra("product", offerProduct);
					startActivity(intent);
					requireActivity().overridePendingTransition(R.anim.offer_page_enter, R.anim.offer_page_exit);
				} else {
					Toast.makeText(getContext(), "This offer is no longer available", Toast.LENGTH_SHORT).show();
				}
			}
		});
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
