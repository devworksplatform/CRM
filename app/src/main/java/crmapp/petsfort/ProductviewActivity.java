package crmapp.petsfort;

import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.*;
import java.util.*;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.Category;
import crmapp.petsfort.JLogics.Models.Product;

public class ProductviewActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
	Product product;
	boolean isViewOnly;
	Integer currentCount = 0;

	private EditText edittext1;
	private LinearLayout linear22,rootLinear,mrpRateLinear,gstDiscountLinear, linear5, countLinear;
	private TextView productSecondaryNameTextView,productNameTextView,mrpTextView,rateTextView,gstTextView,gstRsTextView,productDescriptionTextView,hurryUpTextView,productIdTextView,discountTextView,discountRsTextView,productDescriptionTextviewLabel;
	private TextView mrpLabel,rateLabel,discountLabel,gstLabel;
	private Button addButton;
	private ImageView productImageView,plus,minus;
	private CardView cardview1;
	DecimalFormat df = new DecimalFormat("#.###");
	SharedPreferences localDB;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.productview);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);

		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);

		localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);

		linear22 = (LinearLayout) findViewById(R.id.linear22);
		productImageView = (ImageView) findViewById(R.id.productImageView);
		plus = (ImageView) findViewById(R.id.plus);
		minus = (ImageView) findViewById(R.id.minus);
		edittext1 = (EditText) findViewById(R.id.edittext1);
		productSecondaryNameTextView = (TextView) findViewById(R.id.productSecondaryNameTextView);
		productNameTextView = (TextView) findViewById(R.id.productNameTextView);
		mrpTextView = (TextView) findViewById(R.id.mrpTextView);
		rateTextView = (TextView) findViewById(R.id.rateTextView);
		gstTextView = (TextView) findViewById(R.id.gstTextView);
		gstRsTextView = (TextView) findViewById(R.id.gstRsTextView);
		productDescriptionTextView = (TextView) findViewById(R.id.productDescriptionTextView);
		hurryUpTextView = (TextView) findViewById(R.id.hurryUpTextView);
		productIdTextView = (TextView) findViewById(R.id.productIdTextView);
		discountTextView = (TextView) findViewById(R.id.discountTextView);
		discountRsTextView = (TextView) findViewById(R.id.discountRsTextView);
		productDescriptionTextviewLabel = (TextView) findViewById(R.id.productDescriptionTextviewLabel);
		mrpLabel = (TextView) findViewById(R.id.mrpLabel);
		rateLabel = (TextView) findViewById(R.id.rateLabel);
		gstLabel = (TextView) findViewById(R.id.gstLabel);
		discountLabel = (TextView) findViewById(R.id.discountLabel);

		rootLinear = (LinearLayout) findViewById(R.id.rootLinear);
		mrpRateLinear = (LinearLayout) findViewById(R.id.mrpRateLinear);
		gstDiscountLinear = (LinearLayout) findViewById(R.id.gstDiscountLinear);
		linear5 = (LinearLayout) findViewById(R.id.linear5);
		countLinear = (LinearLayout) findViewById(R.id.countLinear);
		cardview1 = (CardView) findViewById(R.id.cartview1);


		final Typeface normalTypeface = Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf");
		final Typeface boldTypeface = Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf");

		productSecondaryNameTextView.setTypeface(boldTypeface, 0);
		productNameTextView.setTypeface(boldTypeface, 0);
		mrpTextView.setTypeface(boldTypeface, 0);
		rateTextView.setTypeface(boldTypeface, 0);
		gstTextView.setTypeface(boldTypeface, 0);
		discountTextView.setTypeface(boldTypeface, 0);
		productDescriptionTextviewLabel.setTypeface(boldTypeface, 0);
		productDescriptionTextView.setTypeface(normalTypeface, 0);
		productIdTextView.setTypeface(normalTypeface, 0);
		hurryUpTextView.setTypeface(normalTypeface, 0);
		mrpLabel.setTypeface(normalTypeface, 0);
		rateLabel.setTypeface(normalTypeface, 0);
		gstLabel.setTypeface(normalTypeface, 0);
		discountLabel.setTypeface(normalTypeface, 0);
		edittext1.setTypeface(boldTypeface, 0);

	}
	
	private void initializeLogic() {

		product = (Product) getIntent().getSerializableExtra("product");
		isViewOnly = getIntent().getBooleanExtra("isViewOnly",false);

		productNameTextView.setText(product.getProductName());
		productIdTextView.setText("HSN-("+String.valueOf(product.getProductHsn()).concat(") Code-(").concat(product.getProductCid()).concat(")"));

		mrpTextView.setText("₹".concat(df.format(product.getCostMrp())));
		rateTextView.setText("₹".concat(df.format(product.getCostRate())));

		double gst = (product.getCostGst()*product.getCostRate())/100;
		gstTextView.setText("+".concat(df.format(product.getCostGst())).concat("% GST"));
		gstRsTextView.setText(df.format(gst).concat(" ₹"));

		double discount = ((product.getCostMrp())*product.getCostDis())/100;
		discountTextView.setText("-".concat(df.format(product.getCostDis()).concat("%")));
		discountRsTextView.setText(df.format(discount).concat(" ₹"));

		productDescriptionTextView.setText(product.getProductDesc());
		productSecondaryNameTextView.setText("");
		productSecondaryNameTextView.setVisibility(View.GONE);
//		productSecondaryNameTextView.setText(String.format("%s > %s", product.getCatId(), product.getProductName()));

		if(isViewOnly) {
			currentCount = (int) getIntent().getLongExtra("productCount",0);
		} else {
			HashMap<String,Object> map = Business.localDB_SharedPref.getCartProduct(localDB,product.getProductId());
			currentCount = 0;
			if (map.containsKey("count")) {
				Object value = map.get("count");

				if (value instanceof Number) {
					currentCount = ((Number) value).intValue();
				} else if (value instanceof String) {
					try {
						currentCount = Integer.parseInt((String) value);
					} catch (NumberFormatException e) {
						currentCount = 0;
					}
				} else {
					currentCount = 0;
				}
			} else {
				currentCount = 0;
			}

			if(currentCount <= 0) {
				currentCount = 0;
			}
		}

		edittext1.setText(String.valueOf(currentCount));

		if(isViewOnly) {
			hurryUpTextView.setText("Thanks for purchasing this product from us!");
			hurryUpTextView.setTextColor(Color.parseColor("#4CAF50"));
			plus.setEnabled(false);
			minus.setEnabled(false);
			edittext1.setEnabled(false);
			cardview1.setVisibility(View.GONE);
		} else {
			if (product.getStock() < 10) {
				hurryUpTextView.setText("Hurry up ! Last few pieces left !");
				hurryUpTextView.setTextColor(Color.parseColor("#F44336"));
			} else {
				hurryUpTextView.setText("In Stock");
				hurryUpTextView.setTextColor(Color.parseColor("#4CAF50"));
			}
		}

		if (product.getProductImg() != null && !product.getProductImg().isEmpty() && product.getProductImg().get(0) != null && !product.getProductImg().get(0).equals("")) {
			Glide.with(getApplicationContext()).load(Uri.parse(product.getProductImg().get(0))).into(productImageView);
		}

		plus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(),"Product Added to the cart",Toast.LENGTH_SHORT).show();
				edittext1.setText(String.valueOf(currentCount+1));
			}
		});

		minus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(currentCount <= 0) {

				} else {
					edittext1.setText(String.valueOf(currentCount-1));
				}
			}
		});


		edittext1.requestFocus();
		edittext1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();

				int temp = currentCount;
				if (_charSeq.equals("")) {
					currentCount = 0;
				} else {
					try {
						currentCount = Integer.parseInt(_charSeq);
					} catch (Exception e) {
						currentCount = 0;
					}
				}

				if(temp == currentCount) {
					return;
				}

				if(currentCount <= 0) {
					if(!_charSeq.equals("")) {
						edittext1.setText("");
					}
				}

				updateCountToCart();
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});




		linear5.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		gstDiscountLinear.setVisibility(View.GONE);
		mrpRateLinear.setVisibility(View.GONE);
		countLinear.setVisibility(View.GONE);
		productDescriptionTextView.setVisibility(View.GONE);


		Activity activity = this;

		JHelpers.runAfterDelay(activity,300, new Callbacker.Timer(){
			@Override
			public void onEnd() {
				JHelpers.TransitionManager(rootLinear, 200);
				mrpRateLinear.setVisibility(View.VISIBLE);

				JHelpers.runAfterDelay(activity,300, new Callbacker.Timer(){
					@Override
					public void onEnd() {
						JHelpers.TransitionManager(rootLinear, 200);
						gstDiscountLinear.setVisibility(View.VISIBLE);
						productDescriptionTextView.setVisibility(View.VISIBLE);

						JHelpers.runAfterDelay(activity,300, new Callbacker.Timer(){
							@Override
							public void onEnd() {
								JHelpers.TransitionManager(rootLinear, 200);
								countLinear.setVisibility(View.VISIBLE);
							}
						});
					}
				});

			}
		});


		Business.CategoriesApiClient.getCategoriesCallApi(new Callbacker.ApiResponseWaiters.CategoriesApiCallback(){
			@Override
			public void onReceived(Business.CategoriesApiClient.CategoriesApiResponse response) {
				super.onReceived(response);

				if(response.getStatusCode() == 200) {
					try {
						String catId = product.getCatId();
						Category cat = null;
						for (Category _data : response.getCategories()) {
							if(catId.equals(_data.getId())){
								cat = _data;
								break;
							}
						}
						String name;
						if (cat != null){
							name = cat.getName();
						} else {
							name = product.getProductId();
						}

						productSecondaryNameTextView.setText(String.format("%s > %s", name, product.getProductName()));
						JHelpers.TransitionManager(rootLinear,200);
						productSecondaryNameTextView.setVisibility(View.VISIBLE);

					}
					catch (Exception _e) {
						_e.printStackTrace();
					}
				} else {
					productSecondaryNameTextView.setText(String.format("%s", product.getProductName()));
					JHelpers.TransitionManager(rootLinear,200);
					productSecondaryNameTextView.setVisibility(View.VISIBLE);
				}
			}
		});


		cardview1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setClass(getApplicationContext(), FragmentWrapper.class);
				i.putExtra("fragment","cart");
				startActivity(i);
			}
		});

