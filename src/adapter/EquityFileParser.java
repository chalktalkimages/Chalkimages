package adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import utils.Globals;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import data.TickerResearch;

public class EquityFileParser extends TimerTask {

  private static final Logger logger = Logger.getLogger(EquityFileParser.class.getName());
  private static Session session;
  private static ChannelSftp channel;
  public static HashMap<String, TickerResearch> tickerResearchMap =
      new HashMap<String, TickerResearch>();

  static {
    Calendar today = Calendar.getInstance();
    Timer refreshTimer = new Timer();
    refreshTimer.scheduleAtFixedRate(
        new EquityFileParser(),
        today.getTime(),
        TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
  }

  public void run() {
    logger.info("Running scheduled load");
    load();
  }

  public static void load() {
    connect();
    updateMap();
    disconnect();
  }

  private static void updateMap() {

    if (channel != null) {
      try {
        Map<Integer, String> fileMap = new TreeMap<Integer, String>();

        // Add all filenames on the FTP to the TreeMap
        // The TreeMap sorts values in ascending order
        int i = 0;
        for (Object file : channel.ls("/")) {
          ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) file;
          String filename = entry.getFilename();
          if (filename.contains("coverageList") && filename.endsWith(".csv"))
            fileMap.put(++i, filename);
        }
        ;

        if (fileMap.size() > 0) {
          logger.info("Using " + fileMap.get(fileMap.size()) + " from SFTP.");
          // Read the file with the most recent (largest) date
          InputStream in = channel.get(fileMap.get(fileMap.size()));
          BufferedReader br = new BufferedReader(new InputStreamReader(in));
          String line;
          int lineCounter = 0;
          while ((line = br.readLine()) != null) {
            lineCounter++;
            try {
              String[] lineA = line.split(",");
              String ticker = lineA[0].split("-")[0].replaceAll("\"", "");
              String currency = lineA[1];
              String target = lineA[2].replaceAll("\"", "");
              String rating = lineA[3];
              String analyst = lineA[4];
              String researchLink = lineA[5];
              if (currency.equals("CAD") || currency.equals("USD")) {
                TickerResearch tr = new TickerResearch();
                tr.rating = rating;
                tr.target = target;
                tr.researchLink = researchLink;
                tickerResearchMap.put(ticker, tr);
              }
            } catch (Exception e) {
              logger.warn("Failed on line " + lineCounter);
              logger.error(e);
            }
          }
        } else {
          logger.warn("No coverage list file found on SFTP!");
        }

      } catch (IOException i) {
        logger.error(i);
      } catch (SftpException s) {
        logger.error(s);
      } catch (Exception e) {
        logger.error(e);
      }
    } else {
      logger.warn("Channel is null!");
    }
  }

  private static void connect() {
    try {
      JSch jsch = new JSch();

      session = jsch.getSession(Globals.SFTP_username, Globals.SFTP_host);
      session.setPassword(Globals.SFTP_password);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
    } catch (JSchException j) {
      logger.error(j);
    }
  }

  private static void disconnect() {
    if (channel != null) {
      channel.exit();
    }

    if (session != null) {
      session.disconnect();
    }
  }
}
