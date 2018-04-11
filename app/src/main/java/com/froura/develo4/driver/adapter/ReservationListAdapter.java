package com.froura.develo4.driver.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.froura.develo4.driver.R;
import com.froura.develo4.driver.object.ReservationObject;

import java.util.ArrayList;

/**
 * Created by KendrickAndrew on 09/04/2018.
 */

public class ReservationListAdapter extends RecyclerView.Adapter<ReservationListAdapter.ViewHolder> {

    private Context context;
    private ReservationListAdapterListener mListener;
    private ArrayList<ReservationObject> mList = new ArrayList<>();
    private String[] status = {"On Process", "Reserved", "On Trip"};

    public interface ReservationListAdapterListener {
        public void onReservationListClick(ArrayList<ReservationObject> resultList, int position);
    }

    public ReservationListAdapter(Context context, ReservationListAdapterListener mListener, ArrayList<ReservationObject> mList) {
        this.context = context;
        this.mListener = mListener;
        this.mList = mList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView = layoutInflater.inflate(R.layout.adapter_reservation_list, parent, false);
        ViewHolder mPredictionHolder = new ViewHolder(convertView);
        return mPredictionHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.date_txt_vw.setText(mList.get(position).getDatetime());
        holder.pickup_txt_vw.setText(mList.get(position).getPickup_name());
        holder.pickup_txt_vw.setSelected(true);
        holder.dropoff_txt_vw.setText(mList.get(position).getDropoff_name());
        holder.dropoff_txt_vw.setSelected(true);
        holder.status_txt_vw.setText(status[mList.get(position).getStatus()]);
        holder.fare_txt_vw.setText("Php " + mList.get(position).getPrice());
        holder.row_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onReservationListClick(mList, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public void clearHistory() {
        mList.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView pickup_txt_vw;
        public TextView dropoff_txt_vw;
        public TextView date_txt_vw;
        public TextView status_txt_vw;
        public TextView fare_txt_vw;
        public LinearLayout row_layout;
        public ViewHolder(View itemView) {
            super(itemView);
            pickup_txt_vw = itemView.findViewById(R.id.pickup_txt_vw);
            dropoff_txt_vw = itemView.findViewById(R.id.dropoff_txt_vw);
            date_txt_vw = itemView.findViewById(R.id.date_txt_vw);
            row_layout = itemView.findViewById(R.id.row_layout);
            status_txt_vw = itemView.findViewById(R.id.status_txt_vw);
            fare_txt_vw = itemView.findViewById(R.id.fare_txt_vw);
        }
    }
}