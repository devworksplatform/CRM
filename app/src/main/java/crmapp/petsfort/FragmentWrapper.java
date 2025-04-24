package crmapp.petsfort;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crmapp.petsfort.JLogics.Business;
import crmapp.petsfort.JLogics.JHelpers;


public class FragmentWrapper extends AppCompatActivity {

    LinearLayout wrapperLinear,backButtonLinear;
    TextView titleText;
    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.fragment_wrapper_activity);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        wrapperLinear = findViewById(R.id.wrapperLinear);
        backButtonLinear = findViewById(R.id.linear5);
        titleText = findViewById(R.id.titleText);
        init();
    }


    void init() {
        if(getIntent().hasExtra("fragment")){
            if(getIntent().getStringExtra("fragment").equals("cart")){
                titleText.setText("Cart Products");
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.wrapperLinear, new CartFragmentActivity())
                        .commit();
            }
        } else {
            finish();
        }


        backButtonLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }
}
