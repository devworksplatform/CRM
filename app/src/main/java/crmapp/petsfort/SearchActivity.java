package crmapp.petsfort;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.CartProduct;
import crmapp.petsfort.JLogics.Models.Product;
import crmapp.petsfort.JLogics.Models.SubCategory;

public class SearchActivity extends AppCompatActivity {
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

	SharedPreferences localDB;

	private double count1 = 0;
	private double length = 0;
	
	private ArrayList<Product> listmap = new ArrayList<>();
	
	private LinearLayout linear1, linearSearchImage;
	private LinearLayout linear;
	private LinearLayout linear3;
//	private LinearLayout linear4;
//	private LinearLayout linear5;
	private ImageView imageview1;
	private EditText edittext1;
	private ImageView imageview2;
	private LinearLayout main;
	private RecyclerView recyclerview1, recyclerviewLeft;
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

	private CardView cardview1;
	
	private Intent i = new Intent();
	private DatabaseReference product = _firebase.getReference("product");
	private ChildEventListener _product_child_listener;

	DecimalFormat df = new DecimalFormat("#.###");
	JHelpers.LoadingOverlay loadingOverlay = new JHelpers.LoadingOverlay();


	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		userId = Business.localDB_SharedPref.getProxyUID(getSharedPreferences("logindata", Activity.MODE_PRIVATE), userId);
		setContentView(R.layout.search);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();

		if(getIntent().hasExtra("category") && !getIntent().getStringExtra("category").equals("")) {
			drawer();
		} else {
			imageview2.setVisibility(View.GONE);
			recyclerviewLeft.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(recyclerview1 != null && recyclerview1.getAdapter() != null) {
			recyclerview1.getAdapter().notifyDataSetChanged();
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);

		linear1 = findViewById(R.id.linear1);
		linear = findViewById(R.id.linear);
		linear3 = findViewById(R.id.linear3);
//		linear4 = findViewById(R.id.linear4);
		imageview1 = findViewById(R.id.imageview1);
		edittext1 = findViewById(R.id.edittext1);
		imageview2 = findViewById(R.id.imageview2);
		main = findViewById(R.id.main);
		recyclerview1 = findViewById(R.id.recyclerview1);
		recyclerviewLeft = findViewById(R.id.recyclerviewLeft);
		linear12 = findViewById(R.id.linear12);
		linear7 = findViewById(R.id.linear7);
		linear17 = findViewById(R.id.linear17);
		circle2 = findViewById(R.id.circle2);
		linear14 = findViewById(R.id.linear14);
		linear15 = findViewById(R.id.linear15);
		linear16 = findViewById(R.id.linear16);
		linearSearchImage = findViewById(R.id.linear5111);
		circle = findViewById(R.id.circle);
		linear9 = findViewById(R.id.linear9);
		linear10 = findViewById(R.id.linear10);
		linear11 = findViewById(R.id.linear11);
		textview1 = findViewById(R.id.textview1);

		cardview1 = findViewById(R.id.cartview1);


		cardview1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setClass(getApplicationContext(), FragmentWrapper.class);
				i.putExtra("fragment", "cart");
				startActivity(i);
			}
		});


		linearSearchImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		edittext1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				searchForProductAndList(_charSeq, null, null);
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
		_ICC(imageview2, "#9e9e9e", "#9e9e9e");
