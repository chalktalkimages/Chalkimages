package html;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import adapter.FlowAdapter;
import adapter.MailRequest;
import data.Comment;
import data.CommentDetails;
import data.CorporateEvent;
import data.TickerResearch;
import engine.Engine;
import utils.SymbolConverter;

// Builds the HTML content of the report
public class MiningScoopReportBuilder {

  private static final Logger logger = Logger.getLogger(MiningScoopReportBuilder.class.getName());

  private static String name = "";

  private static String[] headerList = {
    "gold", "base metals", "copper supply", "cu supply", "iron ore", "met coal"
  };

  private static ArrayList<Comment> sortMiningComments(ArrayList<Comment> comments) {

    ArrayList<Comment> sortedList = new ArrayList<Comment>();
    ArrayList<Integer> addedCommentIndices = new ArrayList<Integer>();
    int listSize = comments.size();

    for (int i = 0; i < headerList.length; i++) {
      for (int j = 0; j < listSize; j++) {
        String title = comments.get(j).belongsTo().toLowerCase();
        if (title.contains(headerList[i]) && (!addedCommentIndices.contains(j))) {
          sortedList.add(comments.get(j));
          addedCommentIndices.add(j);
        }
      }
    }

    // add the comments that aren't in the default header list at the end:
    for (int i = 0; i < listSize; i++) {
      if (!addedCommentIndices.contains(i)) {
        sortedList.add(comments.get(i));
      }
    }

    return sortedList;
  }

  public static void buildReport(String fullName, ArrayList<CommentDetails> comments) {

    name = fullName;

    String body = Utilities.getHTMLString("MiningScoopTemplate.html");
    ArrayList<Comment> allcomments = sortMiningComments(FlowAdapter.getComments());
    List<CorporateEvent> listEvents = FlowAdapter.getEvents();
    String events = Utilities.buildeventsList(listEvents, "Materials");

    String fileSave = "ChalkTalkEmail.html";
    String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());
    // String imagePath =
    // "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";
    String symbolComments = buildSymbolComments(comments);
    String htmlSignature = Utilities.getHTMLString("materialsSignature.html");
    String miningComments = buildminingComments(allcomments);

    String ChartofDay =
        "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\MiningScoopImages\\ChartofDay.png";
    String Inventories =
        "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\MiningScoopImages\\Inventories.png";
    String MorningPriceChart =
        "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\MiningScoopImages\\MorningPriceChart.png";

    String bellComments = buildBellComments(allcomments);
    String macroComments = buildMacroComments(allcomments);

    String buylist = getIOItradelist("buy");
    String selllist = getIOItradelist("sell");

    body =
        body.replace("{{ChartofDayImage}}", ChartofDay)
            .replace("{{InventoriesImage}}", Inventories)
            .replace("{{MorningPriceChartImage}}", MorningPriceChart);

    body =
        body.replace("{{formattedDate}}", formattedDate)
            .replace("{{symbolComments}}", symbolComments)
            .replace("{{emailSignature}}", htmlSignature)
            .replace("{{miningComments}}", miningComments + "\n" + bellComments)
            .replace("{{bellComments}}", bellComments)
            .replace("{{macroComments}}", macroComments)
            .replace("{{buylist}}", buylist)
            .replace("{{selllist}}", selllist)
            .replace("{{eventTypesDatesNames}}", events)
            .replace(
                "{{group}}",
                (fullName.contains("Bill Liu")
                        || fullName.contains("Chad Reed")
                        || fullName.contains("Bilal Ijaz"))
                    ? "PORTFOLIO"
                    : "EQUITY");

