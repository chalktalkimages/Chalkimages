package html;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  private static boolean titlesOnlyReport = false;

  public static void buildReport(
      String fullName,
      ArrayList<CommentDetails> comments,
      ArrayList<GeneralComment> generalComments,
      ArrayList<String> reportSections,
      boolean titlesOnly) {
    titlesOnlyReport = titlesOnly;
    name = fullName;
    ArrayList<Comment> bellmacroindexcomments = FlowAdapter.getComments();
    String macroComments = getMacroComments(generalComments);
    String fileSave = "ChalkTalkEmail.html";
    //    String fileSave = System.getProperty("user.dir") + "\\templates\\" +
    // "ChalkTalkEmail.html";

    String htmlSignature = Utilities.buildSignature(fullName);
    String body = Utilities.getHTMLString("GeoffMorningReportTemplate.html");
    String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());
    String buyList = getIOItradelist("buy");
    String sellList = getIOItradelist("sell");
    String morningIndications = getMorningIndications(buyList, sellList);
    String indexComments = buildIndexComments(bellmacroindexcomments);
    body = includeSelectedSections(reportSections, body, bellmacroindexcomments);

    body =
        body.replace("{{formattedDateTime}}", formattedDate)
            .replace("{{MorningIndications}}", morningIndications)
            .replace("{{macroComments}}", macroComments)
            .replace(
                "{{SectorSymbolComments}}",
                getNotableNews(
                    comments,
                    Utilities.sectionIncluded(reportSections, "Research Highlights"),
                    Utilities.sectionIncluded(reportSections, "Names in the News")))
            .replace("{{indexComments}}", indexComments)
            .replace("{{emailSignature}}", htmlSignature)
            .replace(
                "{{highlightComments}}",
                buildResearchHighlights(
                    comments,
                    Utilities.sectionIncluded(reportSections, "Research Highlights"),
                    Utilities.sectionIncluded(reportSections, "Names in the News")));

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

  public static String includeSelectedSections(
      ArrayList<String> reportSections, String htmlTemplate, ArrayList<Comment> comments) {
    String body = htmlTemplate;
    if (!Utilities.sectionIncluded(reportSections, "Macro Commentary")) {
      body = body.replace("{{MacroCommentary}}", "");
    } else {
      boolean found = false;

      for (Comment comment : comments) {
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
      if (titlesOnlyReport)
        body =
            body.replace("{{NamesInTheNews}}", Utilities.getHTMLString("TitlesOnlyNamesNews.html"));
      else
        body = body.replace("{{NamesInTheNews}}", Utilities.getHTMLString("GeoffNamesNews.html"));
    }

    if (!Utilities.sectionIncluded(reportSections, "Research Highlights")) {
      body = body.replace("{{ResearchHighlights}}", "");
    } else {

      body =
          body.replace(
              "{{ResearchHighlights}}", Utilities.getHTMLString("GeoffResearchHighlights.html"));
    }

    if (!Utilities.sectionIncluded(reportSections, "Index Events")) {
      body = body.replace("{{IndexEvents}}", "");
    } else {
      boolean found = false;

      for (Comment comment : comments) {
        if (comment.RIC().equals("IndexComment")) {
          found = true;
          break;
        }
      }
      if (found)
        body = body.replace("{{IndexEvents}}", Utilities.getHTMLString("IndexEvents.html"));
      else body = body.replace("{{IndexEvents}}", "");
    }
    return body;
  }

  public static String buildIndexComments(ArrayList<Comment> comments) {

    String result = "";
    String temp = "";
    String indexComment = Utilities.getHTMLString("MacroComment.html");

    for (Comment comment : comments) {
      if (comment.RIC().equals("IndexComment")) { // indicates index comment
        temp = indexComment;
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

  private static String getIOItradelist(String side) {
    String list = "";

    try {
      JSONObject json = Utilities.readJsonFromUrl(Globals.IOI_path + side + "-ioi");
      JSONArray iois = (JSONArray) json.get("iois");
      for (int i = 0; i < iois.length(); i++) {
        JSONObject ioi = iois.getJSONObject(i);
        if (ioi.getBoolean("natural")) {
          String ticker = ioi.getString("tickerPrefix");
          Double price = ioi.getDouble("limit");
          price = Math.round(price * 100.0) / 100.0;
          if (ticker.toLowerCase().contains(".PR".toLowerCase())
              || ticker.toLowerCase().contains(".DB".toLowerCase())) continue;
          if (price > 0.0) {
            list += ticker + " ($" + price.toString() + "), ";
          } else {
            list += ticker + ", ";
          }
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

  public static String getNotableNews(
      ArrayList<CommentDetails> comments,
      boolean includeHighlights,
      boolean includeDetailedComment) {
    String result = "";
    String temp = "";
    String sectorSymbolComment = Utilities.getHTMLString("GeoffSectorSymbolComments.html");
    if (titlesOnlyReport)
      sectorSymbolComment = Utilities.getHTMLString("TitlesOnlySectorSymbolComments.html");
    Map<String, ArrayList<CommentDetails>> sectorComments = getSectorCommentMap(comments);

    Set<String> sectors = sectorComments.keySet();
    for (String sector : sectors) {
      temp = sectorSymbolComment;
      ArrayList<CommentDetails> currComments = sectorComments.get(sector);
      String symbolComments =
          getSymbolComments(currComments, includeHighlights, includeDetailedComment);
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

  public static String buildResearchHighlights(
      ArrayList<CommentDetails> comments,
      boolean includeHighlights,
      boolean includeDetailedComment) {
    String highlightComment = Utilities.getHTMLString("GeoffHighlightComment.html");
    String temp2 = "";
    String highlightAgg = "";
    if (includeHighlights) {

      Collections.sort(comments, Utilities.getComparatorByRIC());

      for (CommentDetails comment : comments) {

        if (!comment.RIC().equals("")) { // symbol is present
          temp2 = highlightComment;
          String ticker =
              SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));

          if (!comment.summary().equals("")) {
            // if detailed comment included, include hyperlink
            if (includeDetailedComment) {
              temp2 =
                  temp2.replace(
                      "{{belongsTo}}",
                      " <a href=\"#{{belongsTo}}\" style=\"color: #000000; text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{belongsTo}}</a>");
              temp2 =
                  temp2.replace(
                      "{{body}}",
                      " <a href=\"#{{belongsTo}}\" style=\"color: #000000; text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{body}}</a>");
              temp2 =
                  temp2.replace(
                      "{{sentiment}}",
                      " <a href=\"#{{belongsTo}}\" style=\"text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{sentiment}}</a>");
            }
            temp2 =
                temp2
                    .replace("{{belongsTo}}", ticker)
                    .replace("{{sentiment}}", Utilities.formatGeoffSentimentNoDash(comment))
                    .replace(
                        "{{body}}", Utilities.parseQuoteComment(comment.summary(), false, true));
            highlightAgg = highlightAgg + temp2 + "\n";
          }
        }
      }
    }
    return highlightAgg;
  }

  public static String getSymbolComments(
      ArrayList<CommentDetails> comments,
      boolean includeHighlights,
      boolean includeDetailedComment) {
    String result = "";
    String temp = "";
    Collections.sort(comments, Utilities.getComparatorByRanking());
    String symbolComment = Utilities.getHTMLString("GeoffSymbolComment.html");
    if (titlesOnlyReport) symbolComment = Utilities.getHTMLString("TitlesOnlySymbolComment.html");
    ScotiaViewParser parser = new ScotiaViewParser();

    try {
      if (includeDetailedComment) {
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
            if (research.target.contains(".00")) {
              research.target = research.target.replace(".00", "");
            }
            if (research.previousTarget.contains(".00")) {
              research.previousTarget = research.previousTarget.replace(".00", "");
            }
            if (titlesOnlyReport) {
              temp =
                  temp.replace(
                      "{{body}}",
                      Utilities.formatGeoffSentiment(comment)
                          + Utilities.parseQuoteComment(comment.summary(), false, true));
              if (comments.size() == 2) {
                if (comments.indexOf(comment) == 0) {
                  temp =
                      temp.replace("{{paddingClass}}", "cell-padding-bottom")
                          .replace("{{linkPaddingClass}}", "cell-padding-bottom");
                } else {
                  temp = temp.replace("{{paddingClass}}", "").replace("{{linkPaddingClass}}", "");
                }
              } else {
                if (comments.indexOf(comment) == 0
                    || comments.indexOf(comment) == (comments.size() - 1)) {
                  temp = temp.replace("{{paddingClass}}", "").replace("{{linkPaddingClass}}", "");
                } else {
                  temp =
                      temp.replace("{{paddingClass}}", "cell-padding")
                          .replace("{{linkPaddingClass}}", "cell-padding");
                }
              }
            } else {
              temp =
                  temp.replace(
                      "{{body}}",
                      Utilities.formatGeoffSentiment(comment)
                          + Utilities.parseQuoteComment(comment.body(), false, true));
              temp = temp.replace("{{paddingClass}}", "").replace("{{linkPaddingClass}}", "");
            }
            if (!research.previousTarget.equals(research.target)
                && !research.previousTarget.equals("N/A")
                && titlesOnlyReport)
              temp =
                  temp.replace(
                      "{{target}}",
                      (research.target.indexOf("$") == -1 ? "$" + research.target : research.target)
                          + " from "
                          + (research.previousTarget.indexOf("$") == -1
                              ? "$" + research.previousTarget
                              : research.previousTarget));
            else
              temp =
                  temp.replace(
                      "{{target}}",
                      research.target.indexOf("$") == -1 ? "$" + research.target : research.target);
            temp =
                temp.replace("{{ticker}}", ticker)
                    .replace(
                        "{{rating}}",
                        getFirstLetters(
                            new ArrayList<String>(Arrays.asList(research.rating.split(" ")))))
                    .replace("{{researchLink}}", research.researchLink)
                    .replace("{{sentiment}}", sentiment);

            result = result + temp + "\n";
          }
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
    Collections.sort(comments, Utilities.getComparatorByRanking());
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

  public static String getFirstLetters(ArrayList<String> text) {
    String firstLetters = "";
    for (String s : text) {
      firstLetters += s.charAt(0);
    }
    return firstLetters;
  }
}
