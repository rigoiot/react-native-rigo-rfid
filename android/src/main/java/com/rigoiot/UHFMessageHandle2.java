package com.rigoiot;

import com.clouiotech.pda.rfid.EPCModel;
import com.clouiotech.pda.rfid.IAsynchronousMessage;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by mythfish on 11/4/2017.
 */

public class UHFMessageHandle2 implements IAsynchronousMessage {

  private final CopyOnWriteArraySet<UHFMessageListener> mListeners =
      new CopyOnWriteArraySet<>();

  public void addListener(UHFMessageListener listener) {
    mListeners.add(listener);
  }

  public void removeListener(UHFMessageListener listener) {
    mListeners.remove(listener);
  }

  @Override
  public void OutPutEPC(EPCModel epcModel) {
    for (UHFMessageListener listener : mListeners) {
      listener.OutPutEPC(epcModel);
    }
  }
}
