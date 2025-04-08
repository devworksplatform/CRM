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
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
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
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;

import shopline.com.JLogics.Business;
import shopline.com.JLogics.Callbacker;
import shopline.com.JLogics.Models.CartProduct;
import shopline.com.JLogics.Models.Product;

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

	private TextView subTotalTextviewLabel,gstTextviewLabel,grandTotalTextviewLabel,creditTextviewLabel,discountTotalTextviewLabel,totalNoDiscountLabelTextView;
	private TextView subTotalTextview,gstTextview,grandTotalTextview,creditTextview,discountTotalTextview,totalNoDiscountTextView;

	private LinearLayout costLinear,progress_overlay;
	DecimalFormat df = new DecimalFormat("#.###");


	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.cart_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		localDB = getContext().getSharedPreferences("localDB", Context.MODE_PRIVATE);
		linear1 = _view.findViewById(R.id.linear1);
		costLinear = _view.findViewById(R.id.costLinear);
		progress_overlay = _view.findViewById(R.id.progress_overlay);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		progressbar1 = _view.findViewById(R.id.progressbar1);

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


		auth = FirebaseAuth.getInstance();

//		cart = _firebase.getReference("datas/cart/".concat(userId));
		HashMap<String,Object> map = Business.localDB_SharedPref.getHashMap(localDB);

		GridLayoutManager gridlayoutManager= new GridLayoutManager(getContext().getApplicationContext(), 1, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
		recyclerview1.setLayoutManager(gridlayoutManager);
		listmap = new ArrayList<>();
		recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));

//		updateCartListFromFirebase();

//		localDB.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
//			@Override
//			public void onSharedPreferenceChanged(SharedPreferences localDB, @Nullable String key) {
//				if(key != null && key.equals(Business.localDB_SharedPref.PREF_KEY)){
//					HashMap<String, Object> cartData = Business.localDB_SharedPref.getCart(localDB);
//					updateCartListUI(cartData);
//				}
//			}
//		});

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


//		cart.addChildEventListener(new ChildEventListener() {
//			@Override
//			public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
//				HashMap<String, Object> cartData = (HashMap<String, Object>) dataSnapshot.getValue();
//				updateCartListUI(cartData);
//			}
//
//			@Override
//			public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//				updateCartListFromFirebase();
//			}
//
//			@Override
//			public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
//				HashMap<String, Object> cartData = (HashMap<String, Object>) dataSnapshot.getValue();
//				updateCartListUI(cartData);
//			}
//
//			@Override
//			public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
//
//			}
//
//			@Override
//			public void onCancelled(@NonNull DatabaseError databaseError) {
//
//			}
//		});


	}



//	private void updateCartListFromFirebase() {
//		cart.addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//				// Get the data as a HashMap
//				HashMap<String, Object> cartData = (HashMap<String, Object>) dataSnapshot.getValue();
//
//				if (cartData == null) {
//					cartData = new HashMap<>();
//				}
//
//				updateCartListUI(cartData);
//			}
//
//			@Override
//			public void onCancelled(@NonNull DatabaseError databaseError) {
//
//			}
//		});
//	}
	private void updateCartListUI(HashMap<String,Object> cartData) {
		Business.BulkDetailsApiClient bulkDetailsApiClient = new Business.BulkDetailsApiClient();

		progress_overlay.setVisibility(View.VISIBLE);

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
					progressbar1.setVisibility(View.VISIBLE);
					recyclerview1.setVisibility(View.GONE);

					subTotalTextview.setText("₹ 0.00");
					gstTextview.setText("₹ 0.00");
					totalNoDiscountTextView.setText("₹ 0.00");
					discountTotalTextview.setText("- ₹ 0.00");
					grandTotalTextview.setText("₹ 0.00");
				} else {
					progressbar1.setVisibility(View.GONE);
					recyclerview1.setVisibility(View.VISIBLE);

					Business.BulkDetailsApiClient.CostDetails costDetails = response.getCostDetails();
					subTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalRate())));
					gstTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotalGst())));
					totalNoDiscountTextView.setText("₹ ".concat(df.format(costDetails.getTotalRate()+costDetails.getTotalGst())));
					discountTotalTextview.setText("- ₹ ".concat(String.valueOf(costDetails.getTotalDiscount())));
					grandTotalTextview.setText("₹ ".concat(String.valueOf(costDetails.getTotal())));
//					creditTextview.setText(String.valueOf(costDetails.getTotalDiscount()));
				}

				recyclerview1.getAdapter().notifyDataSetChanged();
				progress_overlay.setVisibility(View.GONE);
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

//		private void updateCountToCart(DatabaseReference cart_,Long count) {
//			if (count == 0) {
//				cart_.removeValue();
//			} else {
//				HashMap<String,Object> map = new HashMap<>();
//				map.put("count", count);
//				cart_.updateChildren(map);
//			}
//		}

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