//		linear4.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFFF6F2F1));
//		linear4.setElevation((float)1);

		edittext1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);

		textview1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
		circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
		GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false); 
		recyclerview1.setLayoutManager(gridlayoutManager);


		findViewById(R.id.init_loading).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//pass
			}
		});

		final View loadingView = findViewById(R.id.init_loading);
		final long loadingStartTime = System.currentTimeMillis(); // ⏱️ Mark start time

		searchForProductAndList("", null, new SearchCompleteListener() {
			@Override
			public void onSearchCompleted() {
				long elapsed = System.currentTimeMillis() - loadingStartTime;
				long delay = Math.max(0, 1000 - elapsed); // 🕒 Wait remaining time if needed

				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					loadingView.animate()
							.alpha(0f)
							.translationY(-50)
							.setDuration(400)
							.setInterpolator(new AccelerateDecelerateInterpolator())
							.setListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									loadingView.setTranslationY(0);
									loadingView.setVisibility(View.GONE);
									loadingView.setAlpha(1f);

								}
							})
							.start();
				}, delay);
			}
		});

	}
	
	public void _ICC(final ImageView _img, final String _c1, final String _c2) {
		_img.setImageTintList(new android.content.res.ColorStateList(new int[][] {{-android.R.attr.state_pressed},{android.R.attr.state_pressed}},new int[]{Color.parseColor(_c1), Color.parseColor(_c2)}));
	}


	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;

		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration);
		autoTransition.setInterpolator(new android.view.animation.DecelerateInterpolator()); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}


	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {

		ArrayList<Product> _data;
		Context context;

		Typeface typeface;

		public Recyclerview1Adapter(Context context, ArrayList<Product> _arr) {
			this.context = context;
			_data = _arr;
			typeface = Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf");
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
			final LinearLayout linearAddToCart = _view.findViewById(R.id.linear23);
			final LinearLayout linearCounter = _view.findViewById(R.id.linear22);
			final ImageView minus = _view.findViewById(R.id.minus);
			final ImageView plus = _view.findViewById(R.id.plus);
			final EditText edittext1Count = _view.findViewById(R.id.edittext1);
			final ImageView imageview1 = _view.findViewById(R.id.productImageView);
			final TextView textviewRefId = _view.findViewById(R.id.productIdTextView);
			final TextView textviewName = _view.findViewById(R.id.productNameTextView);
			final TextView textviewMRP = _view.findViewById(R.id.mrpTextView);
			final TextView textviewRate = _view.findViewById(R.id.rateTextView);
			final TextView textviewGST = _view.findViewById(R.id.gstTextView);
			final TextView discountShow = _view.findViewById(R.id.discountShow);
//			final LottieAnimationView addToCart = _view.findViewById(R.id.addToCart);



			CartProduct product = new CartProduct(listmap.get((int)_position));

			HashMap<String,Object> map = Business.localDB_SharedPref.getCartProduct(localDB,product.getProductId());

			if(map.containsKey("count")) {
				long currentCount = 0L;
				if (map.containsKey("count")) {
					Object value = map.get("count");
					if (value instanceof Number) {
						currentCount = ((Number) value).intValue();
					} else if (value instanceof String) {
						try {
							currentCount = Integer.parseInt((String) value);
						} catch (NumberFormatException e) {}
					}
				}

				product.productCount = currentCount;
			} else {
				product.productCount = 0L;
			}

			if (product.getProductImg() != null && product.getProductImg().size() > 0 && product.getProductImg().get(0) != null && !product.getProductImg().get(0).equals("")) {
				Glide.with(context).load(Uri.parse(product.getProductImg().get(0))).into(imageview1);
			} else {
				Glide.with(context).load(R.drawable.app_icon).into(imageview1);
			}

//			textviewRefId.setText(product.getProductId());
			String text = "";
//			if(product.getProductHsn() != null && !product.getProductHsn().isEmpty()) {
//				text += "HSN-("+String.valueOf(product.getProductHsn()).concat(")");
//			} else {
//				text += "HSN-(NONE)";
//			}

			if(product.getProductCid() != null && !product.getProductCid().isEmpty()) {
				text += "Code-(".concat(product.getProductCid()).concat(")");
			} else {
				text += "Code-(NONE)";
			}
			textviewRefId.setText(text);
//			textviewRefId.setText("HSN-("+String.valueOf(product.getProductHsn()).concat(") Code-(").concat(product.getProductCid()).concat(")"));
			textviewName.setText(JHelpers.capitalize(product.getProductName()));
			textviewMRP.setText("₹".concat(df.format(product.getCostMrp())));
			textviewRate.setText("₹".concat(df.format(product.getCostRate())));
			textviewGST.setText("+".concat(df.format(product.getCostGst())).concat("% GST"));
			discountShow.setText(df.format(product.getCostDis()).concat("% OFF"));

			textviewRefId.setTypeface(typeface, 0);
			textviewName.setTypeface(typeface, 0);
			textviewMRP.setTypeface(typeface, 0);
			textviewRate.setTypeface(typeface, 0);
			textviewGST.setTypeface(typeface, 0);
			discountShow.setTypeface(typeface, 0);


			if (product.getCostDis() <= 0) {
				discountShow.setVisibility(View.GONE);
			} else {
				discountShow.setVisibility(View.VISIBLE);
			}


			rootLinear.setOnClickListener(_view2 -> {
				Intent intent = new Intent();
				intent.putExtra("product",(Product) product);
				intent.setClass(getApplicationContext(), ProductviewActivity.class);
				startActivity(intent);
			});


			if (product.productCount <= 0) {
				linearAddToCart.setVisibility(View.VISIBLE);
				linearCounter.setVisibility(View.GONE);
			} else {
				linearAddToCart.setVisibility(View.GONE);
				linearCounter.setVisibility(View.VISIBLE);
			}


			linearAddToCart.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					edittext1Count.setText("1");

					linearAddToCart.animate()
							.alpha(0f)
							.translationYBy(-linearAddToCart.getHeight() / 2f)
							.setDuration(150)
							.withEndAction(() -> {
								linearAddToCart.setVisibility(View.GONE);
								linearAddToCart.setAlpha(1);
								linearAddToCart.setTranslationY(0);

								// Prepare and show "Counter" with animation
								linearCounter.setAlpha(0f);
								linearCounter.setTranslationY(linearCounter.getHeight() / 2f);
								linearCounter.setVisibility(View.VISIBLE);

								linearCounter.animate()
										.alpha(1f)
										.translationY(0f)
										.setDuration(150)
										.start();
							})
							.start();

				}
			});


			plus.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					edittext1Count.setText(String.valueOf(product.productCount+1));
				}
			});

			minus.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(product.productCount <= 0) {

					} else {
						edittext1Count.setText(String.valueOf(product.productCount-1));
					}
				}
			});



			if(_holder.watcher != null){
				edittext1Count.removeTextChangedListener(_holder.watcher);
			}

			edittext1Count.setText(product.productCount+"");

			_holder.watcher = new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
					final String _charSeq = _param1.toString();

					int currentCount = Integer.parseInt(String.valueOf(product.productCount));

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
							edittext1Count.setText("");
