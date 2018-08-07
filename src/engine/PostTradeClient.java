package engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import com.posttradeglue.info.BasketInfo;
import com.posttradeglue.info.MarketInfo;
import com.posttradeglue.info.ResultInfo;
import com.posttradeglue.info.SymbolInfo;
import com.posttradeglue.manager.BasketInfoManager;
import com.posttradeglue.manager.ResultInfoManager;
import com.posttradeglue.utils.Routes;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import data.PostTradeOptions;

public class PostTradeClient {
  private static final Logger logger = Logger.getLogger(PostTradeClient.class.getName());

  private static final int USER_INDEX = 0;
  private static final int COUNTRY_INDEX = 4;
  private static final int SIDE_INDEX = 5;
  private static final int TARGET_SHARES_INDEX = 6;
  private static final int TICKER_INDEX = 8;
  private static final int CURRENCY_INDEX = 17;
  private static final int EXECUTED_SHARES_INDEX = 21;
  private static final int AVGPX_INDEX = 22;
  private static final int PRIOR_CLOSEPX_INDEX = 26;
  private static final int OPENPX_INDEX = 28;
  private static final int ARRIVALPX_INDEX = 35;
  private static final int VWAPPX_INDEX = 56; // column BE, current using order vwap
  private static final int LASTPX_INDEX = 79;
  private static final int ORDERVWAP_INDEX = 56; // BE
  private static final int CLOSE_PX_INDEX = 81; // CD

  private static final String SERVER_IP = "t68-w7-sport";
  // private static final String SERVER_IP = "localhost";
  private static final long TIMEOUT_LONG_SECONDS = 60;
  private static final long TIMEOUT_SHORT_SECONDS = 30;

  private static final String fidessaFileName =
      "\\\\t65-w7-eqcash\\incoming\\PostTrade\\sampleposttrade.csv";
  private static final String BENCH0 = "bench0";
  private static final String BENCH1 = "bench1";
  private static final String BENCH2 = "bench2";
  private static final String BENCH3 = "bench3";
  private static final String BENCH4 = "bench4";

  public static byte[] runRequest(PostTradeOptions po) {
    BasketInfo bi = loadSourceData(po);
    return sendToServer(bi);
  }

  private static byte[] sendToServer(BasketInfo bi) {
    // Setup the RabbitMQ connection
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(SERVER_IP);

    try {
      Connection connection = factory.newConnection();
      Channel channel_ = connection.createChannel();
      channel_.exchangeDeclare(Routes.EXCHANGE_NAME_POSTTRADE, "direct");
      String strReplyQueue_ = channel_.queueDeclare().getQueue();
      channel_.queueBind(
          strReplyQueue_, Routes.EXCHANGE_NAME_POSTTRADE, Routes.ROUTING_NAME_IN_POSTTRADE);
      QueueingConsumer consumer_ = new QueueingConsumer(channel_);
      channel_.basicConsume(strReplyQueue_, true, consumer_);

      logger.info("Setup connection with server: " + SERVER_IP);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(baos);

      BasketInfoManager.writeToClient(bi, ds);
      ds.flush();

      AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
      propsBuilder.replyTo(Routes.ROUTING_NAME_IN_POSTTRADE);
      AMQP.BasicProperties props = propsBuilder.build();
      channel_.basicPublish(
          Routes.EXCHANGE_NAME_POSTTRADE, Routes.ROUTING_NAME_POSTTRADE, props, baos.toByteArray());
      logger.info("Sent basket of " + bi.lSymbols.size() + " symbols to server.");

      ResultInfo ri = new ResultInfo();
      // Wait/handle server responses
      boolean initialMessage = true;
      while (true) {
        QueueingConsumer.Delivery delivery = null;
        long timeout = TIMEOUT_LONG_SECONDS;
        if (initialMessage) {
          timeout = TIMEOUT_SHORT_SECONDS;
          delivery = consumer_.nextDelivery(timeout * 1000);
          initialMessage = false;
        } else delivery = consumer_.nextDelivery(timeout * 1000);

        // delivery timed out
        if (delivery == null) {
          logger.info(
              String.format(
                  "No response from PostTrade server after %d seconds! Try again...", timeout));
          channel_.abort();
          return null;
        }

        // unpack delivery
        DataInputStream inFromServer = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
        inFromServer = new DataInputStream(bais);
        ResultInfoManager.readFromClient(ri, inFromServer);

        if (ri.status_.equals(ResultInfo.STATUS_COMPLETE)) {
          logger.info(String.format("Server %s: %s.", ri.status_, ri.message_));
          channel_.abort();
          logger.info("PostTrade completed successfully");
          return ri.pdfFile_;
        } else if (ri.status_.equals(ResultInfo.STATUS_ERROR)) {
          logger.error(
              String.format(
                  "Error: Server %s: %s \nStacktrace: %s",
                  ri.status_, ri.message_, ri.stacktrace_));
          channel_.abort();
          return null;
        }
        // progress update messages
        else {
          logger.info(String.format("%s: %s", ri.status_, ri.message_));
        }
      }
    } catch (Exception e) {
      logger.error("Post Trade failed!", e);
    }
    return null;
  }

