package data;

public class TraderTotal {
  public String traderName = "";
  public Long scotiaTotalVolume = (long) 0;
  public Double scotiaTotalValue = 0.0;
  public Long marketTotalVolume = (long) 0;
  public Double marketTotalValue = 0.0;
  public String volumePercent = "-";
  public String valuePercent = "-";
  public String scotiaVolumePercent = "-";
  public String scotiaValuePercent = "-";
  public String volumePercentofScotia = "-";
  public String valuePercentofScotia = "-";

  public TraderTotal() {}

  public TraderTotal(String traderName) {
    this.traderName = traderName;
  }
}
