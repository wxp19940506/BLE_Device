package com.example.xiaopengwang.ble_device.bean;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by XiaopengWang on 2018/3/16.
 * Email:xiaopeng.wang@qaii.ac.cn
 * QQ:839853185
 * WinXin;wxp19940505
 */

public class AsForjs {
    private Context context;
    private String name1;
    private String namepwd;
    private String pwd2;

    private static CallBack callBack;


    public AsForjs(Context context){
        this.context=context;
    }
    /*
    通过注解@android.webkit.JavascriptInterface
    关联  javaScript 接口
     */
    @android.webkit.JavascriptInterface
    public  void show(){
        Toast.makeText(context,"安卓程序调用成功", Toast.LENGTH_SHORT).show();
    }
    @android.webkit.JavascriptInterface
    public  void show(String str){
        Toast.makeText(context,str, Toast.LENGTH_SHORT).show();
    }

    @android.webkit.JavascriptInterface
    public  boolean showname(String name, String pwd){
        name1=name;
        pwd2=pwd;
        namepwd=name+pwd;
        Toast.makeText(context,"用户名："+name+"密码："+pwd, Toast.LENGTH_SHORT).show();
        if (name.equals("123")&&pwd.equals("123")){
            callBack.changeText(name1,pwd2);
            Log.d("=====","233");
            return true;
        }
        return false;

    }

    public String getname(){
        return name1;

//        if (name1 !=null){
//            return name1;
//        }else {
//            return null;
//        }
    }
    public String getPwd2(){
        return pwd2;
//        if (pwd2 != null){
//            return  pwd2;
//        }else {
//            return null;
//        }
    }


    public static  void  setCallBack(Context context){
        callBack= (CallBack) context;

    }

    public  interface CallBack{
        public  void changeText(String name, String pwd);
    }
}
