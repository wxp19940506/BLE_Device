package com.example.xiaopengwang.ble_device;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xiaopengwang.ble_device.adapters.LeDeviceListAdapter;
import com.example.xiaopengwang.ble_device.service.BluetoothLeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    // 扫描蓝牙按钮
    private Button scan;
    //链接状态
    private TextView link_state;
    // 蓝牙适配器
    BluetoothAdapter mBluetoothAdapter;
    //蓝牙连接状态
    private boolean mConnected = false;
    private String status = "disconnected";
//    // 蓝牙信号强度
//    private ArrayList<Integer> rssis;
    // 自定义Adapter
    LeDeviceListAdapter mleDeviceListAdapter;
    // listview显示扫描到的蓝牙信息
    ListView lv;
    TextView no_data;
    View bottomView;
    // 描述扫描蓝牙的状态
    private boolean mScanning;
    private boolean scan_flag;
    private Handler mHandler;
    int REQUEST_ENABLE_BT = 1;
    private AlertDialog dialog;
    // 蓝牙扫描时间
    private static final long SCAN_PERIOD = 10000;
    private boolean isShow = false;
    //保存两次的数据，对比序号，是否丢包
    private ArrayList saveData,saveListData ;
    //蓝牙service,负责后台的蓝牙服务
    private static BluetoothLeService mBluetoothLeService;
    //蓝牙4.0的UUID,其中0000ffe1-0000-1000-8000-00805f9b34fb是广州汇承信息科技有限公司08蓝牙模块的UUID
    public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    ;
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String EXTRAS_DEVICE_RSSI = "RSSI";

    //蓝牙名字
    private String mDeviceName;
    //蓝牙地址
    private String mDeviceAddress;
    //蓝牙信号值
    private Integer mRssi;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    //蓝牙特征值
    private static BluetoothGattCharacteristic target_chara = null;
    private Handler mhandler = new Handler();
    //显示折线图的控件
    private WebView web_echarts;
    private Handler myHandler = new Handler() {
        // 2.重写消息处理函数
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // 判断发送的消息
                case 1: {
                    // 更新View
                    String state = msg.getData().getString("connect_state");
                    link_state.setText(state);

                    break;
                }

            }
            super.handleMessage(msg);
        }

    };
    //引导字节的低四位
    String fourHighBit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstStep();
    }

    private void firstStep() {
        //初始化控件
        initView();
        // 初始化蓝牙
        init_ble();
        scan_flag = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bind_blue_tooth();
    }

    private void bind_blue_tooth(){
        //绑定广播接收器
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            //根据蓝牙地址，建立连接
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d("request", "Connect request result=" + result);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解除广播接收器
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }
    /* 更新连接状态 */
    private void updateConnectionState(String status) {
        Message msg = new Message();
        msg.what = 1;
        Bundle b = new Bundle();
        b.putString("connect_state", status);
        msg.setData(b);
        //将连接状态更新的UI的textview上
        myHandler.sendMessage(msg);
        System.out.println("connect_state:" + status);

    }
    /* 意图过滤器 */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 广播接收器，负责接收BluetoothLeService类发送的数据
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))//Gatt连接成功
            {
                mConnected = true;
                status = "已连接";
                //更新连接状态
                updateConnectionState(status);
                System.out.println("BroadcastReceiver :" + "device connected");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED//Gatt连接失败
                    .equals(action)) {
                mConnected = false;
                status = "连接失败，请重新扫描";
                mleDeviceListAdapter.clear();
                //更新连接状态
                updateConnectionState(status);
                System.out.println("BroadcastReceiver :"
                        + "device disconnected");

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED//发现GATT服务器
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //获取设备的所有蓝牙服务
                displayGattServices(mBluetoothLeService
                        .getSupportedGattServices());
                System.out.println("BroadcastReceiver :"
                        + "device SERVICES_DISCOVERED");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))//有效数据
            {
                //处理发送过来的数据
                ArrayList s = intent.getExtras().getStringArrayList(
                        BluetoothLeService.EXTRA_DATA);
                // TODO: 2018/4/14  处理蓝牙的数据
                Log.e("data",s.toString());
                display_data(s);

                //数据转化


//                displayData(s );

//                System.out.println("BroadcastReceiver onData:"
//                        + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void display_data(ArrayList s) {
        if (s.size()==10){

            saveData.add(s.get(1));
            if (saveData.size()>2){
                saveData.remove(0);
            }
            web_echarts.loadUrl("file:///android_asset/showEcharts.html");

            final ArrayList trans = transByteToStr(s);
            web_echarts.loadUrl("javascript:showData('" + trans + "');");

            web_echarts.setWebViewClient(new WebViewClient(){
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
//                            if (trans !=null)
                }
            });
        }
    }


    //处理数据

    private ArrayList transByteToStr(ArrayList s) {
        List nums = new ArrayList();
        for (int a = 0; a < s.size(); a++) {
            if (a==0){
                //引导字节的低四位
                fourHighBit =String.valueOf(((String)s.get(a)).charAt(1)).trim();

            }else if (a ==1){
                if (s.get(1) !="63"&&saveData.size()==2 && Integer.parseInt(((String)saveData.get(1)).trim(),16)-Integer.parseInt(((String)saveData.get(0)).trim(),16)>1)
                {
                    Toast.makeText(this,"有丢包现象",Toast.LENGTH_LONG).show();
                }
            }else if( a==3){
                nums.add(((String)s.get(2)).trim()+fourHighBit+s.get(2));
            }else if(a==5){
                nums.add(((String)s.get(4)).trim()+fourHighBit+s.get(5));
            }else if( a==7){
                nums.add(((String)s.get(6)).trim()+fourHighBit+s.get(7));
            }else if(a==9){
                nums.add(((String)s.get(8)).trim()+fourHighBit+s.get(9));
            }
//            String aBinary =hexString2binaryString(String.format("%02X ", s.get(a)));
        }

        if (nums!=null && nums.size()==4){

            for (int a=0;a<nums.size();a++) saveListData.add(Integer.parseInt(((String)nums.get(a)).trim(),16));
            if (saveListData.size()>24){
                saveListData.remove(0);
                saveListData.remove(0);
                saveListData.remove(0);
                saveListData.remove(0);
            }

        }


        return saveListData;
    }

    /**
     * @Title: displayGattServices
     * @Description: TODO(处理蓝牙服务)
     * @param
     * @return void
     * @throws
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";

        // 服务数据,可扩展下拉列表的第一级数据
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // 特征数据（隶属于某一级服务下面的特征值集合）
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

        // 部分层次，所有特征值集合
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            // 获取服务列表
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            // 查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

            gattServiceData.add(currentServiceData);

            System.out.println("Service uuid:" + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

            // 从当前循环所指向的服务中读取特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            // 对于当前循环所指向的服务中的每一个特征值
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                if (gattCharacteristic.getUuid().toString()
                        .equals(HEART_RATE_MEASUREMENT)) {
                    // 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                    mhandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            mBluetoothLeService
                                    .readCharacteristic(gattCharacteristic);
                        }
                    }, 200);

                    // 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    mBluetoothLeService.setCharacteristicNotification(
                            gattCharacteristic, true);
                    target_chara = gattCharacteristic;
                    // 设置数据内容
                    // 往蓝牙模块写入数据
                    // mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                }
                List<BluetoothGattDescriptor> descriptors = gattCharacteristic
                        .getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    System.out.println("---descriptor UUID:"
                            + descriptor.getUuid());
                    // 获取特征值的描述
                    mBluetoothLeService.getCharacteristicDescriptor(descriptor);
                    // mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    // true);
                }

                gattCharacteristicGroupData.add(currentCharaData);
            }
            // 按先后顺序，分层次放入特征值集合中，只有特征值
            mGattCharacteristics.add(charas);
            // 构件第二级扩展列表（服务下面的特征值）
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

    }


    @Override
    public void onClick(View view) {
// TODO Auto-generated method stub

        if (scan_flag)
        {
            scanLeDevice(true);
            showDialog();
        } else
        {
            // TODO: 2018/4/19 优化搜索设备机制

            scanLeDevice(false);
            scan.setText("扫描设备");
//            if (mleDeviceListAdapter.rssis.size() >0)
            //解除广播接收器
            unregisterReceiver(mGattUpdateReceiver);
            mBluetoothLeService = null;
            firstStep();
            bind_blue_tooth();

//            link_state.setText("连接中.. ");
        }
    }
    //初始化控件
    private void initView() {
        mleDeviceListAdapter = new LeDeviceListAdapter(MainActivity.this);
        scan = findViewById(R.id.scan);
        link_state = findViewById(R.id.link_state);
        web_echarts = (WebView) findViewById(R.id.wb_echarts);

        mHandler = new Handler();
        scan.setOnClickListener(this);
        saveData = new ArrayList();
        //存储传到charts的数据
        saveListData = new ArrayList();
    }

    /**
     * @Title: scanLeDevice
     * @Description: TODO(扫描蓝牙设备 )
     * @param enable
     *            (扫描使能，true:扫描开始,false:扫描停止)
     * @return void
     * @throws
     */
    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    scan_flag = true;
