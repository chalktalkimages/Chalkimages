package html;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import adapter.FlowAdapter;
import adapter.MailRequest;
import data.Comment;
import data.CommentDetails;
import data.GeneralComment;
import data.TickerResearch;
import engine.Engine;
import utils.SymbolConverter;

public class ChalktalkReportBuilder {

  private static final Logger logger = Logger.getLogger(ChalktalkReportBuilder.class.getName());

  private static String name = "";

  public static void buildReport(
      String fullName,
      ArrayList<CommentDetails> comments,
      ArrayList<GeneralComment> generalComments,
      ArrayList<String> reportSections) {

    name = fullName;

    Calendar cal = Calendar.getInstance();
    // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String date = sdf.format(cal.getTime());

    Utilities.moveImages(date, "Bell");
    Utilities.moveImages(date, "Marb");
    Utilities.moveImages(date, "Revisions");

    // Git commit/push images to remote repository
    Process p;
    try {
      p = Runtime.getRuntime().exec("C:\\incoming\\ChalkServer\\gitpush.bat");
      p.waitFor();
    } catch (IOException | InterruptedException e) {
      logger.info("Exception ocurred: git push to images remote repository " + e.getMessage());
    }

    ArrayList<Comment> bellmacroindexcomments = FlowAdapter.getComments();

    String fileSave = "ChalkTalkEmail.html";
    //    String fileSave = System.getProperty("user.dir") + "\\templates\\" +
    // "ChalkTalkEmail.html";
    String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());
    String imagePath =
        "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";

    //		String bellImage = "/images/Bell.png";
    //		String revisionsImage = "/images/Revisions.png";
    //		String marbImage = "/images/Marb.png";

    String bellImage = imagePath + "Bell" + date + ".png";
    String revisionsImage = imagePath + "Revisions" + date + ".png";
    String marbImage = imagePath + "Marb" + date + ".png";

    String bellComments = buildBellComments(bellmacroindexcomments);
    String macroComments = buildMacroComments(generalComments);
    String indexComments = buildIndexComments(bellmacroindexcomments);
    ArrayList<String> shortLongComments =
        buildChalktalkSymbolComments(
            comments,
            Utilities.sectionIncluded(reportSections, "Research Highlights"),
            Utilities.sectionIncluded(reportSections, "Names in the News"));
    String highlightComments = shortLongComments.get(1);
    String symbolComments = getSectorNameInNews(comments, reportSections);
    String htmlSignature = Utilities.buildSignature(fullName);

    String body = Utilities.getHTMLString("ChalkTalkTemplate.html");

    body = includeSelectedSections(reportSections, body, bellmacroindexcomments);

    if (fullName.contains("George Gardiner")) {
      bellImage = "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\GeorgeChalktalkImages\\Bell.png";
      marbImage = "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\GeorgeChalktalkImages\\Marb.png";
      revisionsImage =
          "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\GeorgeChalktalkImages\\Revisions.png";
    }

    body =
        body.replace("{{formattedDate}}", formattedDate)
            .replace("{{revisionsImage}}", revisionsImage)
            .replace("{{bellImage}}", bellImage)
            .replace("{{marbImage}}", marbImage)
            .replace("{{bellComments}}", bellComments)
            .replace("{{highlightComments}}", highlightComments)
            .replace("{{symbolComments}}", symbolComments)
            .replace("{{emailSignature}}", htmlSignature)
            .replace("{{macroComments}}", macroComments)
            .replace("{{indexComments}}", indexComments)
            .replace("{{group}}", "EQUITY");

    body = Utilities.replaceOddCharacters(body);

    try (PrintWriter out = new PrintWriter(fileSave)) {
      out.println(body);
      logger.info("Template Printed to: " + fileSave + "\n");

    } catch (FileNotFoundException e) {
      logger.info("Error writing html to file: " + e.getMessage() + "\n");
    }

    try {
      Utilities.updateEmailRecipients(fullName, "Scotiabank Chalk Talk");
    } catch (IOException e) {
      logger.info("Error sending email: " + e.getMessage() + "\n");
    }

    MailRequest.sendRequest();

