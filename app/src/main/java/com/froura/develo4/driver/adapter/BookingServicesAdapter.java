package com.froura.develo4.driver.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.froura.develo4.driver.R;
import com.froura.develo4.driver.objects.BookingObject;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.util.ArrayList;

/**
 * Created by KendrickAndrew on 07/02/2018.
 */

public class BookingServicesAdapter extends RecyclerView.Adapter<BookingServicesAdapter.ViewHolder> {

    private Context mContext;
    private BookingServicesInterface mListener;
    private ArrayList<BookingObject> mResultList = new ArrayList<>();
    private DatabaseReference dbRef;
    private ValueEventListener listener;

    public BookingServicesAdapter(Context mContext, BookingServicesInterface mListener) {
        this.mContext = mContext;
        this.mListener = mListener;
        dbRef = FirebaseDatabase.getInstance().getReference("services");
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mResultList.clear();
                for(DataSnapshot booking : dataSnapshot.getChildren()) {
                    for (DataSnapshot pssngrid : booking.getChildren()) {
                        String pass_id = pssngrid.getKey().toString();
                        LatLng dropoffLoc = new LatLng();
                        String dropoffName = "";
                        LatLng pickupLoc = new LatLng();
                        String pickupName = "";
                        String fare = "";
                        for(DataSnapshot bookingDetails : pssngrid.getChildren()) {
                            switch (bookingDetails.getKey()) {
                                case "dropoff":
                                    for(DataSnapshot dropoffDetails : bookingDetails.getChildren()) {
                                        switch (dropoffDetails.getKey()) {
                                            case "lat":
                                                dropoffLoc.setLatitude(Double.parseDouble(dropoffDetails.getValue().toString()));
                                                break;
                                            case "lng":
                                                dropoffLoc.setLongitude(Double.parseDouble(dropoffDetails.getValue().toString()));
                                                break;
                                            case "name":
                                                dropoffName = dropoffDetails.getValue().toString();
                                                break;
                                        }
                                    }
                                    break;
                                case "pickup":
                                    for(DataSnapshot pickupDetails : bookingDetails.getChildren()) {
                                        switch (pickupDetails.getKey()) {
                                            case "lat":
                                                pickupLoc.setLatitude(Double.parseDouble(pickupDetails.getValue().toString()));
                                                break;
                                            case "lng":
                                                pickupLoc.setLongitude(Double.parseDouble(pickupDetails.getValue().toString()));
                                                break;
                                            case "name":
                                                pickupName = pickupDetails.getValue().toString();
                                                break;
                                        }
                                    }
                                    break;
                                case "fare":
                                    fare = bookingDetails.getValue().toString();
                                    break;
                            }
                        }
                        mResultList.add(new BookingObject(pass_id, pickupName, dropoffName, fare, pickupLoc, dropoffLoc));
                        notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        getBookings();
    }

    public interface BookingServicesInterface {
        void onBookingClick(ArrayList<BookingObject> mResultList, int position);
    }

    private void getBookings() {
        dbRef.addValueEventListener(listener);
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
        holder.pickupTxtVw.setSelected(true);
        holder.dropoffTxtVw.setSelected(true);
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
