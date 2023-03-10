package musashique.tools.blescanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import musashique.tools.blescanner.R;
import musashique.tools.blescanner.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private MyScancallback scancallback;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;


    private final int PERMISSION_REQUEST = 100;

    private Handler handler;
    private final int SCAN_PERIOD = 10000;

    private BluetoothDevice device;


    class BLEDevices {
        public BLEDevices(String name, String address, String uuid, int rssi) {
            this.name = name;
            this.address = address;
            this.uuid = uuid;
            this.rssi = rssi;
            lasttime = System.currentTimeMillis();
        }

        public void refresh(int rssi) {
            this.rssi = rssi;
            lasttime = System.currentTimeMillis();
        }

        /**
         * ????????????10???????????????
         * @return
         */
        public boolean isExpire() {
            return lasttime < System.currentTimeMillis()-10*1000;
        }

        public String toString() {
            return String.format("Addr=[%s] Name=[%s] UUID=[%s] RSSI=[%d]", address, name, uuid, rssi);
        }

        public String name;
        public String address;
        public String uuid;

        public int rssi;

        public long lasttime;
    }

    HashMap<String, BLEDevices> devices = new HashMap<>();

    protected void requestPermission(String perm) {
        if (shouldShowRequestPermissionRationale(perm)) {
            Log.d("shouldShowRequestPermissionRationale", Manifest.permission.BLUETOOTH_SCAN);

        }

        int x = checkSelfPermission(perm);
        Log.d("checkSelfPermission + " + perm, x == 0 ? "OK" : "NG");
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            Log.d("requestPermission", perm);
            requestPermissions(new String[]{perm}, 0);
        }
    }


    protected void requestPermission(String[] perm) {
        requestPermissions(perm, 0);
    }

    Runnable rescan = new Runnable() {
        @Override
        public void run() {
            restartScan();
        }
    };


    public void restartScan() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                        return;
        }
        scanner.stopScan(scancallback);

        refreshList();

        //  ?????????????????????
        scanner.startScan(scancallback);

        handler.postDelayed(rescan, SCAN_PERIOD);

    }

    public void refreshList() {



        TextView textView = findViewById(R.id.DeviceList);
        StringBuffer sb = new StringBuffer();
        for (String key : devices.keySet()) {
            BLEDevices s = devices.get(key);
            if (s.isExpire()) {
                devices.remove(s.address);
                continue;
            }
            sb.append(s + "\n");
        }
        textView.setText(sb.toString());

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("onRequestPermissionsResult", permissions.toString());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        requestPermission(Manifest.permission.BLUETOOTH);
        requestPermission(Manifest.permission.BLUETOOTH_ADVERTISE);
        requestPermission(Manifest.permission.BLUETOOTH_CONNECT);
        requestPermission(Manifest.permission.BLUETOOTH_ADMIN);
//        requestPermission(Manifest.permission.BLUETOOTH_SCAN);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        //BLE??????????????????????????????????????????????????????????????????????????????????????????????????????
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        //Bluetooth?????????????????????????????????
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();


        //bluetooth??????????????????????????????????????????????????????????????????
        if (adapter == null || !adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(intent,PERMISSION_REQUEST);
        } else {


            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(enableBtIntent, 1);

            }


            scanner = adapter.getBluetoothLeScanner();
            scancallback = new MyScancallback();

            //?????????????????????10??????????????????????????????????????????
            handler = new Handler();
            handler.postDelayed(rescan, SCAN_PERIOD);

            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            List<ScanFilter> scanFilters = Arrays.asList(
                    new ScanFilter.Builder()
                            //.setServiceUuid(ParcelUuid.fromString("some uuid"))
                            .build());

            //?????????????????????
            scanner.startScan(scancallback);


        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    class MyScancallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice() == null) return;
            BluetoothDevice device = result.getDevice();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
//                requestPermissions(new String[] { Manifest.permission.BLUETOOTH_CONNECT }, 0);
//                return;
            }

            int rssi = result.getRssi();

            String address = device.getAddress();
            ParcelUuid[] UUIDs = device.getUuids();
            String devuuid = "";
            if (UUIDs != null) {
                for (ParcelUuid uuid : UUIDs) {
                    devuuid = uuid + " ";
                }
            }

            String name = device.getName();
            ScanRecord ssr = result.getScanRecord();
            String devname = ssr.getDeviceName();
            if (devname == null) devname = "No Name";
            List<ParcelUuid> uu = ssr.getServiceUuids();
            String uuid = "";
            if (uu != null) {
                for (ParcelUuid u : uu) {
                    uuid = u + " ";
                }
            }


            if (!uuid.isEmpty()) {
                BLEDevices dev;
                if (devices.containsKey(address)) {
                    dev = devices.get(address);
                    dev.refresh(rssi);
                } else {
                    dev = new BLEDevices(devname, address, uuid, rssi);
                }
                devices.put(address, dev);

                refreshList();

//                Log.d("scanResult", result.toString());
            }




//            textView.append(result.getDevice().getAddress()+" - "+result.getDevice().getName()+"\n");
        }


        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);


            Log.d("onScanFailed","errorCode = " + errorCode);
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    break;
        }

        }


    }
}