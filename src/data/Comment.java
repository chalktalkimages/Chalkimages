package data;
//Defines required fields read from Flow
public class Comment {
	
	private Integer id; 
	private String RIC; 
	private String belongsTo; 
	private String body; 
	private String link;
	
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
	
	public String link() {
		return link;
	}	
	
	public Comment() {
		id = 0; 
		belongsTo = ""; 
		body = ""; 
		link = "";
	}
	

	
	public Comment(Integer id, String RIC, String belongsTo, String body, String link) {
		this.id = id; 
		this.RIC = RIC; 
		this.belongsTo = belongsTo; 
		this.body = body; 
		this.link = link;
	}

}
