package html;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import adapter.MailRequest;
import data.CommentDetails;
import engine.Engine;
import utils.SymbolConverter;

public class ClosingReportBuilder {

  // Constructs and Sends the report

  private static final Logger logger = Logger.getLogger(ClosingReportBuilder.class.getName());

  private static String pathString = "\\\\t65-w7-eqcash\\incoming\\";

  public static void buildReport(
      String fullName,
      ArrayList<CommentDetails> comments,
      Double eret,
      Double evol,
      Double mret,
      Double mvol) {

    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    String date = sdf.format(cal.getTime());

    Utilities.moveImages(date, "ClosingIndexReturns");
    Utilities.moveImages(date, "ClosingSectorsSnapshot");
    Utilities.moveImages(date, "blocklistImage");
    Utilities.moveImages(date, "blocksbySecurityImage");

    // Git commit/push images to remote repository
    Process p;
    try {
      p = Runtime.getRuntime().exec(pathString + "ChalkServer\\gitpush.bat");
      p.waitFor();
    } catch (IOException | InterruptedException e) {
      logger.info("Exception ocurred: git push to images remote repository");
    }

    String body = Utilities.getHTMLString("ClosingTemplate.html");

    String fileSave = "ChalkTalkEmail.html";
    String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());
    String imagePath =
        "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";
    String bellImage = imagePath + "ClosingIndexReturns" + date + ".png";
    String sectorsImage = imagePath + "ClosingSectorsSnapshot" + date + ".png";
    String blocklistImage = imagePath + "blocklistImage" + date + ".png";
    String blocksbySecurityImage = imagePath + "blocksbySecurityImage" + date + ".png";
    String otherComments = buildSymbolComments(comments);
    String htmlSignature = Utilities.getHTMLString("closingSignature.html");

    body =
        body.replace("{{formattedDate}}", formattedDate)
            .replace("{{sectorsImage}}", sectorsImage)
            .replace("{{bellImage}}", bellImage)
            .replace("{{blocklistImage}}", blocklistImage)
            .replace("{{blocksbySecurityImage}}", blocksbySecurityImage)
            .replace("{{otherComments}}", otherComments)
            .replace("{{closingSignature}}", htmlSignature)
            .replace(
                "{{group}}",
                (fullName.contains("Bill Liu")
                        || fullName.contains("Tony Ye")
                        || fullName.contains("Chad Reed")
                        || fullName.contains("Bilal Ijaz")
                        || fullName.contains("Andrew Moffatt")
                    ? "PORTFOLIO"
                    : "EQUITY"));

    body = body.replaceAll(Pattern.quote("ʼ"), "'");
    body = body.replaceAll(Pattern.quote("’"), "'");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("“"), "\"");
    body = body.replaceAll(Pattern.quote("”"), "\"");
    body = body.replaceAll(Pattern.quote("…"), "...");
    body = body.replaceAll(Pattern.quote("—"), "-");

    try (PrintWriter out = new PrintWriter(fileSave)) {
      out.println(body);
      // System.out.print("Template Printed to: " + fileSave + "\n");
      logger.info("Template Printed to: " + fileSave + "\n");

    } catch (FileNotFoundException e) {
      // System.err.println("Error writing html to file: " + e.getMessage() + "\n");
      logger.info("Error writing html to file: " + e.getMessage() + "\n");
    }

    try {
      Utilities.updateEmailRecipients(fullName, "After the Close");
    } catch (IOException e) {
      // System.err.println("Error sending email: " + e.getMessage() + "\n");
      logger.info("Error sending email: " + e.getMessage() + "\n");
    }

    Engine.getInstance().serverStatus.remove(fullName);
    MailRequest.sendRequest();
  }

  public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

    String result = "";
    String temp = "";
    String macroComment = Utilities.getHTMLString("MorningComment.html");

    for (CommentDetails comment : comments) {

      temp = macroComment;
      temp =
          temp.replace(
                  "{{belongsTo}}",
                  SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo())))
              .replace("{{evol}}", String.format("%1$.2f", comment.excessVol))
              .replace(
                  "{{body}}",
                  (comment.sentiment() != null ? comment.sentiment() + ": " : "")
                      + Utilities.parseQuoteComment(comment.body(), false, false));
      if (comment.returns >= 0) {
        temp =
            temp.replace("{{ret}}", "+" + String.format("%1$.2f", comment.returns * 100))
                .replace("{{color}}", "#27AE60");
      } else {
        temp =
            temp.replace("{{ret}}", String.format("%1$.2f", comment.returns * 100))
                .replace("{{color}}", "#C0392B");
      }
      result = result + temp + "\n";
    }

    return result;
  }
}
