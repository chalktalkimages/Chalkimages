package data;

public class BlockSecurity {
	public String ticker;
	public Integer blockCount;
	public Integer volume;
	public Double notional;
	public String scotiaRankbyVolume;
	public String scotiaRankbyValue;
	
	public BlockSecurity() {
		this.ticker = "";
		this.blockCount = 0;
		this.volume = 0;
		this.notional = 0.;
	}
	
	public BlockSecurity(String ticker, Integer blockCount, Integer volume, Double notional, String scotiaRankbyVolume, String scotiaRankbyValue) {
		this.ticker = ticker;
		this.blockCount = blockCount;
		this.volume = volume;
		this.notional = notional;
		this.scotiaRankbyVolume = scotiaRankbyVolume;
		this.scotiaRankbyValue = scotiaRankbyValue;
	}	
}
