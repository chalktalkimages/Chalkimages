package server;

import java.util.Calendar;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import engine.Engine;

@ComponentScan
@EnableAutoConfiguration
public class Application {
  static ConfigurableApplicationContext springApp;
  private static final Logger logger = Logger.getLogger(Application.class.getName());

  public static void main(String[] args) {
    Engine.getInstance();
    springApp = SpringApplication.run(Application.class, args);
    logger.info("Chalktalk Spring app started");

    int nHour = 23;
    int nMin = 30;
    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, nHour);
    today.set(Calendar.MINUTE, nMin);
    today.set(Calendar.SECOND, 0);
    Timer wipeTimer = new Timer();
    wipeTimer.scheduleAtFixedRate(
        new exitProgram(), today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
  }
}
