package data;

public class TickerResearch {

  public String rating;
  public String target;
  public String researchLink;
  public String previousTarget;

  public TickerResearch() {
    this.rating = "N/A";
    this.target = "N/A";
    this.previousTarget = "N/A";
    this.researchLink = "";
  }

  public TickerResearch(String rating, String target, String researchLink, String previousTarget) {
    this.rating = rating;
    this.target = target;
    this.previousTarget = previousTarget;
    this.researchLink = researchLink;
  }

  public void setPreviousTarget(String target) {
    this.previousTarget = target;
  }
}
