package html;


import data.CommentDetails;
import data.GeneralComment;
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

public class HalftimeReportBuilder {

		
		private final static Logger logger = Logger.getLogger(HalftimeReportBuilder.class.getName());
		
		public static void buildReport(String fullName, ArrayList<CommentDetails> comments, Double eret, Double evol, Double mret, Double mvol, ArrayList<GeneralComment> generalComments) {


			String body = Utilities.getHTMLString("HalftimeTemplate.html");

			String fileSave = "ChalkTalkEmail.html"; 
			String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
			String otherComments = buildSymbolComments(comments);
			String macroComments = buildMacroComments(generalComments);
			String htmlSignature = Utilities.getHTMLString("halftimeSignature.html");
			
			body = body.replace("{{formattedDate}}", formattedDate)
					.replace("{{otherComments}}",  otherComments)
					.replace("{{macroComments}}",  macroComments)
					.replace("{{halftimeSignature}}", htmlSignature)
					.replace("{{group}}", (fullName.contains("Bill Liu") || fullName.contains("Tony Ye") || fullName.contains("Chad Reed") || fullName.contains("Bilal Ijaz")  || fullName.contains("Andrew Moffatt") ?"PORTFOLIO":"EQUITY"));

			
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
				Utilities.updateEmailRecipients(fullName, "Scotiabank Halftime Show");
			} catch (IOException e) {
				//System.err.println("Error sending email: " + e.getMessage() + "\n");
				logger.info("Error sending email: " + e.getMessage() + "\n");
			}
			

			Engine.getInstance().serverStatus.remove(fullName);
			MailRequest.sendRequest();
		}
		
		public static String buildMacroComments(ArrayList<GeneralComment> comments) {

			String result = "";
			String temp = "";
			String macroComment = Utilities.getHTMLString("HalftimeMacroComment.html");
			
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
						temp = temp.replace("{{body}}", Utilities.parseQuoteComment(comment.body(), true, false)); 				
					}				
					if (comment.link() != null && !comment.link().equals("") && !comment.link().equals("null")) {
						temp = temp.replace("{{link}}", " <a href=\"{rLink}\">Link</a>".replace("{rLink}", comment.link()));
					}
					else
					{
						temp = temp.replace("{{link}}", "");
					}
				
					result = result + temp + "\n"; 
				
			}			
			return result; 
		}
	
		public static String buildSymbolComments(ArrayList<CommentDetails> comments) {

			String result = "";
			String temp = "";
			String macroComment = Utilities.getHTMLString("HalftimeComment.html");

			for(CommentDetails comment : comments) {

				temp = macroComment;
				temp = temp.replace("{{belongsTo}}", SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo())))
					.replace("{{evol}}", String.format("%1$.2f", comment.excessVol))
					.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + " - " : "" ) + Utilities.parseQuoteComment(comment.body(),true, false)); 
				if (comment.returns >= 0)  // green
				{
					temp =  temp.replace("{{ret}}", "+"+String.format("%1$.2f", comment.returns*100))
					.replace("{{color}}", "#008000");
				}
				else // red
				{
					temp =  temp.replace("{{ret}}", String.format("%1$.2f", comment.returns*100))
					.replace("{{color}}", "#ed1b2e");
				}					
				result = result + temp + "\n"; 
				
			}				
			
			return result; 
		}
			
}
