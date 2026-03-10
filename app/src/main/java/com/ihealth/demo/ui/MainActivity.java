package com.ihealth.demo.ui;

import android.Manifest;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ihealth.demo.R;
import com.ihealth.demo.ui.DeviceAdapter;
import com.ihealth.demo.ui.MainViewModel;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private DeviceAdapter adapter;
    private TextView statusText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        Button scanButton = findViewById(R.id.scan_button);
        RecyclerView recyclerView = findViewById(R.id.device_recycler);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        adapter = new DeviceAdapter(device -> viewModel.connect(device));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        scanButton.setOnClickListener(v -> checkPermissionAndScan());

        viewModel.getDiscoveredDevices().observe(this, devices -> {
            adapter.setDevices(devices);
        });

        viewModel.getStatusMessage().observe(this, message -> {
            statusText.setText("Status: " + message);
        });
    }

    private void checkPermissionAndScan() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE)
                .subscribe(granted -> {
                    if (granted) {
                        viewModel.startScan();
                    } else {
                        Toast.makeText(this, "Permissions required for Bluetooth scanning", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