//                    scan.setText("开始扫描");
//                    link_state.setText("连接中.. ");

                    Log.i("SCAN", "stop.....................");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
			/* 开始扫描蓝牙设备，带mLeScanCallback 回调函数 */
            Log.i("SCAN", "begin.....................");
            mScanning = true;
            scan_flag = false;
//            scan.setText("停止扫描");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else
        {
            Log.i("Stop", "stoping................");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scan_flag = true;
        }

    }
    /**
     * 蓝牙扫描回调函数 实现扫描蓝牙设备，回调蓝牙BluetoothDevice，可以获取name MAC等信息
     *
     * **/
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord)
        {
            // TODO Auto-generated method stub

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
//					/* 讲扫描到设备的信息输出到listview的适配器 */
                    if (isShow){
                        if (mleDeviceListAdapter.rssis.size()==0){
                            no_data.setVisibility(View.VISIBLE);
                        }else {
                            no_data.setVisibility(View.GONE);
                        }
                        mleDeviceListAdapter.addDevice(device, rssi);
                        mleDeviceListAdapter.notifyDataSetChanged();
                    }

                }
            });

            Log.e("Address:" ,device.getAddress());
            Log.e("Name:" ,device.getName());
            Log.e("rssi:" ,rssi+"");

        }
    };

    private void showDialog(){

        bottomView = View.inflate(MainActivity.this,R.layout.dialog_layout,null);//填充ListView布局
        lv = (ListView) bottomView.findViewById(R.id.dia_list);//初始化ListView控件
        no_data = bottomView.findViewById(R.id.no_data);
        if (mleDeviceListAdapter.rssis.size()==0){
            no_data.setVisibility(View.VISIBLE);
        }else {
            no_data.setVisibility(View.GONE);

        }
        lv.setAdapter(mleDeviceListAdapter);//ListView设置适配器
        		/* listview点击函数 */
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int position,
                                    long id)
            {
//                Toast.makeText(MainActivity.this,"点击了",Toast.LENGTH_LONG).show();
                dialog.dismiss();
                isShow = false;
                final BluetoothDevice device = mleDeviceListAdapter
                        .getDevice(position);
                if (device == null)
                    return;
                //处理蓝牙数据
                aboutBlueTooth(device,mleDeviceListAdapter.rssis.get(position));

                if (mScanning)
                {
					/* 停止扫描设备 */
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }



            }
        });
        View titleView = View.inflate(MainActivity.this,R.layout.dialog_title,null);
        dialog = new AlertDialog.Builder(this).setCustomTitle(titleView)
                .setView(bottomView)//在这里把写好的这个listview的布局加载dialog中
                .create();
      dialog.show();
        isShow = true;
    }

    // TODO: 2018/4/14 蓝牙相关处理
        /* BluetoothLeService绑定的回调函数 */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                 Toast.makeText(MainActivity.this,"蓝牙初始化失败，请重试",Toast.LENGTH_LONG).show();
                //                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
            // 根据蓝牙地址，连接设备
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }

    };

    //蓝牙数据接收处理
    private void aboutBlueTooth(BluetoothDevice device, Integer integer) {
        /* 启动蓝牙service */
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mDeviceName = device.getName();
        mDeviceAddress = device.getAddress();
        mRssi = integer;
        webSettingInit(web_echarts);


    }

    private void webSettingInit(WebView web_echarts) {
        if (web_echarts !=null){
            WebSettings ws = web_echarts.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setSupportZoom(true);
//            web_echarts.setWebViewClient(new WebViewClient(){
//                @Override
//                public void onPageFinished(WebView view, String url) {
//                    super.onPageFinished(view, url);
//                    Log.e("tagggg","页面加载完成了");
//                }
//            });
            web_echarts.setWebChromeClient(new WebChromeClient());
//            web_echarts.loadUrl("file:///android_asset/showEcharts.html");

        }
    }
    /**
     * @Title: init_ble
     * @Description: TODO(初始化蓝牙)
     * @param
     * @return void
     * @throws
     */
    private void init_ble() {
        // 手机硬件支持蓝牙
        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes Bluetooth adapter.
        // 获取手机本地的蓝牙适配器
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // 打开蓝牙权限
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

}
