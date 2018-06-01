package data;


import java.util.Calendar;

public class CorporateEvent {


	public String startdate;
	public String enddate;
	public String event;
	public String category;
	public String sector;
	
	public CorporateEvent() {
		startdate = Calendar.getInstance().toString();
		enddate = Calendar.getInstance().toString();
		event = "";
		category = "";
		sector = "";
	}	
	
	
	public CorporateEvent(String startdate, String enddate, String event, String category, String sector) {
		this.startdate = startdate;
		this.enddate = enddate;
		this.event = event;
		this.category = category;
		this.sector = sector;
	}
	
}
