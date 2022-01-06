package com.example.magwifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;


public class WiFiManagent {
    private WifiManager wifiManager = null;
    private ArrayList<ScanResult> wifiList = new ArrayList<>();
    WiFiManagent(Context context){
        wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager !=null;
    }
    private void checkAndOpenWiFi(){
        if (!wifiManager.isWifiEnabled()){
            wifiManager.isWifiEnabled();
            wifiManager.setWifiEnabled(true);
        }
    }
    private void startScan(){
        wifiManager.startScan();
        wifiList = (ArrayList<ScanResult>) wifiManager.getScanResults();
    }

    /**
     * 打开并扫描WiFi信号，将结果存储在列表里
     */
    void scanWiFi(){
        checkAndOpenWiFi();
        startScan();
    }

    /**
     * @return 信号列表
     */
    ArrayList<ScanResult>getWifiList(){
        return wifiList;
    }

    /**
     * @return Mac地址+信号强度+名称 的数组列表
     */
    ArrayList<String> getBasicInfo(){
        if (wifiList.isEmpty())
            return null;
        ArrayList<String>wifiInfo = new ArrayList<>();
        for (ScanResult e:wifiList){
            wifiInfo.add(e.BSSID +" | "+ e.SSID +" : "+ e.level);
        }
        return wifiInfo;
    }
}

