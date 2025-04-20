package crmapp.petsfort;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.Models.Category;

public class Frag1FragmentActivity extends Fragment {
	
//	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();
	
	private LinearLayout linear1;
	private RecyclerView recyclerview1;
	private ProgressBar progressbar1;
	
//	private DatabaseReference product = _firebase.getReference("datas/category");
	private ChildEventListener _product_child_listener;
	private Intent i = new Intent();
	
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.frag1_fragment, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}
	
	private void initialize(Bundle _savedInstanceState, View _view) {
		linear1 = _view.findViewById(R.id.linear1);
		recyclerview1 = _view.findViewById(R.id.recyclerview1);
		progressbar1 = _view.findViewById(R.id.progressbar1);
	}
	
	private void initializeLogic() {

		Business.CategoriesApiClient.getCategoriesCallApi(new Callbacker.ApiResponseWaiters.CategoriesApiCallback(){
			@Override
			public void onReceived(Business.CategoriesApiClient.CategoriesApiResponse response) {
				super.onReceived(response);

				if(response.getStatusCode() == 200) {
					try {
						GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
						for (Category _data : response.getCategories()) {
							HashMap<String, Object> _map = new HashMap<>();
							_map.put("name", _data.getName());
							_map.put("img", _data.getImage());
							listmap.add(_map);
						}
					}
					catch (Exception _e) {
						_e.printStackTrace();
					}
				}
				recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));

				if (listmap.size() == 0) {
					progressbar1.setVisibility(View.VISIBLE);
					recyclerview1.setVisibility(View.GONE);
				}
				else {
					progressbar1.setVisibility(View.GONE);
					recyclerview1.setVisibility(View.VISIBLE);
				}

			}
		});

//		product.addListenerForSingleValueEvent(new ValueEventListener() {
//			@Override
//			public void onDataChange(DataSnapshot _dataSnapshot) {
//				listmap = new ArrayList<>();
//				try {
//					GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
//					for (DataSnapshot _data : _dataSnapshot.getChildren()) {
//						HashMap<String, Object> _map = _data.getValue(_ind);
//						_map.put("key", _data.getKey());
//						_map.put("order", Integer.parseInt(_data.getKey().replaceAll("\\D", "")));
//						listmap.add(_map);
//					}
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//						Collections.sort(listmap, Comparator.comparingInt(m -> Integer.parseInt(m.get("order").toString())));
//					}
//                }
//				catch (Exception _e) {
//					_e.printStackTrace();
//				}
//				recyclerview1.setAdapter(new Recyclerview1Adapter(listmap));
//
//				if (listmap.size() == 0) {
//					progressbar1.setVisibility(View.VISIBLE);
//					recyclerview1.setVisibility(View.GONE);
//				}
//				else {
//					progressbar1.setVisibility(View.GONE);
//					recyclerview1.setVisibility(View.VISIBLE);
//				}
//			}
//			@Override
//			public void onCancelled(DatabaseError _databaseError) {
//			}
//		});
		GridLayoutManager gridlayoutManager= new GridLayoutManager(getContext().getApplicationContext(), 4, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
		recyclerview1.setLayoutManager(gridlayoutManager);

	}
	
	public void _reverse(final ArrayList<HashMap<String, Object>> _mapname) {
		Collections.reverse(_mapname);
	}
	
	public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.product, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final androidx.cardview.widget.CardView cardview1 = _view.findViewById(R.id.cardview1);
			final androidx.cardview.widget.CardView cardview2 = _view.findViewById(R.id.cardview2);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
//			final LinearLayout linear2 = _view.findViewById(R.id.linear2);
			final ImageView imageview1 = _view.findViewById(R.id.imageview1);
			final TextView textview2 = _view.findViewById(R.id.textview2);

			if (listmap.get((int)_position).containsKey("name")) {
				textview2.setText(listmap.get((int)_position).get("name").toString());
			}
			if (listmap.get((int)_position).containsKey("img")) {
				Glide.with(getContext().getApplicationContext()).load(Uri.parse(listmap.get((int)_position).get("img").toString())).into(imageview1);
			}


			textview2.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/sailes.ttf"), 1);
			cardview1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					Intent intent = new Intent();
					intent.putExtra("category", listmap.get((int)_position).get("key").toString());
					intent.setClass(getContext().getApplicationContext(), SearchActivity.class);
					startActivity(intent);
				}
			});
			cardview2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					Intent intent = new Intent();
					intent.putExtra("category", listmap.get((int)_position).get("key").toString());
					intent.setClass(getContext().getApplicationContext(), SearchActivity.class);
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
