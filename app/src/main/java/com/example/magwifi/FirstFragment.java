package com.example.magwifi;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class FirstFragment extends Fragment {
    private SensorManager sensorManager;
    private Sensor sensor;
    double xm=0,ym=0,zm=0,timestamp=0;

    private ListView wifiListView;
    private WiFiManagent wiFiManagent;
    private String fileName = null;
    private boolean isNewFile = true;
    private boolean isRecording = false;

    private Float x = 0f;
    private Float y = 0f;
    private Float z = 0f;

    private ArrayList<String> bledata_compare = null; //用于比较
    private ArrayList<String> bledata = null;    //用于显示
    private ArrayList<String> bledata2 = null;    //用于存储
    ArrayAdapter<String> adapter = null;
    private ListView BLEListView;

    //获取蓝牙适配器
    private BluetoothAdapter bleadapter = BluetoothAdapter.getDefaultAdapter();

    //开启蓝牙
    private void enableBle(){
        if(!bleadapter.isEnabled()){
            bleadapter.enable();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
        sensorManager= (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensor=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //设置传感器的监听
        sensorManager.registerListener(MSensorListener, sensor,sensorManager.SENSOR_DELAY_FASTEST);

        Button scanWiFi = getActivity().findViewById(R.id.scanwifi);
        Button scanMag = getActivity().findViewById(R.id.scanmag);
        Button scanBLE = getActivity().findViewById(R.id.scanbluetooth);
        Button write2FileButton = getActivity().findViewById(R.id.save);
        EditText XeditText = getActivity().findViewById(R.id.xeditTextNumber);
        EditText YeditText = getActivity().findViewById(R.id.yeditTextNumber);
        EditText ZeditText = getActivity().findViewById(R.id.zeditTextNumber);

        wifiListView = getActivity().findViewById(R.id.wifilist);
        wiFiManagent = new WiFiManagent(getActivity());
        BLEListView = getActivity().findViewById(R.id.Bluetoothlist);
        getActivity().requestPermissions(new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION},1);


        scanWiFi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collecting_WiFi();
            }
        });
        scanMag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collecting_Mag();
            }
        });
        scanBLE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collecting_BLE();
            }
        });
        write2FileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //获取坐标
                x = Float.valueOf(XeditText.getText().toString());
                y = Float.valueOf(YeditText.getText().toString());
                z = Float.valueOf(ZeditText.getText().toString());
                //写入文件
                if (isNewFile){
                    fileName = getCurrentTime()+".txt";
                    isNewFile =false;
                }
                try {
                    write2File(fileName);
                    Toast.makeText(getActivity(),"内容已写入"+fileName,Toast.LENGTH_SHORT).show();
                }catch (IOException e){
                    Log.e("写入文件出错",e.toString());
                }
            }
        });
    }
    String getCurrentTime(){
        Date date = new Date(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        return simpleDateFormat.format(date);
    }
    public void onPause(){
//        sensorManager.unregisterListener(MSensorListener);
        super.onPause();
    }

//    写入文件
    void write2File(String fileName) throws IOException {
        ArrayList<ScanResult> wifiList = wiFiManagent.getWifiList();
        File file = new File(this.getActivity().getExternalFilesDir(null),fileName);
        FileWriter fileWriter = new FileWriter(file,!isNewFile);
        fileWriter.write(timestamp+"|"+x + "|" + y + "|" + z + "|"+xm + "|" + ym + "|" + zm + ' ');
        StringBuilder stringBuilder = new StringBuilder();
        for (ScanResult e:wifiList){
            stringBuilder.append(e.BSSID).append('|').append(e.level).append(' ');
        }
        try {
            stringBuilder.append("# ");
            for (int i = 0; i < bledata2.size(); i++) {
                stringBuilder.append(bledata2.get(i)).append(' ');
            }
        } catch (Exception e) {
            Log.e("写入ble文件出错",e.toString());
            e.printStackTrace();
        }
        stringBuilder.append("\r");
        fileWriter.write(stringBuilder.toString());
        fileWriter.flush();
        fileWriter.close();
    }


//    WiFi数据采集
    private void collecting_WiFi(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        System.out.println(wiFiManagent.getBasicInfo());
        wiFiManagent.scanWiFi();
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, wiFiManagent.getBasicInfo());
        System.out.println(arrayAdapter);
        wifiListView.setAdapter(arrayAdapter);
    }

//    地磁数据采集
    final SensorEventListener MSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //获取传感器不断变化的值
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                xm = sensorEvent.values[0];
                ym = sensorEvent.values[1];
                zm = sensorEvent.values[2];
                timestamp=sensorEvent.timestamp;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private void collecting_Mag(){
        //获取传感器的类型分辨率和延迟
//        String s="名称"+sensor.getName()+
//                "类型"+sensor.getType()+
//                "分辨率"+sensor.getResolution()+
//                "延迟"+sensor.getMaxDelay();
//        showMessage(s);
        TextView textView= (TextView) getActivity().findViewById(R.id.textView);
        String s2 = "时间: "+timestamp+"\n磁场强度 X轴: " + xm +
                "  Y轴: " + ym +
                "  Z轴: " + zm;
        textView.setText(s2);
    }
    private void showMessage(String message){
        View view=getActivity().findViewById(R.id.cam);
        final Snackbar snackbar=Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("关闭", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }
//    蓝牙数据采集
    private void collecting_BLE(){
        //创建容器，用于数据传递
        this.bledata_compare = new ArrayList<>();
        this.bledata = new ArrayList<>();
        this.bledata2 = new ArrayList<>();
        //开启蓝牙
        enableBle();
        //开始扫描
        bleadapter.startLeScan(leScanCallback);
        adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_list_item_1, this.bledata);
        //显示
        BLEListView.setAdapter(adapter);
    }
    //数据回传接口
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            final ble_device ble_device = new ble_device();  //此处为新建的一个类,为ble_device
            ble_device.ble_address = bluetoothDevice.getAddress();

            if(bledata_compare.contains(ble_device.ble_address)) {
            }   //若列表中已经有了相应设备信息，则不添加进去
            else {
                bledata_compare.add(ble_device.ble_address);
                bledata.add(bluetoothDevice.getAddress()+" | "+bluetoothDevice.getName()+" : "+rssi);
                bledata2.add(bluetoothDevice.getAddress()+"|"+rssi);
                adapter.notifyDataSetChanged();
            }
        }
    };

}