//							linearCounter.setVisibility(View.GONE);
//							linearAddToCart.setVisibility(View.VISIBLE);
							linearCounter.animate()
									.alpha(0f)
									.translationYBy(linearCounter.getHeight() / 2f)
									.setDuration(150)
									.withEndAction(() -> {
										linearCounter.setVisibility(View.GONE);
										linearCounter.setAlpha(1);
										linearCounter.setTranslationY(0);

										// Prepare and show "Add to Cart" with animation
										linearAddToCart.setAlpha(0f);
										linearAddToCart.setTranslationY(-linearAddToCart.getHeight() / 2f);
										linearAddToCart.setVisibility(View.VISIBLE);

										linearAddToCart.animate()
												.alpha(1f)
												.translationY(0f)
												.setDuration(150)
												.start();
									})
									.start();

						}
						product.productCount = 0L;

						Business.localDB_SharedPref.deleteCartProduct(localDB,product.getProductId());
					} else {

						if(currentCount > product.getStock()){
							edittext1Count.setText(String.valueOf(product.getStock()));
							Toast.makeText(getApplicationContext(),"Maximum Purchase Count Reached",Toast.LENGTH_SHORT).show();
							return;
						}

						product.productCount = (long) currentCount;

						HashMap<String,Object> map = new HashMap<>();
						map.put("count", product.productCount);
						Business.localDB_SharedPref.updateCartProduct(localDB,product.getProductId(),map);
					}
				}

				@Override
				public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

				}

				@Override
				public void afterTextChanged(Editable _param1) {

				}
			};
			edittext1Count.addTextChangedListener(_holder.watcher);
		}
		private int lastAnimatedPosition = -1;


		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public TextWatcher watcher;
			public ViewHolder(View v) {
				super(v);
			}
		}
	}

	static class SearchCompleteListener {
		public void onSearchCompleted(){
		}
	}

	private void searchForProductAndList(String product_name, List<HashMap<String, String>> filtersSubCats, SearchCompleteListener searchCompleteListener) {
		Business.QueryApiClient queryApiClient = new Business.QueryApiClient();

		// Create filters list
		List<HashMap<String, String>> filters = new ArrayList<>();

		if (getIntent().hasExtra("category") && !getIntent().getStringExtra("category").equals("")) {
			//Main Category
			HashMap<String, String> filterAND = new HashMap<>();
			filterAND.put("field", "cat_id");
			filterAND.put("operator", "eq");
			filterAND.put("value", getIntent().getStringExtra("category"));
			filters.add(filterAND);

			if(filtersSubCats != null) {
				filters.addAll(filtersSubCats);
			} else {
				ArrayList<HashMap<String, String>> filters_t = new ArrayList<>();
				for (int i = 0; i < selectedPositions.size(); i++) {
					int index = selectedPositions.get(i);
					HashMap<String, String> filter = new HashMap<>();
					filter.put("field", "cat_sub");
					filter.put("operator", "contains");
					filter.put("value", myDataList.get(index).getId());
					filters_t.add(filter);
				}

				filters.addAll(filters_t);
			}
		} else {
			//Main Category
			HashMap<String, String> filterAND = new HashMap<>();
			filterAND.put("field", "cat_id");
			filterAND.put("operator", "neq");
			filterAND.put("value", "PLACE_HOLDER");
			filters.add(filterAND);
		}

		if(!product_name.equals("")) {
			//Product Name
			HashMap<String, String> filter3 = new HashMap<>();
			filter3.put("field", "product_name");
			filter3.put("operator", "contains");
			filter3.put("value", product_name);
			filters.add(filter3);

			filter3 = new HashMap<>();
			filter3.put("field", "product_hsn");
			filter3.put("operator", "contains");
			filter3.put("value", product_name);
			filters.add(filter3);

			filter3 = new HashMap<>();
			filter3.put("field", "product_cid");
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



//		loadingOverlay.show(SearchActivity.this);
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

				recyclerview1.setAdapter(new Recyclerview1Adapter(getApplicationContext(),listmap));
				GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
				recyclerview1.setLayoutManager(gridlayoutManager);

				if(searchCompleteListener != null){
					searchCompleteListener.onSearchCompleted();
				}
//				loadingOverlay.hide(SearchActivity.this);
			}
		});
	}





	void drawer() {

		RecyclerView staggeredRecyclerView = findViewById(R.id.recyclerviewLeft);

//		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//		LinearLayout drawer = (LinearLayout) findViewById(R.id._nav_view);
//		LinearLayout rootLinear = (LinearLayout) drawer.findViewById(R.id.rootLinear);


//		TextView textview1 = drawer.findViewById(R.id.textview1);
//		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);



		String catId = getIntent().getStringExtra("category");

		Business.SubCategoriesApiClient.getSubCategoriesCallApi(catId, new Callbacker.ApiResponseWaiters.SubCategoriesApiCallback(){
			@Override
			public void onReceived(Business.SubCategoriesApiClient.SubCategoriesApiResponse response) {
				if (response.getStatusCode() == 200) {

					myDataList = response.getSubCategories();

//					int selectedColor = 0xFFC0A95A; // Soft Gold - upscale and premium
//					int selectedColor = 0xFF3949AB; // Indigo 600 - elegant and professional
//					int selectedColor = 0xFF64B5F6; // Blue 300 (Material Design)
					int selectedColor = 0xFF546E7A; // Blue Grey 600 - sleek, modern, and less flashy
					int unselectedColor = 0xFFE0E0E0; // Grey 300 (Material Design)

					SelectableStaggeredGridAdapter adapter = new SelectableStaggeredGridAdapter(myDataList, selectedColor, unselectedColor);
					StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL); // Same values as in XML
					staggeredRecyclerView.setLayoutManager(layoutManager);
					staggeredRecyclerView.setAdapter(adapter);
//					ArrayList<Integer> selectedItems = adapter.getSelectedPositions();
				}
			}
		});



