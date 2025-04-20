package crmapp.petsfort;

import android.animation.*;
import android.content.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.internal.LinkedTreeMap;

import java.text.*;
import java.util.ArrayList;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.CartProduct;
import crmapp.petsfort.JLogics.Models.Product;
import crmapp.petsfort.JLogics.Models.User;

public class CartFragmentActivity extends Fragment {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
	SharedPreferences localDB;

	private double n = 0;
	private String st = "";
	private double pos = 0;
	private double count1 = 0;
	private double length = 0;
	private String charq = "";
	
	private ArrayList<CartProduct> listmap = new ArrayList<>();
	
	private LinearLayout linear1;
	private RecyclerView recyclerview1;
	private ProgressBar progressbar1;
	
	private FirebaseAuth auth;
	
//	private DatabaseReference cart;
	private ChildEventListener _cart_child_listener;
	private Intent i = new Intent();

	private TextView subTotalTextviewLabel,gstTextviewLabel,grandTotalTextviewLabel,creditTextviewLabel,discountTotalTextviewLabel,totalNoDiscountLabelTextView,textview1;
	private TextView subTotalTextview,gstTextview,grandTotalTextview,creditTextview,discountTotalTextview,totalNoDiscountTextView,textviewTotal,textviewTotalLabel,textviewConfirmLabel;

	private RelativeLayout linear2;
	private LinearLayout costLinear,progress_overlay,linearDrag,confirmOrderLinear,linearNoData;
	private LinearLayout linear10,linear11,linear15,linear16,circle,circle2;
	private ImageView bottomDragImage;
	DecimalFormat df = new DecimalFormat("#.###");

	private boolean isOpen = false;


	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.cart_fragment, _container, false);
		FirebaseApp.initializeApp(getContext());
		initialize(_savedInstanceState, _view);
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		auth = FirebaseAuth.getInstance();
		localDB = getContext().getSharedPreferences("localDB", Context.MODE_PRIVATE);
		linear1 = _view.findViewById(R.id.linear1);
		linear2 = _view.findViewById(R.id.linear2);
		costLinear = _view.findViewById(R.id.costLinear);
		linearNoData = _view.findViewById(R.id.linearNoData);
		linearNoData.setVisibility(View.GONE);

		linearDrag = _view.findViewById(R.id.linearDrag);
		confirmOrderLinear = _view.findViewById(R.id.confirmOrderLinear);

		circle = _view.findViewById(R.id.circle);
		circle2 = _view.findViewById(R.id.circle2);
		linear10 = _view.findViewById(R.id.linear10);
		linear11 = _view.findViewById(R.id.linear11);
		linear15 = _view.findViewById(R.id.linear15);
		linear16 = _view.findViewById(R.id.linear16);

		progress_overlay = _view.findViewById(R.id.progress_overlay);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		progressbar1 = _view.findViewById(R.id.progressbar1);
		bottomDragImage = _view.findViewById(R.id.bottomDragImage);

		subTotalTextview = _view.findViewById(R.id.subTotalTextView);
		subTotalTextview.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		subTotalTextviewLabel = _view.findViewById(R.id.subtotalLabelTextView);
		subTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		gstTextview = _view.findViewById(R.id.gstTotalTextView);
		gstTextview.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		gstTextviewLabel = _view.findViewById(R.id.gstTotalLabelTextView);
		gstTextviewLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		grandTotalTextview = _view.findViewById(R.id.grandTotalTextView);
		grandTotalTextview.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		grandTotalTextviewLabel = _view.findViewById(R.id.grandTotalLabelTextView);
		grandTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		creditTextview = _view.findViewById(R.id.creditTextView);
		creditTextview.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		creditTextviewLabel = _view.findViewById(R.id.creditLabelTextView);
		creditTextviewLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		discountTotalTextview = _view.findViewById(R.id.discountTotalTextView);
		discountTotalTextview.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		discountTotalTextviewLabel = _view.findViewById(R.id.discountTotalLabelTextView);
		discountTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		totalNoDiscountTextView = _view.findViewById(R.id.totalNoDiscountTextView);
		totalNoDiscountTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		totalNoDiscountLabelTextView = _view.findViewById(R.id.totalNoDiscountLabelTextView);
		totalNoDiscountLabelTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		textviewTotal = _view.findViewById(R.id.textviewTotal);
		textviewTotal.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
		textviewTotalLabel = _view.findViewById(R.id.textviewTotalLabel);
		textviewTotalLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		textviewConfirmLabel = _view.findViewById(R.id.textviewConfirmLabel);
		textviewConfirmLabel.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);

		textview1 = _view.findViewById(R.id.textview1);
		textview1.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);

		textview1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));



