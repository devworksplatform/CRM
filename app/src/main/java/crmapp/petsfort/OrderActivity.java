package crmapp.petsfort;

import android.content.Intent;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.JHelpers;

public class OrderActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
	
	private ArrayList<Business.OrderQueryApiClient.Order> listmap = new ArrayList<>();
	
	private LinearLayout linear2;
	private LinearLayout linear1;
	private LinearLayout linear5;
	private LinearLayout linear4;
	private LinearLayout linear10;
	private LinearLayout linear11, rootLinear;
	private LinearLayout circle;
	private LinearLayout linear15;
	private LinearLayout linear16;
	private LinearLayout circle2;
	private LinearLayout noDataLinear;

	private ImageView imageview1;
	private TextView textview1,textviewTemp;
	private RecyclerView recyclerview1;
	private ProgressBar progressbar1;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.order);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		rootLinear = findViewById(R.id.rootLinear);
		linear2 = findViewById(R.id.linear2);
		linear1 = findViewById(R.id.linear1);
		linear5 = findViewById(R.id.linear5);
		linear4 = findViewById(R.id.linear4);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		textviewTemp = findViewById(R.id.textviewTemp);
		recyclerview1 = findViewById(R.id.recyclerview1);
		progressbar1 = findViewById(R.id.progressbar1);

		linear10 = findViewById(R.id.linear10);
		linear11 = findViewById(R.id.linear11);
		circle = findViewById(R.id.circle);
		linear15 = findViewById(R.id.linear15);
		linear16 = findViewById(R.id.linear16);
		circle2 = findViewById(R.id.circle2);

		noDataLinear = findViewById(R.id.noDataLinear);
		noDataLinear.setVisibility(View.GONE);



		linear5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				finish();
			}
		});


		// Assuming you have instantiated your OrderQueryApiClient somewhere in your activity or fragment
		Business.OrderQueryApiClient orderApiClient = new Business.OrderQueryApiClient();

		// Prepare the query data as a HashMap
		HashMap<String, Object> queryData = new HashMap<>();

		// Create the filters list
		List<Map<String, String>> filters = new ArrayList<>();
		Map<String, String> filter = new HashMap<>();
		filter.put("field", "user_id");
		filter.put("operator", "eq");
		filter.put("value", userId);
		filters.add(filter);
		queryData.put("filters", filters);

		// Add other parameters
		queryData.put("order_by", "created_at");
		queryData.put("order_direction", "DESC");
		queryData.put("limit", 1000);
		queryData.put("offset", 0);

		linear1.setVisibility(View.VISIBLE);
		progressbar1.setVisibility(View.VISIBLE);
		recyclerview1.setVisibility(View.GONE);
		noDataLinear.setVisibility(View.GONE);

		orderApiClient.callApi(queryData, new Business.OrderQueryApiClient.OrderApiCallback() {
			@Override
			public void onReceived(Business.OrderQueryApiClient.OrderQueryApiResponse response) {
				if(response.getStatusCode() == 200) {
					listmap = response.getOrders();
				}
				GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 1, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
				recyclerview1.setLayoutManager(gridlayoutManager);
				recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));
				recyclerview1.getAdapter().notifyDataSetChanged();

				JHelpers.TransitionManager(rootLinear, 600);
				if(listmap.isEmpty()) {
					linear1.setVisibility(View.GONE);
					progressbar1.setVisibility(View.GONE);
					recyclerview1.setVisibility(View.GONE);
					noDataLinear.setVisibility(View.VISIBLE);
				} else {
					linear1.setVisibility(View.VISIBLE);
					progressbar1.setVisibility(View.GONE);
					recyclerview1.setVisibility(View.VISIBLE);
					noDataLinear.setVisibility(View.GONE);
				}
			}
		});
	}
	
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);

		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

		textviewTemp.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));

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
	
	
	public void _NavStatusBarColor(final String _color1, final String _color2) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			Window w = this.getWindow();	w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);	w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			w.setStatusBarColor(Color.parseColor("#" + _color1.replace("#", "")));	w.setNavigationBarColor(Color.parseColor("#" + _color2.replace("#", "")));
		}
	}
	
	
	public void _RoundAndBorder(final View _view, final String _color1, final double _border, final String _color2, final double _round) {
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
		gd.setColor(Color.parseColor(_color1));
		gd.setCornerRadius((int) _round);
		gd.setStroke((int) _border, Color.parseColor(_color2));
		_view.setBackground(gd);
	}
	
	
	public void _addCardView(final View _layoutView, final double _margins, final double _cornerRadius, final double _cardElevation, final double _cardMaxElevation, final boolean _preventCornerOverlap, final String _backgroundColor) {
		androidx.cardview.widget.CardView cv = new androidx.cardview.widget.CardView(this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		int m = (int)_margins;
		lp.setMargins(m,m,m,m);
		cv.setLayoutParams(lp);
		int c = Color.parseColor(_backgroundColor);
		cv.setCardBackgroundColor(c);
		cv.setRadius((float)_cornerRadius);
		cv.setCardElevation((float)_cardElevation);
		cv.setMaxCardElevation((float)_cardMaxElevation);
		cv.setPreventCornerOverlap(_preventCornerOverlap);
		if(_layoutView.getParent() instanceof LinearLayout){
			ViewGroup vg = ((ViewGroup)_layoutView.getParent());
			vg.removeView(_layoutView);
			vg.removeAllViews();
			vg.addView(cv);
			cv.addView(_layoutView);
		}else{
			
		}
	}
	
	
	public void _ICC(final ImageView _img, final String _c1, final String _c2) {
		_img.setImageTintList(new android.content.res.ColorStateList(new int[][] {{-android.R.attr.state_pressed},{android.R.attr.state_pressed}},new int[]{Color.parseColor(_c1), Color.parseColor(_c2)}));
	}
	
	
	public void _reverse(final ArrayList<Business.OrderQueryApiClient.Order> _mapname) {
		Collections.reverse(_mapname);
	}
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<Business.OrderQueryApiClient.Order> _data;
		
		public Recyclerview1Adapter(ArrayList<Business.OrderQueryApiClient.Order> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.orderview, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final CardView card1 = _view.findViewById(R.id.card1);
			final LinearLayout linear3 = _view.findViewById(R.id.linear3);
			final TextView OrderId = _view.findViewById(R.id.OrderId);
			final TextView OrderStatus = _view.findViewById(R.id.OrderStatus);
			final TextView OrderDetail = _view.findViewById(R.id.OrderDetail);

			Business.OrderQueryApiClient.Order order = _data.get(_position);

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
			String formattedDate = java.time.LocalDateTime.parse(dateString).format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));

			OrderDetail.setText("Date : ".concat(formattedDate)
					.concat("\nItems: ".concat(String.valueOf(order.getItemsDetail().size())))
					.concat(", Cost: Rs.").concat(String.valueOf(order.getTotal())).concat(" ₹"));

			OrderId.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
			OrderStatus.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
			OrderDetail.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);

			linear3.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Business.OrderQueryApiClient.Order order = listmap.get((int)_position);

					Intent intent = new Intent();
					intent.putExtra("order",order);
					intent.setClass(getApplicationContext(), OrderCartViewActivity.class);
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
