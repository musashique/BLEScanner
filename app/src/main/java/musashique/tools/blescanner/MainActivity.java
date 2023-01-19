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


    HashMap<String, String> devices = new HashMap<>();

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


        /*
        requestPermission(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN});

        requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        requestPermission(Manifest.permission.BLUETOOTH);
        requestPermission(Manifest.permission.BLUETOOTH_CONNECT);
        requestPermission(Manifest.permission.BLUETOOTH_ADMIN);
        requestPermission(Manifest.permission.BLUETOOTH_SCAN);


        if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.d("shouldShowRequestPermissionRationale", Manifest.permission.BLUETOOTH_SCAN);

        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("No Permission", Manifest.permission.BLUETOOTH_SCAN);
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
//            return;
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("No Permission", Manifest.permission.BLUETOOTH_SCAN);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            return;
        }


         */


/*
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, 0);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 0);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 0);
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 0);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

         */


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        //BLE対応端末かどうかを調べる。対応していない場合はメッセージを出して終了
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        //Bluetoothアダプターを初期化する
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();


        //bluetoothの使用が許可されていない場合は許可を求める。
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

            //スキャニングを10秒後に停止
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    scanner.stopScan(scancallback);
                    finish();
                }
            }, SCAN_PERIOD);

            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            List<ScanFilter> scanFilters = Arrays.asList(
                    new ScanFilter.Builder()
                            //.setServiceUuid(ParcelUuid.fromString("some uuid"))
                            .build());

            //スキャンの開始
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

            int rss = result.getRssi();

            String address = device.getAddress();

            ScanRecord sr = result.getScanRecord();

            ParcelUuid[] UUIDs = device.getUuids();
            if (UUIDs != null) {
                for (ParcelUuid uuid : UUIDs) {

                }
            }
            String name = device.getName();
            if (name != null) {
                ScanRecord ssr = result.getScanRecord();
                List<ParcelUuid> uu = ssr.getServiceUuids();
                String uuid = "";
                if (uu != null) {
                    for (ParcelUuid u : uu) {
                        uuid = u + " ";
                    }
                }
                Object devname = ssr.getDeviceName();

                SparseArray<byte[]> ms =  ssr.getManufacturerSpecificData();
                if (ms != null) {
                    for (int i=0; i<ms.size(); i++) {
                        byte[] bb = ms.valueAt(i);
                        String msd = new String(bb);
//                        Log.d("msd", msd);
                    }
                }


                String value = String.format("Address=[%s] Device=[%s] UUID=[%s] RSS=[%d]", address, devname, uuid, rss);
                devices.put(address, value);

                TextView textView = findViewById(R.id.DeviceList);
                StringBuffer sb = new StringBuffer();
                for (String s : devices.values()) {
                    sb.append(s + "\n");
                }
                textView.setText(sb.toString());


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