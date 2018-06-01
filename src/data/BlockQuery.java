package data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import blockdata.BlockTrade;

public class BlockQuery {
	public List<String> tickers;
	public List<BlockTrade.TradeType> types = new ArrayList<BlockTrade.TradeType>();
	public List<String> brokers = new ArrayList<String>();
	public List<String> sectors = new ArrayList<String>();
	public List<String> subIndustries = new ArrayList<String>();
	public List<String> indices = new ArrayList<String>();
	public Calendar starttime, endtime;
	public boolean sortByVolume = false;
}
