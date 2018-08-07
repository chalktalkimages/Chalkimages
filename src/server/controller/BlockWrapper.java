package server.controller;

import java.util.ArrayList;

import data.BlockDetails;

public class BlockWrapper {

  public String fullname;
  public ArrayList<BlockDetails> blocks;
  public Boolean isFlow;

  public BlockWrapper() {
    fullname = "";
    blocks = new ArrayList<BlockDetails>();
    isFlow = true;
  }

  public BlockWrapper(String fullname, ArrayList<BlockDetails> blocks, Boolean isFlow) {
    this.fullname = fullname;
    this.blocks = blocks;
    this.isFlow = isFlow;
  }
}
