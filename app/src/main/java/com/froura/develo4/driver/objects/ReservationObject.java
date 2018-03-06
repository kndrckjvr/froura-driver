package com.froura.develo4.driver.objects;

/**
 * Created by kjcosca on 06/03/2018.
 */

public class ReservationObject {
    private String reservation_id;
    private String passenger_id;
    private String pickup;
    private String dropoff;
    private String pickup_id;
    private String dropoff_id;
    private String pickupLat;
    private String pickupLng;
    private String dropoffLat;
    private String dropoffLng;
    private String duration;
    private String date;
    private String price;
    private String notes;

    public ReservationObject(String reservation_id, String passenger_id, String pickup, String dropoff, String pickup_id, String dropoff_id, String pickupLat, String pickupLng, String dropoffLat, String dropoffLng, String duration, String date, String price, String notes) {
        this.reservation_id = reservation_id;
        this.passenger_id = passenger_id;
        this.pickup = pickup;
        this.dropoff = dropoff;
        this.pickup_id = pickup_id;
        this.dropoff_id = dropoff_id;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.duration = duration;
        this.date = date;
        this.price = price;
        this.notes = notes;
    }

    public String getReservation_id() {
        return reservation_id;
    }

    public String getPassenger_id() {
        return passenger_id;
    }

    public String getPickup() {
        return pickup;
    }

    public String getDropoff() {
        return dropoff;
    }

    public String getPickup_id() {
        return pickup_id;
    }

    public String getDropoff_id() {
        return dropoff_id;
    }

    public String getPickupLat() {
        return pickupLat;
    }

    public String getPickupLng() {
        return pickupLng;
    }

    public String getDropoffLat() {
        return dropoffLat;
    }

    public String getDropoffLng() {
        return dropoffLng;
    }

    public String getDuration() {
        return duration;
    }

    public String getDate() {
        return date;
    }

    public String getPrice() {
        return price;
    }

    public String getNotes() {
        return notes;
    }
}
