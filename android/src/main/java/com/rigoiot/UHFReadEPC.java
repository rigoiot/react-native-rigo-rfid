package com.rigoiot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.clouiotech.pda.demo.PublicData;
import com.clouiotech.pda.rfid.EPCModel;
import com.clouiotech.pda.rfid.IAsynchronousMessage;
import com.clouiotech.port.Adapt;
import com.clouiotech.util.Helper.*;

import android.media.*;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.*;

/**
 * @author RFID_lx 读标签窗体
 */
public class UHFReadEPC extends UHFBase implements
		IAsynchronousMessage {

	private static boolean isStartPingPong = false; //
	private boolean isKeyDown = false; // 板机是否按下
	private boolean isLongKeyDown = false; // 板机是否是长按状态
	private int keyDownCount = 0; // 板机按下次数

	private int readTime = 0;
	private int lastReadCount = 0;
	private int totalReadCount = 0; // 总读取次数
	private int speed = 0; // 读取速度
	private static int _ReadType = 0; // 0 为读EPC，1 为读TID
	private static String _NowReadParam = _NowAntennaNo + "|1"; // 读标签参数
	private HashMap<String, EPCModel> hmList = new HashMap<String, EPCModel>();
	private Object hmList_Lock = new Object();
	private boolean flag = true; //
	private Boolean IsFlushList = true; // 是否刷列表
	private Object beep_Lock = new Object();

	private static boolean isPowerLowShow = false;

	private boolean usingBackBattery = false;;

	private final int MSG_RESULT_READ = MSG_USER_BEG + 1; // 常量读
	private final int MSG_FLUSH_READTIME = MSG_USER_BEG + 2;
	private final int MSG_UHF_POWERLOW = MSG_USER_BEG + 3;

	final String FILE_NAME = "/mnt/sdcard/UHF_Read_Result.csv";
	private boolean bSaveResult = false;
		
	@Override
	protected void msgProcess(Message msg) {
		switch (msg.what) {
		case MSG_RESULT_READ:
			ShowList(); // 刷新列表
			break;
		case MSG_FLUSH_READTIME:
			if (lb_ReadTime != null) { // 刷新读取时间
				readTime++;
				lb_ReadTime.setText("Time:" + readTime + "S");
			}
			break;
		case MSG_UHF_POWERLOW:
			ShowPowerLow();
			break;
		default:
			super.msgProcess(msg);
			break;
		}
	}

	// 读功能
	public void Read(View v) {
		Button btnRead = (Button) v;
		String controlText = btnRead.getText().toString();
		deleteFile(FILE_NAME);
		if (controlText.equals(getString(R.string.btn_read))) {
			();
			btnRead.setText(getString(R.string.btn_read_stop));
			sp_ReadType.setEnabled(false);
		} else {
			Pingpong_Stop();
			btnRead.setText(getString(R.string.btn_read));
			sp_ReadType.setEnabled(true);
			write("Data,Count\r\n");
			SaveData();
		}
	}

	@Override
	public boolean deleteFile(String filePath) {
	    File file = new File(filePath);
	        if (file.isFile() && file.exists()) {
	        return file.delete();
	        }
	        return false;
	    }

	// 间歇性读
	public void PingPong_Read() {
		if (isStartPingPong)
			return;
		isStartPingPong = true;
		Clear(null);
		Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
			@Override
			public void run() {
				while (isStartPingPong) {
					try {
						if (!isPowerLowShow) {
							if (usingBackBattery && !canUsingBackBattery()) {
								Pingpong_Stop();
								sendMessage(MSG_UHF_POWERLOW, null);
								break;
							}

							if (PublicData._IsCommand6Cor6B.equals("6C")) {// 读6C标签
								CLReader.Read_EPC(_NowReadParam);
							} else {// 读6B标签
								CLReader.Get6B(_NowAntennaNo + "|1" + "|1"
										+ "|" + "1,000F");
							}

							Thread.sleep(PublicData._PingPong_ReadTime);

							if (PublicData._PingPong_StopTime > 0) {
								CLReader.Stop();
								Thread.sleep(PublicData._PingPong_StopTime);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	// 停止间歇性读
	public void Pingpong_Stop() {
		isStartPingPong = false;
		CLReader.Stop();
	}

	public void Clear(View v) {
		totalReadCount = 0;
		readTime = 0;
		hmList.clear();
		ShowList();
	}

	// 返回主页
	public void Back(View v) {
		if (btn_Read.getText().toString()
				.equals(getString(R.string.btn_read_stop))) {
			ShowMsg(getString(R.string.uhf_please_stop), null);
			return;
		}
		ReadEPCActivity.this.finish();
	}

	protected void Init() {
		usingBackBattery = canUsingBackBattery();
		if (!UHF_Init(usingBackBattery, this)) { // 打开模块电源失败
			ShowMsg(getString(R.string.uhf_low_power_info),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							ReadEPCActivity.this.finish();
						}
					});
		} else {
			try {
				super.UHF_GetReaderProperty(); // 获得读写器的能力
				_NowReadParam = _NowAntennaNo + "|1";
				Thread.sleep(20);
				CLReader.Stop(); // 停止指令
				Thread.sleep(20);
				super.UHF_SetTagUpdateParam(); // 设置标签重复上传时间为20ms
			} catch (Exception ee) {
			}
			IsFlushList = true;
			// 刷新线程
			Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() {
				@Override
				public void run() {
					while (IsFlushList) {
						try {
							sendMessage(MSG_RESULT_READ, null);
							Thread.sleep(1000); // 一秒钟刷新一次
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});

			Helper_ThreadPool.ThreadPool_StartSingle(new Runnable() { // 蜂鸣器发声
						@Override
						public void run() {
							while (IsFlushList) {
								synchronized (beep_Lock) {
									try {
										beep_Lock.wait();
									} catch (InterruptedException e) {
									}
								}
								if (IsFlushList) {
									toneGenerator
											.startTone(ToneGenerator.TONE_PROP_BEEP);
								}

							}
						}
					});

			listView = (ListView) this.findViewById(R.id.lv_Main);
			tv_TitleTagID = (TextView) findViewById(R.id.tv_TitleTagID);
			lb_ReadTime = (TextView) findViewById(R.id.lb_ReadTime);
			lb_ReadSpeed = (TextView) findViewById(R.id.lb_ReadSpeed);
			lb_TagCount = (TextView) findViewById(R.id.lb_TagCount);
			btn_Read = (Button) findViewById(R.id.btn_Read);
			btn_Read.setText(getString(R.string.btn_read));
			sp_ReadType = (Spinner) findViewById(R.id.sp_ReadType);
			//

			sp_ReadType
					.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

						@Override
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3) {
							if (isStartPingPong)
								return;
							int selectItem = sp_ReadType
									.getSelectedItemPosition();
							if (PublicData._IsCommand6Cor6B.equals("6C")) {// 读6C标签
								if (selectItem == 0) {
									_ReadType = 0;
									_NowReadParam = _NowAntennaNo + "|1";
									tv_TitleTagID.setText("EPC");
								} else if (selectItem == 1) {
									_ReadType = 1;
									_NowReadParam = _NowAntennaNo + "|1|2,0006";
									tv_TitleTagID.setText("TID");
								} else if (selectItem == 2) {
									_ReadType = 2;
									_NowReadParam = _NowAntennaNo
											+ "|1|3,000006";
									tv_TitleTagID.setText("UserData");
								}
							} else {
								if (selectItem == 0) {
									_ReadType = 0;
									tv_TitleTagID.setText("EPC");
								} else if (selectItem == 1) {
									_ReadType = 1;
									tv_TitleTagID.setText("TID");
								} else if (selectItem == 2) {
									_ReadType = 2;
									tv_TitleTagID.setText("UserData");
								}
							}

						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {

						}

					});

		}
		return;
	}

	// 释放资源
	protected void Dispose() {
		isStartPingPong = false;
		IsFlushList = false;
		synchronized (beep_Lock) {
			beep_Lock.notifyAll();
		}
		UHF_Dispose();
	}

	protected void ShowList() {
		if (!isStartPingPong)
			return;
		sa = new SimpleAdapter(this, GetData(), R.layout.epclist_item,
				new String[] { "EPC", "ReadCount" }, new int[] {
						R.id.EPCList_TagID, R.id.EPCList_ReadCount });
		listView.setAdapter(sa);
		listView.invalidate();
		if (lb_ReadTime != null) { // 刷新读取时间
			readTime++;
			lb_ReadTime.setText("Time:" + readTime / 1 + "S");
		}
		if (lb_ReadSpeed != null) { // 刷新读取速度
			if (flag) {
				speed = totalReadCount - lastReadCount;
				if (speed < 0)
					speed = 0;
				lastReadCount = totalReadCount;
				if (lb_ReadSpeed != null) {
					lb_ReadSpeed.setText("SP:" + speed + "T/S");
				}
			}
			// flag = !flag;
		}
		if (lb_TagCount != null) { // 刷新标签总数
			lb_TagCount.setText("Total:" + hmList.size());
		}
	}

	// 获得更新数据源
	@SuppressWarnings({ "rawtypes", "unused" })
	protected List<Map<String, Object>> GetData() {
		List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
		synchronized (hmList_Lock) {
			// if(hmList.size() > 0){ //
			Iterator iter = hmList.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				EPCModel val = (EPCModel) entry.getValue();
				Map<String, Object> map = new HashMap<String, Object>();
				if (_ReadType == 0) {
					map.put("EPC", val._EPC);
				} else if (_ReadType == 1) {
					map.put("EPC", val._TID);
				} else {
					map.put("EPC", val._UserData);
				}
				map.put("ReadCount", val._TotalCount);
				rt.add(map);
			}
			// }
		}
		return rt;
	}

	// 保存数据源
		@SuppressWarnings({ "rawtypes", "unused" })
		protected boolean SaveData() {
			if (!bSaveResult) {
				return false;
			}
			
			//List<Map<String, Object>> rt = new ArrayList<Map<String, Object>>();
			synchronized (hmList_Lock) {
				// if(hmList.size() > 0){ //
				Iterator iter = hmList.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					String key = (String) entry.getKey();
					EPCModel val = (EPCModel) entry.getValue();
					//Map<String, Object> map = new HashMap<String, Object>();
					
					if (_ReadType == 0) {
						write(val._EPC+",");
					} else if (_ReadType == 1) {
						write(val._TID+",");
					} else {
						write(val._UserData+",");
					}
					write(val._TotalCount+"\r\n");
					//rt.add(map);
					
				
				}
			}
			//return rt;
			return true;
		}
	
	private void write(String content) {
		try {
			// 以追加的方式打开文件输出流
			FileOutputStream fileOut  = new FileOutputStream(FILE_NAME,true);
			// 写入数据
			fileOut.write(content.getBytes());
			// 关闭文件输出流
			fileOut.close();
		} catch (Exception e) {
				e.printStackTrace();
		}
	}	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// 创建
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		this.setContentView(R.layout.uhf_read);
	}

	@Override
	protected void onDestroy() {
		// 释放
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// 待机锁屏
		super.onPause();
		Dispose();
	}

	@Override
	protected void onResume() {
		// 待机恢复
		super.onResume();
		Init();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("CL7202K3", "onKeyDown keyCode = " + keyCode);
		if (keyCode == 131 || keyCode == 135) { // 按下扳机
			deleteFile(FILE_NAME);
			btn_Read.setText(getString(R.string.btn_read_stop));
			sp_ReadType.setEnabled(false);
			btn_Read.setClickable(false);
			if (!isKeyDown) {
				isKeyDown = true; //
				if (!isStartPingPong) {
					Clear(null);
					Pingpong_Stop(); // 停止间歇性读
					isStartPingPong = true;
					CLReader.Read_EPC(_NowReadParam);
					if (PublicData._IsCommand6Cor6B.equals("6C")) {// 读6C标签
						CLReader.Read_EPC(_NowReadParam);
					} else {// 读6B标签
						CLReader.Get6B(_NowAntennaNo + "|1" + "|1" + "|"
								+ "1,000F");
					}
				}
			} else {
				if (keyDownCount < 10000)
					keyDownCount++;
			}
			if (keyDownCount > 100) {
				isLongKeyDown = true;
			}
			if (isLongKeyDown) { // 长按事件

			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d("CL7202K3", "onKeyUp keyCode = " + keyCode);
		if (keyCode == 131 || keyCode == 135) { // 放开扳机
			CLReader.Stop();
			isStartPingPong = false;
			keyDownCount = 0;
			isKeyDown = false;
			isLongKeyDown = false;
			btn_Read.setText(getString(R.string.btn_read));
			sp_ReadType.setEnabled(true);
			btn_Read.setClickable(true);
			write("Data,Count\r\n");
			SaveData();
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void OutPutEPC(EPCModel model) {
		if (!isStartPingPong)
			return;
		try {
			synchronized (hmList_Lock) {
				if (hmList.containsKey(model._EPC + model._TID)) {
					EPCModel tModel = hmList.get(model._EPC + model._TID);
					tModel._TotalCount++;
				} else {
					hmList.put(model._EPC + model._TID, model);
				}
			}
			synchronized (beep_Lock) {
				beep_Lock.notify();
			}
			totalReadCount++;
		} catch (Exception ex) {
			Log.d("Debug", "标签输出异常：" + ex.getMessage());
		}

	}

	// 判断副电电量
	private Boolean canUsingBackBattery() {
		if (Adapt.getPowermanagerInstance().getBackupPowerSOC() < low_power_soc) {
			return false;
		}
		return true;
	}

	private void ShowPowerLow() {
		new AlertDialog.Builder(ReadEPCActivity.this)
				.setTitle(getString(R.string.str_confirm))
				// 设置对话框标题
				.setMessage(getString(R.string.uhf_low_power_consumption))
				// 设置显示的内容
				.setPositiveButton(getString(R.string.str_ok),
						new DialogInterface.OnClickListener() {// 添加确定按钮
							@Override
							public void onClick(DialogInterface dialog,
									int which) {// 确定按钮的响应事件
								UHF_Init(false, ReadEPCActivity.this);
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								PingPong_Read();
								isPowerLowShow = false;
							}
						})
				.setNegativeButton(getString(R.string.str_cancel),
						new DialogInterface.OnClickListener() {// 添加返回按钮
							@Override
							public void onClick(DialogInterface dialog,
									int which) {// 响应事件
								Dispose();
								isPowerLowShow = false;
								ReadEPCActivity.this.finish();
							}
						}).show();// 在按键响应事件中显示此对话框

	}
}
