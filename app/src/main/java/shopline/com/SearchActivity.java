package shopline.com;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.*;
import com.google.android.material.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

import shopline.com.JLogics.Business;
import shopline.com.JLogics.Callbacker;
import shopline.com.JLogics.Models.Product;

public class SearchActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private double count1 = 0;
	private double length = 0;
	
	private ArrayList<Product> listmap = new ArrayList<>();
	
	private LinearLayout linear1;
	private LinearLayout linear;
	private LinearLayout linear3;
	private LinearLayout linear4;
	private LinearLayout linear5;
	private ImageView imageview1;
	private EditText edittext1;
	private ImageView imageview2;
	private LinearLayout main;
	private RecyclerView recyclerview1;
	private LinearLayout linear12;
	private LinearLayout linear7;
	private LinearLayout linear17;
	private LinearLayout circle2;
	private LinearLayout linear14;
	private LinearLayout linear15;
	private LinearLayout linear16;
	private LinearLayout circle;
	private LinearLayout linear9;
	private LinearLayout linear10;
	private LinearLayout linear11;
	private TextView textview1;
	
	private Intent i = new Intent();
	private DatabaseReference product = _firebase.getReference("product");
	private ChildEventListener _product_child_listener;

	DecimalFormat df = new DecimalFormat("#.###");

	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.search);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
		linear = findViewById(R.id.linear);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
		linear5 = findViewById(R.id.linear5);
		imageview1 = findViewById(R.id.imageview1);
		edittext1 = findViewById(R.id.edittext1);
		imageview2 = findViewById(R.id.imageview2);
		main = findViewById(R.id.main);
		recyclerview1 = findViewById(R.id.recyclerview1);
		linear12 = findViewById(R.id.linear12);
		linear7 = findViewById(R.id.linear7);
		linear17 = findViewById(R.id.linear17);
		circle2 = findViewById(R.id.circle2);
		linear14 = findViewById(R.id.linear14);
		linear15 = findViewById(R.id.linear15);
		linear16 = findViewById(R.id.linear16);
		circle = findViewById(R.id.circle);
		linear9 = findViewById(R.id.linear9);
		linear10 = findViewById(R.id.linear10);
		linear11 = findViewById(R.id.linear11);
		textview1 = findViewById(R.id.textview1);
		
		linear5.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (edittext1.getText().toString().equals("")) {
					finish();
				}
				else {
					edittext1.setText("");
				}
			}
		});
		
		edittext1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				searchForProductAndList(_charSeq);
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
	}
	
	private void initializeLogic() {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);
		_ICC(imageview1, "#9e9e9e", "#9e9e9e");
		linear4.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFFF6F2F1));
		linear4.setElevation((float)1);
		edittext1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false); 
		recyclerview1.setLayoutManager(gridlayoutManager);

		searchForProductAndList("");
	}
	
	public void _ICC(final ImageView _img, final String _c1, final String _c2) {
		_img.setImageTintList(new android.content.res.ColorStateList(new int[][] {{-android.R.attr.state_pressed},{android.R.attr.state_pressed}},new int[]{Color.parseColor(_c1), Color.parseColor(_c2)}));
	}
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<Product> _data;
		
		public Recyclerview1Adapter(ArrayList<Product> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.product_search, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			final LinearLayout rootLinear = _view.findViewById(R.id.rootLinear);
			final androidx.cardview.widget.CardView cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final ImageView imageview1 = _view.findViewById(R.id.productImageView);
			final TextView textviewRefId = _view.findViewById(R.id.productIdTextView);
			final TextView textviewName = _view.findViewById(R.id.productNameTextView);
			final TextView textviewMRP = _view.findViewById(R.id.mrpTextView);
			final TextView textviewRate = _view.findViewById(R.id.rateTextView);
			final TextView textviewGST = _view.findViewById(R.id.gstTextView);
			final TextView discountShow = _view.findViewById(R.id.discountShow);
//			final Button addToCartButton = _view.findViewById(R.id.addToCartButton);


			if (listmap.get((int)_position).getProductImg() != null && listmap.get((int)_position).getProductImg().size() > 0 && listmap.get((int)_position).getProductImg().get(0) != null && !listmap.get((int)_position).getProductImg().get(0).equals("")) {
				Glide.with(getApplicationContext()).load(Uri.parse(listmap.get((int)_position).getProductImg().get(0))).into(imageview1);
			}


			textviewRefId.setText(listmap.get((int)_position).getProductId());
			textviewName.setText(listmap.get((int)_position).getProductName());
			textviewMRP.setText("₹".concat(df.format(listmap.get((int)_position).getCostMrp())));
			textviewRate.setText("₹".concat(df.format(listmap.get((int)_position).getCostRate())));
			textviewGST.setText("+".concat(df.format(listmap.get((int)_position).getCostGst())).concat("% GST"));
			discountShow.setText(df.format(listmap.get((int)_position).getCostDis()).concat("% OFF"));

			textviewRefId.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			textviewName.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			textviewMRP.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			textviewRate.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			textviewGST.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
			discountShow.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);


			if (listmap.get((int)_position).getCostDis() <= 0) {
				discountShow.setVisibility(View.GONE);
			} else {
				discountShow.setVisibility(View.VISIBLE);
			}


			rootLinear.setOnClickListener(_view2 -> {
                Product product = listmap.get((int)_position);

                Intent intent = new Intent();
                intent.putExtra("product",product);
                intent.setClass(getApplicationContext(), ProductviewActivity.class);
                startActivity(intent);
            });

