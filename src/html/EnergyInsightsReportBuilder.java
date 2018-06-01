package html;

import data.Comment;
import data.CommentDetails;
import data.EnergyCommentDetails;
import data.TickerResearch;
import engine.Engine;
import adapter.FlowAdapter;
import adapter.MailRequest;
import utils.SymbolConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


//Builds the HTML content of the report
public class EnergyInsightsReportBuilder {	
	
	private final static Logger logger = Logger.getLogger(EnergyInsightsReportBuilder.class
			.getName());
	

	private static String name = "";

	public static void buildReport(String fullName, ArrayList<EnergyCommentDetails> comments) {

		
		name = fullName;
		
		String body = Utilities.getHTMLString("EnergyInsightsTemplate.html");
		ArrayList<Comment> bellmacroindexcomments = FlowAdapter.getComments(); 

		String fileSave = "ChalkTalkEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		
		ArrayList<CommentDetails> al_cd = new ArrayList<CommentDetails>();
		for (EnergyCommentDetails d : comments) {
			al_cd.add(new CommentDetails(d.id, d.RIC, d.belongsTo, d.body, d.ranking, d.returns, d.excessVol, d.sector, d.prevclose, d.sentiment, d.summary));
		}
		
		ArrayList<String> symbolComments = buildSymbolComments(al_cd, comments); 
		String bellComments = ChalktalkReportBuilder.buildBellComments(bellmacroindexcomments);
		String htmlSignature = Utilities.buildSignature(fullName);
			
		String buylist = getIOItradelist("buy");
		String selllist = getIOItradelist("sell");		

		body = body.replace("{{formattedDate}}", formattedDate)
			.replace("{{symbolComments}}",  symbolComments.get(0))
			.replace("{{symbolDetailComments}}",  symbolComments.get(1))
			.replace("{{bellComments}}", bellComments)
			.replace("{{emailSignature}}", htmlSignature)
			.replace("{{buylist}}", buylist)
			.replace("{{selllist}}", selllist)			
			.replace("{{group}}", (fullName.contains("Bill Liu") || fullName.contains("Chad Reed") || fullName.contains("Bilal Ijaz"))?"PORTFOLIO":"EQUITY");

		
		body = body.replaceAll(Pattern.quote("ʼ"), "'");
		body = body.replaceAll(Pattern.quote("’"), "'");
		body = body.replaceAll(Pattern.quote("–"), "-");
		body = body.replaceAll(Pattern.quote("–"), "-");
		body = body.replaceAll(Pattern.quote("“"), "\"");
		body = body.replaceAll(Pattern.quote("”"), "\"");
		body = body.replaceAll(Pattern.quote("…"), "...");
		body = body.replaceAll(Pattern.quote("—"), "-");
		
		try (PrintWriter out = new PrintWriter(fileSave)) {
			out.println(body);
			//System.out.print("Template Printed to: " + fileSave + "\n");
			logger.info("Template Printed to: " + fileSave + "\n");
			
		} 
		catch (FileNotFoundException e) {
			//System.err.println("Error writing html to file: " + e.getMessage() + "\n");
			logger.info("Error writing html to file: " + e.getMessage() + "\n");
		}
		
		
		try {
			Utilities.updateEmailRecipients(fullName, "Scotia Daily Energy Insights");
		} catch (IOException e) {
			//System.err.println("Error sending email: " + e.getMessage() + "\n");
			logger.info("Error sending email: " + e.getMessage() + "\n");
		}
		

		Engine.getInstance().serverStatus.remove(fullName);
		MailRequest.sendRequest();
	}
	


