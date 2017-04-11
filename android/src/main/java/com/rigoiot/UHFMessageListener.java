package com.rigoiot;

import com.clouiotech.pda.rfid.EPCModel;

/**
 * Created by mythfish on 11/4/2017.
 */

public interface UHFMessageListener {
  void OutPutEPC(EPCModel epcModel);
}
