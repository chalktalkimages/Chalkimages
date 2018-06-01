package data;

public class BlockBroker {
	public String name;
	public Integer blockCount;
	public Integer shares;
	public Double notional;
	
	public BlockBroker() {
		this.name = "";
		this.blockCount = 0;
		this.shares = 0;
		this.notional = 0.;
	}
	
	public BlockBroker(String name, Integer blockCount, Integer shares, Double notional) {
		this.name = name;
		this.blockCount = blockCount;
		this.shares = shares;
		this.notional = notional;
	}	
}
