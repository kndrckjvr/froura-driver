package com.froura.develo4.driver.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.froura.develo4.driver.R;
import com.froura.develo4.driver.objects.BookingObject;
import com.froura.develo4.driver.objects.ReservationObject;

import java.util.ArrayList;

/**
 * Created by KendrickAndrew on 07/02/2018.
 */

public class ReservationServicesAdapter extends RecyclerView.Adapter<ReservationServicesAdapter.ViewHolder> {

    private Context mContext;
    private ReservationServicesInterface mListener;
    public static ArrayList<ReservationObject> mResultList = new ArrayList<>();

    public ReservationServicesAdapter(Context mContext, ReservationServicesInterface mListener) {
        this.mContext = mContext;
        this.mListener = mListener;
    }

    public interface ReservationServicesInterface {
        void onReservationClick(ArrayList<ReservationObject> mResultList, int position);
    }

    @Override
    public ReservationServicesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = layoutInflater.inflate(R.layout.booking_adapter, parent, false);
        ViewHolder mPredictionHolder = new ViewHolder(convertView);
        return mPredictionHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.pickupTxtVw.setText(mResultList.get(position).getPickup());
        holder.dropoffTxtVw.setText(mResultList.get(position).getDropoff());
        holder.pickupTxtVw.setSelected(true);
        holder.dropoffTxtVw.setSelected(true);
        holder.fareTxtVw.setText("Price: ₱ " +mResultList.get(position).getPrice());
        holder.acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onReservationClick(mResultList, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mResultList != null ? mResultList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView pickupTxtVw;
        public TextView dropoffTxtVw;
        public TextView fareTxtVw;
        public TextView date;
        public Button acceptBtn;

        public ViewHolder(View v) {
            super(v);
            pickupTxtVw = v.findViewById(R.id.pickup);
            dropoffTxtVw = v.findViewById(R.id.dropoff);
            fareTxtVw = v.findViewById(R.id.fare);
            acceptBtn = v.findViewById(R.id.acceptBtn);
            date = v.findViewById(R.id.date);
        }
    }
}
