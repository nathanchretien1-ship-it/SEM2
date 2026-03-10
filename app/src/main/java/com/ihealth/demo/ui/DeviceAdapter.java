package com.ihealth.demo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ihealth.demo.R;
import com.ihealth.demo.model.DeviceModel;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<DeviceModel> devices = new ArrayList<>();
    private final OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceModel device);
    }

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setDevices(List<DeviceModel> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel device = devices.get(position);
        holder.name.setText(device.getName());
        holder.mac.setText("MAC: " + device.getMac());
        holder.status.setText("Status: " + device.getStatus());
        holder.data.setText("Last Data: " + (device.getLastData().isEmpty() ? "None" : device.getLastData()));
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, mac, status, data;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.device_name);
            mac = itemView.findViewById(R.id.device_mac);
            status = itemView.findViewById(R.id.device_status);
            data = itemView.findViewById(R.id.device_data);
        }
    }
}