  private static BasketInfo loadSourceData(PostTradeOptions po) {
    BufferedReader br = null;
    String line = "";
    String cvsSplitBy = ",";
    BasketInfo bi = new BasketInfo();
    bi.strBenchMarkZero_ =
        po.benchmarkMap.containsKey(BENCH0) ? po.benchmarkMap.get(BENCH0) : "PNC/Open Gap";
    bi.strBenchMarkOne_ =
        po.benchmarkMap.containsKey(BENCH1) ? po.benchmarkMap.get(BENCH1) : "Arrival";
    bi.strBenchMarkTwo_ =
        po.benchmarkMap.containsKey(BENCH2) ? po.benchmarkMap.get(BENCH2) : "Open";
    bi.strBenchMarkThree_ =
        po.benchmarkMap.containsKey(BENCH3) ? po.benchmarkMap.get(BENCH3) : "VWAP";
    bi.strBenchMarkFour_ =
        po.benchmarkMap.containsKey(BENCH4) ? po.benchmarkMap.get(BENCH4) : "Last";
    try {
      br = new BufferedReader(new FileReader(fidessaFileName));
      while ((line = br.readLine()) != null) {
        String[] fields = line.split(cvsSplitBy);

        if (fields.length > 0 && (bi.strUser_ == null || bi.strUser_.isEmpty()))
          bi.strUser_ = fields[USER_INDEX];

        SymbolInfo si = new SymbolInfo();
        if (fields[SIDE_INDEX].equals("B")) si.bBuy_ = true;
        else si.bBuy_ = false;

        si.strName_ = fields[TICKER_INDEX];
        si.nTargetShares_ = parseInt(fields[TARGET_SHARES_INDEX]);
        si.nExecutedShares_ = parseInt(fields[EXECUTED_SHARES_INDEX]);
        si.fAveragePrice_ = parseDouble(fields[AVGPX_INDEX]);
        si.fClose_ = parseDouble(fields[PRIOR_CLOSEPX_INDEX]);
        si.fOpen_ = parseDouble(fields[OPENPX_INDEX]);
        si.fArrival_ = parseDouble(fields[ARRIVALPX_INDEX]);
        si.fVwap_ = parseDouble(fields[VWAPPX_INDEX]);
        si.fLast_ = parseDouble(fields[LASTPX_INDEX]);
        si.fOVwap_ = parseDouble(fields[ORDERVWAP_INDEX]);
        si.fLastTsx_ = parseDouble(fields[CLOSE_PX_INDEX]);

        MarketInfo e = new MarketInfo();
        e.isCanadian = true; // hack to show fills by market
        e.marketName = fields[COUNTRY_INDEX];
        e.shares = si.nExecutedShares_;
        if (po.fxRateMap.containsKey(fields[CURRENCY_INDEX]))
          si.fFx_ = po.fxRateMap.get(fields[CURRENCY_INDEX]);
        si.lMarkets.add(e);
        bi.lSymbols.add(si);
      }

    } catch (FileNotFoundException e) {
      logger.error("Failed to find file " + fidessaFileName, e);
    } catch (IOException e) {
      logger.error("IO Exception when opening file", e);
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    bi.sortSymbolInfoByName();

    // sort by country
    Collections.sort(
        bi.lSymbols,
        new Comparator<SymbolInfo>() {
          public int compare(SymbolInfo o1, SymbolInfo o2) {
            return o1.lMarkets.get(0).marketName.compareTo(o2.lMarkets.get(0).marketName);
          }
        });
    bi.strBasketID_ = po.basketID;
    bi.strFooter_ = "programtrading@scotiabank.com 212-225-6617";
    bi.bCalcUnrealized_ = po.calcUnreal;

    return bi;
  }

  private static Double parseDouble(String d) {
    try {
      if (d != null) return Double.parseDouble(d.replace("\"", "").replace(",", "").trim());
    } catch (NumberFormatException e) {
      logger.error("Failed parsing string to double:" + d, e);
    }
    return 0.0;
  }

  private static Integer parseInt(String i) {
    try {
      return i != null ? Integer.parseInt(i.replace("\"", "").replace(",", "").trim()) : 0;
    } catch (NumberFormatException e) {
      logger.error("Failed parsing string to int:" + i, e);
    }
    return 0;
  }
}
