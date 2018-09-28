package html;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import adapter.FlowAdapter;
import adapter.MailRequest;
import data.Comment;
import data.CommentDetails;
import data.GeneralComment;
import data.TickerResearch;
import engine.Engine;
import utils.Globals;
import utils.SymbolConverter;

public class GeoffMorningReportBuilder {
  private static final Logger logger = Logger.getLogger(ChalktalkReportBuilder.class.getName());
  private static String name = "";

  public static void buildReport(
      String fullName,
      ArrayList<CommentDetails> comments,
      ArrayList<GeneralComment> generalComments,
      ArrayList<String> reportSections) {
    name = fullName;
    ArrayList<Comment> bellmacroindexcomments = FlowAdapter.getComments();
    String macroComments = getMacroComments(generalComments);
    String fileSave = "ChalkTalkEmail.html";
    String htmlSignature = Utilities.getHTMLString("GeoffDarlingSignature.html");
    String body = Utilities.getHTMLString("GeoffMorningReportTemplate.html");
    String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());
    String buyList = getIOItradelist("buy");
    String sellList = getIOItradelist("sell");
    String morningIndications = getMorningIndications(buyList, sellList);

    if (!Utilities.sectionIncluded(reportSections, "Macro Commentary")) {
      body = body.replace("{{MacroCommentary}}", "");
    } else {
      boolean found = false;

      for (Comment comment : bellmacroindexcomments) {
        if (comment.RIC().equals("MacroComment")) {
          found = true;
          break;
        }
      }
      if (found)
        body = body.replace("{{MacroCommentary}}", Utilities.getHTMLString("MacroCommentary.html"));
      else body = body.replace("{{MacroCommentary}}", "");
    }

    if (!Utilities.sectionIncluded(reportSections, "Names in the News")) {
      body = body.replace("{{NamesInTheNews}}", "");
    } else {
      body = body.replace("{{NamesInTheNews}}", Utilities.getHTMLString("GeoffNamesNews.html"));
    }

    body =
        body.replace("{{formattedDateTime}}", formattedDate)
            .replace("{{MorningIndications}}", morningIndications)
            .replace("{{macroComments}}", macroComments)
            .replace("{{SectorSymbolComments}}", getNotableNews(comments))
            .replace("{{emailSignature}}", htmlSignature);

    body = Utilities.replaceOddCharacters(body);

    try (PrintWriter out = new PrintWriter(fileSave)) {
      out.println(body);
      logger.info("Template Printed to: " + fileSave + "\n");

    } catch (FileNotFoundException e) {
      logger.info("Error writing html to file: " + e.getMessage() + "\n");
    }

    try {
      Utilities.updateEmailRecipients(fullName, "Morning Notes");
    } catch (IOException e) {
      logger.info("Error sending email: " + e.getMessage() + "\n");
    }

    MailRequest.sendRequest();

