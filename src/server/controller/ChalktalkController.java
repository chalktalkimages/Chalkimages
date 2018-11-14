package server.controller;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import adapter.EquityFileParser;
import data.FlowStories;
import data.PostTradeOptions;
import data.TickerResearch;
import engine.Engine;
import engine.PostTradeClient;
import html.ScotiaViewParser;
import utils.SymbolConverter;

@RestController
public class ChalktalkController {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private ScotiaViewParser svp = new ScotiaViewParser();

  public ChalktalkController() {

    // reload parser every 30 min
    scheduler.scheduleAtFixedRate(
        () -> {
          svp = new ScotiaViewParser();
        },
        0,
        30,
        TimeUnit.MINUTES);
  }

  @RequestMapping(
      value = "/send-Chalktalk",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void generateChalktalk(@RequestBody RequestWrapper wrapper) {
    Engine.getInstance()
        .generateChalktalk(
            wrapper.id,
            wrapper.user,
            wrapper.flow,
            wrapper.energyReturn,
            wrapper.energyEVol,
            wrapper.materialsReturn,
            wrapper.materialsEVol,
            wrapper.generalComments,
            wrapper.reportSections);
  }

  @RequestMapping(
      value = "/send-BlockReport",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void generateBlockReport(@RequestBody BlockWrapper wrapper) {
    Engine.getInstance().generateBlockReport(wrapper.fullname, wrapper.blocks, wrapper.isFlow);
  }

  @RequestMapping(
      value = "/send-morning-BlockReport",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void generatemorningBlockReport(@RequestBody BlockWrapper wrapper) {
    Engine.getInstance()
        .generatemorningBlockReport(wrapper.fullname, wrapper.blocks, wrapper.isFlow, false);
  }

  @RequestMapping(
      value = "/send-morning-BlockReport-Fiscal",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void generatemorningBlockReportFiscal(@RequestBody BlockWrapper wrapper) {
    Engine.getInstance()
        .generatemorningBlockReport(wrapper.fullname, wrapper.blocks, wrapper.isFlow, true);
  }

  @RequestMapping(
      value = "/send-trader-analyst-BlockReport",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void traderAnalystBlockReport(@RequestBody BlockWrapper wrapper) {
    Engine.getInstance().generateTraderAnalystBlockReport(wrapper.fullname, wrapper.blocks);
  }

  @RequestMapping(
      value = "/send-trader-market-share-BlockReport",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void traderMarketShareReport(@RequestBody BlockWrapper wrapper)
      throws Exception {
    Engine.getInstance().generateMarketShareReport(wrapper.fullname, wrapper.blocks);
  }

  @RequestMapping(
      value = "/send-Energy-Insights",
      method = RequestMethod.POST,
      headers = "Accept=application/json")
  public @ResponseBody void generateEnergyInsights(
      @RequestBody EnergyInsightsRequestWrapper wrapper) {
    Engine.getInstance()
        .generateEnergyInsights(
            wrapper.id,
            wrapper.user,
            wrapper.flow,
            wrapper.energyReturn,
            wrapper.energyEVol,
            wrapper.materialsReturn,
            wrapper.materialsEVol,
            wrapper.generalComments);
  }

  @RequestMapping(
      value = "/get-Server-Status",
      method = RequestMethod.GET,
      headers = "Accept=application/json")
  public @ResponseBody Status getServerStatus(
      @RequestParam(value = "fullname", defaultValue = "") String fullname) {
    String s = Engine.getInstance().serverStatus.get(fullname);
    return new Status(s);
  }

  @RequestMapping(
      value = "/send-PostTradeReport",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Async
  public @ResponseBody byte[] generatePostTradeReport(@RequestBody PostTradeOptions po) {
    byte[] posttradeReport = PostTradeClient.runRequest(po);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/pdf"));
    String filename = "posttrade.pdf";
    headers.setContentDispositionFormData(filename, filename);
    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
    @SuppressWarnings("unused")
    ResponseEntity<byte[]> response =
        new ResponseEntity<byte[]>(posttradeReport, headers, HttpStatus.OK);
    return posttradeReport;
  }

  @RequestMapping(
      value = "/get-scotiaview-comments",
      method = RequestMethod.GET,
      headers = "Accept=application/json")
  public @ResponseBody List<FlowStories> getScotiaviewComments() {
    return Engine.getInstance().getScotiaviewStories();
  }

  @RequestMapping(
      value = "/get-scotiaview-ratings",
      method = RequestMethod.GET,
      headers = "Accept=application/json")
  public @ResponseBody TickerResearch getScotiaviewRatings(
      @RequestParam(value = "ric", defaultValue = "") String ric) {
    if (ric == null || ric.isEmpty()) return new TickerResearch();
    String ticker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(ric));
    if (!svp.isLoaded()) // reload if its not good
    svp = new ScotiaViewParser();
    return svp.getSymbolResearch(ric, ticker);
  }

  @RequestMapping(
      value = "/refresh-coverage-list",
      method = RequestMethod.GET,
      headers = "Accept=application/json")
  public @ResponseBody String refreshCoverageList() {
    EquityFileParser.load();
    int n = EquityFileParser.tickerResearchMap.size();
    return "Refreshed: " + n + " names.";
  }
}
