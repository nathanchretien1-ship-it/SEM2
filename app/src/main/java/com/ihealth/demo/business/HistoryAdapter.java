package com.ihealth.demo.business;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ihealth.demo.R;
import com.ihealth.demo.business.data.MeasurementEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<MeasurementEntity> measurements;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public void setMeasurements(List<MeasurementEntity> measurements) {
        this.measurements = measurements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (measurements != null) {
            MeasurementEntity current = measurements.get(position);
            holder.tvDate.setText(dateFormat.format(new Date(current.timestamp)));

            if ("PO3".equals(current.deviceType) || (current.bpm != null && current.spo2 != null)) {
                holder.tvDeviceType.setText("Oxymètre (PO3)");
                holder.ivIcon.setImageResource(R.drawable.ic_blood);
                holder.tvValue1.setText(current.bpm != null ? current.bpm + " bpm" : "-- bpm");
                holder.tvValue2.setText(current.spo2 != null ? "SpO2: " + current.spo2 + "%" : "-- %");
                holder.tvValue2.setVisibility(View.VISIBLE);
            } else if ("NT13B".equals(current.deviceType) || current.temperature != null) {
                holder.tvDeviceType.setText("Thermomètre (NT13B)");
                holder.ivIcon.setImageResource(R.drawable.ic_temp);
                holder.tvValue1.setText(current.temperature != null ? current.temperature + " °C" : "-- °C");
                holder.tvValue2.setVisibility(View.GONE);
            } else {
                holder.tvDeviceType.setText(current.deviceType);
                holder.ivIcon.setImageResource(R.drawable.ic_dashboard);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (measurements != null)
            return measurements.size();
        else return 0;
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvDeviceType;
        private final TextView tvDate;
        private final TextView tvValue1;
        private final TextView tvValue2;

        private HistoryViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.item_icon);
            tvDeviceType = itemView.findViewById(R.id.item_device_type);
            tvDate = itemView.findViewById(R.id.item_date);
            tvValue1 = itemView.findViewById(R.id.item_value1);
            tvValue2 = itemView.findViewById(R.id.item_value2);
        }
    }
}