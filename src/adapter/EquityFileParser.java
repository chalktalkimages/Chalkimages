package adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import data.FlowStories;
import data.TickerResearch;
import engine.Engine;
import utils.Globals;

public class EquityFileParser extends TimerTask {

  private static final Logger logger = Logger.getLogger(EquityFileParser.class.getName());
  private static Session session;
  private static ChannelSftp channel;
  public static HashMap<String, TickerResearch> tickerResearchMap =
      new HashMap<String, TickerResearch>();
  public static HashMap<String, String> currentTargetMap = new HashMap<String, String>();
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
        HashMap<String, String> tempPriceTargetChangeMap =
            Engine.getInstance().priceTargetChangesMap;
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
          CSVParser fileParser = new CSVParser(br, CSVFormat.RFC4180);
          List<CSVRecord> records = fileParser.getRecords();
          fileParser.close();

          // Track number of occurrences of each story
          Iterator<CSVRecord> fileIterator = records.iterator();
          HashMap<String, Integer> storyCounter = new HashMap<String, Integer>();
          while (fileIterator.hasNext()) {
            CSVRecord record = fileIterator.next();
            String headline = record.get(6);
            headline = headline.replace("’", "'");
            if (storyCounter.containsKey(headline)) {
              storyCounter.put(headline, storyCounter.get(headline).intValue() + 1);
            } else {
              storyCounter.put(headline, 1);
            }
          }

          // Re-populate tickerResearchMap and flowStoryList
          fileIterator = records.iterator();
          flowStoryList.clear();
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
              headline = headline.replace("’", "'");

              SimpleDateFormat dateFormatter = new SimpleDateFormat("M/dd/yyyy hh:mm:ss a");
              Calendar publishDate = Calendar.getInstance();
              publishDate.setTime(dateFormatter.parse(record.get(7)));

              String story = record.get(8);
              story = story.replace("’", "'");

              // Only keep CAD, USD stories with frequency <= 3
              if ((currency.equals("CAD") || currency.equals("USD"))) {
                TickerResearch tr = new TickerResearch();
                tr.rating = rating;
                tr.target = target;
                tr.researchLink = researchLink;
                tickerResearchMap.put(ticker, tr);
                if (tempPriceTargetChangeMap.containsKey(ticker))
                  tr.previousTarget = tempPriceTargetChangeMap.get(ticker);

                if (storyCounter.get(headline) <= 3) {
                  Calendar today = Calendar.getInstance();
                  today.set(Calendar.HOUR_OF_DAY, 0);
                  today.set(Calendar.MINUTE, 0);
                  today.set(Calendar.SECOND, 0);
                  today.set(Calendar.MILLISECOND, 0);
                  FlowStories fs = new FlowStories();
                  fs.author = analyst;
                  fs.link = researchLink;
                  fs.ticker = ticker;
                  fs.story = story;
                  fs.headline = headline;
                  if (publishDate.after(today)) flowStoryList.add(fs);
                }
              }

              currentTargetMap.put(ticker, target);

            } catch (Exception e) {
              logger.warn("Failed on line " + lineCounter);
              logger.error(e);
            }
          }
          Calendar marketOpen = Calendar.getInstance();
          marketOpen.set(Calendar.HOUR_OF_DAY, 9);
          marketOpen.set(Calendar.MINUTE, 30);
          marketOpen.set(Calendar.SECOND, 0);
          if (Calendar.getInstance().before(marketOpen)) {
            logger.info("Getting price target Changes");
            Engine.getInstance().populatePriceTargetChanges();
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
