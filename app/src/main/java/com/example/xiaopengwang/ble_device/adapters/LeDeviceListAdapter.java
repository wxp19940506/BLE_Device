package com.example.xiaopengwang.ble_device.adapters;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.xiaopengwang.ble_device.R;

import java.util.ArrayList;

/**
 * Created by XiaopengWang on 2018/4/13.
 * Email:xiaopeng.wang@qaii.ac.cn
 * QQ:839853185
 * WinXin;wxp19940505
 */

public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    // 蓝牙信号强度
    public ArrayList<Integer> rssis ;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Activity context )
    {
        super();
        this.rssis = new ArrayList<Integer>();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mInflator = context.getLayoutInflater();
    }

    public void addDevice(BluetoothDevice device, int rssi)
    {
        if (!mLeDevices.contains(device))
        {
            mLeDevices.add(device);
            rssis.add(rssi);
        }
    }

    public BluetoothDevice getDevice(int position)
    {
        return mLeDevices.get(position);
    }

    public void clear()
    {
        mLeDevices.clear();
        rssis.clear();
    }

    @Override
    public int getCount()
    {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i)
    {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    /**
     * 重写getview
     *
     * **/
    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {

        // General ListView optimization code.
        // 加载listview每一项的视图
        view = mInflator.inflate(R.layout.listitem, null);
        // 初始化三个textview显示蓝牙信息
        TextView deviceAddress = (TextView) view
                .findViewById(R.id.tv_deviceAddr);
        TextView deviceName = (TextView) view
                .findViewById(R.id.tv_deviceName);
        TextView rssi = (TextView) view.findViewById(R.id.tv_rssi);

        BluetoothDevice device = mLeDevices.get(i);
        deviceAddress.setText(device.getAddress());
        deviceName.setText(device.getName());
        rssi.setText("" + rssis.get(i));

        return view;
    }
}
