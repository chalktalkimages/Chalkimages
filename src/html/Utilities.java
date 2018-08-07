package html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import adapter.FlowAdapter;
import data.CommentDetails;
import data.CorporateEvent;
import data.EnergyCommentDetails;
import data.GeneralComment;
import engine.Engine;
import user.UserEmail;

public class Utilities {

  private static final Logger logger = Logger.getLogger(Utilities.class.getName());

  public static void updateStatus(String name, String currTicker) {
    Engine.getInstance()
        .serverStatus
        .put(name, "Retrieving ScotiaView data for " + currTicker + " ... ");
  }

  public static boolean sectionIncluded(ArrayList<String> sections, String sectionTarget) {
    for (String section : sections) {
      if (section.equals(sectionTarget)) return true;
    }

    return false;
  }

  public static Comparator<CommentDetails> getComparatorByRanking() {
    return new Comparator<CommentDetails>() {
      @Override
      public int compare(CommentDetails comment2, CommentDetails comment1) {
        if ((comment2.ranking - comment1.ranking) > 0) return 1;
        else if ((comment2.ranking - comment1.ranking) < 0) return -1;
        else {
          if (comment2.belongsTo() == null || comment1.belongsTo() == null) return 0;
          if ((comment2.belongsTo().compareTo(comment1.belongsTo())) > 0) return 1;
          else if ((comment2.belongsTo().compareTo(comment1.belongsTo())) < 0) return -1;
          else {
            return 0;
          }
        }
      }
    };
  }

  public static Comparator<GeneralComment> getGeneralComparatorByRanking() {
    return new Comparator<GeneralComment>() {
      @Override
      public int compare(GeneralComment comment2, GeneralComment comment1) {
        return comment2.ranking - comment1.ranking;
      }
    };
  }

  public static Comparator<CommentDetails> getComparatorByReturns() {
    return new Comparator<CommentDetails>() {
      @Override
      public int compare(CommentDetails comment2, CommentDetails comment1) {
        return (int) ((comment1.returns - comment2.returns) * 10000000);
      }
    };
  }

  public static Comparator<EnergyCommentDetails> getEnergyComparatorByRanking() {
    return new Comparator<EnergyCommentDetails>() {
      @Override
      public int compare(EnergyCommentDetails comment2, EnergyCommentDetails comment1) {
        return comment2.ranking - comment1.ranking;
      }
    };
  }

  public static Comparator<CommentDetails> getComparatorByRIC() {
    return new Comparator<CommentDetails>() {
      @Override
      public int compare(CommentDetails comment2, CommentDetails comment1) {
        if (comment2.belongsTo() == null || comment1.belongsTo() == null) return 0;
        if ((comment2.belongsTo().compareTo(comment1.belongsTo())) > 0) return 1;
        else if ((comment2.belongsTo().compareTo(comment1.belongsTo())) < 0) return -1;
        else {
          return 0;
        }
      }
    };
  }