    Engine.getInstance().serverStatus.remove(fullName);
  }

  private static String getIOItradelist(String side) {
    String list = "";

    try {
      JSONObject json = Utilities.readJsonFromUrl(Globals.IOI_path + side + "-ioi");
      JSONArray iois = (JSONArray) json.get("iois");
      for (int i = 0; i < iois.length(); i++) {
        JSONObject ioi = iois.getJSONObject(i);
        String ticker = ioi.getString("tickerPrefix");
        Double price = ioi.getDouble("limit");
        price = Math.round(price * 100.0) / 100.0;
        if (price > 0.0) {
          list += ticker + " ($" + price.toString() + "), ";
        }
      }
      list = list.replaceAll(", $", "");
      if (list == "") {
        list = "None";
      }
    } catch (Exception e) {
      logger.info("Error in getting list of IOI buys/sells: " + e.getMessage());
    }

    return list;
  }

  public static String getNotableNews(ArrayList<CommentDetails> comments) {
    String result = "";
    String temp = "";
    String sectorSymbolComment = Utilities.getHTMLString("GeoffSectorSymbolComments.html");
    Map<String, ArrayList<CommentDetails>> sectorComments = getSectorCommentMap(comments);

    Set<String> sectors = sectorComments.keySet();
    for (String sector : sectors) {
      temp = sectorSymbolComment;
      ArrayList<CommentDetails> currComments = sectorComments.get(sector);
      String symbolComments = getSymbolComments(currComments);
      temp = temp.replace("{{sector}}", sector).replace("{{symbolComments}}", symbolComments);
      result = result + temp + "\n";
    }
    return result;
  }

  public static String getMorningIndications(String buy, String sell) {
    String morningIndications = Utilities.getHTMLString("GeoffMorningIndications.html");
    morningIndications =
        morningIndications.replace("{{buylist}}", buy).replace("{{selllist}}", sell);
    return morningIndications;
  }

  public static String getSymbolComments(ArrayList<CommentDetails> comments) {
    String result = "";
    String temp = "";
    String symbolComment = Utilities.getHTMLString("GeoffSymbolComment.html");
    ScotiaViewParser parser = new ScotiaViewParser();

    try {

      for (CommentDetails comment : comments) {

        if (!comment.RIC().equals("")) { // symbol is present
          temp = symbolComment;
          String ticker =
              SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));
          TickerResearch research;
          if (comment.tickerResearch != null
              && !comment.tickerResearch.rating.isEmpty()
              && !comment.tickerResearch.rating.equals("N/A")) research = comment.tickerResearch;
          else {
            Utilities.updateStatus(name, ticker);
            research = parser.getSymbolResearch(comment.belongsTo(), ticker);
          }
          String sentiment = "";
          if (comment.sentiment() != null) {
            if (comment.sentiment().equals("Positive")) {
              sentiment += "<font color='#27AE60'>+ve</font> ";
            } else if (comment.sentiment().equals("Negative")) {
              sentiment += "<font color='#ed1b2e'>-ve</font> ";
            }
          }
          temp =
              temp.replace("{{ticker}}", ticker)
                  .replace(
                      "{{body}}",
                      Utilities.formatSentiment(comment)
                          + Utilities.parseQuoteComment(comment.body(), false, true))
                  .replace("{{rating}}", research.rating)
                  .replace(
                      "{{target}}",
                      research.target.indexOf("$") == -1 ? "$" + research.target : research.target)
                  .replace("{{researchLink}}", research.researchLink)
                  .replace("{{sentiment}}", sentiment);
          ;
          result = result + temp + "\n";
        }
      }

    } catch (Exception e) {
      // e.printStackTrace();
      logger.info("Error: " + e.getMessage());
    }

    return result;
  }

  public static Map<String, ArrayList<CommentDetails>> getSectorCommentMap(
      ArrayList<CommentDetails> comments) {
    Map<String, ArrayList<CommentDetails>> sectorComments =
        new LinkedHashMap<String, ArrayList<CommentDetails>>();
    for (int i = 0; i < comments.size(); i++) {
      CommentDetails comment = comments.get(i);
      String sector = comment.sector();
      ArrayList<CommentDetails> currSectorComments;
      if (sectorComments.get(sector) == null) {
        currSectorComments = new ArrayList<CommentDetails>();
      } else {
        currSectorComments = sectorComments.get(sector);
      }
      currSectorComments.add(comment);
      sectorComments.put(sector, currSectorComments);
    }
    return sectorComments;
  }

  public static String getMacroComments(ArrayList<GeneralComment> comments) {

    String result = "";
    String temp = "";
    String macroComment = Utilities.getHTMLString("MacroComment.html");

    for (GeneralComment comment : comments) {
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
      if (comment.link() != null && !comment.link().equals("") && !comment.link().equals("null")) {
        temp =
            temp.replace(
                "{{link}}", " <a href=\"{rLink}\">Link</a>".replace("{rLink}", comment.link()));
      } else {
        temp = temp.replace("{{link}}", "");
      }
      result = result + temp + "\n";
    }
    return result;
  }
}