	private static String getIOItradelist(String side)
	{
		
		String list = "";
		
	    try {
			JSONObject json = Utilities.readJsonFromUrl("http://t65-w7-eqcash:8001/" + side + "-ioi");
			JSONArray iois = (JSONArray) json.get("iois");
			for (int i = 0; i < iois.length(); i++)
			{
				JSONObject obj = iois.getJSONObject(i);
				JSONObject partialsmap = obj.getJSONObject("partialsMap");
				
				Iterator array = partialsmap.keys();
				String key = "";
				
				while (array.hasNext())
				{
					key = (String) array.next();
				}
				JSONObject a;
				String ric;
				String ticker;
				if (!key.equals(""))
				{
					
					 a = partialsmap.getJSONObject(key);
					 ticker = a.getString("ticker");
					 ric = a.getString("RIC");
				}
				else
				{
					a = obj;
					ticker = a.getString("tickerPrefix");
					ric = a.getString("ticker");
				}
				
				
				boolean working = a.getBoolean("working");
				boolean retail = a.getBoolean("retail");
				
				if (!working && !retail && ticker != null && ric.endsWith(".TO") && !ric.contains("db.TO") && Utilities.isEnergyUtilTicker(ric))
				{
					int numshares = obj.getInt("shares");
					list += ", " + String.format("%1$d", numshares) + " " + ticker;
					//counter++;
				}
			}
			list = list.replaceFirst(", ", "");
		} catch (Exception e) {
			logger.info("Error in getting list of IOI buys/sells: " + e.getMessage());
		}
		
	    
	    return list;
	}
	


	
	//Build the HTML for the symbol-specific comments
	public static ArrayList<String> buildSymbolComments(ArrayList<CommentDetails> comments, ArrayList<EnergyCommentDetails> ecomments) {

		ArrayList<String> commentStrings = new ArrayList<String>();
		String result = "";
		String temp = "";
		String symbolComment = Utilities.getHTMLString("SymbolCommentLink.html");
		String result2 = "";
		String temp2 = "";
		String detailedsymbolComment = Utilities.getHTMLString("EnergySymbolComment.html");	
		ScotiaViewParser parser = new ScotiaViewParser();
	
		
		try {

			for(EnergyCommentDetails d : ecomments) {
				CommentDetails comment = new CommentDetails(d.id, d.RIC, d.belongsTo, d.body, d.ranking, d.returns, d.excessVol, d.sector, d.prevclose, d.sentiment, d.summary);
				if(!comment.RIC().equals("")) { //symbol is present
					temp = symbolComment;
					temp2 = detailedsymbolComment;
					String ticker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));
					TickerResearch research;
					Utilities.updateStatus(name, ticker);
					research = parser.getSymbolResearch(comment.belongsTo(), ticker);
					
					temp = temp
							.replace("{{belongsTo}}", ticker)
							.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + " - " : "" ) + Utilities.parseQuoteComment(comment.body(), false, true))
							.replace("{{rating}}", research.rating)
							.replace("{{target}}", research.target)
							.replace("{{prevclose}}", comment.prevclose.toString())
							.replace("{{researchLink}}", research.researchLink); 
					temp2 = temp2
							.replace("{{FirmName}}", Utilities.toTitleCase(d.name))
							.replace("{{belongsTo}}", ticker)
							.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + " - " : "" ) + Utilities.parseQuoteComment(comment.body(), false, true))
							.replace("{{rating}}", research.rating)
							.replace("{{prevclose}}", comment.prevclose.toString())
							.replace("{{target}}", research.target)
							.replace("{{researchLink}}", research.researchLink); 
							; 
					
					result = result + temp + "\n"; 
					result2 = result2 + temp2 + "\n"; 

				} 
			}			

		} catch (Exception e) {
			//e.printStackTrace();
			logger.info("Error: " + e.getMessage());
		}
		commentStrings.add(result);
		commentStrings.add(result2);

		return commentStrings;
	}

	
	
	//Build the HTML for the before-the-bell-comments
	public static String buildBellComments(ArrayList<Comment> comments) {

		String result = "";
		String temp = "";
		String bellComment = Utilities.getHTMLString("MacroComment.html");
		
		for(Comment comment : comments) {
			if(comment.RIC().equals("BellComment")) { //indicates before the bell comment
				temp = bellComment;
				if (comment.belongsTo() == null)
				{
					temp = temp.replace("{{belongsTo}}", " ");
				}
				else
				{
					temp = temp.replace("{{belongsTo}}", comment.belongsTo());
				}
				if  (comment.body() == null)
				{
					temp = temp.replace("{{body}}", " "); 
				}
				else
				{
					temp = temp.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), false, true)); 				
				}
				result = result + temp + "\n"; 
			} 
		}			
		return result; 
	}

	
	public static void main(String[] Args ) {

	}
	
}


