package com.froura.develo4.driver.object;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by KendrickAndrew on 09/04/2018.
 */

public class ReservationObject {
    private int database_id;
    private String driver_id;
    private String pickup_name;
    private String dropoff_name;
    private LatLng pickupLatLng;
    private LatLng dropoffLatLng;
    private String pickup_id;
    private String dropoff_id;
    private String datetime;
    private String price;
    private String note;
    private int status;

    public ReservationObject(int database_id, String driver_id, String pickup_name, String dropoff_name, LatLng pickupLatLng, LatLng dropoffLatLng, String pickup_id, String dropoff_id, String datetime, String price, String note, int status) {
        this.database_id = database_id;
        this.driver_id = driver_id;
        this.pickup_name = pickup_name;
        this.dropoff_name = dropoff_name;
        this.pickupLatLng = pickupLatLng;
        this.dropoffLatLng = dropoffLatLng;
        this.pickup_id = pickup_id;
        this.dropoff_id = dropoff_id;
        this.datetime = datetime;
        this.price = price;
        this.note = note;
        this.status = status;
    }

    public int getDatabase_id() {
        return database_id;
    }

    public String getDriver_id() {
        return driver_id;
    }

    public String getPickup_name() {
        return pickup_name;
    }

    public String getDropoff_name() {
        return dropoff_name;
    }

    public LatLng getPickupLatLng() {
        return pickupLatLng;
    }

    public LatLng getDropoffLatLng() {
        return dropoffLatLng;
    }

    public String getPickup_id() {
        return pickup_id;
    }

    public String getDropoff_id() {
        return dropoff_id;
    }

    public String getDatetime() {
        return datetime;
    }

    public String getPrice() {
        return price;
    }

    public String getNote() {
        return note;
    }

    public int getStatus() {
        return status;
    }
}
