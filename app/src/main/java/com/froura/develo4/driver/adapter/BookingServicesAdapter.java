package com.froura.develo4.driver.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.froura.develo4.driver.R;
import com.froura.develo4.driver.objects.BookingObject;

import java.util.ArrayList;

/**
 * Created by KendrickAndrew on 07/02/2018.
 */

public class BookingServicesAdapter extends RecyclerView.Adapter<BookingServicesAdapter.ViewHolder> {

    private Context mContext;
    private BookingServicesInterface mListener;
    public static ArrayList<BookingObject> mResultList = new ArrayList<>();

    public BookingServicesAdapter(Context mContext, BookingServicesInterface mListener) {
        this.mContext = mContext;
        this.mListener = mListener;
    }

    public interface BookingServicesInterface {
        public void onBookingClick(ArrayList<BookingObject> mResultList, int position);
    }

    @Override
    public BookingServicesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = layoutInflater.inflate(R.layout.booking_adapter, parent, false);
        ViewHolder mPredictionHolder = new ViewHolder(convertView);
        return mPredictionHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.pickupTxtVw.setText(mResultList.get(position).getPickup());
        holder.dropoffTxtVw.setText(mResultList.get(position).getDropoff());
        holder.fareTxtVw.setText("Fare: ₱ " +mResultList.get(position).getFare());
        holder.acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onBookingClick(mResultList, position);
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
        public Button acceptBtn;

        public ViewHolder(View v) {
            super(v);
            pickupTxtVw = v.findViewById(R.id.pickup);
            dropoffTxtVw = v.findViewById(R.id.dropoff);
            fareTxtVw = v.findViewById(R.id.fare);
            acceptBtn = v.findViewById(R.id.acceptBtn);
        }
    }
}