//		_firebase.getReference("datas/category/".concat(product.getCatId())).addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(DataSnapshot _dataSnapshot) {
//				if (_dataSnapshot.exists()) {
//					String name = _dataSnapshot.child("name").getValue(String.class);
//
//					if(name == null || name.isEmpty()) {
//						name = product.getProductId();
//					}
//
//					productSecondaryNameTextView.setText(String.format("%s > %s", name, product.getProductName()));
//					JHelpers.TransitionManager(rootLinear,200);
//					productSecondaryNameTextView.setVisibility(View.VISIBLE);
//				} else {
//					productSecondaryNameTextView.setText(String.format("%s", product.getProductName()));
//					JHelpers.TransitionManager(rootLinear,200);
//					productSecondaryNameTextView.setVisibility(View.VISIBLE);
//				}
//
//			}
//			@Override
//			public void onCancelled(DatabaseError _databaseError) {
//			}
//		});
	}


	private void updateCountToCart() {
		if(product.getStock() < currentCount) {
			currentCount = product.getStock();
			Toast.makeText(getApplicationContext(),"Maximum Purchase Count Reached",Toast.LENGTH_SHORT).show();
			edittext1.setText(String.valueOf(currentCount));
		}

		if (currentCount <= 0) {
			Business.localDB_SharedPref.deleteCartProduct(localDB,product.getProductId());
		} else {
			HashMap<String,Object> map = new HashMap<>();
			map.put("count", currentCount);
			Business.localDB_SharedPref.updateCartProduct(localDB,product.getProductId(),map);
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
	
	
	public void _rippleRoundStroke(final View _view, final String _focus, final String _pressed, final double _round, final double _stroke, final String _strokeclr) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(Color.parseColor(_focus));
		GG.setCornerRadius((float)_round);
		GG.setStroke((int) _stroke,
		Color.parseColor("#" + _strokeclr.replace("#", "")));
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ Color.parseColor(_pressed)}), GG, null);
		_view.setBackground(RE);
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
