package html;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import adapter.DBHandle;
import adapter.EquityFileParser;
import data.FlowStories;
import data.SecInfo;
import data.TickerResearch;
import utils.SymbolConverter;

public class ScotiaViewParser {

  private static final Logger logger = Logger.getLogger(ScotiaViewParser.class.getName());

  private Map<String, String> loginCookies;

  private HashMap<String, ArrayList<SecInfo>> tickerIDMap;

  private final int attemptLimit = 5;

  private boolean loginsuccess = false;
  private boolean secMapsuccess = false;

  private static final String lastestResearchURL1 = "https://www.scotiaview.com/eq";
  private static final String lastestResearchURL2 = "https://www.scotiaview.com/eq?pageno=2";
  private static final String lastestResearchURL3 = "https://www.scotiaview.com/eq?pageno=3";

  public ScotiaViewParser() {
    tickerIDMap = new HashMap<String, ArrayList<SecInfo>>();
    loginsuccess = getLoginCookies();
    if (loginsuccess) {
      secMapsuccess = populateSecMap();
    }
  }

  private boolean getLoginCookies() {
    int attemptCounter = 0;
    try {

      Connection.Response res =
          Jsoup.connect("https://www.scotiaview.com/cs/Satellite")
              .userAgent(
                  "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36")
              .method(Method.POST)
              .proxy("proxyprd.scotia-capital.com", 8080)
              .data("pagename", "SCResearch/Controllers/Login")
              .data("username", "bliu2108")
              .data("password", "bliu2108")
              .header("X-Requested-With", "XMLHttpRequest")
              .timeout(15 * 1000)
              .followRedirects(false)
              .execute();

      loginCookies = res.cookies();

      return true;

    } catch (IOException e) {
      if (attemptCounter < attemptLimit) {
        attemptCounter++;
        return getLoginCookies(); // try again if this is the first attempt
      }
      logger.info(
          "Error in logging into Scotiaview after 5 attempts ... No symbol data will be provided: "
              + e.getMessage());
      return false;
    }
  }