  public static void moveImages(String date, String name) // name = Bell, Marb, Revisions
      {

    File s = new File("C:\\incoming\\ChalkServer\\Chalkimages\\" + name + ".png");
    File d = new File("C:\\incoming\\ChalkServer\\Chalkimages\\archive\\" + name + ".png");
    try {
      FileUtils.copyFile(s, d);
      Path source = Paths.get("C:\\incoming\\ChalkServer\\Chalkimages\\archive\\" + name + ".png");
      Files.move(
          source, source.resolveSibling(name + date + ".png"), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      logger.info("moveImages method encountered exception: " + e.getMessage());
    }
  }

  public static String formatSentiment(CommentDetails comment) {
    if (comment.sentiment() != null
        && !comment.sentiment().contains("null")
        && !comment.sentiment().equals("")) {
      if (comment.sentiment().contains("Positive"))
        return "<font color=\"#27AE60\">" + comment.sentiment() + "</font>" + " - ";
      else if (comment.sentiment().contains("Negative"))
        return "<font color=\"#C0392B\">" + comment.sentiment() + "</font>" + " - ";
      else return comment.sentiment() + " - ";
    } else return "";
  }

  public static String formatSentimentNoDash(CommentDetails comment) {
    if (comment.sentiment() != null
        && !comment.sentiment().contains("null")
        && !comment.sentiment().equals("")) {
      if (comment.sentiment().contains("Positive"))
        return "<font color=\"#27AE60\">" + comment.sentiment() + "</font>";
      else if (comment.sentiment().contains("Negative"))
        return "<font color=\"#C0392B\">" + comment.sentiment() + "</font>";
      else return "<font color=\"#5a5a5a\">" + comment.sentiment() + "</font>";
    } else return "";
  }

  public static String parseQuoteComment(String comment, boolean isAndrewMoffatt, boolean isChalk) {

    boolean flag = true;
    String symbol;
    int i;
    String htmlSymbol;
    int charAfter, period, comma, bracket, space;
    ArrayList<String> symbolList = new ArrayList<String>();
    String substring = comment;

    while (flag) {
      i = substring.indexOf("#");
      if (i < 0) {
        break;
      }
      substring = substring.substring(i + 1); // gets everything after #

      space = substring.indexOf(" ");
      period = substring.indexOf(".");
      comma = substring.indexOf(",");
      bracket = substring.indexOf(")");
      if (space == -1) {
        space = 100000;
      }
      if (period == -1) {
        period = 100000;
      }
      if (comma == -1) {
        comma = 100000;
      }
      if (bracket == -1) {
        bracket = 100000;
      }

      if (space < comma && space < bracket && space < period) {
        charAfter = space;
      } else if (period < comma && period < bracket && period < space) {
        charAfter = period;
      } else if (comma < period && comma < bracket && comma < space) {
        charAfter = comma;
      } else if (bracket < period && bracket < comma && bracket < space) {
        charAfter = bracket;
      } else {
        charAfter = substring.length();
      }

      symbol = substring.substring(0, charAfter);
      if (!symbolList.contains(symbol)) {
        symbolList.add(symbol);
      }
    }

    for (String ticker : symbolList) {
      substring = '#' + ticker; // substring to replace in value
      htmlSymbol = FlowAdapter.getQuote(ticker, isAndrewMoffatt, isChalk); // quote in html
      comment = comment.replace(substring, htmlSymbol);
    }

    return comment;
  }

  public static void updateEmailRecipients(String fullName, String subjectLine) throws IOException {

    final File tmpFile = new File("config" + ".tmp");
    final File file = new File("config");
    PrintWriter pw = new PrintWriter(tmpFile);
    BufferedReader br = new BufferedReader(new FileReader(file));
    final String toAdd = "to=" + getEmailRecipients(fullName);
    for (String line; (line = br.readLine()) != null; ) {
      if (line.startsWith("to")) {
        line = toAdd;
      } else if (line.startsWith("body")) {
        line = "body=\\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild\\ChalkTalkEmail.html";
      } else if (line.startsWith("subject")) {
        line = "subject=" + subjectLine;
      } else if (line.startsWith("attachmentLoc")) {
        line = "attachmentLoc=";
      } else if (line.startsWith("attachments")) {
        line = "attachments=";
      }
      pw.println(line);
    }

    br.close();
    pw.close();
    file.delete();
    File file2 = new File("config");
    tmpFile.renameTo(file2);
  }

  public static String getEmailRecipients(String fullName) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      UserEmail[] users = mapper.readValue(new File("users.json"), UserEmail[].class);

      for (int i = 0; i < users.length; i++) {
        if (users[i].fullName.contains(fullName)) {
          return users[i].emailRecipients;
        }
      }
    } catch (JsonParseException e) {
      logger.info("Error parsing users.json file \n");
    } catch (JsonMappingException e) {
      logger.info("Error parsing users.json file \n");
    } catch (IOException e) {
      logger.info("Error parsing users.json file \n");
    }

