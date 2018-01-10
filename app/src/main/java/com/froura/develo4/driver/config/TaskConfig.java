package com.froura.develo4.driver.config;

/**
 * Created by KendrickCosca on 11/27/2017.
 */

public final class TaskConfig {
    public static final String HTTP_HOST = "http://192.168.1.9";
    public static final String DIR_URL = "/froura-web/mobile";
//    public static final String HTTP_HOST = "";
//    public static final String DIR_URL = "";
    public static final String DIR_ACTION_URL = DIR_URL + "/";
    public static final String CHECK_USER_URL = HTTP_HOST + DIR_ACTION_URL + "check_user";
}
