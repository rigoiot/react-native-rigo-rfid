
package com.rigoiot;

import android.os.Build;
import android.util.Log;

import com.clouiotech.pda.rfid.EPCModel;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;

public class RNRigoRfidModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNRigoRfidModule";

  private final ReactApplicationContext reactContext;

  private final UHFBase mUHF = new UHFBase();

  public RNRigoRfidModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNRigoRfid";
  }

  /**
   * Utility methods related to physical devies and emulators.
   */
  private boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk".equals(Build.PRODUCT);
  }

  /**
   * 读数据
   *
   * @param type     标签类型，6C或6B
   * @param param    指定读数据的输入参数
   * @param readTime 读等待完成时间
   * @param stopTime 读停止等待完成时间
   * @param cb       结果回调
   * @return 是否可以读取
   */
  @ReactMethod
  public void read(final String type, final String param, final int readTime, final int stopTime, final Callback cb) {
    Log.v(TAG, "Read()");
    if (isEmulator()) {
      cb.invoke("Not supported", null);
      return;
    }

    UHFMessageHandle handle = new UHFMessageHandle() {
      @Override
      public void OutPutEPC(EPCModel epcModel) {
        WritableMap map = Arguments.createMap();
        map.putString("EPC", epcModel._EPC);
        map.putString("TID", epcModel._TID);
        map.putString("SensorData", epcModel._SensorData);
        map.putString("UserData", epcModel._UserData);
        map.putString("TagetData", epcModel._TagetData);
        map.putString("TagType", epcModel._TagType);
        cb.invoke(null, map);
      }
    };

    if (!mUHF.Read(type, param, readTime, stopTime, handle)) {
      cb.invoke("Read failed", null);
    }
  }

  /**
   * 停止读写操作
   *
   */
  @ReactMethod
  public void Stop() {
    if (isEmulator()) {
      return;
    }
    mUHF.Stop();
  }
}
