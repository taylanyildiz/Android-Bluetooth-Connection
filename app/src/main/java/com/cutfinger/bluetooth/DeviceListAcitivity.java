package com.cutfinger.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DeviceListAcitivity extends AppCompatActivity {

    private static final String TAG = "DeviceListActivity";
    public static final String EXTRA_DEVICE_ADRESS = "device_adress";
    private static final String NO_DEVICES_PAIRED = "Eşleştirilmiş Cihaz Yok";
    private static final String NO_DEVICES_NEW = "Yeni Cihaz Bulunmadı";
    private static final int REQUEST_ACCES_COARSE_LOCATION = 1;

    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> pairedDevices;

    private ArrayAdapter<String> newDevicesAdapter;
    private ArrayAdapter<String> pairedDevicesAdapter;

    private Button button_scan;
    private ListView pairedDevicesListView,newDevicesListView;
    private TextView textViewPairedDevice,textViewNewDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);
        setResult(Activity.RESULT_CANCELED);
        findViewByIds();
        checkCoarseLocationPermission();
        listDevices();
        BluetoothFilter();
        listPairedDevices();

        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
    }

    private void listDevices(){
        pairedDevicesAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        newDevicesAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        pairedDevicesListView.setAdapter(pairedDevicesAdapter);
        pairedDevicesListView.setOnItemClickListener(deviceListener);
        newDevicesListView.setAdapter(newDevicesAdapter);
        newDevicesListView.setOnItemClickListener(deviceListener);
    }

    private void BluetoothFilter(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(btReceiver,filter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private boolean checkCoarseLocationPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCES_COARSE_LOCATION);
            return false;
        }else{
            return true;
        }

    }



    private void listPairedDevices(){
        pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            textViewPairedDevice.setVisibility(View.VISIBLE);
            for(BluetoothDevice device : pairedDevices){
                pairedDevicesAdapter.add(device.getName()+"\n"+device.getAddress());
                pairedDevicesAdapter.notifyDataSetChanged();
            }
        }else{

            pairedDevicesAdapter.add(NO_DEVICES_PAIRED);
        }
    }

    private void doDiscovery(){
        setProgressBarIndeterminateVisibility(true);
        setTitle("Cihaz aranıyor...");
        textViewNewDevice.setVisibility(View.VISIBLE);
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    newDevicesAdapter.add(device.getName()+"\n"+device.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setProgressBarIndeterminateVisibility(false);
                Toast.makeText(getApplicationContext(),"Arama Bitti",Toast.LENGTH_SHORT).show();
                setTitle("Cihaz Seçiniz");
                if(newDevicesAdapter.getCount() == 0){
                    newDevicesAdapter.add(NO_DEVICES_NEW);
                }
            }
        }
    };

    private AdapterView.OnItemClickListener deviceListener =
            new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    btAdapter.cancelDiscovery();

                    String info = ((TextView) view).getText().toString();
                    String adress = info.substring(info.length()-17);
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_DEVICE_ADRESS,adress);
                    setResult(Activity.RESULT_OK,intent);
                    btAdapter.cancelDiscovery();
                    finish();
                }
            };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(btAdapter != null){
            btAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(btReceiver);
    }

    private void findViewByIds(){
        button_scan = (Button) findViewById(R.id.button_scan);
        pairedDevicesListView = (ListView) findViewById(R.id.devices_paired);
        newDevicesListView = (ListView) findViewById(R.id.devices_new);
        textViewPairedDevice = (TextView) findViewById(R.id.title_paired);
        textViewNewDevice = (TextView) findViewById(R.id.title_new);
    }
}