package com.froura.develo4.driver.config;

/**
 * Created by KendrickCosca on 11/27/2017.
 */

public final class TaskConfig {
    public static final String HTTP_HOST = "http://192.168.1.7";
    public static final String DIR_URL = "/froura-web/public/mobile";
//    public static final String HTTP_HOST = "";
//    public static final String DIR_URL = "";
    public static final String DIR_ACTION_URL = DIR_URL + "/";
    public static final String GET_DRIVER_DATA_URL = HTTP_HOST + DIR_ACTION_URL + "driver_data";
}