//		cart = _firebase.getReference("datas/cart/".concat(userId));
		HashMap<String,Object> map = Business.localDB_SharedPref.getHashMap(localDB);

		GridLayoutManager gridlayoutManager= new GridLayoutManager(getContext().getApplicationContext(), 1, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
		recyclerview1.setLayoutManager(gridlayoutManager);
		listmap = new ArrayList<>();
		recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));

		final Handler handler = new Handler(Looper.getMainLooper());
		final Runnable cartUpdateRunnable = new Runnable() {
			private HashMap<String, Object> previousCartData = null;

			@Override
			public void run() {
				HashMap<String, Object> currentCartData = Business.localDB_SharedPref.getCart(localDB);
				if (previousCartData == null || !previousCartData.equals(currentCartData)) {
					updateCartListUI(currentCartData);
					previousCartData = currentCartData;
				}
				handler.postDelayed(this, 100);
			}
		};

		// To start the periodic updates:
		handler.postDelayed(cartUpdateRunnable, 100);

//		updateCartListUI(Business.localDB_SharedPref.getCart(localDB));


		linearDrag.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleTranslateY(linear2, isOpen);
				rotateDragImageView(bottomDragImage, isOpen);
				isOpen = !isOpen;
			}
		});

		confirmOrderLinear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(getContext().getApplicationContext(), OrderreviewActivity.class);
				startActivity(intent);
			}
		});




		Business.UserDataApiClient.getUserDataCallApi(userId, new Callbacker.ApiResponseWaiters.UserDataApiCallback(){
			@Override
			public void onReceived(Business.UserDataApiClient.UserDataApiResponse _data) {
				double credits = 0;
				if(_data.getStatusCode() == 200 && _data.getUser() != null) {
					credits = _data.getUser().credits;
				}
				creditTextview.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(credits)));
			}
		});