  private boolean populateSecMap() {
    int attemptCounter = 0;
    try {
      Document SecuritiesJSONPage =
          Jsoup.connect(
                  "https://www.scotiaview.com/cs/Satellite?pagename=SCResearch/Search/JsonAutoSuggest&pcfs=0&requesttype=all")
              .userAgent(
                  "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
              .referrer("http://www.google.com")
              .proxy("proxyprd.scotia-capital.com", 8080)
              .cookies(loginCookies)
              .timeout(15 * 1000)
              .get();
      String JSONHtml = SecuritiesJSONPage.html();
      // String SecuritiesJSONHtml = JSONHtml.substring(JSONHtml.indexOf("SC_Issuer")+12,
      // JSONHtml.indexOf("SC_Industry")-5);
      String SecuritiesJSONHtml =
          JSONHtml.substring(JSONHtml.indexOf("SC_Issuer") + 12, JSONHtml.length());
      // System.out.println(SecuritiesJSONHtml);

      String[] securitiesStrings = SecuritiesJSONHtml.split(Pattern.quote("##"));

      //			HashMap<String, ArrayList<SecInfo>> tickerIDMap = new HashMap<String,
      // ArrayList<SecInfo>>();
      //
      for (int i = 0; i < securitiesStrings.length; i++) {
        String[] secDetails = securitiesStrings[i].split(Pattern.quote("||")); // two elements
        String[] secNameTicker = secDetails[0].split(Pattern.quote("|"));
        String ticker;
        String exchange;
        String secID;
        if (secNameTicker.length < 3) // no ticker
        {
          continue;
        } else // exists
        {
          ticker = secNameTicker[1];
          exchange = secNameTicker[2];
          secID = secDetails[1].replace("SC_Security:", "");
          if (tickerIDMap.containsKey(ticker)) {
            ArrayList<SecInfo> oldList = tickerIDMap.get(ticker);
            oldList.add(new SecInfo(secID, exchange));
          } else {

            ArrayList<SecInfo> newList = new ArrayList<SecInfo>();
            newList.add(new SecInfo(secID, exchange));
            tickerIDMap.put(ticker, newList);
          }
        }
      }

      return true;
    } catch (IOException e) {
      if (attemptCounter < attemptLimit) {
        attemptCounter++;
        return populateSecMap(); // try again if this is the first attempt
      }
      logger.info(
          "Error in getting security map from Scotiaview after 5 attempts ... no symbol data provided in report: "
              + e.getMessage());
      return false;
    }
  }

  private static String getExchange(String ric) {
    if (ric.contains(".TO")) {
      return "TSE";
    } else if (ric.contains(".V")) {
      return "TSX";
    } else if (ric.contains(".A")) {
      return "AMEX";
    } else if (ric.contains(".N")) {
      return "NYSE";
    } else if (ric.contains(".O")) {
      return "NASDAQ";
    } else if (ric.contains(".OQ")) {
      return "OTCBB";
    } else // assume .TO
    {
      return "TSE";
    }
  }

  private TickerResearch getPageDetails(String secID) {
    String rating = "N/A";
    String target = "N/A";
    String researchLink = "";

    int attemptCounter = 0;
    try {
      Document doc =
          Jsoup.connect("https://www.scotiaview.com/company/" + secID)
              .userAgent(
                  "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
              .referrer("http://www.google.com")
              .proxy("proxyprd.scotia-capital.com", 8080)
              .cookies(loginCookies)
              .timeout(10 * 1000)
              .get();
      Elements contents = doc.getElementsByTag("h6");
      for (Element content : contents) {
        if (content.text().equals("Rating")) {
          rating = content.nextElementSibling().text();
        } else if (content.text().equals("Target")) {
          target = content.nextElementSibling().text();
          if (target.contains("1 yr: ") && target.replace("1 yr: ", "").length() != 0) {
            target = target.replace("1 yr: ", "");
          }
        }
      }
      Element link = doc.select("a[target='_new']").first();
      if (link == null) researchLink = "";
      else researchLink = link.attr("href");
    } catch (Exception e) {
      if (attemptCounter < attemptLimit) {
        attemptCounter++;
        return getPageDetails(secID); // try again if this is the first attempt
      }
      logger.info("Failed to get symbol data for ticker after 5 attempts " + e.getMessage());
      return null;
    }

    if (rating.equals("N/A") && target.equals("N/A") && researchLink.equals("")) return null;
    else return new TickerResearch(rating, target, researchLink);
  }

  public TickerResearch getSymbolResearch(String ric, String ticker) {
    TickerResearch research = new TickerResearch();
    if (isLoaded()) {
    	if(EquityFileParser.tickerResearchMap.containsKey(ticker)){
    		research = EquityFileParser.tickerResearchMap.get(ticker);
    	}
    	else{
    		research = getResearch(ric, ticker);
    	}
    }

    TickerResearch altResearch = null;
    if (research.target.equals("N/A") && loginsuccess && secMapsuccess) {
      String altRIC = DBHandle.getAltRIC(ric);
      if (!altRIC.equals("")) {
        String altTicker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(altRIC));
        
        if(EquityFileParser.tickerResearchMap.containsKey(altTicker)){
        	altResearch = EquityFileParser.tickerResearchMap.get(altTicker);
        }
        else{
        	altResearch = getResearch(altRIC, altTicker);
        }

        if (!altResearch.target.equals("N/A") || !altResearch.researchLink.equals("")) {
          research = altResearch;
        }
      }
    }

    if (research.rating.equals("")) research.rating = "N/A";
    if (research.target.equals("1 yr:")) research.target = "N/A";

    return research;
  }

  private TickerResearch getResearch(String ric, String ticker) {
    String exchange = getExchange(ric);
    ArrayList<SecInfo> secIDs = tickerIDMap.get(ticker);
    if (secIDs == null) {
      return new TickerResearch("N/A", "N/A", "");
    } else {
      boolean found = false;
      String ID = null;
      for (SecInfo s : secIDs) {
        if (s.Exchange.equals(exchange)) {
          found = true;
          ID = s.ID;
        }
      }
      // check any arbitrary exchange then
      if (!found && secIDs.size() > 0) {
        for (SecInfo s : secIDs) {
          found = true;
          ID = s.ID;
          break;
        }
      }
      if (found) {
        TickerResearch result = getPageDetails(ID);
        if (result == null) {
          TickerResearch altResult = null;
          boolean success = false;

          for (SecInfo s : secIDs) {
            if (!ID.equals(s.ID)) {
              altResult = getPageDetails(s.ID);
            }
            if (altResult != null) {
              success = true;
              break;
            }
          }
          if (success) {
            return altResult;
          } else {
            return new TickerResearch("N/A", "N/A", "");
          }
        } else {
          return result;
        }
      } else return new TickerResearch("N/A", "N/A", "");
    }
  }

  public List<FlowStories> getScotiaviewStories() {
    List<FlowStories> results = getStories(lastestResearchURL1);
    results.addAll(getStories(lastestResearchURL2));
    results.addAll(getStories(lastestResearchURL3));
    return results;
  }

  public List<FlowStories> getStories(String url) {

    int attemptCounter = 0;
    Calendar yesterday = Calendar.getInstance();
    yesterday.add(Calendar.DAY_OF_YEAR, -1);
    yesterday.set(Calendar.HOUR_OF_DAY, 14);
    yesterday.set(Calendar.MINUTE, 59);
    // 2:59 cut off
    List<FlowStories> fs = new ArrayList<FlowStories>();
    try {
      Document doc =
          Jsoup.connect(url)
              .userAgent(
                  "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
              .referrer("http://www.google.com")
              .proxy("proxyprd.scotia-capital.com", 8080)
              .cookies(loginCookies)
              .timeout(10 * 1000)
              .get();
      Elements contents = doc.getElementsByTag("tr");
      boolean header = false;
      SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");
      FlowStories newStory = new FlowStories();
      for (Element content : contents) {
        String rowType = content.className();
        if ((rowType.equals("odd-row") || rowType.equals("even-row")) && !header) {
          String publishDateString = content.getElementsByClass("align-rt").text();
          Calendar pubtime = Calendar.getInstance();
          pubtime.setTime(formatter.parse(publishDateString));
          if (pubtime.before(yesterday)) continue; // skip old entries
          String pdfURL = content.getElementsByTag("a").get(1).attr("href");
          newStory = new FlowStories();
          newStory.link = pdfURL;
          newStory.headline = filterComment(content.text());
          header = true;
        } else if (rowType.equals("childRow") && header) {
          Elements es = content.getElementsByTag("a");
          for (Element e : es) {
            if (e.attr("href").contains("authors")) newStory.author = e.text();
            else if (e.attr("href").contains("company")) newStory.ticker = e.text();
          }
          es = content.getElementsByTag("p");
          if (es.size() >= 3) newStory.story = filterComment(es.get(2).text());

          fs.add(newStory);
          header = false;
        }
      }
    } catch (Exception e) {
      if (attemptCounter < attemptLimit) {
        attemptCounter++;
      }
      logger.info("Failed to get today's stories main page after 5 attempts " + e.getMessage());
      return fs;
    }
    return fs;
  }

  private String filterComment(String content) {
    String result = content;
    result = result.replace("OUR TAKE: ", "");
    if (result.startsWith("Flash") || result.startsWith("Daily")) {
      result = result.substring(result.indexOf("-") + 1);
      result = result.substring(result.indexOf("-") + 1);
      result =
          result.replaceAll(
              ".[a-zA-Z]{3} [0-9]{2}, [0-9]{4} [0-9:]{5}", ""); // remove date Jan 10, 2018 16:15
      result = result.replaceAll(".(\\w+) [0-9]{2}, [0-9]{4}", ""); // remove date January 11, 2018
    }
    result = result.trim();
    result = StringUtils.trimTrailingCharacter(result, ',');
    return result;
  }

  public boolean isLoaded() {
    return loginsuccess && secMapsuccess;
  }
}