//		rootLinear.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				//pass
//			}
//		});

		imageview2.setVisibility(View.GONE);
//		imageview2.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//					drawerLayout.closeDrawer(GravityCompat.START);
//				} else {
//					drawerLayout.openDrawer(GravityCompat.START);
//				}
//			}
//		});


	}


	private ArrayList<Integer> selectedPositions = new ArrayList<>();
	ArrayList<SubCategory> myDataList = new ArrayList<>();
	public  class SelectableStaggeredGridAdapter extends RecyclerView.Adapter<SelectableStaggeredGridAdapter.ViewHolder> {

		private ArrayList<SubCategory> dataList;
		private final int selectedColor;
		private final int unselectedColor;

		public SelectableStaggeredGridAdapter(ArrayList<SubCategory> dataList, int selectedColor, int unselectedColor) {
			this.dataList = dataList;
			this.selectedColor = selectedColor;
			this.unselectedColor = unselectedColor;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staggered_selectable, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			SubCategory subcat = dataList.get(position);
			holder.itemText.setText(subcat.getName());


			holder.itemText.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);

			int fromColor = unselectedColor;
			int toColor = unselectedColor;
//			try{
//				fromColor = ((ColorDrawable) holder.itemRootContainer.getBackground()).getColor();
//				toColor = selectedPositions.contains(position) ? selectedColor : unselectedColor;
//			} catch (Exception e){
//
//			}

			if (selectedPositions.contains(position)) {
//				holder.itemRootContainer.setBackgroundColor(selectedColor);
				fromColor = unselectedColor;
				toColor = selectedColor;
				holder.itemText.setTextColor(Color.WHITE);
			} else {
				holder.itemText.setTextColor(Color.BLACK);
				holder.itemRootContainer.setBackgroundColor(unselectedColor);
			}

			ObjectAnimator colorFade = ObjectAnimator.ofObject(
					holder.itemRootContainer,
					"backgroundColor",
					new ArgbEvaluator(),
					fromColor,
					toColor
			);
			colorFade.setDuration(300); // duration in ms
			colorFade.start();


			holder.itemRootContainer.animate()
					.setDuration(300)
					.setInterpolator(new DecelerateInterpolator())
					.start();



			if(subcat.getImage() != null && subcat.getImage() != "null" && !subcat.getImage().isEmpty()) {
				holder.item_image.setVisibility(View.VISIBLE);
				Glide.with(getApplicationContext()).load(Uri.parse(subcat.getImage())).into(holder.item_image);
			} else {
				holder.item_image.setVisibility(View.GONE);
			}

			holder.itemView.setOnClickListener(v -> {

				int alreadySelected = -1;
				if(!selectedPositions.isEmpty()){
					alreadySelected = selectedPositions.get(0);
				}
				selectedPositions.clear();
//				if (selectedPositions.contains(position)) {
//					selectedPositions.remove(Integer.valueOf(position));
//				} else {
//					selectedPositions.add(position);
//				}
				if(alreadySelected != position) {
					selectedPositions.add(position);
				}
				notifyItemChanged(position); // Only update the clicked item
				if(alreadySelected != -1){
					notifyItemChanged(alreadySelected); // Only update the previously selected item
				}

//				notifyDataSetChanged();



				searchForProductAndList(edittext1.getText().toString().trim(), null, null);

				//animate

			});
		}

		@Override
		public int getItemCount() {
			return dataList.size();
		}

		public static class ViewHolder extends RecyclerView.ViewHolder {
			LinearLayout itemRootContainer;
			TextView itemText;
			ImageView item_image;

			public ViewHolder(View itemView) {
				super(itemView);
				itemRootContainer = itemView.findViewById(R.id.item_root_container);
				itemText = itemView.findViewById(R.id.item_text);
				item_image = itemView.findViewById(R.id.item_image);
			}
		}

		public ArrayList<Integer> getSelectedPositions() {
			return selectedPositions;
		}


		public void clearSelections() {
			selectedPositions.clear();
			notifyDataSetChanged();
		}
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
