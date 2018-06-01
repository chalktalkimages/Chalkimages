package data;
//Defines required fields read from Flow
public class GeneralComment {
	
	public String belongsTo; 
	public String body; 
	public Integer ranking;
	public String link;
	
	
	public String belongsTo() {
		return belongsTo; 
	}
	public String body() {
		return body; 
	}
	public Integer ranking() {
		return ranking;
	}
	
	public String link() {
		return link;
	}
	
	public GeneralComment() {

		belongsTo = ""; 
		body = ""; 
		ranking = 0;
		link = "";

	}
	
	public GeneralComment(String belongsTo, String body, Integer ranking, String link) { 
		this.belongsTo = belongsTo; 
		this.body = body; 
		this.ranking = ranking;
		this.link = link;
	}

}
