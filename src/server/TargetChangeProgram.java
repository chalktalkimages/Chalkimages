package server;

import java.util.TimerTask;

import org.apache.log4j.Logger;

import engine.Engine;

public class TargetChangeProgram extends TimerTask {

  private static final Logger logger = Logger.getLogger(TargetChangeProgram.class.getName());

  public void run() {
    logger.info("Populating Target Changes Map");
    Engine.getInstance().populatePriceTargetChanges();
  }
}
