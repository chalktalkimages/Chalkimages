package user;

public class UserEmail {
	public String fullName;
	public String htmlSignature;
	public String emailRecipients;
	
	
	public UserEmail()
	{
		this.fullName = "";
		this.htmlSignature = "";
		this.emailRecipients = "";
	}
	
	public UserEmail(String fullName, String htmlSignature, String emailRecipients)
	{
		this.fullName = fullName;
		this.htmlSignature = htmlSignature;
		this.emailRecipients = emailRecipients;
	}

}
