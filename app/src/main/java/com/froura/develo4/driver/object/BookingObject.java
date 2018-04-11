package com.froura.develo4.driver.object;


import com.mapbox.mapboxsdk.geometry.LatLng;

/**
 * Created by KendrickAndrew on 07/02/2018.
 */

public class BookingObject {
    private String uid;
    private String pickup;
    private String dropoff;
    private String fare;
    private LatLng pickupLatLng;
    private LatLng dropoffLatLng;

    public BookingObject(String uid, String pickup, String dropoff, String fare, LatLng pickupLatLng, LatLng dropoffLatLng) {
        this.uid = uid;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.fare = fare;
        this.pickupLatLng = pickupLatLng;
        this.dropoffLatLng = dropoffLatLng;
    }

    public String getUid() {
        return uid;
    }

    public String getPickup() {
        return pickup;
    }

    public String getDropoff() {
        return dropoff;
    }

    public String getFare() {
        return fare;
    }

    public LatLng getPickupLatLng() {
        return pickupLatLng;
    }

    public LatLng getDropoffLatLng() {
        return dropoffLatLng;
    }
}