//		_firebase.getReference("datas/users/details/".concat(userId)).addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(DataSnapshot _dataSnapshot) {
//				double credits = 0;
//				String creditsStr = "0";
//				if (_dataSnapshot.exists() && _dataSnapshot.hasChild("credits")) {
//					creditsStr = _dataSnapshot.child("credits").getValue(String.class);
//					try{
//						credits = Double.parseDouble(creditsStr);
//					} catch (Exception e) {}
//				}
//
//				creditTextview.setText("₹ ".concat(JHelpers.formatDoubleToRupeesString(credits)));
//			}
//
//			@Override
//			public void onCancelled(DatabaseError _databaseError) { }
//		});

	}




	private final int AnimateDuration = 300;

	private ValueAnimator animator; // Global animator

	private void toggleTranslateY(View targetView, boolean isOpen) {
		if (animator != null && animator.isRunning()) {
			animator.cancel();
		}

		float currentY = targetView.getTranslationY();
		float targetDp = isOpen ? -50f : -280f;
		float targetPx = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, targetDp, targetView.getResources().getDisplayMetrics()
		);

		animator = ValueAnimator.ofFloat(currentY, targetPx);
		animator.setDuration(AnimateDuration);
		animator.setInterpolator(new LinearInterpolator());
		animator.addUpdateListener(animation -> {
			float value = (float) animation.getAnimatedValue();
			targetView.setTranslationY(value);
		});
		animator.start();
	}


	private ValueAnimator rotateAnimator; // Global for rotation

	private void rotateDragImageView(ImageView imageView, boolean isOpen) {
		if (rotateAnimator != null && rotateAnimator.isRunning()) {
			rotateAnimator.cancel();
		}

		float from = imageView.getRotation();
		float to = isOpen ? 0f : 180f;

		rotateAnimator = ValueAnimator.ofFloat(from, to);
		rotateAnimator.setDuration(AnimateDuration);
		rotateAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		rotateAnimator.addUpdateListener(animation -> {
			float value = (float) animation.getAnimatedValue();
			imageView.setRotation(value);
		});
		rotateAnimator.start();
	}



	private void updateCartListUI(HashMap<String,Object> cartData) {
		Business.BulkDetailsApiClient bulkDetailsApiClient = new Business.BulkDetailsApiClient();

//		progress_overlay.setVisibility(View.VISIBLE);
//		linearNoData.setVisibility(View.GONE);
//		recyclerview1.setVisibility(View.GONE);
//		costLinear.setVisibility(View.GONE);

		bulkDetailsApiClient.callApi(cartData, new Callbacker.ApiResponseWaiters.BulkDetailsApiCallback() {
			@Override
			public void onReceived(Business.BulkDetailsApiClient.BulkDetailsApiResponse response) {
				if (response.getStatusCode() == 200) {
					listmap.clear();
					for (Product product : response.getProducts()) {
						try {
							CartProduct cartProduct = new CartProduct(product);

							LinkedTreeMap<String,Object> temp = (LinkedTreeMap<String,Object>) cartData.get(cartProduct.getProductId());
							if(temp != null) {
								cartProduct.productCount = (long) Double.parseDouble(String.valueOf(temp.get("count")));
							} else {
								cartProduct.productCount = 0L;
							}
//							HashMap<String,Object> productBaseInfo = (HashMap<String,Object>) cartData.get(cartProduct.getProductId());
//							cartProduct.productCount = (Long) productBaseInfo.get("count");
							listmap.add(cartProduct);
						} catch (Exception e){
							int g=0;
						}
					}
				} else {
					listmap.clear();
				}


				if (listmap.isEmpty()) {
					progressbar1.setVisibility(View.GONE);
					progress_overlay.setVisibility(View.GONE);
					recyclerview1.setVisibility(View.GONE);
					costLinear.setVisibility(View.GONE);
					linearNoData.setVisibility(View.VISIBLE);

					subTotalTextview.setText("₹ 0.00");
					gstTextview.setText("₹ 0.00");
					totalNoDiscountTextView.setText("₹ 0.00");
					discountTotalTextview.setText("- ₹ 0.00");
					grandTotalTextview.setText("₹ 0.00");
					textviewTotal.setText("₹ 0.00");
				} else {
					progressbar1.setVisibility(View.GONE);
					progress_overlay.setVisibility(View.GONE);
					costLinear.setVisibility(View.VISIBLE);
					recyclerview1.setVisibility(View.VISIBLE);
					linearNoData.setVisibility(View.GONE);

					Business.BulkDetailsApiClient.CostDetails costDetails = response.getCostDetails();

					JHelpers.TransitionManager(costLinear, 300);
					subTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalRate())));
					gstTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalGst())));
					totalNoDiscountTextView.setText("₹ ".concat(df.format(costDetails.getTotalRate()+costDetails.getTotalGst())));
					discountTotalTextview.setText("- ₹ ".concat(String.valueOf(costDetails.getTotalDiscount())));
					grandTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
					textviewTotal.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
