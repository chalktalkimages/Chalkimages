package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import BlockReport.BlockReportGenerator;
import adapter.DBHandle;
import adapter.EquityFileParser;
import adapter.MailRequest;
import data.BlockAnalyst;
import data.BlockBroker;
import data.BlockDetails;
import data.BlockSecurity;
import data.CommentDetails;
import data.EnergyCommentDetails;
import data.FlowStories;
import data.GeneralComment;
import data.TraderSummary;
import html.ChalktalkReportBuilder;
import html.ClosingReportBuilder;
import html.EnergyInsightsReportBuilder;
import html.EnergyReportBuilder;
import html.GeoffMorningReportBuilder;
import html.HalftimeReportBuilder;
import html.MaterialsReportBuilder;
import html.MiningScoopReportBuilder;
import html.MobileGeoffMorningReportBuilder;
import html.MorningReportBuilder;
import html.ScotiaViewParser;
import html.Utilities;
import user.User;

public class Engine {
  private static Engine singleton_ = null;

  public ConcurrentHashMap<String, String> serverStatus = new ConcurrentHashMap<String, String>();

  // ticker, historical price in string, used to store old prices in ticker research in
  // EquityFileParser
  public HashMap<String, String> historicalPriceTargetChangesMap = new HashMap<String, String>();

  // tickers that changed target prices, current price in string
  public HashMap<String, String> priceTargetChangesMap = new HashMap<String, String>();

  private static final Logger logger = Logger.getLogger(Engine.class.getName());

  public static synchronized Engine getInstance() {
    if (singleton_ == null) singleton_ = new Engine();

    return singleton_;
  }

  public static void updateEmailRecipientsBlockReport(String fullName) throws IOException {

    final File tmpFile = new File("config" + ".tmp");
    final File file = new File("config");
    PrintWriter pw = new PrintWriter(tmpFile);
    BufferedReader br = new BufferedReader(new FileReader(file));
    final String toAdd = "to=" + Utilities.getEmailRecipients(fullName);
    for (String line; (line = br.readLine()) != null; ) {
      if (line.startsWith("to")) {
        line = toAdd;
      } else if (line.startsWith("body")) {
        line = "body=\\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild\\BlockReportEmail.html";
      } else if (line.startsWith("subject")) {
        line = "subject=Block Trade Report";
      } else if (line.startsWith("attachmentLoc")) {
        line = "attachmentLoc=\\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild";
      } else if (line.startsWith("attachments")) {
        line = "attachments=BlockTradeReport.xls";
      }
      pw.println(line);
    }

    br.close();
    pw.close();
    file.delete();
    File file2 = new File("config");
    tmpFile.renameTo(file2);
  }

  public static void updateEmailRecipientsMorningBlockReport(String fullName) throws IOException {

    final File tmpFile = new File("config" + ".tmp");
    final File file = new File("config");
    PrintWriter pw = new PrintWriter(tmpFile);
    BufferedReader br = new BufferedReader(new FileReader(file));
    final String toAdd = "bcc=" + Utilities.getEmailRecipients(fullName);
    for (String line; (line = br.readLine()) != null; ) {
      if (line.startsWith("bcc")) {
        line = toAdd;
      } else if (line.startsWith("to")) {
        line = "to=";
      } else if (line.startsWith("from")) {
        line = "from=george.gardiner@scotiabank.com";
      } else if (line.startsWith("body")) {
        line = "body=\\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild\\BlockReportEmail.html";
      } else if (line.startsWith("subject")) {
        if (fullName.contains("Colin")) line = "subject=Colin Block Trade Report";
        else if (fullName.contains("Paul")) line = "subject=Paul Block Trade Report";
        else if (fullName.contains("Scott")) line = "subject=Scott Block Trade Report";
        else if (fullName.contains("Ian")) line = "subject=Rusty Block Trade Report";
        else if (fullName.contains("Mining")) line = "subject=Mining Block Trade Report";
        else if (fullName.contains("R305")) line = "subject=Analyst/Trader Block Trade Report";
        else if (fullName.contains("R300")) line = "subject=Weekly Traders Market Share Report";
        else line = "subject=Block Trade Report";
      } else if (line.startsWith("attachmentLoc")) {
        if (fullName.contains("R305") || fullName.contains("R300")) line = "attachmentLoc=";
        else line = "attachmentLoc=\\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild";
      } else if (line.startsWith("attachments")) {
        if (fullName.contains("R305") || fullName.contains("R300")) line = "attachments=";
        else line = "attachments=BlockTradeReport.xls";
      }

      pw.println(line);
    }

    br.close();
    pw.close();
    file.delete();
    File file2 = new File("config");
    tmpFile.renameTo(file2);
  }

  // clear bcc field and change 'to' to "bill liu' again
  public void defaultEmailConfig() throws IOException {

    final File tmpFile = new File("config" + ".tmp");
    final File file = new File("config");
    PrintWriter pw = new PrintWriter(tmpFile);
    BufferedReader br = new BufferedReader(new FileReader(file));
    for (String line; (line = br.readLine()) != null; ) {
      if (line.startsWith("bcc")) {
        line = "bcc=";
      } else if (line.startsWith("to")) {
        line = "to=";
      } else if (line.startsWith("from")) {
        line = "from=puneet.kaur@scotiabank.com";
      }

      pw.println(line);
    }

    br.close();
    pw.close();
    file.delete();
    File file2 = new File("config");
    tmpFile.renameTo(file2);
  }

