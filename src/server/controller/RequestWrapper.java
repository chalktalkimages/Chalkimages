package server.controller;

import java.util.ArrayList;

import user.User;
import data.CommentDetails;
import data.GeneralComment;

public class RequestWrapper {
	
	public int id;
	public User user;
	public ArrayList<CommentDetails> flow;
	public Double energyReturn;
	public Double energyEVol;
	public Double materialsReturn;
	public Double materialsEVol;	
	public ArrayList<GeneralComment> generalComments;
	public ArrayList<String> reportSections;
	public RequestWrapper()
	{
		id = 0;
		user = new User();
		flow = new ArrayList<CommentDetails>();
		energyReturn=(double) 0;
		energyEVol=(double) 0;
		materialsReturn=(double) 0;
		materialsEVol=(double) 0;
		generalComments = new ArrayList<GeneralComment>();
		reportSections = new ArrayList<String>();
	}
	public RequestWrapper(int id, User user, ArrayList<CommentDetails> flow, Double energyReturn, Double energyEVol, Double materialsReturn, Double materialsEVol, ArrayList<GeneralComment> generalComments, ArrayList<String> reportSections)
	{
		this.id = id;
		this.user = user;
		this.flow = flow;
		this.energyReturn=energyReturn;
		this.energyEVol=energyEVol;
		this.materialsReturn = materialsReturn;
		this.materialsEVol=materialsEVol;
		this.generalComments = generalComments;
		this.reportSections = reportSections;
	}
}