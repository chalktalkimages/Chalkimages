package data;

public class BlockDetails {

  public String brokerName;
  public Double notional;
  public Integer tradeSize;
  public String ticker;
  public String brokerID;
  public Double tradePrice;
  public Long tradeTime;
  public String trader, analyst;
  public String fullName = "";

  public BlockDetails() {
    this.brokerName = "";
    this.notional = 0.;
    this.tradeSize = 0;
    this.ticker = "";
    this.brokerID = "";
    this.tradePrice = 0.;
    this.tradeTime = (long) 0;
    this.trader = "";
    this.analyst = "";
  }

  public BlockDetails(
      String brokerName,
      Double notional,
      Integer tradeSize,
      String ticker,
      String brokerID,
      Double tradePrice,
      Long tradeTime,
      String trader,
      String analyst) {
    this.brokerName = brokerName;
    this.notional = notional;
    this.tradeSize = tradeSize;
    this.ticker = ticker;
    this.brokerID = brokerID;
    this.tradePrice = tradePrice;
    this.tradeTime = tradeTime;
    this.trader = trader;
    this.analyst = analyst;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    BlockDetails other = (BlockDetails) obj;
    if (analyst == null) {
      if (other.analyst != null) return false;
    } else if (!analyst.equals(other.analyst)) return false;
    if (brokerID == null) {
      if (other.brokerID != null) return false;
    } else if (!brokerID.equals(other.brokerID)) return false;
    if (brokerName == null) {
      if (other.brokerName != null) return false;
    } else if (!brokerName.equals(other.brokerName)) return false;
    if (fullName == null) {
      if (other.fullName != null) return false;
    } else if (!fullName.equals(other.fullName)) return false;
    if (notional == null) {
      if (other.notional != null) return false;
    } else if (!notional.equals(other.notional)) return false;
    if (ticker == null) {
      if (other.ticker != null) return false;
    } else if (!ticker.equals(other.ticker)) return false;
    if (tradePrice == null) {
      if (other.tradePrice != null) return false;
    } else if (!tradePrice.equals(other.tradePrice)) return false;
    if (tradeSize == null) {
      if (other.tradeSize != null) return false;
    } else if (!tradeSize.equals(other.tradeSize)) return false;
    if (tradeTime == null) {
      if (other.tradeTime != null) return false;
    } else if (!tradeTime.equals(other.tradeTime)) return false;
    return true;
  }
}
