package data;

import java.util.HashMap;
import java.util.Map;

public class PostTradeOptions {
	public Map<String, Double> fxRateMap = new HashMap<String, Double>();
	public Map<String, String> benchmarkMap = new HashMap<String, String>();
	public String basketID = "";
	public boolean calcUnreal = false;
}
