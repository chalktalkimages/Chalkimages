package data;
//Defines required fields read from Flow
public class EnergyCommentDetails {
	
	public String name;
	public Integer id; 
	public String RIC; 
	public String belongsTo; 
	public String body; 
	public Integer ranking;
	public Double returns;
	public Double excessVol;
	public String sector;
	public Double prevclose;
	public String sentiment;
	public String summary;
	
	public String name() {
		return name;
	}
	
	public Integer id() {
		return id; 
	}
	public String RIC() {
		return RIC; 
	}
	public String belongsTo() {
		return belongsTo; 
	}
	public String body() {
		return body; 
	}
	public Integer ranking() {
		return ranking;
	}
	public Double returns() {
		return returns;
	}	
	public Double excessVol() {
		return excessVol;
	}	
	public String sector() {
		return sector;
	}
	
	public Double prevclose() {
		return prevclose;
	}
	
	public String sentiment() {
		return sentiment;
	}
	
	public String summary() {
		return summary;
	}
		
	
	public EnergyCommentDetails() {
		name = "";
		id = 0; 
		belongsTo = ""; 
		body = ""; 
		RIC = "";
		ranking = 0;
		returns = (double) 0;
		excessVol = (double) 0;
		sector = "";
		prevclose = 0.;
		sentiment = "";
		summary = "";
	}
	
	public EnergyCommentDetails(String name, Integer id, String RIC, String belongsTo, String body, Integer ranking, Double returns, Double excessVol, String sector, Double prevclose, String sentiment, String summary) {
		this.name = name;
		this.id = id; 
		this.RIC = RIC; 
		this.belongsTo = belongsTo; 
		this.body = body; 
		this.ranking = ranking;
		this.returns = returns;
		this.excessVol = excessVol;
		this.sector = sector;
		this.prevclose = prevclose;
		this.sentiment = sentiment;
		this.summary = summary;
	}

}
