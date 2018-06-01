package html;

import data.CommentDetails;
import data.TickerResearch;
import engine.Engine;
import adapter.MailRequest;
import utils.SymbolConverter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


//Builds the HTML content of the report
public class EnergyReportBuilder {	
	
	private final static Logger logger = Logger.getLogger(EnergyReportBuilder.class
			.getName());
	
	private static String name = "";
	
	public static void buildReport(String fullName, ArrayList<CommentDetails> comments) {

		name = fullName;
		
        //Calendar cal = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        //String date = sdf.format(cal.getTime());

		String body = Utilities.getHTMLString("EnergyChalkTalkTemplate.html");
		//ArrayList<Comment> bellmacroindexcomments = FlowAdapter.getComments(); 

		String fileSave = "ChalkTalkEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		//String imagePath = "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";
		String symbolComments = buildSymbolComments(comments); 
		String htmlSignature = Utilities.buildSignature(fullName);
			
		

		body = body.replace("{{formattedDate}}", formattedDate)
			.replace("{{symbolComments}}",  symbolComments)
			.replace("{{emailSignature}}", htmlSignature)
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
			Utilities.updateEmailRecipients(fullName, "Scotia Energy Morning Blast");
		} catch (IOException e) {
			//System.err.println("Error sending email: " + e.getMessage() + "\n");
			logger.info("Error sending email: " + e.getMessage() + "\n");
		}
		

		Engine.getInstance().serverStatus.remove(fullName);
		MailRequest.sendRequest();
	}
	
	//Build the HTML for the symbol-specific comments
	public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

		String result = "";
		String temp = "";
		String symbolComment = Utilities.getHTMLString("EnergyShortComment.html");
		ScotiaViewParser parser = new ScotiaViewParser();
		
		try {
			for(CommentDetails comment : comments) {
				if(!comment.RIC().equals("")) { //symbol is present
					temp = symbolComment;
					String ticker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo()));
					TickerResearch research;
					if(comment.tickerResearch != null )
						research = comment.tickerResearch;
					else {
						Utilities.updateStatus(name, ticker);
						research = parser.getSymbolResearch(comment.belongsTo(), ticker);
					}
					temp = temp
							.replace("{{belongsTo}}", ticker)
							.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + " - " : "" ) + Utilities.parseQuoteComment(comment.body(), false, true))
							.replace("{{rating}}", research.rating)
							.replace("{{target}}", research.target)
							.replace("{{prevclose}}", comment.prevclose.toString())
							.replace("{{researchLink}}", research.researchLink); 
					result = result + temp + "\n"; 
				} 
			}			

		} catch (Exception e) {
			//e.printStackTrace();
			logger.info("Error: " + e.getMessage());
		}
		
		
		return result;
	}


	
}