    body = body.replaceAll(Pattern.quote("ʼ"), "'");
    body = body.replaceAll(Pattern.quote("’"), "'");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("“"), "\"");
    body = body.replaceAll(Pattern.quote("”"), "\"");
    body = body.replaceAll(Pattern.quote("…"), "...");
    body = body.replaceAll(Pattern.quote("—"), "-");
    body = body.replaceAll(Pattern.quote("€"), "&#8364;");

    try (PrintWriter out = new PrintWriter(fileSave)) {
      out.println(body);
      // System.out.print("Template Printed to: " + fileSave + "\n");
      logger.info("Template Printed to: " + fileSave + "\n");

    } catch (FileNotFoundException e) {
      // System.err.println("Error writing html to file: " + e.getMessage() + "\n");
      logger.info("Error writing html to file: " + e.getMessage() + "\n");
    }

    try {
      Utilities.updateEmailRecipients(fullName, "Scotia Daily Morning Scoop");
    } catch (IOException e) {
      // System.err.println("Error sending email: " + e.getMessage() + "\n");
      logger.info("Error sending email: " + e.getMessage() + "\n");
    }

    Engine.getInstance().serverStatus.remove(fullName);
    MailRequest.sendRequest();
  }

  public static String buildMacroComments(ArrayList<Comment> comments) {

    String result = "";
    String temp = "";
    String macroComment = Utilities.getHTMLString("MacroComment.html");

    for (Comment comment : comments) {
      if (comment.RIC().equals("MacroComment")) {
        temp = macroComment;

        if (comment.belongsTo() == null) {
          temp = temp.replace("{{belongsTo}}", " ");
        } else {
          temp = temp.replace("{{belongsTo}}", comment.belongsTo());
        }
        if (comment.body() == null) {
          temp = temp.replace("{{body}}", " ");
        } else {
          temp = temp.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), false, true));
        }
        if (comment.link() != null
            && !comment.link().equals("")
            && !comment.link().equals("null")) {
          temp =
              temp.replace(
                  "{{link}}", " <a href=\"{rLink}\">Link</a>".replace("{rLink}", comment.link()));
        } else {
          temp = temp.replace("{{link}}", "");
        }
        result = result + temp + "\n";
      }
    }

    return result;
  }

  // Build the HTML for the before-the-bell-comments
  public static String buildminingComments(ArrayList<Comment> comments) {

    String result = "";
    String temp = "";
    String bellComment = Utilities.getHTMLString("MacroComment.html");

    for (Comment comment : comments) {
      if (comment.RIC().equals("MiningComment")) { // indicates before the bell comment
        temp = bellComment;
        if (comment.belongsTo() == null) {
          temp = temp.replace("{{belongsTo}}", " ");
        } else {
          temp = temp.replace("{{belongsTo}}", comment.belongsTo());
        }
        if (comment.body() == null) {
          temp = temp.replace("{{body}}", " ");
        } else {
          temp = temp.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), false, true));
        }
        temp = temp.replace("{{link}}", "");
        result = result + temp + "\n";
      }
    }
    return result;
  }

  // Build the HTML for the before-the-bell-comments
  public static String buildBellComments(ArrayList<Comment> comments) {

    String result = "";
    String temp = "";
    String bellComment = Utilities.getHTMLString("MacroComment.html");

    for (Comment comment : comments) {
      if (comment.RIC().equals("BellComment")
          && comment.belongsTo() != null
          && comment.belongsTo().contains("Currencies")) { // indicates before the bell comment
        temp = bellComment;
        if (comment.belongsTo() == null) {
          temp = temp.replace("{{belongsTo}}", " ");
        } else {
          temp = temp.replace("{{belongsTo}}", comment.belongsTo());
        }
        if (comment.body() == null) {
          temp = temp.replace("{{body}}", " ");
        } else {
          temp = temp.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), false, true));
        }
        if (comment.link() != null
            && !comment.link().equals("")
            && !comment.link().equals("null")) {
          temp =
              temp.replace(
                  "{{link}}", " <a href=\"{rLink}\">Link</a>".replace("{rLink}", comment.link()));
        } else {
          temp = temp.replace("{{link}}", "");
        }
        result = result + temp + "\n";
      }
    }
    return result;
  }

  // Build the HTML for the symbol-specific comments
  public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

    String result = "";
    String temp = "";
    String symbolComment = Utilities.getHTMLString("MiningScoopComment.html");
    ScotiaViewParser parser = new ScotiaViewParser();

    try {
      for (CommentDetails comment : comments) {

        if (!comment.RIC().equals("")) { // symbol is present
          temp = symbolComment;
          String ticker =
              SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));
          Utilities.updateStatus(name, ticker);
          TickerResearch research = parser.getSymbolResearch(comment.belongsTo(), ticker);

          String analystName = Utilities.getAnalyst(comment.RIC());

          temp =
              temp.replace("{{belongsTo}}", ticker)
                  .replace(
                      "{{body}}",
                      Utilities.formatSentiment(comment)
                          + Utilities.parseQuoteComment(comment.body(), false, true))
                  .replace("{{rating}}", research.rating)
                  .replace("{{target}}", research.target)
                  .replace("{{analystName}}", analystName)
                  .replace("{{researchLink}}", research.researchLink);
          result = result + temp + "\n";
        }
      }

    } catch (Exception e) {
      // e.printStackTrace();
      logger.info("Error: " + e.getMessage());
    }

    return result;
  }

  private static String getIOItradelist(String side) {

    String list = "";

    try {
      JSONObject json = Utilities.readJsonFromUrl("http://t65-w7-eqcash:8001/" + side + "-ioi");
      JSONArray iois = (JSONArray) json.get("iois");
      for (int i = 0; i < iois.length(); i++) {
        JSONObject obj = iois.getJSONObject(i);
        JSONObject partialsmap = obj.getJSONObject("partialsMap");

        Iterator array = partialsmap.keys();
        String key = "";

        while (array.hasNext()) {
          key = (String) array.next();
        }
        JSONObject a;
        String ric;
        String ticker;
        if (!key.equals("")) {

          a = partialsmap.getJSONObject(key);
          ticker = a.getString("ticker");
          ric = a.getString("RIC");
        } else {
          a = obj;
          ticker = a.getString("tickerPrefix");
          ric = a.getString("ticker");
        }

        boolean working = a.getBoolean("working");
        boolean retail = a.getBoolean("retail");

        if (!working
            && !retail
            && ticker != null
            && ric.endsWith(".TO")
            && !ric.contains("db.TO")
            && Utilities.isMaterialsTicker(ric)) {
          int numshares = obj.getInt("shares");
          list += ", " + String.format("%1$d", numshares) + " " + ticker;
          // counter++;
        }
      }
      list = list.replaceFirst(", ", "");
    } catch (Exception e) {
      logger.info("Error in getting list of IOI buys/sells: " + e.getMessage());
    }

    return list;
  }

  public static void main(String[] args) throws IOException, InterruptedException, SQLException {}
}
