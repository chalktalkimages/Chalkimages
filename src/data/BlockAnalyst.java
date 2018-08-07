package data;

public class BlockAnalyst {
  public String analystName = "";
  public String traderName = "";
  public String brokerCode = "";
  public String brokerName = "";

  public Integer PYblockCount = 0;
  public Integer PYshares = 0;
  public Double PYnotional = 0.0;

  public Integer YTDblockCount = 0;
  public Integer YTDshares = 0;
  public Double YTDnotional = 0.0;

  public Integer MTDblockCount = 0;
  public Integer MTDshares = 0;
  public Double MTDnotional = 0.0;

  public BlockAnalyst() {}

  public BlockAnalyst(BlockDetails b) {
    this.analystName = b.analyst;
    this.brokerCode = b.brokerID;
    this.brokerName = b.brokerName;
    this.traderName = b.trader;
  }
}