    Engine.getInstance().serverStatus.remove(fullName);
  }

  public static String includeSelectedSections(
      ArrayList<String> reportSections, String htmlTemplate, ArrayList<Comment> comments) {
    String body = htmlTemplate;
    if (!Utilities.sectionIncluded(reportSections, "Pertinent Revisions")) {
      body = body.replace("{{PertinentRevisions}}", "");
    } else {
      body =
          body.replace(
              "{{PertinentRevisions}}", Utilities.getHTMLString("PertinentRevisions.html"));
    }

    if (!Utilities.sectionIncluded(reportSections, "Research Highlights")) {
      body = body.replace("{{ResearchHighlights}}", "");
    } else {

      body =
          body.replace(
              "{{ResearchHighlights}}", Utilities.getHTMLString("ResearchHighlights.html"));
    }

    if (!Utilities.sectionIncluded(reportSections, "Before the Bell")) {
      body = body.replace("{{BeforeBell}}", "");
    } else {
      boolean found = false;

      for (Comment comment : comments) {
        if (comment.RIC().equals("BellComment")) {
          found = true;
          break;
        }
      }
      if (found) body = body.replace("{{BeforeBell}}", Utilities.getHTMLString("BeforeBell.html"));
      else body = body.replace("{{BeforeBell}}", "");
    }

    if (!Utilities.sectionIncluded(reportSections, "Names in the News")) {
      body = body.replace("{{NamesNews}}", "");
    } else {
      body = body.replace("{{NamesNews}}", Utilities.getHTMLString("NamesNews.html"));
    }

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

    if (!Utilities.sectionIncluded(reportSections, "Canadian M&A Update")) {
      body = body.replace("{{CanadianMA}}", "");
    } else {
      body = body.replace("{{CanadianMA}}", Utilities.getHTMLString("CanadianMA.html"));
    }
    return body;
  }

  // Build the HTML for the index-events-comments
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

  // Build the HTML for the before-the-bell-comments
  public static String buildBellComments(ArrayList<Comment> comments) {

    String result = "";
    String temp = "";
    String bellComment = Utilities.getHTMLString("MacroComment.html");

    for (Comment comment : comments) {
      if (comment.RIC().equals("BellComment")) { // indicates before the bell comment
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

  public static String buildMacroComments(ArrayList<GeneralComment> comments) {

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

  // Build the HTML for the symbol-specific comments
  public static ArrayList<String> buildChalktalkSymbolComments(
      ArrayList<CommentDetails> comments,
      boolean includeHighlights,
      boolean includeDetailedComment) {

    ArrayList<String> commentStrings = new ArrayList<String>();
    String result = "";
    String highlightAgg = "";
    String temp = "";
    String temp2 = "";
    String symbolComment = Utilities.getHTMLString("ChalktalkSymbolComment.html");
    String highlightComment = Utilities.getHTMLString("highlightComment.html");
    ScotiaViewParser parser = new ScotiaViewParser();
    Collections.sort(comments, Utilities.getComparatorByRanking());

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

            temp =
                temp.replace("{{belongsTo}}", ticker)
                    .replace(
                        "{{body}}",
                        Utilities.formatSentiment(comment)
                            + Utilities.parseQuoteComment(comment.body(), false, true))
                    .replace("{{rating}}", research.rating)
                    .replace(
                        "{{target}}",
                        research.target.indexOf("$") == -1
                            ? "$" + research.target
                            : research.target)
                    .replace("{{researchLink}}", research.researchLink);
            if (includeHighlights) {
              temp =
                  temp.replace(
                      "{{linkHighlight}}",
                      " <a href=\"#Research Highlights\" title=\"Back to Research Highlights\">Back</a>");
            } else {
              temp = temp.replace("{{linkHighlight}}", "");
            }

            result = result + temp + "\n";
          }
        }
      }

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
                        " <a href=\"#Our Take on {{belongsTo}}\" style=\"color:#5a5a5a; text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{belongsTo}}</a>");
                temp2 =
                    temp2.replace(
                        "{{body}}",
                        " <a href=\"#Our Take on {{belongsTo}}\" style=\"color:#5a5a5a; text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{body}}</a>");
                temp2 =
                    temp2.replace(
                        "{{sentiment}}",
                        " <a href=\"#Our Take on {{belongsTo}}\" style=\"text-decoration: none;\" title=\"Click to read our take on {{belongsTo}}\">{{sentiment}}</a>");
              }
              String summary = Utilities.parseQuoteComment(comment.summary(), false, true);
              if (summary.length() > 100) {
                summary = summary.substring(0, 99);
                summary += "...";
              }
              temp2 =
                  temp2
                      .replace("{{belongsTo}}", ticker)
                      .replace("{{sentiment}}", Utilities.formatSentimentNoDash(comment))
                      .replace("{{body}}", summary);
              highlightAgg = highlightAgg + temp2 + "\n";
            }
          }
        }
      }

    } catch (Exception e) {
      // e.printStackTrace();
      logger.info("Error: " + e.getMessage());
    }

    commentStrings.add(result);
    commentStrings.add(highlightAgg);

    return commentStrings;
  }

  // Build the HTML for the symbol-specific comments
  public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

    String result = "";
    String temp = "";
    String symbolComment = Utilities.getHTMLString("ChalktalkSymbolComment.html");
    ScotiaViewParser parser = new ScotiaViewParser();
    Collections.sort(comments, Utilities.getComparatorByRanking());

    try {

      for (CommentDetails comment : comments) {

        if (!comment.RIC().equals("")) { // symbol is present
          temp = symbolComment;
          String ticker =
              SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));
          TickerResearch research;
          if (comment.tickerResearch != null) research = comment.tickerResearch;
          else {
            Utilities.updateStatus(name, ticker);
            research = parser.getSymbolResearch(comment.belongsTo(), ticker);
          }

          temp =
              temp.replace("{{belongsTo}}", ticker)
                  .replace(
                      "{{body}}",
                      Utilities.formatSentiment(comment)
                          + Utilities.parseQuoteComment(comment.body(), false, true))
                  .replace("{{rating}}", research.rating)
                  .replace("{{target}}", research.target)
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

  public static String getSectorNameInNews(
      ArrayList<CommentDetails> comments, ArrayList<String> reportSections) {
    String result = "";
    String temp = "";
    String sectorSymbolComment = Utilities.getHTMLString("ChalkTalkSectorSymbolComment.html");
    Map<String, ArrayList<CommentDetails>> sectorComments = getSectorCommentMap(comments);

    Set<String> sectors = sectorComments.keySet();
    for (String sector : sectors) {
      temp = sectorSymbolComment;
      ArrayList<CommentDetails> currComments = sectorComments.get(sector);
      ArrayList<String> symbolComments =
          buildChalktalkSymbolComments(
              currComments,
              Utilities.sectionIncluded(reportSections, "Research Highlights"),
              Utilities.sectionIncluded(reportSections, "Names in the News"));
      temp =
          temp.replace("{{sector}}", sector).replace("{{symbolComments}}", symbolComments.get(0));
      result = result + temp + "\n";
    }
    return result;
  }

  public static Map<String, ArrayList<CommentDetails>> getSectorCommentMap(
      ArrayList<CommentDetails> comments) {
    Collections.sort(comments, Utilities.getComparatorByRanking());
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

  public static void main(String[] args) throws IOException, InterruptedException, SQLException {}
}
