package com.sebswebs.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import com.sebswebs.myapplication.databinding.ActivityNoSuchUserBinding;

public final class NoSuchUser extends AppCompatActivity {
    private ActivityNoSuchUserBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityNoSuchUserBinding.inflate(this.getLayoutInflater());
        this.setContentView((View) binding.getRoot());

        String barcodeInfo = this.getIntent().getStringExtra("barcodeValue");
        List barcodeParts = Arrays.asList(barcodeInfo.split(","));

        this.binding.membershipNum.setText((CharSequence) barcodeParts.get(0));
        this.binding.tvHooray.setText((CharSequence) barcodeParts.get(1));
        this.binding.MemberDateOfBirth.setText((CharSequence) barcodeParts.get(2));
        this.binding.btScanAnother.setOnClickListener(new View.OnClickListener() {
            public void onClick(@Nullable View view) {
                Log.e("TAG", "messaged"); NoSuchUser.this.switchToMain();
            }
        });
    }

    private final void switchToMain() {
        Intent switchActivityIntent = new Intent((Context) this, MainActivity.class);
        this.startActivity(switchActivityIntent);
    }
}