  public void generatemorningBlockReport(
      String fullname, ArrayList<BlockDetails> blocks, java.lang.Boolean isFlow, boolean isFiscal) {
    logger.info("Block Report build request received from: " + fullname + "\n");
    if (blocks == null || blocks.isEmpty()) {
      logger.info("Empty block list, report and email not generated.");
      return;
    }

    Collection<BlockBroker> blockbroker = BlockReportGenerator.getBlocksByBroker(blocks);
    Collection<BlockSecurity> blocksecurity =
        BlockReportGenerator.getBlocksBySecurity(blocks, isFlow);
    BlockReportGenerator.saveBlockEmailHtml(fullname, blockbroker, blocksecurity, isFiscal);
    HashMap<String, ArrayList<BlockDetails>> blockbyTimeBroker =
        BlockReportGenerator.sortbyBrokerTimeTicker(blocks);
    BlockReportGenerator.createBlockReportExcel(blockbroker, blocksecurity, blockbyTimeBroker);

    try {
      updateEmailRecipientsMorningBlockReport(fullname);
      MailRequest.sendRequest();

    } catch (IOException e) {
      logger.info("Error updating email recipients and sending email" + e.getMessage() + "\n");
    }
  }

  public void generateTraderAnalystBlockReport(String fullname, ArrayList<BlockDetails> blocks) {
    logger.info("Trader Analyst Block Report build request received from: " + fullname + "\n");

    Map<String, List<BlockAnalyst>> analystMap = BlockReportGenerator.getBlocksByAnalyst(blocks);
    Map<String, List<BlockAnalyst>> traderMap = BlockReportGenerator.getBlocksByTrader(blocks);

    BlockReportGenerator.saveAnalystTraderEmailHtml(fullname, analystMap, traderMap);

    // BlockReportGenerator.createBlockReportExcel(blockbroker, blocksecurity, blockbyTimeBroker);

    try {
      updateEmailRecipientsMorningBlockReport(fullname);
      MailRequest.sendRequest();

    } catch (IOException e) {
      logger.error("Error updating email recipients and sending email" + e.getMessage() + "\n", e);
    }
  }

  public void generateMarketShareReport(String fullname, ArrayList<BlockDetails> blocks)
      throws Exception {
    logger.info("Trader Analyst Block Report build request received from: " + fullname + "\n");
    //		System.out.println("herererrererer");
    //		System.out.println(blocks.size());
    Map<String, Map<String, TraderSummary>> traderMap =
        BlockReportGenerator.getMarketSharesByTrader(blocks);

    BlockReportGenerator.saveMarketShareSummaryEmailHtml(fullname, traderMap);

    // BlockReportGenerator.createBlockReportExcel(blockbroker, blocksecurity, blockbyTimeBroker);

    try {
      updateEmailRecipientsMorningBlockReport(fullname);
      MailRequest.sendRequest();

    } catch (IOException e) {
      logger.info("Error updating email recipients and sending email" + e.getMessage() + "\n");
    }
  }

  public void generateBlockReport(
      String fullname, ArrayList<BlockDetails> blocks, java.lang.Boolean isFlow) {

    try {
      defaultEmailConfig();
    } catch (IOException e1) {
      logger.error("Error in editing config file for emailer ...");
    }

    logger.info("Block Report build request received from: " + fullname + "\n");
    if (blocks == null || blocks.isEmpty()) {
      logger.info("Empty block list, report and email not generated.");
      return;
    }

    Collection<BlockBroker> blockbroker = BlockReportGenerator.getBlocksByBroker(blocks);
    Collection<BlockSecurity> blocksecurity =
        BlockReportGenerator.getBlocksBySecurity(blocks, isFlow);
    BlockReportGenerator.saveBlockEmailHtml(fullname, blockbroker, blocksecurity, false);
    HashMap<String, ArrayList<BlockDetails>> blockbyTimeBroker =
        BlockReportGenerator.sortbyBrokerTimeTicker(blocks);
    BlockReportGenerator.createBlockReportExcel(blockbroker, blocksecurity, blockbyTimeBroker);

    try {
      updateEmailRecipientsBlockReport(fullname);
      MailRequest.sendRequest();
    } catch (IOException e) {
      logger.info("Error updating email recipients and sending email" + e.getMessage() + "\n");
    }
  }

