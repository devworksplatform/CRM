package shopline.com;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.Glide;
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
import java.util.*;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

import shopline.com.JLogics.Business;
import shopline.com.JLogics.Models.Product;

public class ProductviewActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//	DatabaseReference cart;
	Product product;
	Integer currentCount = 0;

	private EditText edittext1;
	private TextView productSecondaryNameTextView,productNameTextView,mrpTextView,rateTextView,gstTextView,gstRsTextView,productDescriptionTextView,hurryUpTextView,productIdTextView,discountTextView,discountRsTextView;
	private Button addButton;
	private ImageView productImageView;
	DecimalFormat df = new DecimalFormat("#.###");
	SharedPreferences localDB;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.productview);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(0xFFFFFFFF);

		localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);

		addButton = (Button) findViewById(R.id.addButton);
		productImageView = (ImageView) findViewById(R.id.productImageView);
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
	}
	
	private void initializeLogic() {

		product = (Product) getIntent().getSerializableExtra("product");

		productNameTextView.setText(product.getProductName());
		productIdTextView.setText(String.valueOf(product.getProductId()));

		mrpTextView.setText("₹".concat(df.format(product.getCostMrp())));
		rateTextView.setText("₹".concat(df.format(product.getCostRate())));

		double gst = (product.getCostGst()*product.getCostRate())/100;
		gstTextView.setText("+".concat(df.format(product.getCostGst())).concat("% GST"));
		gstRsTextView.setText(df.format(gst).concat(" ₹"));

		double discount = ((product.getCostRate()+gst)*product.getCostDis())/100;
		discountTextView.setText("-".concat(df.format(product.getCostDis()).concat("%")));
		discountRsTextView.setText(df.format(discount).concat(" ₹"));

		productDescriptionTextView.setText(product.getProductDesc());
		productSecondaryNameTextView.setText(String.format("%s > %s", product.getCatId(), product.getProductName()));

//		cart = _firebase.getReference("datas/cart/" + userId + "/products/" + String.valueOf(product.getProductId()));
//		_Custom_Loading(true);

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
			edittext1.setText("");
		} else {
			edittext1.setText(String.valueOf(currentCount));
		}



//		cart.addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//				_Custom_Loading(false);
//				currentCount = 0;
//				if (dataSnapshot.exists()) {
//					// Product already exists in the cart
//					if (dataSnapshot.hasChild("count")) {
//						Object value = dataSnapshot.child("count").getValue();
//
//						if (value instanceof Number) {
//							currentCount = ((Number) value).intValue();
//						} else if (value instanceof String) {
//							try {
//								currentCount = Integer.parseInt((String) value);
//							} catch (NumberFormatException e) {
//								currentCount = 0;
//							}
//						} else {
//							currentCount = 0;
//						}
//					} else {
//						currentCount = 0;
//					}
//
//				}
//
//				if(currentCount <= 0) {
//					edittext1.setText("");
//				} else {
//					edittext1.setText(String.valueOf(currentCount));
//				}
//
//			}
//
//			@Override
//			public void onCancelled(@NonNull DatabaseError databaseError) {
//				// Handle errors here
//				System.err.println("Error checking cart item: " + databaseError.getMessage());
//			}
//		});


		if (product.getStock() < 10) {
			hurryUpTextView.setText("Hurry up ! Last few pieces left !");
			hurryUpTextView.setTextColor(Color.parseColor("#F44336"));
		} else {
			hurryUpTextView.setText("In Stock");
			hurryUpTextView.setTextColor(Color.parseColor("#4CAF50"));
		}

		if (product.getProductImg() != null && !product.getProductImg().isEmpty() && product.getProductImg().get(0) != null && !product.getProductImg().get(0).equals("")) {
			Glide.with(getApplicationContext()).load(Uri.parse(product.getProductImg().get(0))).into(productImageView);
		}

		addButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(),"Product Added to the cart",Toast.LENGTH_SHORT).show();

				currentCount += 1;
				edittext1.setText(String.valueOf(currentCount));

//				updateCountToCart(currentCount);

			}
		});


		edittext1.requestFocus();
		edittext1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();

				if (_charSeq.equals("")) {
					currentCount = 0;
				} else {
					try {
						currentCount = Integer.parseInt(_charSeq);
					} catch (Exception e) {
						currentCount = 0;
					}
				}

				if(currentCount <= 0) {
					if(!_charSeq.equals("")) {
						edittext1.setText("");
					}
				}

				updateCountToCart(currentCount);
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});
	}


	private void updateCountToCart(Integer count) {
		if (count == 0) {
//			cart.removeValue();
			Business.localDB_SharedPref.deleteCartProduct(localDB,product.getProductId());
		} else {
			HashMap<String,Object> map = new HashMap<>();
			map.put("count", count);
//			_Custom_Loading(true);
//			cart.updateChildren(map).addOnCompleteListener(task -> _Custom_Loading(false));
//			cart.updateChildren(map);
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
