package com.rigoiot;

import java.util.HashMap;
import java.util.Timer;

import com.clouiotech.pda.rfid.EPCModel;
import com.clouiotech.pda.rfid.IAsynchronousMessage;
import com.clouiotech.pda.rfid.uhf.UHF;
import com.clouiotech.pda.rfid.uhf.UHFReader;
import com.clouiotech.port.Adapt;
import com.clouiotech.util.Helper.Helper_ThreadPool;

import com.facebook.react.bridge.Callback;

import android.annotation.SuppressLint;
import android.util.Log;

public class UHFBase {

  static String TAG = "UHFBase";

	static Boolean _UHFSTATE = false; // 模块是否已经打开
	static int _NowAntennaNo = 1; // 读写器天线编号
	static int _UpDataTime = 0; // 重复标签上传时间，控制标签上传速度不要太快
	static int _Max_Power = 30; // 读写器最大发射功率
	static int _Min_Power = 0; // 读写器最小发射功率

	static int low_power_soc = 10;

  private static boolean isStartRead = false; // 是否启动间歇读

	public static UHF CLReader = UHFReader.getUHFInstance();

	/**
	 * 超高频模块初始化
	 *
	 * @return 是否初始化成功
	 */
	public Boolean UHF_Init(IAsynchronousMessage handle) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {
				rt = CLReader.OpenConnect(canUsingBackBattery(), handle);
				if (rt) {
					_UHFSTATE = true;
				}
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.e(TAG, "UHF上电出现异常：" + ex.getMessage());
		}
		return rt;
	}

  /**
   * 超高频模块释放
   *
   */
	public void UHF_Dispose() {
		if (_UHFSTATE == true) {
      CLReader.Stop();
			CLReader.CloseConnect();
			_UHFSTATE = false;
		}
	}

	/**
	 * 获得读写器的读写能力
	 */
	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("serial")
	public void UHF_GetReaderProperty() {
		String propertyStr = CLReader.GetReaderProperty();
		Log.e(TAG, "获得读写器能力：" + propertyStr);
		String[] propertyArr = propertyStr.split("\\|");
		HashMap<Integer, Integer> hm_Power = new HashMap<Integer, Integer>() {
			{
				put(1, 1);
				put(2, 3);
				put(3, 7);
				put(4, 15);
			}
		};
		if (propertyArr.length > 3) {
			try {
				_Max_Power = Integer.parseInt(propertyArr[0]);
				_Min_Power = Integer.parseInt(propertyArr[1]);
				int powerIndex = Integer.parseInt(propertyArr[2]);
				_NowAntennaNo = hm_Power.get(powerIndex);
			} catch (Exception ex) {
				Log.e(TAG, "获得读写器能力失败,转换失败！");
			}
		} else {
			Log.e(TAG, "获得读写器能力失败！");
		}
	}

	/**
	 * 设置标签上传参数
	 */
	public void UHF_SetTagUpdateParam() {
		// 先查询当前的设置是否一致，如果不一致才设置
		String searchRT = CLReader.GetTagUpdateParam();
		String[] arrRT = searchRT.split("\\|");
		if (arrRT.length >= 2) {
			int nowUpDataTime = Integer.parseInt(arrRT[0]);
			Log.e(TAG, "查标签上传时间：" + nowUpDataTime);
			if (_UpDataTime != nowUpDataTime) {
				CLReader.SetTagUpdateParam("1," + _UpDataTime); // 设置标签重复上传时间为20ms
				Log.e(TAG, "设置标签上传时间...");
			} else {

			}
		} else {
			Log.e(TAG, "查询标签上传时间失败...");
		}
	}

	// 判断副电电量
	public Boolean canUsingBackBattery() {
		if (Adapt.getPowermanagerInstance().getBackupPowerSOC() < low_power_soc) {
			return false;
		}
		return true;
	}

  /**
   * 读数据
   *
   * @param type
   *            标签类型，6C或6B
   * @param param
   *            指定读数据的输入参数
   * @param readTime
   *            读等待完成时间
   * @param stopTime
   *            读停止等待完成时间
   * @param readStart
   *            读用户区的开始位置
   * @param readLen
   *            读用户区的长度
   *
   * @return 是否可以读取
   */
  public boolean Read(final String type,
                      final int readType,
                      final String param,
                      final int readTime,
                      final int stopTime,
                      final int readStart,
                      final int readLen,
                      final UHFMessageHandle handle) {

    // 先释放模块再初始化
    UHF_Dispose();
    if (!UHF_Init(handle)) {
      return false;
    }

    try {
      UHF_GetReaderProperty(); // 获得读写器的能力
      Thread.sleep(20);
      CLReader.Stop(); // 停止指令
      Thread.sleep(20);
      UHF_SetTagUpdateParam(); // 设置标签重复上传时间为20ms
    } catch (Exception e) {
      Log.e(TAG, "获得读写器的能力/设置标签！");
      e.printStackTrace();
      return false;
    }

    isStartRead = true;
    Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
      @Override
      public void run() {
        while (isStartRead) {
          try {
            if (type.equals("6C")) { // 读6C标签
              if (1 == readType) {
                UHFReader._TagEM.GetEPC_TID(_NowAntennaNo, 1);
              } else if (2 == readType) {
                UHFReader._TagEM.GetEPC_TID_UserData(_NowAntennaNo, 1, readStart, readLen);
              } else {
                UHFReader._TagEM.GetEPC(_NowAntennaNo, 1);
              }
            } else { // 读6B标签
              CLReader.Get6B(_NowAntennaNo + param);
            }

            // 等待读完成
            Thread.sleep(readTime);

            if (stopTime > 0) {
              CLReader.Stop();
              // 等待停止完成
              Thread.sleep(stopTime);
            }
          } catch (Exception e) {
            Log.e(TAG, "读数据异常！");
            e.printStackTrace();
          }
        }
      }
    });

    return true;
  }

  /**
   * 停止读写操作
   *
   */
  public void Stop() {
    isStartRead = false;
    CLReader.Stop();
  }

	/**
	 * 写数据
	 *
	 * @param type
	 *            标签类型，6C或6B
	 * @param writeType
	 *            写入数据的类型，0: EPC，1: UserData
	 * @param TID
	 *            匹配的TID
	 * @param data
	 *            写入数据
	 *
	 * @return 是否写入成功
	 */
	public boolean Write(final String type,
                       final int writeType,
                       final String TID,
                       final String data,
                       final UHFMessageHandle handle) {

		// 先释放模块再初始化
		UHF_Dispose();
		if (!UHF_Init(handle)) {
			return false;
		}

		int dataLen = data.length() % 4 == 0 ? data.length() / 4 : data.length() / 4 + 1;

    try {
			if (type.equals("6C")) { // 6C标签
				if (1 == writeType) {
          return UHFReader._Tag6C.WriteUserData_MatchTID(_NowAntennaNo, data, TID, 0) != -1;
				} else {
          return UHFReader._Tag6C.WriteEPC_MatchTID(_NowAntennaNo, data, TID, 0) != -1;
				}
			} else { // 6B标签
        return false;
      }
		} catch (Exception e) {
			Log.e(TAG, "写数据异常！");
			e.printStackTrace();
		}

		return true;
	}
}
