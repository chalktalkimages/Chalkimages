package data;

public class TraderSummary {
  public String traderName = "";
  public String ticker = "";
  public String fullName = "";
  public Long scotiaVolume = (long) 0;
  public Double scotiaValue = 0.0;
  public Long marketVolume = (long) 0;
  public Double marketValue = 0.0;

  public TraderSummary() {}

  public TraderSummary(BlockDetails block) {
    this.traderName = block.trader;
    this.ticker = block.ticker;
    this.fullName = block.fullName;
    this.marketVolume = (long) block.tradeSize;
    this.marketValue = block.notional;
    if (block.brokerID.equals("85")) {
      this.scotiaVolume = (long) block.tradeSize;
      this.scotiaValue = block.notional;
    }
  }
}
