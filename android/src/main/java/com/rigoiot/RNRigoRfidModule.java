
package com.rigoiot;

import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import com.clouiotech.pda.rfid.EPCModel;
import com.clouiotech.pda.rfid.uhf.UHFReader;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

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

  // Send event to JS
  private void sendEvent(String eventName,
                         @Nullable WritableMap params) {
    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

    /**
     * 读数据
     *
     * @param type     标签类型，6C或6B
     * @param param    指定读数据的输入参数
     * @param readTime 读等待完成时间
     * @param stopTime 读停止等待完成时间
     * @param readStart 读用户区的开始位置
     * @param readLen   读用户区的长度
     * @param cb       结果回调
     * @return 是否可以读取
     */
  @ReactMethod
  public void read(final String type,
                   final int readType,
                   final String param,
                   final int readTime,
                   final int stopTime,
                   final int readStart,
                   final int readLen,
                   final Callback cb) {
    Log.i(TAG, "Read()");
    if (isEmulator()) {
      cb.invoke("Not supported", false);
      WritableMap map = Arguments.createMap();
      map.putString("EPC", "111111111");
      map.putString("TID", "222222222");
      map.putString("SensorData", "010101010101");
      map.putDouble("Temperature", -64);
      map.putString("UserData", "模拟数据，无效温度");
      map.putString("TagetData", "0000000000");
      map.putString("TagType", "6C");
      sendEvent("rigoiotRFIDEvent", map);

      WritableMap map2 = Arguments.createMap();
      map2.putString("EPC", "000000000");
      map2.putString("TID", "111111111");
      map2.putString("SensorData", "010101010101");
      map2.putDouble("Temperature", -20.5);
      map2.putString("UserData", "模拟数据");
      map2.putString("TagetData", "0000000000");
      map2.putString("TagType", "6C");
      sendEvent("rigoiotRFIDEvent", map2);
      return;
    }

    UHFMessageHandle handle = new UHFMessageHandle() {
      @Override
      public void OutPutEPC(EPCModel epcModel) {
        Log.i(TAG, "Read(): " + epcModel.toString());
        WritableMap map = Arguments.createMap();
        map.putString("EPC", epcModel._EPC);
        map.putString("TID", epcModel._TID);
        map.putString("SensorData", epcModel._SensorData);
        map.putDouble("Temperature", UHFReader._TagEM.ConvetTemp(epcModel));
        map.putString("UserData", epcModel._UserData);
        map.putString("TagetData", epcModel._TagetData);
        map.putString("TagType", epcModel._TagType);
        sendEvent("rigoiotRFIDEvent", map);
      }
    };

    if (!mUHF.Read(type, readType, param, readTime, stopTime, readStart, readLen, handle)) {
      cb.invoke("Read failed", false);
    }

    cb.invoke("", true);
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
