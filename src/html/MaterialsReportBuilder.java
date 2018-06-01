package html;

import data.Comment;
import data.CommentDetails;
import data.GeneralComment;
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
import java.util.regex.Pattern;
import org.apache.log4j.Logger;


//Builds the HTML content of the report
public class MaterialsReportBuilder {	

	private final static Logger logger = Logger.getLogger(MaterialsReportBuilder.class
			.getName());
	
	private static String[] headerList = {"gold", "base metals", "copper supply", "cu supply", "iron ore", "met coal"};

	private static ArrayList<Comment> sortMiningComments(ArrayList<Comment> comments) {
		
		ArrayList<Comment> sortedList = new ArrayList<Comment>();
		ArrayList<Integer> addedCommentIndices = new ArrayList<Integer>();
		int listSize = comments.size();
		
		for (int i = 0; i < headerList.length; i++) {
			for (int j = 0; j < listSize; j++) {
				String title = comments.get(j).belongsTo().toLowerCase();
				if (title.contains(headerList[i]) && (!addedCommentIndices.contains(j))) {
					sortedList.add(comments.get(j));
					addedCommentIndices.add(j);
				}
			}
		}
		
		// add the comments that aren't in the default header list at the end:
		for (int i = 0; i < listSize; i++) {
			if (!addedCommentIndices.contains(i)) {
				sortedList.add(comments.get(i));
			}
		}
		
		return sortedList;
	}

	public static void buildReport(String fullName, ArrayList<CommentDetails> comments) {

        //Calendar cal = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        //String date = sdf.format(cal.getTime());
		// Git commit/push images to remote repository
		
		String body = Utilities.getHTMLString("MaterialsChalkTalkTemplate.html");
		ArrayList<Comment> allcomments = sortMiningComments(FlowAdapter.getComments()); 

		String fileSave = "ChalkTalkEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		//String imagePath = "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";
		String symbolComments = ChalktalkReportBuilder.buildSymbolComments(comments); 
		String htmlSignature = Utilities.getHTMLString("materialsSignature.html");
		String miningComments = buildminingComments(allcomments);
			
		String image1 = "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\DailyScoop\\image1.png";

		body = body.replace("{{image1}}", image1);
			
		body = body.replace("{{formattedDate}}", formattedDate)
			.replace("{{symbolComments}}",  symbolComments)
			.replace("{{emailSignature}}", htmlSignature)
			.replace("{{miningComments}}", miningComments)
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
			Utilities.updateEmailRecipients(fullName, "Scotia Daily Morning Scoop Blast");
		} catch (IOException e) {
			//System.err.println("Error sending email: " + e.getMessage() + "\n");
			logger.info("Error sending email: " + e.getMessage() + "\n");
		}
		

		Engine.getInstance().serverStatus.remove(fullName);
		MailRequest.sendRequest();
	}
	
	//Build the HTML for the before-the-bell-comments
	public static String buildminingComments(ArrayList<Comment> comments) {

		String result = "";
		String temp = "";
		String bellComment = Utilities.getHTMLString("MacroComment.html");
		
		for(Comment comment : comments) {
			if(comment.RIC().equals("MiningComment")) { //indicates before the bell comment
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


	
	//Build the HTML for the index-events-comments
	public static String buildIndexComments(ArrayList<Comment> comments) {

		String result = "";
		String temp = "";
		String indexComment = Utilities.getHTMLString("MacroComment.html");
		
		for(Comment comment : comments) {
			if(comment.RIC().equals("IndexComment")) { //indicates index comment
				temp = indexComment;
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
	

	public static String buildMacroComments(ArrayList<GeneralComment> comments) {

		String result = "";
		String temp = "";
		String macroComment = Utilities.getHTMLString("MacroComment.html");
		
		for(GeneralComment comment : comments) {
			temp = macroComment;
			
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
		return result; 
	}

	
	//Build the HTML for the symbol-specific comments
	public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

		String result = "";
		String temp = "";
		String symbolComment = Utilities.getHTMLString("SymbolComment.html");
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
						research = parser.getSymbolResearch(comment.belongsTo(), ticker);
					}
					
					temp = temp
							.replace("{{belongsTo}}", ticker)
							.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), false, true))
							.replace("{{rating}}", research.rating)
							.replace("{{target}}", research.target)
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


