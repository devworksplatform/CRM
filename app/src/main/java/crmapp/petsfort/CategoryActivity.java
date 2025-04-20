package crmapp.petsfort;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.Models.Category;

public class CategoryActivity extends AppCompatActivity {

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	private ArrayList<HashMap<String, Object>> listmap = new ArrayList<>();

	private LinearLayout linear1;
	private RecyclerView recyclerview1;
	private ProgressBar progressbar1;

//	private DatabaseReference product = _firebase.getReference("datas/category");
	private ChildEventListener _product_child_listener;
	private Intent i = new Intent();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.category_fragment);
		initialize();
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}

	private void initialize() {
		linear1 = findViewById(R.id.linear1);
		recyclerview1 = findViewById(R.id.recyclerview1);
		progressbar1 = findViewById(R.id.progressbar1);

	}

	private void initializeLogic() {

		LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
		recyclerview1.setLayoutManager(layoutManager);
		recyclerview1.setAdapter(new CategoryActivity.Recyclerview1Adapter(listmap));


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
				recyclerview1.setAdapter(new CategoryActivity.Recyclerview1Adapter(listmap));

				if (listmap.isEmpty()) {
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
//					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//						Collections.sort(listmap, Comparator.comparingInt(m -> Integer.parseInt(m.get("order").toString())));
//					}
//				}
//				catch (Exception _e) {
//					_e.printStackTrace();
//				}
//				recyclerview1.setAdapter(new CategoryActivity.Recyclerview1Adapter(listmap));
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

	}


	public class Recyclerview1Adapter extends RecyclerView.Adapter<CategoryActivity.Recyclerview1Adapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public Recyclerview1Adapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public CategoryActivity.Recyclerview1Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.categories, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new CategoryActivity.Recyclerview1Adapter.ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(CategoryActivity.Recyclerview1Adapter.ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final TextView textview1 = _view.findViewById(R.id.textview1);

			if (listmap.get((int)_position).containsKey("name")) {
				textview1.setText(listmap.get((int)_position).get("name").toString());
			}

			textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);
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
