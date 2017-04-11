package com.rigoiot;

import java.util.HashMap;
import com.clouiotech.pda.rfid.IAsynchronousMessage;
import com.clouiotech.pda.rfid.uhf.UHF;
import com.clouiotech.pda.rfid.uhf.UHFReader;
import com.clouiotech.port.Adapt;
import android.annotation.SuppressLint;
import android.util.Log;

/**
 * @author RFID_lx Activity 基类
 */
public class UHFBase {
  static String TAG = "UHFBase"; 
	static Boolean _UHFSTATE = false; // 模块是否已经打开
	// static int _PingPong_ReadTime = 10000; // 默认是100:3
	// static int _PingPong_StopTime = 300;
	static int _NowAntennaNo = 1; // 读写器天线编号
	static int _UpDataTime = 0; // 重复标签上传时间，控制标签上传速度不要太快
	static int _Max_Power = 30; // 读写器最大发射功率
	static int _Min_Power = 0; // 读写器最小发射功率

	static int low_power_soc = 10;

	public static UHF CLReader = UHFReader.getUHFInstance();

	/**
	 * 超高频模块初始化
	 * 
	 * @param log
	 *            接口回调方法
	 * @return 是否初始化成功
	 */
	public Boolean UHF_Init(boolean usingBackupPower, IAsynchronousMessage log) {
		Boolean rt = false;
		try {
			if (_UHFSTATE == false) {
				rt = CLReader.OpenConnect(usingBackupPower, log);
				if (rt) {
					_UHFSTATE = true;
				}
				
				Thread.sleep(500);
			} else {
				rt = true;
			}
		} catch (Exception ex) {
			Log.d(TAG, "UHF上电出现异常：" + ex.getMessage());
		}
		return rt;
	}

	/**
	 * 超高频模块释放
	 */
	public void UHF_Dispose() {
		if (_UHFSTATE == true) {
			CLReader.CloseConnect();
			_UHFSTATE = false;
		}
	}

	/**
	 * 获得读写器的读写能力
	 */
	@SuppressLint("UseSparseArrays")
	@SuppressWarnings("serial")
	protected void UHF_GetReaderProperty() {
		String propertyStr = CLReader.GetReaderProperty();
		Log.d(TAG, "获得读写器能力：" + propertyStr);
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
				Log.d(TAG, "获得读写器能力失败,转换失败！");
			}
		} else {
			Log.d(TAG, "获得读写器能力失败！");
		}
	}

	/**
	 * 设置标签上传参数
	 */
	protected void UHF_SetTagUpdateParam() {
		// 先查询当前的设置是否一致，如果不一致才设置
		String searchRT = CLReader.GetTagUpdateParam();
		String[] arrRT = searchRT.split("\\|");
		if (arrRT.length >= 2) {
			int nowUpDataTime = Integer.parseInt(arrRT[0]);
			Log.d(TAG, "查标签上传时间：" + nowUpDataTime);
			if (_UpDataTime != nowUpDataTime) {
				CLReader.SetTagUpdateParam("1," + _UpDataTime); // 设置标签重复上传时间为20ms
				Log.d(TAG, "设置标签上传时间...");
			} else {

			}
		} else {
			Log.d(TAG, "查询标签上传时间失败...");
		}
	}

	// 判断副电电量
	protected Boolean canUsingBackBattery() {
		if (Adapt.getPowermanagerInstance().getBackupPowerSOC() < low_power_soc) {
			return false;
		}
		return true;
	}
}
