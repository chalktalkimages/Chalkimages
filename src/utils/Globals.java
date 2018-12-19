package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Globals {

  private static final Logger logger = LoggerFactory.getLogger(Globals.class.getName());
  public static String SFTP_host;
  public static String SFTP_username;
  public static String SFTP_password;
  public static String IOI_path;
  public static String EMAIL_USER, EMAIL_PW;

  public static void loadProperties() {

    logger.info("Loading global properties");
    Properties prop = new Properties();
    InputStream input = null;

    try {

      input = new FileInputStream("config.properties");
      prop.load(input);

      Enumeration<?> e = prop.propertyNames();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        String value = prop.getProperty(key);
        logger.info(key + "=" + value);
      }

      SFTP_host = prop.getProperty("SFTP_host", "66.155.100.247");
      SFTP_username = prop.getProperty("SFTP_username", "ScotiaTrading_UAT");
      SFTP_password = prop.getProperty("SFTP_password", "123456Trade!");
      IOI_path = prop.getProperty("IOI_path", "http://t65-w7-eqcash:8001/");
      EMAIL_USER = prop.getProperty("EMAIL_USER", "nmathur");
      EMAIL_PW = prop.getProperty("EMAIL_PW", "123456Nm");
    } catch (IOException ex) {
      ex.printStackTrace();
      logger.error("Error loading properties file, using default debug values...", ex);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          logger.error("Failed to close properties file!", e);
          e.printStackTrace();
        }
      }
    }
  }
}