  public void generateChalktalk(
      int reportType,
      User user,
      ArrayList<CommentDetails> comments,
      Double eret,
      Double evol,
      Double mret,
      Double mvol,
      ArrayList<GeneralComment> generalComments,
      ArrayList<String> reportSections,
      boolean ranked) {

    logger.info("Report build request received from: " + user.firstName + "\n");
    if (!serverStatus.containsKey(user.getFullname()))
      serverStatus.put(user.getFullname(), "Initializing Report Build ...");

    try {
      defaultEmailConfig();
    } catch (IOException e1) {
      logger.error("Error in editing config file for emailer ...");
    }

    if (!ranked) {
      Collections.sort(comments, Utilities.getComparatorByTrendScore());
    } else {
      Collections.sort(comments, Utilities.getComparatorByRanking());
    }
    Collections.sort(generalComments, Utilities.getGeneralComparatorByRanking());

    if (reportType == 1) // chalk talk
    {
      ChalktalkReportBuilder.buildReport(
          user.getFullname(), comments, generalComments, reportSections, ranked);
    } else if (reportType == 2) // morning update
    {
      MorningReportBuilder.buildReport(user.getFullname(), comments, eret, evol, mret, mvol);
    } else if (reportType == 3) // halftime update
    {
      // sort by performance in descending order
      Collections.sort(comments, Utilities.getComparatorByReturns());
      HalftimeReportBuilder.buildReport(
          user.getFullname(), comments, eret, evol, mret, mvol, generalComments);
    } else if (reportType == 4) // closing update
    {
      ClosingReportBuilder.buildReport(user.getFullname(), comments, eret, evol, mret, mvol);
    } else if (reportType == 5) {

      EnergyReportBuilder.buildReport(user.getFullname(), comments);

    } else if (reportType == 6) {

      MaterialsReportBuilder.buildReport(user.getFullname(), comments);

    } else if (reportType == 8) {
      MiningScoopReportBuilder.buildReport(user.getFullname(), comments);
    } else if (reportType == 9) {
      GeoffMorningReportBuilder.buildReport(
          user.getFullname(), comments, generalComments, reportSections, false);
    } else if (reportType == 10) {
      GeoffMorningReportBuilder.buildReport(
          user.getFullname(), comments, generalComments, reportSections, true);
    } else if (reportType == 11) {
      MobileGeoffMorningReportBuilder.buildReport(
          user.getFullname(), comments, generalComments, reportSections, true, ranked);
    }
  }

  public void generateEnergyInsights(
      int reportType,
      User user,
      ArrayList<EnergyCommentDetails> comments,
      Double eret,
      Double evol,
      Double mret,
      Double mvol,
      ArrayList<GeneralComment> generalComments) {

    logger.info("Report build request received from: " + user.firstName + "\n");

    if (!serverStatus.containsKey(user.getFullname()))
      serverStatus.put(user.getFullname(), "Initializing Report Build ...");

    try {
      defaultEmailConfig();
    } catch (IOException e1) {
      logger.error("Error in editing config file for emailer ...");
    }

    Collections.sort(comments, Utilities.getEnergyComparatorByRanking());

    EnergyInsightsReportBuilder.buildReport(user.getFullname(), comments);
  }

  public List<FlowStories> getScotiaviewStories() {
    logger.info("Requesting today's Scotia View analyst stories...");
    ScotiaViewParser parser = new ScotiaViewParser();
    return parser.getScotiaviewStories();
  }

  public void populatePriceTargetChanges() {
    Map<String, String> previousTargetMap = DBHandle.historicalTargetPriceMap;
    for (Map.Entry<String, String> target : EquityFileParser.currentTargetMap.entrySet()) {
      String ticker = target.getKey();
      String price = target.getValue();
      // Check if data from database contains ticker, this would mean you need to potentially modify
      // database
      if (previousTargetMap.containsKey(ticker)) {
        if (!previousTargetMap
            .get(ticker)
            .equals(price)) { // Check if the database price is the same as current price
          if (!priceTargetChangesMap.containsKey(ticker)) { // Check if ticker is in changes map
            DBHandle.modifyPriceTarget(ticker, price);
            logger.info(
                "Target Price Change, modifying database for ticker: "
                    + ticker
                    + " from price "
                    + previousTargetMap.get(ticker)
                    + " to "
                    + price);
            historicalPriceTargetChangesMap.put(ticker, previousTargetMap.get(ticker));
            priceTargetChangesMap.put(ticker, price);
          } else { // If ticker exists in change map
            if (!priceTargetChangesMap
                .get(ticker)
                .equals(price)) { // Check if the price is the same, if not, modify
              DBHandle.modifyPriceTarget(ticker, price);
              logger.info(
                  "Target Price Change, modifying database for ticker: "
                      + ticker
                      + " from price "
                      + previousTargetMap.get(ticker)
                      + " to "
                      + price);
              historicalPriceTargetChangesMap.put(ticker, previousTargetMap.get(ticker));
              priceTargetChangesMap.put(ticker, price);
            }
          }
        }
      } else { // If ticker not in database map, then it needs to be added to the database
        if (!historicalPriceTargetChangesMap.containsKey(
            ticker)) { // If the change isn't recorded, perform the action, otherwise do nothing
          historicalPriceTargetChangesMap.put(ticker, price);
          logger.info("Adding ticker to database: " + ticker + " , " + price);
          DBHandle.addPriceTarget(ticker, price);
        }
      }
    }
  }

  public static void main(String[] args) {
    // System.out.println(" <a href=\"{rLink}>\"Link</a>".replace("{rLink}",
    // "https://scotiaview.com"));
  }
}