//					creditTextview.setText(String.valueOf(costDetails.getTotalDiscount()));
				}

				recyclerview1.getAdapter().notifyDataSetChanged();
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



	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<CartProduct> _data;
		
		public Recyclerview1Adapter(ArrayList<CartProduct> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
//			View _v = _inflater.inflate(R.layout.cart, null);
			View _v = _inflater.inflate(R.layout.cart, parent, false);

			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

//			final ViewGroup rootView = _view.findViewById(R.id.rootView);
//			Blurry.with(getContext()).radius(25).sampling(2).onto(rootView);
//			rootView.post(new Runnable() {
//				@Override
//				public void run() {
//					Blurry.with(getContext())
//							.radius(15)
//							.sampling(1)
//							.onto(rootView);
//				}
//			});

			final LinearLayout detailsLinear1 = _view.findViewById(R.id.detailsLinear1);

//			int[] colorsCRNHT = { Color.parseColor("#FFE8FFFF"), Color.parseColor("#FFE8FFFF") };
//			android.graphics.drawable.GradientDrawable CRNHT = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, colorsCRNHT);
//			int radius = 40;
//			CRNHT.setCornerRadii(new float[]{(int)radius,(int)radius,(int)0,(int)0,(int)0,(int)0,(int)radius,(int)radius});
//			CRNHT.setStroke((int) 0, Color.parseColor("#000000"));
//			detailsLinear1.setElevation((float) 0);
//			detailsLinear1.setBackground(CRNHT);
//			detailsLinear1.setTranslationX(-30);


			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final LinearLayout linear26 = _view.findViewById(R.id.linear26);
			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final LinearLayout linear3 = _view.findViewById(R.id.linear3);
			final CardView cardview1 = _view.findViewById(R.id.cardview1);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final LinearLayout linear4 = _view.findViewById(R.id.linear4);
			final LinearLayout linear5 = _view.findViewById(R.id.linear5);
			final LinearLayout linear21 = _view.findViewById(R.id.linear21);
			final TextView name = _view.findViewById(R.id.name);

			final TextView discountShow = _view.findViewById(R.id.discountShow);
			final TextView rateTextView = _view.findViewById(R.id.rateTextView);
			final TextView totalTextView = _view.findViewById(R.id.totalTextView);
			final TextView gstTextView = _view.findViewById(R.id.gstTextView);
			final TextView gstTotalTextView = _view.findViewById(R.id.gstTotalTextView);
			final TextView discountTextView = _view.findViewById(R.id.discountTextView);
			final TextView discountTotalTextView = _view.findViewById(R.id.discountTotalTextView);


//			final TextView textview9 = _view.findViewById(R.id.textview9);
			final LinearLayout linear22 = _view.findViewById(R.id.linear22);
			final ImageView minus = _view.findViewById(R.id.minus);
			final LinearLayout linear30 = _view.findViewById(R.id.linear30);
			final ImageView plus = _view.findViewById(R.id.plus);
			final ImageView imageview3 = _view.findViewById(R.id.imageview3);
			final TextView textview10 = _view.findViewById(R.id.textview10);
			final ImageView imageview4 = _view.findViewById(R.id.imageview4);
//			final LinearLayout linear27 = _view.findViewById(R.id.linear27);
			final LinearLayout linear28 = _view.findViewById(R.id.linear28);
//			final LinearLayout linear20 = _view.findViewById(R.id.linear20);
			final TextView textview11 = _view.findViewById(R.id.textview11);
			final TextView textview8 = _view.findViewById(R.id.textview8);


			CartProduct product = listmap.get((int)_position);




			rateTextView.setText("₹".concat(df.format(product.getCostRate())));
			totalTextView.setText("₹".concat(df.format(product.getCostRate()*product.productCount)));

			double gst = (product.getCostGst()*product.getCostRate())/100;
			gstTextView.setText("+".concat(df.format(product.getCostGst())).concat(" % GST"));//.concat("₹".concat(String.valueOf(gst))).concat(")"));
			gstTotalTextView.setText("+".concat(df.format(gst*product.productCount)).concat(" ₹"));


			double discount = ((product.getCostRate()+gst)*product.getCostDis())/100;
			discountShow.setText(df.format(product.getCostDis()).concat("% OFF"));
//			discountShow.setText("₹".concat(df.format(discount)).concat(" OFF"));

			discountTextView.setText("-".concat(df.format(discount)).concat(" ₹ OFF"));
			discountTotalTextView.setText("-".concat(df.format(discount * product.productCount)).concat(" ₹"));


			name.setText(product.getProductName());
			if (product.getProductImg() != null && !product.getProductImg().isEmpty() && product.getProductImg().get(0) != null && !product.getProductImg().get(0).equals("")) {
				Glide.with(getContext().getApplicationContext()).load(Uri.parse(product.getProductImg().get(0))).into(imageview1);
			}
//			textview9.setText("Variants : ".concat(listmap.get((int)_position).get("size").toString()));

			//------------------------------------design
			_rippleRoundStroke(textview8, "#4b69ff", "#40FFFFFF", 16, 0, "#000000");
			textview8.setElevation((float)2);
			name.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
			discountShow.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/salesbold.ttf"), 0);
			textview8.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			textview11.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			totalTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			rateTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			gstTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			gstTotalTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			discountTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);
			discountTotalTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 0);


			if (product.getCostDis() <= 0) {
				discountShow.setVisibility(View.GONE);
			} else {
				discountShow.setVisibility(View.VISIBLE);
			}

			linear22.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)16, (int)2, 0xFFBDBDBD, 0xFFFFFFFF));
			linear22.setElevation((float)1);
			textview11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)16, (int)2, 0xFFBDBDBD, 0xFFFFFFFF));
			textview11.setElevation((float)1);
			textview11.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
