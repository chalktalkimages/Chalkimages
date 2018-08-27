package adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.filters.CsrfPreventionFilter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;

import utils.Globals;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import data.FlowStories;
import data.TickerResearch;

public class EquityFileParser extends TimerTask {

  private static final Logger logger = Logger.getLogger(EquityFileParser.class.getName());
  private static Session session;
  private static ChannelSftp channel;
  public static HashMap<String, TickerResearch> tickerResearchMap =
      new HashMap<String, TickerResearch>();
  public static List<FlowStories> flowStoryList = new ArrayList<FlowStories>();

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
    update();
    disconnect();
  }

  private static void update() {

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

        if (fileMap.size() > 0) {
          logger.info("Using " + fileMap.get(fileMap.size()) + " from SFTP.");
          // Read the file with the most recent (largest) date
          InputStream in = channel.get(fileMap.get(fileMap.size()));
          BufferedReader br = new BufferedReader(new InputStreamReader(in));

          flowStoryList.clear(); // Clear the list before
          // re-populating
          CSVParser fileParser = new CSVParser(br, CSVFormat.RFC4180);
          Iterator<CSVRecord> fileIterator = fileParser.iterator();
          int lineCounter = 0;
          while (fileIterator.hasNext()) {
            lineCounter++;
            try {
              CSVRecord record = fileIterator.next();

              String ticker = record.get(0).split("-")[0];
              String currency = record.get(1);
              String target = record.get(2);
              String rating = record.get(3);
              String analyst = record.get(4);
              String researchLink = record.get(5);
              String headline = record.get(6);

              SimpleDateFormat dateFormatter = new SimpleDateFormat("M/dd/yyyy hh:mm:ss a");
              Calendar publishDate = Calendar.getInstance();
              publishDate.setTime(dateFormatter.parse(record.get(7)));

              String story = record.get(8);

              if (currency.equals("CAD") || currency.equals("USD")) {
                TickerResearch tr = new TickerResearch();
                tr.rating = rating;
                tr.target = target;
                tr.researchLink = researchLink;
                tickerResearchMap.put(ticker, tr);

                FlowStories fs = new FlowStories();
                fs.author = analyst;
                fs.headline = headline;
                fs.link = researchLink;
                fs.story = story;
                fs.ticker = ticker;

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                if (publishDate.after(today)) flowStoryList.add(fs);
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

  public static void main(String[] args) {
    Globals.loadProperties();
    load();
  }
}