//			addToCartButton.setOnClickListener(_view1 -> {
//                Product product = listmap.get((int)_position);
//
//                Intent intent = new Intent();
//                intent.putExtra("product",product);
//                intent.setClass(getApplicationContext(), ProductviewActivity.class);
//                startActivity(intent);
//            });
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




	private void searchForProductAndList(String product_name) {
		Business.QueryApiClient queryApiClient = new Business.QueryApiClient();

		// Create filters list
		List<HashMap<String, String>> filters = new ArrayList<>();

		if (getIntent().hasExtra("category") && !getIntent().getStringExtra("category").equals("")) {
			//Main Category
			HashMap<String, String> filter1 = new HashMap<>();
			filter1.put("field", "cat_id");
			filter1.put("operator", "eq");
			filter1.put("value", getIntent().getStringExtra("category"));
			filters.add(filter1);

			//Sub Category
			HashMap<String, String> filter2 = new HashMap<>();
			filter2.put("field", "cat_sub");
			filter2.put("operator", "contains");
			filter2.put("value", "subcat6,");
//					filters.add(filter2);

		}

		if(!product_name.equals("")) {
			//Product Name
			HashMap<String, String> filter3 = new HashMap<>();
			filter3.put("field", "product_name");
			filter3.put("operator", "contains");
			filter3.put("value", product_name);
			filters.add(filter3);
		}

		// Create main HashMap
		HashMap<String, Object> apiRequestData = new HashMap<>();
		apiRequestData.put("filters", filters);
		apiRequestData.put("limit", 1000);
		apiRequestData.put("offset", 0);
		apiRequestData.put("order_by", "product_name");
		apiRequestData.put("order_direction", "ASC");

		queryApiClient.callApi(apiRequestData, new Callbacker.ApiResponseWaiters.QueryApiCallback(){
			@Override
			public void onReceived(Business.QueryApiClient.QueryApiResponse response) {
				if (response.getStatusCode() == 200) {
					listmap = response.getProducts();
				} else {
					listmap = new ArrayList<>();
				}

				if (listmap.isEmpty()) {
					main.setVisibility(View.VISIBLE);
				} else {
					main.setVisibility(View.GONE);
				}

//				System.out.println(listmap);

				recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));
				GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
				recyclerview1.setLayoutManager(gridlayoutManager);
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
