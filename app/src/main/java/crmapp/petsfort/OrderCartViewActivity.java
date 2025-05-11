package crmapp.petsfort;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.CartProduct;
import crmapp.petsfort.JLogics.Models.Product;

public class OrderCartViewActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

	private ArrayList<CartProduct> listmap = new ArrayList<>();
	
	private LinearLayout linear1;
	private RecyclerView recyclerview1;
	private ProgressBar progressbar1;
	

//	private DatabaseReference cart;
	private ChildEventListener _cart_child_listener;
	private Intent i = new Intent();

	private TextView subTotalTextviewLabel,gstTextviewLabel,grandTotalTextviewLabel,creditTextviewLabel,discountTotalTextviewLabel,totalNoDiscountLabelTextView,textview1,productSecondaryNameTextView;
	private TextView subTotalTextview,gstTextview,grandTotalTextview,creditTextview,discountTotalTextview,totalNoDiscountTextView,textviewTotal,textviewTotalLabel,textviewConfirmLabel;

	private RelativeLayout linear2;
	private LinearLayout costLinear,progress_overlay,linearDrag,confirmOrderLinear,linearNoData,statusLinear;
	private LinearLayout linear10,linear11,linear15,linear16,circle,circle2,linear5;
	private ImageView bottomDragImage;
	private CardView viewBill;
	DecimalFormat df = new DecimalFormat("#.###");

	private boolean isOpen = false;

	Business.OrderQueryApiClient.Order order;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.order_cart_view_activity);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
	}
	
	private void initialize(Bundle _savedInstanceState) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);

		linear1 = findViewById(R.id.linear1);
		linear2 = findViewById(R.id.linear2);
		linear5 = findViewById(R.id.linear5);
		costLinear = findViewById(R.id.costLinear);
		linearNoData = findViewById(R.id.linearNoData);
		linearNoData.setVisibility(View.GONE);

		linearDrag = findViewById(R.id.linearDrag);
		confirmOrderLinear = findViewById(R.id.confirmOrderLinear);

		circle = findViewById(R.id.circle);
		circle2 = findViewById(R.id.circle2);
		linear10 = findViewById(R.id.linear10);
		linear11 = findViewById(R.id.linear11);
		linear15 = findViewById(R.id.linear15);
		linear16 = findViewById(R.id.linear16);

		progress_overlay = findViewById(R.id.progress_overlay);
		recyclerview1 = findViewById(R.id.recyclerview1);
		progressbar1 = findViewById(R.id.progressbar1);
		bottomDragImage = findViewById(R.id.bottomDragImage);
		viewBill = findViewById(R.id.viewBill);

		productSecondaryNameTextView = findViewById(R.id.productSecondaryNameTextView);
		productSecondaryNameTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		subTotalTextview = findViewById(R.id.subTotalTextView);
		subTotalTextview.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		subTotalTextviewLabel = findViewById(R.id.subtotalLabelTextView);
		subTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		gstTextview = findViewById(R.id.gstTotalTextView);
		gstTextview.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		gstTextviewLabel = findViewById(R.id.gstTotalLabelTextView);
		gstTextviewLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		grandTotalTextview = findViewById(R.id.grandTotalTextView);
		grandTotalTextview.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		grandTotalTextviewLabel = findViewById(R.id.grandTotalLabelTextView);
		grandTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		creditTextview = findViewById(R.id.creditTextView);
		creditTextview.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		creditTextviewLabel = findViewById(R.id.creditLabelTextView);
		creditTextviewLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		discountTotalTextview = findViewById(R.id.discountTotalTextView);
		discountTotalTextview.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		discountTotalTextviewLabel = findViewById(R.id.discountTotalLabelTextView);
		discountTotalTextviewLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		totalNoDiscountTextView = findViewById(R.id.totalNoDiscountTextView);
		totalNoDiscountTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		totalNoDiscountLabelTextView = findViewById(R.id.totalNoDiscountLabelTextView);
		totalNoDiscountLabelTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		textviewTotal = findViewById(R.id.textviewTotal);
		textviewTotal.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		textviewTotalLabel = findViewById(R.id.textviewTotalLabel);
		textviewTotalLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		textviewConfirmLabel = findViewById(R.id.textviewConfirmLabel);
		textviewConfirmLabel.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		textview1 = findViewById(R.id.textview1);
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);

		textview1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));


		order = (Business.OrderQueryApiClient.Order) getIntent().getSerializableExtra("order");

		String userRole = getSharedPreferences("logindata", Activity.MODE_PRIVATE).getString("role", "0");
		if (userRole.equals("2") || userRole.equals("4")) {
			viewBill.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String url = "https://pets-fort.web.app/bill.html?orderid="+order.getOrderId(); // Replace with the desired URL
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
				}
			});
		} else {
			viewBill.setVisibility(View.GONE);
		}

		GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 1, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
		recyclerview1.setLayoutManager(gridlayoutManager);
		listmap = new ArrayList<>();
		recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));

		final TextView OrderId = findViewById(R.id.OrderId);
		final TextView OrderStatus = findViewById(R.id.OrderStatus);
		final TextView OrderDetail = findViewById(R.id.OrderDetail);

		OrderId.setText(order.getOrderId());
		try{
			Business.JOrderStatus status = Business.JOrderStatus.valueOf(order.getOrderStatus());
			OrderStatus.setText(status.getVisibleText());
			OrderStatus.setBackgroundResource(status.getDrawableRes());
		} catch (Exception e) {
			OrderStatus.setText("Order Status Error");
			OrderStatus.setBackgroundColor(Color.parseColor("#ffffff"));
		}

		String dateString = order.getCreatedAt();
		OrderDetail.setText("Date : ".concat(dateString)
				.concat("\nItems: ".concat(String.valueOf(order.getItemsDetail().size())))
				.concat(", Cost: Rs.").concat(String.valueOf(order.getTotal())).concat(" ₹"));

		OrderId.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		OrderStatus.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
		OrderDetail.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);


		creditTextview.setText("XXX");
		updateCartListUI(order);

		linearDrag.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleTranslateY(linear2, isOpen);
				rotateDragImageView(bottomDragImage, isOpen);
				isOpen = !isOpen;
			}
		});

		linear5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		confirmOrderLinear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		confirmOrderLinear.setVisibility(View.GONE);
		creditTextview.setVisibility(View.GONE);
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



	private void updateCartListUI(Business.OrderQueryApiClient.Order order) {
		listmap.clear();
		for (Product product : order.getItemsDetail()) {
			try {
				CartProduct cartProduct = new CartProduct(product);

				Map<String,Object> temp = order.getItems().get(cartProduct.getProductId());
				if(temp != null) {
					cartProduct.productCount = (long) Double.parseDouble(String.valueOf(temp.get("count")));
				} else {
					cartProduct.productCount = 0L;
				}
				listmap.add(cartProduct);
			} catch (Exception e){
				int g=0;
			}
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

			Business.BulkDetailsApiClient.CostDetails costDetails = order.getCostDetails();

			JHelpers.TransitionManager(costLinear, 300);
			subTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalRate())));
			gstTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalGst())));
			totalNoDiscountTextView.setText("₹ ".concat(df.format(costDetails.getTotalMrp())));
			discountTotalTextview.setText("- ₹ ".concat(String.valueOf(costDetails.getTotalDiscount())));
			grandTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
			textviewTotal.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
		}

		recyclerview1.getAdapter().notifyDataSetChanged();

	}

	
	public void _rippleRoundStroke(final View _view, final String _focus, final String _pressed, final double _round, final double _stroke, final String _strokeclr) {
		GradientDrawable GG = new GradientDrawable();
		GG.setColor(Color.parseColor(_focus));
		GG.setCornerRadius((float)_round);
		GG.setStroke((int) _stroke,
		Color.parseColor("#" + _strokeclr.replace("#", "")));
		RippleDrawable RE = new RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ Color.parseColor(_pressed)}), GG, null);
		_view.setBackground(RE);
	}



	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<CartProduct> _data;
		
		public Recyclerview1Adapter(ArrayList<CartProduct> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = OrderCartViewActivity.this.getLayoutInflater();
//			View _v = _inflater.inflate(R.layout.cart, null);
			View _v = _inflater.inflate(R.layout.cart, parent, false);

			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final TextView name = _view.findViewById(R.id.name);

			final View middleView = _view.findViewById(R.id.middleView);
			final TextView discountShow = _view.findViewById(R.id.discountShow);
			final TextView rateTextView = _view.findViewById(R.id.rateTextView);
			final TextView totalTextView = _view.findViewById(R.id.totalTextView);
			final TextView gstTextView = _view.findViewById(R.id.gstTextView);
			final TextView gstTotalTextView = _view.findViewById(R.id.gstTotalTextView);
			final TextView discountTextView = _view.findViewById(R.id.discountTextView);
			final TextView discountTotalTextView = _view.findViewById(R.id.discountTotalTextView);


			final LinearLayout linear22 = _view.findViewById(R.id.linear22);
			final ImageView minus = _view.findViewById(R.id.minus);
			final ImageView plus = _view.findViewById(R.id.plus);
			final TextView textview10 = _view.findViewById(R.id.textview10);
			final TextView textview11 = _view.findViewById(R.id.textview11);
			final TextView textview8 = _view.findViewById(R.id.textview8);

			CartProduct product = listmap.get((int)_position);

			rateTextView.setText("₹".concat(df.format(product.getCostRate())));
			totalTextView.setText("₹".concat(df.format(product.getCostRate()*product.productCount)));

			double gst = (product.getCostGst()*product.getCostRate())/100;
			gstTextView.setText("+".concat(df.format(product.getCostGst())).concat(" % GST"));//.concat("₹".concat(String.valueOf(gst))).concat(")"));
			gstTotalTextView.setText("+".concat(df.format(gst*product.productCount)).concat(" ₹"));


			double discount = ((product.getCostMrp())*product.getCostDis())/100;
			discountShow.setText(df.format(product.getCostDis()).concat("% OFF"));

			discountTextView.setText("-".concat(df.format(discount)).concat(" ₹ OFF"));
			discountTotalTextView.setText("-".concat(df.format(discount * product.productCount)).concat(" ₹"));


			name.setText(JHelpers.capitalize(JHelpers.capitalize(product.getProductName())));
			if (product.getProductImg() != null && !product.getProductImg().isEmpty() && product.getProductImg().get(0) != null && !product.getProductImg().get(0).equals("")) {
				Glide.with(getApplicationContext()).load(Uri.parse(product.getProductImg().get(0))).into(imageview1);
			}

			//------------------------------------design
			_rippleRoundStroke(textview8, "#4b69ff", "#40FFFFFF", 16, 0, "#000000");
			textview8.setElevation((float)2);
			name.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
			discountShow.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
			textview8.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			textview11.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			totalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			rateTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			gstTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			gstTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			discountTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			discountTotalTextView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);


			if (product.getCostDis() <= 0) {
				discountShow.setVisibility(View.GONE);
			} else {
				discountShow.setVisibility(View.VISIBLE);
			}

			linear22.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)16, (int)2, 0xFFBDBDBD, 0xFFFFFFFF));
			linear22.setElevation((float)1);
//			textview11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)16, (int)2, 0xFFBDBDBD, 0xFFFFFFFF));
//			textview11.setElevation((float)1);
			textview11.setVisibility(View.GONE);
			middleView.setVisibility(View.GONE);

			textview10.setText(String.valueOf(product.productCount));
			plus.setEnabled(false);
			minus.setEnabled(false);

			textview8.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View _view) {
					CartProduct cart_product = listmap.get((int)_position);
					Product product = listmap.get((int)_position);

					Intent intent = new Intent();
					intent.putExtra("isViewOnly",true);
					intent.putExtra("product",product);
					intent.putExtra("productCount", cart_product.productCount);
					intent.setClass(getApplicationContext(), ProductviewActivity.class);
					startActivity(intent);
				}
			});
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
