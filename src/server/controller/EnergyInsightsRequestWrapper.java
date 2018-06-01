package server.controller;

import java.util.ArrayList;

import user.User;
import data.EnergyCommentDetails;
import data.GeneralComment;

public class EnergyInsightsRequestWrapper {
	
	public int id;
	public User user;
	public ArrayList<EnergyCommentDetails> flow;
	public Double energyReturn;
	public Double energyEVol;
	public Double materialsReturn;
	public Double materialsEVol;	
	public ArrayList<GeneralComment> generalComments;
	public EnergyInsightsRequestWrapper()
	{
		id = 0;
		user = new User();
		flow = new ArrayList<EnergyCommentDetails>();
		energyReturn=(double) 0;
		energyEVol=(double) 0;
		materialsReturn=(double) 0;
		materialsEVol=(double) 0;
		generalComments = new ArrayList<GeneralComment>();
	}
	public EnergyInsightsRequestWrapper(int id, User user, ArrayList<EnergyCommentDetails> flow, Double energyReturn, Double energyEVol, Double materialsReturn, Double materialsEVol, ArrayList<GeneralComment> generalComments)
	{
		this.id = id;
		this.user = user;
		this.flow = flow;
		this.energyReturn=energyReturn;
		this.energyEVol=energyEVol;
		this.materialsReturn = materialsReturn;
		this.materialsEVol=materialsEVol;
		this.generalComments = generalComments;
	}
}