    return "";
  }

  // Reads given filename from the user directory into a string
  public static String getHTMLString(String fileName) {
    String contents = "";
    try {
      String line = "";
      BufferedReader br =
          new BufferedReader(
              new FileReader(System.getProperty("user.dir") + "\\templates\\" + fileName));
      while ((line = br.readLine()) != null) {
        contents = contents + line + "\n";
      }
      br.close();
    } catch (IOException e) {
      // System.err.println("Error: " + e.getMessage());
      logger.info("Error: " + e.getMessage());
    }
    return contents;
  }

  public static String getSignatureHtml(String fullName) {
    ObjectMapper mapper = new ObjectMapper();

    try {
      UserEmail[] users = mapper.readValue(new File("users.json"), UserEmail[].class);

      for (int i = 0; i < users.length; i++) {
        if (users[i].fullName.contains(fullName)) {
          return users[i].htmlSignature;
        }
      }
      return "";
    } catch (JsonParseException e) {
      // System.err.println("Error parsing users.json file \n");
      logger.info("Error parsing users.json file \n");
    } catch (JsonMappingException e) {
      logger.info("Error parsing users.json file \n");
    } catch (IOException e) {
      logger.info("Error parsing users.json file \n");
    }

    return "";
  }

  public static String buildSignature(String fullName) {

    String html;

    if (fullName.contains("George Gardiner")) {
      html = "morningSignature.html";
    } else {
      html = getSignatureHtml(fullName);
    }

    if (html.equals("")) {
      return "";
    }

    String sig = getHTMLString(html);

    return sig;
  }

  public static boolean isEnergyUtilTicker(String ric) {

    try {
      JSONObject json = readJsonFromUrl("http://t65-w7-eqcash:9001/get-security-ric?ric=" + ric);
      if (json != null) {
        String sector = json.getString("sector");
        if (sector != null && sector.contains("Utilities") || sector.contains("Energy")) {
          return true;
        } else return false;
      }
    } catch (Exception e) {
      logger.info(
          "Error in checking whether ric is from energy or utility sector: "
              + ric
              + " "
              + e.getMessage());
    }
    return false;
  }

  public static boolean isMaterialsTicker(String ric) {

    try {
      JSONObject json = readJsonFromUrl("http://t65-w7-eqcash:9001/get-security-ric?ric=" + ric);
      if (json != null) {
        String sector = json.getString("sector");
        if (sector != null && sector.contains("Materials")) {
          return true;
        } else return false;
      }
    } catch (Exception e) {
      // logger.info("Error in checking whether ric is from energy or utility sector: " + ric + " "
      // + e.getMessage());
    }

    return false;
  }

  public static String getAnalyst(String ric) {
    java.sql.Connection connection = null;

    String analystName = "";

    try {

      String connectionString =
          "jdbc:jtds:sqlserver://eessql.gss.scotia-capital.com:5150;"
              + "user=dmamso;"
              + "password=abc1234$6;";

      connection = DriverManager.getConnection(connectionString);

      Statement cmd = connection.createStatement();
      String cmdString =
          "SELECT [FULL_NAME] FROM [Portfolio].[dbo].[AnalystCoverage] WHERE [RIC] = '{ticker}'"
              .replace("{ticker}", ric);
      ResultSet rs = cmd.executeQuery(cmdString);

      while (rs.next()) {
        // ... get column values from this record
        analystName = rs.getString("FULL_NAME");
      }
    } catch (Exception e) {
      // e.printStackTrace();
      logger.info("Error: " + e.getMessage());
    } finally {
      if (connection != null)
        try {
          connection.close();
        } catch (Exception e) {
        }
    }

    if (analystName.equals("")) analystName = "N/A";
    return analystName;
  }

  // Build the HTML for events
  public static String buildeventsList(List<CorporateEvent> list, String sector) {

    String result = "";
    String temp = "";
    String subresult = "";
    String descriptiontemplate = Utilities.getHTMLString("EventDescriptionTemplate.html");
    String eventtemplate = Utilities.getHTMLString("EventTemplate.html");
    String[] categories =
        new String[] {
          "Scotiabank Hosted Marketing Events",
          "Analyst/Sales Marketing",
          "Company Hosted Marketing Events & Conferences",
          "Site Tours",
          "Other"
        };

    for (int i = 0; i < categories.length; i++) {
      String category = categories[i];
      boolean found = false;
      subresult = "";
      for (CorporateEvent event : list) {
        if (event.category.equals(category) && event.sector.equals(sector)) {
          found = true;
          temp = descriptiontemplate;
          String start = dateConverter(event.startdate);
          String end = dateConverter(event.enddate);
          String dateRange = start;
          if (!end.equals(start)) {
            dateRange += " &#8212; " + end;
          }
          temp = temp.replace("{{dateRange}}", dateRange);

          if (event.event != null) {
            temp = temp.replace("{{event}}", event.event);
          } else {
            temp = temp.replace("{{event}}", " ");
          }
          subresult += temp + "\n";
        }
      }
      if (found) {
        result +=
            eventtemplate
                .replace(
                    "{{eventCategory}}",
                    "Upcoming " + category + (category.contains("Other") ? " Events" : ""))
                .replace("{{eventList}}", subresult);
      }
    }

    return result + "<br>";
  }

  private static String dateConverter(String sqldate) {

    DateFormat df = new SimpleDateFormat("yyyy-M-dd");
    String month = "";
    int day = 1;
    try {
      Date date = df.parse(sqldate);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      month = getMonthForInt(cal.get(Calendar.MONTH));
      day = cal.get(Calendar.DAY_OF_MONTH);
    } catch (ParseException e) {

    }
    return month + " " + String.format("%d", day);
  }

  private static String getMonthForInt(int num) {
    String month = "wrong";
    DateFormatSymbols dfs = new DateFormatSymbols();
    String[] months = dfs.getMonths();
    if (num >= 0 && num <= 11) {
      month = months[num];
    }
    return month;
  }

  public static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
    InputStream is = new URL(url).openStream();
    try {
      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      String jsonText = readAll(rd);
      JSONObject json = new JSONObject(jsonText);
      return json;
    } finally {
      is.close();
    }
  }

  public static String toTitleCase(String givenString) {
    givenString = givenString.toLowerCase();
    String[] arr = givenString.split(" ");
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < arr.length; i++) {
      sb.append(Character.toUpperCase(arr[i].charAt(0))).append(arr[i].substring(1)).append(" ");
    }
    return sb.toString().trim();
  }

  public static String replaceOddCharacters(String string) {
    String body = string;
    body = body.replaceAll(Pattern.quote("ʼ"), "'");
    body = body.replaceAll(Pattern.quote("’"), "'");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("–"), "-");
    body = body.replaceAll(Pattern.quote("“"), "\"");
    body = body.replaceAll(Pattern.quote("”"), "\"");
    body = body.replaceAll(Pattern.quote("…"), "...");
    body = body.replaceAll(Pattern.quote("—"), "-");
    body = body.replaceAll(Pattern.quote("€"), "&#8364;");
    return body;
  }
}
