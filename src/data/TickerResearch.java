package data;

public class TickerResearch {

	public String rating;
	public String target;
	public String researchLink;
	
	
	public TickerResearch()
	{
		this.rating = "N/A";
		this.target = "N/A";
		this.researchLink = "";
	}
	
	public TickerResearch(String rating, String target, String researchLink)
	{
		this.rating = rating;
		this.target = target;
		this.researchLink = researchLink;
	}


}
