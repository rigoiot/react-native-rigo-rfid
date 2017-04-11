package com.rigoiot;

import java.util.Timer;

/**
 * Created by mythfish on 11/4/2017.
 */

public class TimerTask extends java.util.TimerTask {
  Runnable mRunner;
  Timer mTimer;
  TimerTask(Runnable runner, Timer timer) {
    mRunner = runner;
    mTimer = timer;
  }
  public void run() {
    mTimer.cancel(); //Terminate the timer thread
    if (mRunner != null) {
      mRunner.run();
    }
  }
}
