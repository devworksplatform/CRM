package crmapp.petsfort;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.*;
import java.util.ArrayList;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.Callbacker;
import crmapp.petsfort.JLogics.JHelpers;
import crmapp.petsfort.JLogics.Models.User;

public class ChooseUserActivity extends AppCompatActivity {

//    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private SharedPreferences userss;


    private ArrayList<User> listmap = null;
    private ArrayList<User> search_listmap = new ArrayList<>();

    private SharedPreferences localDB;
    private LinearLayout linear2;
    private LinearLayout linear1;
    private LinearLayout linear5,linearLogout;
    private LinearLayout linear4;
    private LinearLayout linear10;
    private LinearLayout linear11, rootLinear;
    private LinearLayout circle;
    private LinearLayout linear15;
    private LinearLayout linear16;
    private LinearLayout circle2;
    private LinearLayout noDataLinear;

    private ImageView imageview1;
    private TextView textviewTemp;
    private EditText edittext1;
    private RecyclerView recyclerview1;
    private ProgressBar progressbar1;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.choose_user);
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        rootLinear = findViewById(R.id.rootLinear);
        linear2 = findViewById(R.id.linear2);
        linear1 = findViewById(R.id.linear1);
        linear5 = findViewById(R.id.linear5);
        linearLogout = findViewById(R.id.linearLogout);
        linear4 = findViewById(R.id.linear4);
        imageview1 = findViewById(R.id.imageview1);
        edittext1 = findViewById(R.id.edittext1);
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

        localDB = getSharedPreferences("localDB", Context.MODE_PRIVATE);

        linearLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                Business.JFCM.unSubscribeAll();
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                finishAffinity();
            }
        });

        linear1.setVisibility(View.VISIBLE);
        progressbar1.setVisibility(View.VISIBLE);
        recyclerview1.setVisibility(View.GONE);
        noDataLinear.setVisibility(View.GONE);
        search_listmap = new ArrayList<>();
        listmap = null;

        GridLayoutManager gridlayoutManager= new GridLayoutManager(getApplicationContext(), 1, GridLayoutManager.VERTICAL,true); gridlayoutManager.setReverseLayout(false);
        recyclerview1.setLayoutManager(gridlayoutManager);
        recyclerview1.setAdapter(new ChooseUserActivity.Recyclerview1Adapter(search_listmap));

        Business.UserDataApiClient.getAllUsersCallApi(new Callbacker.ApiResponseWaiters.UserDataApiCallback() {
            @Override
            public void onReceived(Business.UserDataApiClient.UserDataApiResponse response) {
                super.onReceived(response);
                if(response.getStatusCode() == 200) {
                    listmap = (ArrayList<User>) response.getUsers();
                    listmap.removeIf(user -> !user.role.equals("1"));
                    searchForUsersAndList(edittext1.getText().toString());
                } else {
                    Toast.makeText(ChooseUserActivity.this, "Server Busy, Try again later", Toast.LENGTH_SHORT).show();
                    JHelpers.TransitionManager(rootLinear, 600);
                    linear1.setVisibility(View.GONE);
                    progressbar1.setVisibility(View.GONE);
                    recyclerview1.setVisibility(View.GONE);
                    noDataLinear.setVisibility(View.VISIBLE);
                }
            }
        });


        edittext1.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                final String _charSeq = _param1.toString();
                searchForUsersAndList(_charSeq);
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });


        userss = getSharedPreferences("logindata", Activity.MODE_PRIVATE);


    }

    private void initializeLogic() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);

        edittext1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);

        textviewTemp.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
        linear10.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
        linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
        circle.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));
        linear15.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
        linear16.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFE8E8E8));
        circle2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFE8E8E8));

    }


    public void searchForUsersAndList(String searchTxt) {
        if(searchTxt.isEmpty() || listmap == null || listmap.isEmpty()) {
            if(listmap == null || listmap.isEmpty()) {
                JHelpers.TransitionManager(rootLinear, 400);
                linear1.setVisibility(View.GONE);
                progressbar1.setVisibility(View.GONE);
                recyclerview1.setVisibility(View.GONE);
                noDataLinear.setVisibility(View.VISIBLE);
            } else {
                search_listmap.clear();
                search_listmap.addAll(listmap);
                recyclerview1.getAdapter().notifyDataSetChanged();

                JHelpers.TransitionManager(rootLinear, 400);
                linear1.setVisibility(View.VISIBLE);
                progressbar1.setVisibility(View.GONE);
                recyclerview1.setVisibility(View.VISIBLE);
                noDataLinear.setVisibility(View.GONE);
            }
        } else {
            searchTxt = searchTxt.toLowerCase();
            search_listmap.clear();
            for (User user : listmap) {
                if (user.name.toLowerCase().contains(searchTxt)) {
                    search_listmap.add(user);
                } else if (user.email.toLowerCase().contains(searchTxt)) {
                    search_listmap.add(user);
                } else if (user.uid.toLowerCase().contains(searchTxt)) {
                    search_listmap.add(user);
                }
            }

            recyclerview1.getAdapter().notifyDataSetChanged();

            if(search_listmap.isEmpty()) {
                JHelpers.TransitionManager(rootLinear, 400);
                linear1.setVisibility(View.GONE);
                progressbar1.setVisibility(View.GONE);
                recyclerview1.setVisibility(View.GONE);
                noDataLinear.setVisibility(View.VISIBLE);
            } else {
                JHelpers.TransitionManager(rootLinear, 400);
                linear1.setVisibility(View.VISIBLE);
                progressbar1.setVisibility(View.GONE);
                recyclerview1.setVisibility(View.VISIBLE);
                noDataLinear.setVisibility(View.GONE);
            }

        }
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


    public void _reverse(final ArrayList<User> _mapname) {
        Collections.reverse(_mapname);
    }

    public class Recyclerview1Adapter extends RecyclerView.Adapter<Recyclerview1Adapter.ViewHolder> {

        ArrayList<User> _data;

        public Recyclerview1Adapter(ArrayList<User> _arr) {
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

            User user = _data.get(_position);

            OrderId.setText(user.name + " (" + user.email + ")");

            try{
                if(user.isBlocked == 1) {
                    OrderStatus.setText("Blocked");
                    OrderStatus.setBackgroundResource(Business.JOrderStatus.valueOf("ORDER_CANCELLED").getDrawableRes());
                } else {
                    OrderStatus.setText("Active");
                    OrderStatus.setBackgroundResource(Business.JOrderStatus.valueOf("ORDER_DELIVERED").getDrawableRes());
                }
            } catch (Exception e) {
                OrderStatus.setText("User Status Error");
                OrderStatus.setBackgroundColor(Color.parseColor("#ffffff"));
            }



            OrderDetail.setText((user.uid)
                    .concat(", Credits: Rs.").concat(String.valueOf(user.credits)).concat(" ₹"));

            OrderId.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
            OrderStatus.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/salesbold.ttf"), 0);
            OrderDetail.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/sailes.ttf"), 0);

            linear3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Business.localDB_SharedPref.setProxyUID(userss, user.uid);
                    Business.localDB_SharedPref.clearCart(localDB);
                    Intent intent = new Intent();

                    intent.putExtra("proxy_id", user.id);
                    intent.putExtra("proxy_uid", user.uid);
                    intent.putExtra("proxy_name", user.name);
                    intent.putExtra("proxy_email", user.email);

                    intent.setClass(getApplicationContext(), PrincipalActivity.class);
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