//					cart.child(listmap.get((int)_position).get("key").toString()).removeValue();
//					updateCountToCart(_firebase.getReference("datas/cart/" + userId + "/products/" + String.valueOf(product.getProductId())), 0L);
					updateCountToCart(product.getProductId(), 0L);
					updateCartListUI(Business.localDB_SharedPref.getCart(localDB));
				}
			});

			textview10.setText(String.valueOf(product.productCount));
			plus.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {

					if(product.getStock() < product.productCount+1) {
						Toast.makeText(getContext().getApplicationContext(),"Maximum Purchase Count Reached",Toast.LENGTH_SHORT).show();
						return;
					}

					product.productCount = product.productCount + 1;
//					textview10.setText(String.valueOf(product.productCount));
//					updateCountToCart(_firebase.getReference("datas/cart/" + userId + "/products/" +  String.valueOf(product.getProductId())), product.productCount);
					updateCountToCart(product.getProductId(), product.productCount);
					updateCartListUI(Business.localDB_SharedPref.getCart(localDB));
				}
			});
			minus.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					product.productCount = product.productCount - 1;
//					textview10.setText(String.valueOf(product.productCount));
//					updateCountToCart(_firebase.getReference("datas/cart/" + userId + "/products/" +  String.valueOf(product.getProductId())), product.productCount);
					updateCountToCart(product.getProductId(), product.productCount);
					updateCartListUI(Business.localDB_SharedPref.getCart(localDB));
				}
			});
			textview8.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					Product product = listmap.get((int)_position);

					Intent intent = new Intent();
					intent.putExtra("product",product);
					intent.setClass(getContext().getApplicationContext(), ProductviewActivity.class);
					startActivity(intent);
				}
			});
		}

		private void updateCountToCart(String productID,Long count) {
			if (count == 0) {
//				cart_.removeValue();
				Business.localDB_SharedPref.deleteCartProduct(localDB,productID);
			} else {
				HashMap<String,Object> map = new HashMap<>();
				map.put("count", count);
//				cart_.updateChildren(map);
				Business.localDB_SharedPref.updateCartProduct(localDB,productID,map);
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
