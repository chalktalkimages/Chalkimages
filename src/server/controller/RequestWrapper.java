package server.controller;

import java.util.ArrayList;

import data.CommentDetails;
import data.GeneralComment;
import user.User;

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
  public boolean ranked;

  public RequestWrapper() {
    id = 0;
    user = new User();
    flow = new ArrayList<CommentDetails>();
    energyReturn = (double) 0;
    energyEVol = (double) 0;
    materialsReturn = (double) 0;
    materialsEVol = (double) 0;
    generalComments = new ArrayList<GeneralComment>();
    reportSections = new ArrayList<String>();
    ranked = false;
  }

  public RequestWrapper(
      int id,
      User user,
      ArrayList<CommentDetails> flow,
      Double energyReturn,
      Double energyEVol,
      Double materialsReturn,
      Double materialsEVol,
      ArrayList<GeneralComment> generalComments,
      ArrayList<String> reportSections,
      boolean ranked) {
    this.id = id;
    this.user = user;
    this.flow = flow;
    this.energyReturn = energyReturn;
    this.energyEVol = energyEVol;
    this.materialsReturn = materialsReturn;
    this.materialsEVol = materialsEVol;
    this.generalComments = generalComments;
    this.reportSections = reportSections;
    this.ranked = ranked;
  }
}
