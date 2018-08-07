package data;

public class FlowStories {
  public String ticker = "";
  public String headline = "";
  public String story = "";
  public String link = "";
  public String author = "";

  @Override
  public String toString() {
    return "FlowStories [ticker="
        + ticker
        + ", headline="
        + headline
        + ", story="
        + story
        + ", link="
        + link
        + ", author="
        + author
        + "]";
  }
}
