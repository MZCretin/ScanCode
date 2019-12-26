package com.cretin.tools.scancode;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @date: on 2019-10-31
 * @author: a112233
 * @email: mxnzp_life@163.com
 * @desc: 打开扫描二维码的配置
 */
public class ScanConfig implements Parcelable {
    //是否显示打开闪光灯的入口 默认显示
    private boolean isShowFlashlight = true;
    //是否显示相册入口 默认显示
    private boolean isShowGalary = true;
    //扫描成功是否需要提示音 默认需要
    private boolean isNeedRing;
//    //打开闪光灯的提示语 默认轻触打开
//    private String openFlashlightTips;
//    //打开闪光灯的图片资源文件
//    private int openFlashlightRes;
//    //关闭闪光灯的提示语 默认轻触打开
//    private String closeFlashlightTips;
//    //关闭闪光灯的图片资源文件
//    private int closeFlashlightRes;
//    //相册按钮的提示语 默认 相册
//    private String garalyTips;
//    //相册按钮的资源文件
//    private int garalyRes;
//    //扫面页面的标题
//    private String title;

    public boolean isShowFlashlight() {
        return isShowFlashlight;
    }

    public ScanConfig setShowFlashlight(boolean showFlashlight) {
        isShowFlashlight = showFlashlight;
        return this;
    }

    public boolean isShowGalary() {
        return isShowGalary;
    }

    public ScanConfig setShowGalary(boolean showGalary) {
        isShowGalary = showGalary;
        return this;
    }

    public boolean isNeedRing() {
        return isNeedRing;
    }

    public ScanConfig setNeedRing(boolean needRing) {
        isNeedRing = needRing;
        return this;
    }

//    public String getOpenFlashlightTips() {
//        return openFlashlightTips;
//    }
//
//    public ScanConfig setOpenFlashlightTips(String openFlashlightTips) {
//        this.openFlashlightTips = openFlashlightTips;
//        return this;
//    }
//
//    public int getOpenFlashlightRes() {
//        return openFlashlightRes;
//    }
//
//    public ScanConfig setOpenFlashlightRes(int openFlashlightRes) {
//        this.openFlashlightRes = openFlashlightRes;
//        return this;
//    }
//
//    public String getCloseFlashlightTips() {
//        return closeFlashlightTips;
//    }
//
//    public ScanConfig setCloseFlashlightTips(String closeFlashlightTips) {
//        this.closeFlashlightTips = closeFlashlightTips;
//        return this;
//    }
//
//    public int getCloseFlashlightRes() {
//        return closeFlashlightRes;
//    }
//
//    public ScanConfig setCloseFlashlightRes(int closeFlashlightRes) {
//        this.closeFlashlightRes = closeFlashlightRes;
//        return this;
//    }
//
//    public String getGaralyTips() {
//        return garalyTips;
//    }
//
//    public ScanConfig setGaralyTips(String garalyTips) {
//        this.garalyTips = garalyTips;
//        return this;
//    }
//
//    public int getGaralyRes() {
//        return garalyRes;
//    }
//
//    public ScanConfig setGaralyRes(int garalyRes) {
//        this.garalyRes = garalyRes;
//        return this;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public ScanConfig setTitle(String title) {
//        this.title = title;
//        return this;
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.isShowFlashlight ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isShowGalary ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isNeedRing ? (byte) 1 : (byte) 0);
//        dest.writeString(this.openFlashlightTips);
//        dest.writeInt(this.openFlashlightRes);
//        dest.writeString(this.closeFlashlightTips);
//        dest.writeInt(this.closeFlashlightRes);
//        dest.writeString(this.garalyTips);
//        dest.writeInt(this.garalyRes);
//        dest.writeString(this.title);
    }

    public ScanConfig() {
    }

    protected ScanConfig(Parcel in) {
        this.isShowFlashlight = in.readByte() != 0;
        this.isShowGalary = in.readByte() != 0;
        this.isNeedRing = in.readByte() != 0;
//        this.openFlashlightTips = in.readString();
//        this.openFlashlightRes = in.readInt();
//        this.closeFlashlightTips = in.readString();
//        this.closeFlashlightRes = in.readInt();
//        this.garalyTips = in.readString();
//        this.garalyRes = in.readInt();
//        this.title = in.readString();
    }

    public static final Creator<ScanConfig> CREATOR = new Creator<ScanConfig>() {
        @Override
        public ScanConfig createFromParcel(Parcel source) {
            return new ScanConfig(source);
        }

        @Override
        public ScanConfig[] newArray(int size) {
            return new ScanConfig[size];
        }
    };
}
