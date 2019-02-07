package server;

import java.util.Calendar;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import adapter.DBHandle;
import adapter.EquityFileParser;
import engine.Engine;
import utils.Globals;

@ComponentScan
@EnableAutoConfiguration
public class Application {
  static ConfigurableApplicationContext springApp;
  private static final Logger logger = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) {
    Engine.getInstance();
    springApp = SpringApplication.run(Application.class, args);
    logger.info("Chalktalk Spring app started");

    Globals.loadProperties();
    new DBHandle();
    new EquityFileParser();

    int nHour = 23;
    int nMin = 30;
    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, nHour);
    today.set(Calendar.MINUTE, nMin);
    today.set(Calendar.SECOND, 0);
    Timer wipeTimer = new Timer();
    Timer priceTargetChangesTimer = new Timer();
    wipeTimer.scheduleAtFixedRate(
        new exitProgram(), today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));

    // Run check for target changes at 7:32am everyday.
    Calendar priceTargetDate = Calendar.getInstance();
    priceTargetDate.set(Calendar.HOUR_OF_DAY, 6);
    priceTargetDate.set(Calendar.MINUTE, 15);
    priceTargetDate.set(Calendar.SECOND, 0);

    if (Calendar.getInstance().before(priceTargetDate)) {
      priceTargetChangesTimer.schedule(new TargetChangeProgram(), priceTargetDate.getTime());
    }
    logger.info("Populate Target Changes scheduled at " + priceTargetDate.getTime());
  }
}
