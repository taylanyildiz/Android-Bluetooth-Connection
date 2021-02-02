package com.cutfinger.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SharedPreferences sp ;
    private SharedPreferences.Editor editor;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;
    private static final int FAILED = 3;
    private static final int RECEIVED = 4;
    private static final int BT_ENABLE = 1;
    private static final int DEVICE_LIST = 2;
    private static final int SEND_SMS_REQUEST_CODE = 1;
    private SmsManager smsManager;
    private static final String APP_NAME = "HC_06";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");
    private BluetoothAdapter adapter;
    private BluetoothDevice device;
    private Intent blueIntent;
    private Intent deviceIntent;
    private SendReceive sendReceive;
    private ImageView imageViewBluetooth;
    private TextView textViewBlueDurum,textViewadressInfo,textViewBaglantıDurum,textViewBlueData,textViewBlueData2,textViewSmsdata;
    private FloatingActionButton bluetooth,addPhone;
    private ConstraintLayout constraintLayoutDinleyici;
    private View viewPhone;
    private EditText editText1,editText2,editText3,editText4,editText5;
    private String adress;
    private int i = 0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BT_ENABLE){
            if(resultCode == RESULT_OK){
                Toast.makeText(getApplicationContext(),"Bluetooth Açıldı",Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == DEVICE_LIST){
            if(resultCode == RESULT_OK){
                adress = data.getExtras().getString(DeviceListAcitivity.EXTRA_DEVICE_ADRESS);
                device =adapter.getRemoteDevice(adress);
                mes("Cihaza Bağlanılıyor.."+"\nadres : "+device.getAddress()+"\ncihaz : "+device.getName());
                ClientClass clientClass = new ClientClass(device);
                clientClass.start();
                ServerClientClass serverClass=new ServerClientClass();
                serverClass.start();

            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Cihaz Seçilmedi",Toast.LENGTH_SHORT).show();
            }
        }

    }
    private class ClientClass extends Thread {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device){
            this.device = device;
            try {
                socket=device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                socket.connect();
                Message message2=Message.obtain();
                message2.what = CONNECTED;
                handler.sendMessage(message2);
                sendReceive=new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what = FAILED;
                handler.sendMessage(message);
            }
        }
    }
    private class ServerClientClass extends Thread {
        private BluetoothServerSocket serverSocket;
        public ServerClientClass(){
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what = FAILED;
                handler.sendMessage(message);
            }
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while(socket == null){
                try{
                    Message message=Message.obtain();
                    message.what=CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                }catch(Exception e){
                    Message message=Message.obtain();
                    message.what=FAILED;
                    handler.sendMessage(message);
                }
                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=CONNECTED;
                    handler.sendMessage(message);
                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    break;
                }

            }
        }
    }
    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    i++;
                    handler.obtainMessage(RECEIVED, bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(int bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    String tempMsg,data1,data2;
    int s = 0;
    private Handler handler = new Handler(new Handler.Callback(){

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case FAILED:
                    textViewBaglantıDurum.setText("Bağlantı Durum : Başarısız");
                    textViewadressInfo.setText("-------");
                    textViewBlueData.setText("--------------------");
                    textViewSmsdata.setText("---------------------");
                    constraintLayoutDinleyici.setVisibility(View.INVISIBLE);
                    break;
                case CONNECTING:
                    textViewBaglantıDurum.setText("Bağlantı Durum : Bağlanılıyor");
                    textViewadressInfo.setText("-------");
                    textViewBlueData.setText("--------------------");
                    textViewSmsdata.setText("---------------------");
                    constraintLayoutDinleyici.setVisibility(View.INVISIBLE);
                    break;
                case CONNECTED:
                    textViewBaglantıDurum.setText("Bağlantı Durum :Başarılı");
                    constraintLayoutDinleyici.setVisibility(View.VISIBLE);
                    textViewadressInfo.setText(adress);
                    textViewBlueData.setText("--------------------");
                    textViewSmsdata.setText("---------------------");
                    break;
                case RECEIVED:
                    textViewBaglantıDurum.setText("Bağlantı Durum : Dinleniyor");
                    byte[] readBuff1= (byte[]) msg.obj;
                    tempMsg = new String(readBuff1, 0, msg.arg1);
                    if(i == 1){
                        textViewBlueData.setText(tempMsg);
                        data1 = tempMsg;
                    }
                    if(i == 2){
                        textViewBlueData2.setText(tempMsg);
                        data2 = tempMsg;
                        i = 0;
                        if(data2.equals("100")){
                            textViewSmsdata.setText("Mesaj Gönderiliyor\nPort : "+data1+"\nEnerji Bilgisi :"+data2);
                            if(n1 != null){
                                if(!n1.isEmpty()) {
                                    smsManager.sendTextMessage(n1,null,data1+" Numaralı Portta " +
                                            "Elektrik Kesintisi Var",null,null);
                                }
                            }
                            if(n2 != null){
                                if(!n2.isEmpty()) {
                                    smsManager.sendTextMessage(n2,null,data1+" Numaralı Portta " +
                                            "Elektrik Kesintisi Var",null,null);
                                }
                            }
                            if(n3 != null){
                                if(!n3.isEmpty()) {
                                    smsManager.sendTextMessage(n3,null,data1+" Numaralı Portta " +
                                            "Elektrik Kesintisi Var",null,null);
                                }
                            }
                            if(n4 != null){
                                if(!n4.isEmpty()) {
                                    smsManager.sendTextMessage(n4,null,data1+" Numaralı Portta " +
                                            "Elektrik Kesintisi Var",null,null);
                                }
                            }
                            if(n5 != null){
                                if(!n5.isEmpty()) {
                                    smsManager.sendTextMessage(n5,null,data1+" Numaralı Portta " +
                                            "Elektrik Kesintisi Var",null,null);
                                }
                            }
                        }
                        else if(data2.equals("200")){
                            textViewSmsdata.setText("Mesaj Gönderiliyor\nPort : "+data1+"\nEnerji Bilgisi :"+data2);
                            if(n1 != null){
                                if(!n1.isEmpty()) {
                                    smsManager.sendTextMessage(n1,null,data1+" Numaralı Port'a " +
                                            "Elektrik Geldi",null,null);
                                }
                            }
                            if(n2 != null){
                                if(!n2.isEmpty()) {
                                    smsManager.sendTextMessage(n2,null,data1+" Numaralı Port'a " +
                                            "Elektrik Geldi",null,null);
                                }
                            }
                            if(n3 != null){
                                if(!n3.isEmpty()) {
                                    smsManager.sendTextMessage(n3,null,data1+" Numaralı Port'a " +
                                            "Elektrik Geldi",null,null);
                                }
                            }
                            if(n4 != null){
                                if(!n4.isEmpty()) {
                                    smsManager.sendTextMessage(n4,null,data1+" Numaralı Port'a " +
                                            "Elektrik Geldi",null,null);
                                }
                            }
                            if(n5 != null){
                                if(!n5.isEmpty()) {
                                    smsManager.sendTextMessage(n5,null,data1+" Numaralı Port'a " +
                                            "Elektrik Geldi",null,null);
                                }
                            }

                        }
                    }

                    break;
            }
            return true;
        }
    });

    private int b = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ById();
        checkBluetoothAvailable();
        checkPermissionSMS();
        constraintLayoutDinleyici.setVisibility(View.VISIBLE);
        sp = getSharedPreferences("Phone",MODE_PRIVATE);
        editor = sp.edit();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                b++;
                if(b%2 == 0){
                    sendReceive.write(97);
                    new Handler().postDelayed(new Runnable(){
                        @Override
                        public void run() {

                            sendReceive.write(49);
                            sendReceive.write(48);
                            sendReceive.write(48);
                        }
                    },100);
                }
                if(b%2 == 1){
                    sendReceive.write(98);
                    new Handler().postDelayed(new Runnable(){
                        @Override
                        public void run() {

                            sendReceive.write(50);
                            sendReceive.write(48);
                            sendReceive.write(48);
                        }
                    },100);
                }
            }
        });
        bluetooth.setOnClickListener(this);
        addPhone.setOnClickListener(this);

    }
    private void ById(){
        textViewBlueData2 = findViewById(R.id.textViewBlueData2);
        textViewBlueDurum = findViewById(R.id.textViewBlueDurum);
        imageViewBluetooth = findViewById(R.id.imageViewBluetooth);
        bluetooth = findViewById(R.id.bluetooth);
        addPhone = findViewById(R.id.addPhone);
        constraintLayoutDinleyici = findViewById(R.id.constraintLayoutDinleyici);
        textViewadressInfo = findViewById(R.id.textViewadressInfo);
        textViewBaglantıDurum = findViewById(R.id.textViewBaglantıDurum);
        textViewBlueData = findViewById(R.id.textViewBlueData);
        textViewSmsdata = findViewById(R.id.textViewSmsdata);
    }
    @Override
    protected void onResume() {
        super.onResume();
        n1 = sp.getString("number1","");
        n2 = sp.getString("number2","");
        n3 = sp.getString("number3","");
        n4 = sp.getString("number4","");
        n5 = sp.getString("number5","");
        if(adapter != null && !adapter.isEnabled()){
            mes("Bluetooth Açılıyor...");
            startActivityForResult(blueIntent,BT_ENABLE);
            mes("Bluetooth Açıldı");
            textViewBlueDurum.setText("AÇIK");
            imageViewBluetooth.setImageResource(R.drawable.bluetooth_on);
        }else if(adapter == null){
            mes("Bluetooth Açılamadı");
        }
    }
    private String n1,n2,n3,n4,n5;
    private ArrayList<String> numbers;
    private void addPhoneNumber(){
        numbers = new ArrayList<String>();
        viewPhone = getLayoutInflater().inflate(R.layout.phone_number,null);
        editText1 = viewPhone.findViewById(R.id.editTextNumber1);
        editText2 = viewPhone.findViewById(R.id.editTextNumber2);
        editText3 = viewPhone.findViewById(R.id.editTextNumber3);
        editText4 = viewPhone.findViewById(R.id.editTextNumber4);
        editText5 = viewPhone.findViewById(R.id.editTextNumber5);
        editText1.setText(sp.getString("number1",""));
        editText2.setText(sp.getString("number2",""));
        editText3.setText(sp.getString("number3",""));
        editText4.setText(sp.getString("number4",""));
        editText5.setText(sp.getString("number5",""));
        AlertDialog.Builder phoneBuild = new AlertDialog.Builder(MainActivity.this);
        phoneBuild.setTitle("Telefon Numarası Ekleyin");
        phoneBuild.setIcon(R.drawable.ic_baseline_contact_phone_24);
        phoneBuild.setView(viewPhone);
        phoneBuild.setPositiveButton("Kaydet",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                n1 = editText1.getText().toString();
                n2 = editText2.getText().toString();
                n3 = editText3.getText().toString();
                n4 = editText4.getText().toString();
                n5 = editText5.getText().toString();

                if(!n1.isEmpty()){
                    editor.putString("number1",n1);
                    editor.commit();
                }else{
                    editor.remove("number1");
                    n1="";
                    editor.commit();
                }
                if(!n2.isEmpty()){
                    editor.putString("number2",n2);
                    editor.commit();
                }else{
                    editor.remove("number2");
                    n2="";
                    editor.commit();
                }
                if(!n3.isEmpty()){
                    editor.putString("number3",n3);
                    editor.commit();
                }else{
                    editor.remove("number3");
                    n3="";
                    editor.commit();
                }
                if(!n4.isEmpty()){
                    editor.putString("number4",n4);
                    editor.commit();
                }else{
                    editor.remove("number4");
                    n4="";
                    editor.commit();
                }
                if(!n5.isEmpty()){
                    editor.putString("number5",n5);
                    editor.commit();
                }else{
                    editor.remove("number5");
                    n5="";
                    editor.commit();
                }
            }
        } );
        phoneBuild.create().show();

    }
    public void checkPermissionSMS(){
        ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.SEND_SMS},
                SEND_SMS_REQUEST_CODE );
        smsManager = SmsManager.getDefault();
    }
    private void mes(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void checkBluetoothAvailable(){
        adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null){
            blueIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            blueControl();
        }
        else if(adapter == null){
            mes("Bluetooth Özelliği Bulunamaktadır..");
            textViewBlueDurum.setText("------");
            imageViewBluetooth.setImageResource(R.drawable.bluetooth_off);
        }
    }
    private void blueControl(){
        if(adapter.isEnabled()){
            mes("Bluetooth Açık");
            textViewBlueDurum.setText("AÇIK");
            imageViewBluetooth.setImageResource(R.drawable.bluetooth_on);
        }
        if(!adapter.isEnabled()){
            mes("Bluetooth Kapalı");
            textViewBlueDurum.setText("KAPALI");
            imageViewBluetooth.setImageResource(R.drawable.bluetooth_off);
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bluetooth:
                if(adapter != null ){
                    deviceIntent = new Intent(MainActivity.this,DeviceListAcitivity.class);
                    startActivityForResult(deviceIntent,DEVICE_LIST);
                }else{
                    mes("Bluetooth Özelliği Bulunamadı");
                }
                break;
            case R.id.addPhone:
                addPhoneNumber();
                break;
        }
    }

}