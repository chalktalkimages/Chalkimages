package BlockReport;

import html.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import adapter.DBHandle;
import jxl.Workbook;
import jxl.write.Alignment;
import jxl.write.Border;
import jxl.write.BorderLineStyle;
import jxl.write.Colour;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import utils.SymbolConverter;
import data.BlockAnalyst;
import data.BlockBroker;
import data.BlockDetails;
import data.BlockSecurity;
import data.TraderSummary;
import data.TraderTotal;

public class BlockReportGenerator {
	
	
	private final static Logger logger = Logger.getLogger(BlockReportGenerator.class.getName());

	
	// returns the first three words of the broker name 
	private static String getTruncatedName(String brokerName) {
	    String [] arr = brokerName.split("\\s+"); 
         //Splits words & assign to the arr[]  ex : arr[0] -> Copying ,arr[1] -> first
        int N=2; // NUMBER OF WORDS THAT YOU NEED
        String nWords="";

        // concatenating number of words that you required
        for(int i=0; i < N && i < arr.length ; i++){
            if (i == 0) 
            	nWords = arr[i] ;         
            else
            	nWords = nWords + " " + arr[i] ;         
        }		
		
	    return nWords;
	}
	
	public static String formatLargeNumber(long number, String prefix) {
		double num = Math.abs(number);
		String result = "";
		if(num >= 1000000000)
			result = Math.round((Math.round(num / 10000000.0) / 100.0) * 100.0)/100.0 + "B";
		else if (num >= 1000000)
			result = Math.round((Math.round(num / 100000.0) / 10.0) * 10.0)/10.0 + "M";
        else if (num >= 1000)
            result = Math.round(num / 1000.0) + "K";
        else
            result = Long.toString(Math.round(num));
		if(!result.isEmpty()) {
			result = prefix + result;
			if(number < 0)
				result = "-" + result;
		}
		return result;
	}
	

	// groups by broker
	public static Collection<BlockBroker> getBlocksByBroker(ArrayList<BlockDetails> blocks) {
		HashMap<String, BlockBroker> brokerblockmap = new HashMap<String, BlockBroker>();
		for (BlockDetails block : blocks) {
			if (!brokerblockmap.containsKey(block.brokerName)) {
				BlockBroker bb = new BlockBroker(block.brokerName, 1, block.tradeSize, block.notional);
				brokerblockmap.put(block.brokerName, bb);
			}
			else {
				BlockBroker bb = brokerblockmap.get(block.brokerName);
				bb.blockCount++;
				bb.shares += block.tradeSize;
				bb.notional += block.notional;
				brokerblockmap.put(block.brokerName, bb);
			}
		}
		return brokerblockmap.values();
	}
	
	public static TreeMap<String, List<BlockAnalyst>> getBlocksByTrader(ArrayList<BlockDetails> blocks) {
		return getBlocksByMap(blocks, false);
	}
	
	public static TreeMap<String, List<BlockAnalyst>> getBlocksByAnalyst(ArrayList<BlockDetails> blocks) {
		return getBlocksByMap(blocks, true);
	}
	
	// groups by analyst or trader, broker, aggregate fiscal, year, month
	private static TreeMap<String, List<BlockAnalyst>> getBlocksByMap(ArrayList<BlockDetails> blocks, boolean isAnalyst) {
		TreeMap<String, List<BlockAnalyst>> blockmap = new TreeMap<String, List<BlockAnalyst>>();
		
		Calendar lastMonth = Calendar.getInstance();
		lastMonth.add(Calendar.MONTH, -1);
		lastMonth.set(Calendar.HOUR_OF_DAY, 1);
		
		Calendar fiscalYear = Calendar.getInstance();
		fiscalYear.set(Calendar.MONTH, Calendar.NOVEMBER);
		fiscalYear.set(Calendar.DAY_OF_MONTH, 1);
		if(fiscalYear.after(Calendar.getInstance())) // if after today, reduce year by 1
			fiscalYear.add(Calendar.YEAR, -1);
			
		Calendar pastYear = Calendar.getInstance();
		pastYear.set(Calendar.DAY_OF_YEAR, 1);
		pastYear.set(Calendar.HOUR_OF_DAY, 1);
		pastYear.add(Calendar.YEAR, -1);

		for (BlockDetails block : blocks) {
			String key = isAnalyst ? block.analyst : block.trader;
			if(key == null || key.length() <= 1 )
				continue;
			BlockAnalyst existingBa = null;
			if (!blockmap.containsKey(key)) {
				existingBa = new BlockAnalyst(block);
				blockmap.put(key, new LinkedList<BlockAnalyst>(Arrays.asList(existingBa)));
			}
			if(existingBa == null) {
				List<BlockAnalyst> balist = blockmap.get(key);
				for(BlockAnalyst ba : balist) {
					if(ba.brokerCode.equals(block.brokerID)) {
						existingBa = ba;
						break;
					}
				}
				if(existingBa == null) {
					existingBa = new BlockAnalyst(block);
					balist.add(existingBa);
				}
			}
			
			if(block.tradeTime > pastYear.getTimeInMillis()) {
				existingBa.PYshares += block.tradeSize;
				existingBa.PYnotional += block.notional;
				existingBa.PYblockCount++;
			}
			if(block.tradeTime > fiscalYear.getTimeInMillis()) {
				existingBa.YTDshares += block.tradeSize;
				existingBa.YTDnotional += block.notional;
				existingBa.YTDblockCount++;
			}
			if(block.tradeTime > lastMonth.getTimeInMillis()) {
				existingBa.MTDshares += block.tradeSize;
				existingBa.MTDnotional += block.notional;
				existingBa.MTDblockCount++;
			}
		}
		return blockmap;
	}

	private static Map<String, String> getScotiaRanking(ArrayList<BlockDetails> blocks, String category) {
		// a map with ticker as key and an inner map with brokerID and total trade volume for each broker as value
		Map<String, Map<String, Double>> results = new HashMap<String, Map<String, Double>>();
		for (BlockDetails block : blocks) {
			if (results.containsKey(block.ticker)) {
				Map<String, Double> currKey = results.get(block.ticker);
				if (!currKey.containsKey(block.brokerID)) {
					if (category.equals("volume")) {
						currKey.put(block.brokerID, block.tradeSize.doubleValue());
					} else {
						currKey.put(block.brokerID, block.notional);
					}
					results.put(block.ticker, currKey);
				} else {
					Double currTotal = currKey.get(block.brokerID);
					if (category.equals("volume")) {
						currTotal += block.tradeSize.doubleValue();
					} else {
						currTotal += block.notional;
					}
					currKey.put(block.brokerID, currTotal);
					results.put(block.ticker, currKey);
				}
			} else {
				Map<String, Double> currKey = new HashMap<String, Double>();
				if (category.equals("volume")) {
					currKey.put(block.brokerID, block.tradeSize.doubleValue());
				} else {
					currKey.put(block.brokerID, block.notional);
				}
				results.put(block.ticker, currKey);
			}
		}
		// a map with ticker as key, Scotiabank ranking as value
		Map<String, String> scotiaRank = new HashMap<String, String>();
		for (Map.Entry<String, Map<String, Double>> result : results.entrySet()) {
			String ticker = result.getKey();
			Map<String, Double> currKey = result.getValue();
			if (!currKey.containsKey("85")) {
				// if Scotiabank didn't execute this, set it '-'
				scotiaRank.put(ticker, "-");
			} else {
				Integer currRank = 1;
				Double scotiaValue = currKey.get("85");
				for (Map.Entry<String, Double> brokerPair : currKey.entrySet()) {
					if (brokerPair.getValue() > scotiaValue) {
						currRank += 1;
					}
				}
				scotiaRank.put(ticker, Integer.toString(currRank));
			}
		}
		return scotiaRank;
	}
	
	// groups by security
	public static Collection<BlockSecurity> getBlocksBySecurity(ArrayList<BlockDetails> blocks, java.lang.Boolean isFlow) {
		HashMap<String, BlockSecurity> secblockmap = new HashMap<String, BlockSecurity>();
		
		// helper to get rankings
		Map<String, String> scotiaRankbyVolume = getScotiaRanking(blocks, "volume");
		Map<String, String> scotiaRankbyValue = getScotiaRanking(blocks, "value");
		
		// if data from flow: do not need to convert RIC to ticker
		if (isFlow) {
			for (BlockDetails block : blocks) {
				if (!secblockmap.containsKey(block.ticker)) {
					BlockSecurity bs = new BlockSecurity(block.ticker, 1, block.tradeSize, block.notional, scotiaRankbyVolume.get(block.ticker), scotiaRankbyValue.get(block.ticker));
					secblockmap.put(block.ticker, bs);
				}
				else {
					BlockSecurity bs = secblockmap.get(block.ticker);
					bs.blockCount++;
					bs.volume += block.tradeSize;
					bs.notional += block.notional;
					secblockmap.put(block.ticker, bs);
				}
			}		
		}
		else {
			for (BlockDetails block : blocks) {
				String ticker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(block.ticker));
				if (!secblockmap.containsKey(ticker)) {
					BlockSecurity bs = new BlockSecurity(ticker, 1, block.tradeSize, block.notional, scotiaRankbyVolume.get(block.ticker), scotiaRankbyValue.get(block.ticker));
					secblockmap.put(ticker, bs);
				}
				else {
					BlockSecurity bs = secblockmap.get(ticker);
					bs.blockCount++;
					bs.volume += block.tradeSize;
					bs.notional += block.notional;
					secblockmap.put(ticker, bs);
				}
			}
		}
		
		
		return secblockmap.values();
	}
	
	private static String formatATRow(List<BlockAnalyst> blocklist, String rowTemplate) {
		Integer count = 1;
		String rowshtml = "";
		for (BlockAnalyst b: blocklist) {
			if (count > 10) { break; } 
			rowshtml += rowTemplate.replace("{{rank}}", count.toString())
									.replace("{{name}}", getTruncatedName(b.brokerName))
									.replace("{{PYnumtrades}}", b.PYblockCount.toString())
									.replace("{{PYvolume}}", formatLargeNumber(b.PYshares, ""))
									.replace("{{PYvalue}}", formatLargeNumber(b.PYnotional.longValue(), "$"))
									.replace("{{YTDnumtrades}}", b.YTDblockCount.toString())
									.replace("{{YTDvolume}}", formatLargeNumber(b.YTDshares,""))
									.replace("{{YTDvalue}}", formatLargeNumber(b.YTDnotional.longValue(), "$"))
									.replace("{{MTDnumtrades}}", b.MTDblockCount.toString())
									.replace("{{MTDvolume}}", formatLargeNumber(b.MTDshares, ""))
									.replace("{{MTDvalue}}", formatLargeNumber(b.MTDnotional.longValue(), "$"));
			count++;
		}
		return rowshtml;
	}
	
	private static String[] generateATTable(Map<String, List<BlockAnalyst>> dataMap, String tableTemplate, String rowTemplate ) {
		String blockTableValue = "", blockTableVolume = "";
		for(Entry<String, List<BlockAnalyst>> entry : dataMap.entrySet()) {
			String key = entry.getKey();
			List<BlockAnalyst> blocklist = entry.getValue();
			String tablehtml = "";
			tablehtml = tableTemplate.replace("{{category}}", key);
			// sort by value
			Collections.sort(blocklist, new Comparator<BlockAnalyst>() {
				@Override
				public int compare(BlockAnalyst block1, BlockAnalyst block2)
				{
					return (block2.YTDnotional - block1.YTDnotional) > 0 ? 1 : ((block2.YTDnotional - block1.YTDnotional) < 0 ? -1 : 0);
				}
			});
			tablehtml = tablehtml.replace("{{atTableRow}}", formatATRow(blocklist, rowTemplate));
			blockTableValue += tablehtml;
			
			// sort by volume
			tablehtml = tableTemplate.replace("{{category}}", key);
			Collections.sort(blocklist, new Comparator<BlockAnalyst>() {
				@Override
				public int compare(BlockAnalyst block1, BlockAnalyst block2)
				{
					return (block2.YTDshares- block1.YTDshares) > 0 ? 1 : ((block2.YTDshares - block1.YTDshares) < 0 ? -1 : 0);
				}
			});
			tablehtml = tablehtml.replace("{{atTableRow}}", formatATRow(blocklist, rowTemplate));
			blockTableVolume += tablehtml;
		}
		return new String[] { blockTableValue, blockTableVolume };
	}
	
	public static void saveAnalystTraderEmailHtml(String fullname, Map<String, List<BlockAnalyst>> analystMap, Map<String, List<BlockAnalyst>> traderMap) {
		
		String fileSave = "BlockReportEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		String reportHtml = Utilities.getHTMLString("ATBlockReportTemplate.html");
		String tableTemplate = Utilities.getHTMLString("ATBlockTable.html");
		String rowTemplate = Utilities.getHTMLString("ATBlockTableRow.html");
		
		String[] analystATTables = generateATTable(analystMap, tableTemplate, rowTemplate);
		reportHtml = reportHtml.replace("{{analystBlockTableValue}}", analystATTables[0]);
		reportHtml = reportHtml.replace("{{analystBlockTableVolume}}", analystATTables[1]);
		
		String[] traderATTables = generateATTable(traderMap, tableTemplate, rowTemplate);
		reportHtml = reportHtml.replace("{{traderBlockTableValue}}", traderATTables[0]);
		reportHtml = reportHtml.replace("{{traderBlockTableVolume}}", traderATTables[1]);
		
		reportHtml = reportHtml.replace("{{formattedDate}}", formattedDate).replace("{{emailSignature}}", Utilities.buildSignature(fullname));					
		
		try (PrintWriter out = new PrintWriter(fileSave)) {
			out.println(reportHtml);			
		} 
		catch (FileNotFoundException e) {
		}	
	}
	
	private static boolean checkExists(ArrayList<BlockDetails> blocks, BlockDetails blockdetail) {
		for (BlockDetails block : blocks) {
			if (block.equals(blockdetail)) {
				logger.info("same tradeds found");
				return true;
			}
		}
		return false;
	}
	
	private static ArrayList<BlockDetails> filterBlocks(ArrayList<BlockDetails> blocks) {
		ArrayList<BlockDetails> filteredBlocks = new ArrayList<BlockDetails>();
		for (BlockDetails block : blocks) {
			if (!checkExists(filteredBlocks, block)) {
				filteredBlocks.add(block);
			}
		}
		return filteredBlocks;
	}
	
	public static Map<String, Map<String, TraderSummary>> getMarketSharesByTrader(ArrayList<BlockDetails> blocks) throws Exception {
		ArrayList<BlockDetails> blocksNew = filterBlocks(blocks);
		ArrayList<BlockDetails> blocksWithName = new ArrayList<BlockDetails>();
		
		Map<String, String> nameMap = new DBHandle().nameMap;
		for (BlockDetails block : blocksNew) {
			if (nameMap.containsKey(block.ticker)){
				block.fullName = nameMap.get(block.ticker);
			}
			blocksWithName.add(block);
		}
		
		Map<String, Map<String, TraderSummary>> traderMap = new HashMap<String, Map<String, TraderSummary>>();
		for (BlockDetails block : blocksWithName) {
			if (!traderMap.containsKey(block.trader)) {
				TraderSummary newTS = new TraderSummary(block);
				Map<String, TraderSummary> tradersummarymap = new HashMap<String, TraderSummary>();
				tradersummarymap.put(block.ticker, newTS);
				traderMap.put(block.trader, tradersummarymap);
			} else {
				Map<String, TraderSummary> tradersummarymap = traderMap.get(block.trader);
				if (!tradersummarymap.containsKey(block.ticker)) {
					TraderSummary ts = new TraderSummary(block);
					tradersummarymap.put(block.ticker, ts);
					traderMap.put(block.trader, tradersummarymap);
				} else {
					TraderSummary ts = tradersummarymap.get(block.ticker);
					ts.marketVolume += block.tradeSize;
					ts.marketValue += block.notional;
					if (block.brokerID.equals("85")) {
						ts.scotiaVolume += block.tradeSize;
						ts.scotiaValue += block.notional;
					}
					tradersummarymap.put(block.ticker, ts);
					traderMap.put(block.trader, tradersummarymap);
				}
			}
		}
		return traderMap;
	}
	
	private static String formatTotalMarketRow(ArrayList<TraderTotal> traderTotal, String rowTemplate) {
		Integer count = 1;
		String rowshtml = "";
		for (TraderTotal tradertotal: traderTotal) {
			rowshtml += rowTemplate.replace("{{tradername}}", tradertotal.traderName)
									.replace("{{blockvolume}}", formatLargeNumber(tradertotal.marketTotalVolume.longValue(), ""))
									.replace("{{volumepercent}}", tradertotal.volumePercent + "%")
									.replace("{{blockvalue}}", formatLargeNumber(tradertotal.marketTotalValue.longValue(), "$"))
									.replace("{{valuepercent}}", tradertotal.valuePercent + "%");
			count++;
		}
		return rowshtml;
	}
	
	private static String formatScotiaVStotalRow(ArrayList<TraderTotal> traderTotal, String rowTemplate) {
		Integer count = 1;
		String rowshtml = "";
		for (TraderTotal tradertotal: traderTotal) {
			rowshtml += rowTemplate.replace("{{tradername}}", tradertotal.traderName)
									.replace("{{scotiavolume}}", formatLargeNumber(tradertotal.scotiaTotalVolume.longValue(), ""))
									.replace("{{scotiavolumepercent}}", tradertotal.scotiaVolumePercent + "%")
									.replace("{{scotiavalue}}", formatLargeNumber(tradertotal.scotiaTotalValue.longValue(), "$"))
									.replace("{{scotiavaluepercent}}", tradertotal.scotiaValuePercent + "%");
			count++;
		}
		return rowshtml;
	}
	
	private static String formatTraderPercentScotiaRow(ArrayList<TraderTotal> traderTotal, String rowTemplate) {
		Integer count = 1;
		String rowshtml = "";
		for (TraderTotal tradertotal: traderTotal) {
			rowshtml += rowTemplate.replace("{{tradername}}", tradertotal.traderName)
									.replace("{{blockvolume}}", tradertotal.volumePercentofScotia + "%")
									.replace("{{blockvalue}}", tradertotal.valuePercentofScotia + "%");
			count++;
		}
		return rowshtml;
	}
	
	
	private static String formatMBDetailRow(ArrayList<TraderSummary> blocks, String rowTemplate) {
		Integer count = 1;
		String rowshtml = "";
		for (TraderSummary block: blocks) {
			rowshtml += rowTemplate.replace("{{ticker}}", block.ticker)
									.replace("{{name}}", block.fullName)
									.replace("{{scotiavolume}}", block.scotiaVolume.toString())
									.replace("{{scotiavalue}}", formatLargeNumber(block.scotiaValue.longValue(), ""))
									.replace("{{marketvolume}}", block.marketVolume.toString())
									.replace("{{marketvalue}}", formatLargeNumber(block.marketValue.longValue(), "$"));
			count++;
		}
		return rowshtml;
	}
	

	
	private static Map<String, TraderTotal> generateTraderTotal(Map<String, Map<String, TraderSummary>> traderMap) {
		Map<String, TraderTotal> traderTotalMap = new HashMap<String, TraderTotal>();
		Long scotiaVolume = (long) 0;
		Double scotiaValue = 0.0;
		Long marketVolume = (long) 0;
		Double marketValue = 0.0;
		for (Entry<String, Map<String, TraderSummary>> traderSummary : traderMap.entrySet()) {
			String traderName = traderSummary.getKey();
			Map<String, TraderSummary> traderInfo = traderSummary.getValue();
			TraderTotal traderTotal = new TraderTotal(traderName);
			for (Entry<String, TraderSummary> tsInfo : traderInfo.entrySet()) {
				TraderSummary tsSummary = tsInfo.getValue();
				traderTotal.marketTotalVolume += tsSummary.marketVolume;
				traderTotal.marketTotalValue += tsSummary.marketValue;
				traderTotal.scotiaTotalVolume += tsSummary.scotiaVolume;
				traderTotal.scotiaTotalValue += tsSummary.scotiaValue;
			}
			scotiaVolume += traderTotal.scotiaTotalVolume;
			scotiaValue += traderTotal.scotiaTotalValue;
			marketVolume += traderTotal.marketTotalVolume;
			marketValue += traderTotal.marketTotalValue;
			
			traderTotalMap.put(traderName, traderTotal);
		}
		for (Entry<String, TraderTotal> traderTM : traderTotalMap.entrySet()) {
			String traderName = traderTM.getKey();
			TraderTotal tt = traderTM.getValue();
			DecimalFormat df = new DecimalFormat("#.00");
			if (marketVolume > 0) {
				tt.volumePercent = df.format(100 * tt.marketTotalVolume.doubleValue() / marketVolume.doubleValue());
			}
			if (marketValue > 0) {
				tt.valuePercent = df.format(100 * tt.marketTotalValue / marketValue);
			}
			if (tt.marketTotalVolume > 0) {
				tt.scotiaVolumePercent = df.format(100 * tt.scotiaTotalVolume.doubleValue() / tt.marketTotalVolume.doubleValue());
			}
			if (tt.marketTotalValue > 0) {
				tt.scotiaValuePercent = df.format(100 * tt.scotiaTotalValue / tt.marketTotalValue);
			}
			if (scotiaVolume > 0) {
				tt.volumePercentofScotia = df.format(100 * tt.scotiaTotalVolume.doubleValue() / scotiaVolume.doubleValue());
			}
			if (scotiaValue > 0) {
				tt.valuePercentofScotia = df.format(100 * tt.scotiaTotalValue / scotiaValue);
			}	
			traderTotalMap.put(traderName, tt);
		}
		return traderTotalMap;
		
	}
	
	private static String[] generateTraderSummaryTable(Map<String, Map<String, TraderSummary>> traderMap, String[] tableTemplates, String[] rowTemplates) {
		String[] result = new String[4];
		Map<String, TraderTotal> traderTotalMap = generateTraderTotal(traderMap);
		for (int i = 0; i < tableTemplates.length; i++) {
			String blockTableValue = "";
			String tableTemplate = tableTemplates[i];
			String rowTemplate = rowTemplates[i];
			if (i != 3) {
				ArrayList<TraderTotal> traderList = new ArrayList<TraderTotal>();
				for (Entry<String, TraderTotal> tradertotalmap : traderTotalMap.entrySet()) {
					TraderTotal tradertotal = tradertotalmap.getValue();
					traderList.add(tradertotal);
				}
				Collections.sort(traderList, new Comparator<TraderTotal>() {
					@Override
					public int compare(TraderTotal tt1, TraderTotal tt2) {
						return (tt1.traderName.compareTo(tt2.traderName));
					}
				});
				if (i == 0) {
					String tablehtml = "";
					tablehtml = tableTemplate.replace("{{atTableRow}}", formatTotalMarketRow(traderList, rowTemplate));
					blockTableValue += tablehtml;
				} else if (i == 1) {
					String tablehtml = "";
					tablehtml = tableTemplate.replace("{{atTableRow}}", formatScotiaVStotalRow(traderList, rowTemplate));
					blockTableValue += tablehtml;
				} else {
					String tablehtml = "";
					tablehtml = tableTemplate.replace("{{atTableRow}}", formatTraderPercentScotiaRow(traderList, rowTemplate));
					blockTableValue += tablehtml;
				}
				
			}	
			if (i == 3) {
				for (Entry<String, Map<String, TraderSummary>> tradermapsummary : traderMap.entrySet()) {
					String tradername = tradermapsummary.getKey();
					Map<String, TraderSummary> summarymap = tradermapsummary.getValue();
					ArrayList<TraderSummary> summarylist = new ArrayList<TraderSummary>();
					for (Entry<String, TraderSummary> summary : summarymap.entrySet()) {
						TraderSummary singlesummary = summary.getValue();
						singlesummary.ticker = SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(singlesummary.ticker));
						summarylist.add(singlesummary);
					}
					String tablehtml = "";
					tablehtml = tableTemplate.replace("{{category}}", tradername);
					// sort by value
					Collections.sort(summarylist, new Comparator<TraderSummary>() {
						@Override
						public int compare(TraderSummary summary1, TraderSummary summary2)
						{
							return (summary1.ticker.compareTo(summary2.ticker));
						}
						});
					tablehtml = tablehtml.replace("{{atTableRow}}", formatMBDetailRow(summarylist, rowTemplate));
					blockTableValue += tablehtml;
				}
			}	
			result[i] = blockTableValue;
		}
		return result;
	}
	
	public static void saveMarketShareSummaryEmailHtml(String fullname, Map<String, Map<String, TraderSummary>> traderMap) throws Exception {
		

		String fileSave = "BlockReportEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		String reportHtml = Utilities.getHTMLString("MarketBlockReportTemplate.html");
		String[] tableSummaryTemplates = new String[] {Utilities.getHTMLString("MarketBlockSummaryTotalTable.html"), Utilities.getHTMLString("MarketBlockSummaryVSTable.html"), Utilities.getHTMLString("MarketBlockSummaryPercentTable.html"), Utilities.getHTMLString("MarketBlockDetailTable.html")};
		
		String[] rowSummaryTemplates = new String[] {Utilities.getHTMLString("MarketBlockSummaryTotalRow.html"), Utilities.getHTMLString("MarketBlockSummaryVSRow.html"), Utilities.getHTMLString("MarketBlockSummaryPercentRow.html"), Utilities.getHTMLString("MarketBlockDetailRow.html")};

		String[] traderSummaryTables = generateTraderSummaryTable(traderMap, tableSummaryTemplates, rowSummaryTemplates);
		reportHtml = reportHtml.replace("{{totalMarketBlockTable}}", traderSummaryTables[0]);
		reportHtml = reportHtml.replace("{{scotiaVStotalBlockTable}}", traderSummaryTables[1]);
		reportHtml = reportHtml.replace("{{traderPercentTable}}", traderSummaryTables[2]);					
		reportHtml = reportHtml.replace("{{brokerDetailTradeTable}}", traderSummaryTables[3]);

		reportHtml = reportHtml.replace("{{formattedDate}}", formattedDate).replace("{{emailSignature}}", Utilities.buildSignature(fullname));
		try (PrintWriter out = new PrintWriter(fileSave)) {
			out.println(reportHtml);			
		} 
		catch (FileNotFoundException e) {
		}	
	}

	public static void saveBlockEmailHtml(String fullname, Collection<BlockBroker> blockbroker, Collection<BlockSecurity> blocksecurity) {
		
		ArrayList<BlockBroker> blockbrokerList = new ArrayList<BlockBroker>(blockbroker);
		ArrayList<BlockSecurity> blocksecurityList = new ArrayList<BlockSecurity>(blocksecurity);
		
		String fileSave = "BlockReportEmail.html"; 
		String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
		String reportHtml = Utilities.getHTMLString("BlockReportTemplate.html");
		String rowTemplate = Utilities.getHTMLString("blockTableRow.html");
		String rowTemplate1 = Utilities.getHTMLString("blockTableRow1.html");
		
		String rowshtml = "";
		// sort by value for Broker
		Collections.sort(blockbrokerList, new Comparator<BlockBroker>() {
			@Override
			public int compare(BlockBroker block1, BlockBroker block2)
			{
				return (block2.notional - block1.notional) > 0 ? 1 : ((block2.notional - block1.notional) < 0 ? -1 : 0);
			}
		});
		Integer count = 1;
		for (BlockBroker b: blockbrokerList) {
			if (count > 10) { break; } 
			rowshtml += rowTemplate.replace("{{rank}}", count.toString()).replace("{{name}}", b.name).replace("{{numtrades}}", b.blockCount.toString()).replace("{{volume}}", NumberFormat.getNumberInstance(Locale.US).format(b.shares)).replace("{{value}}", "$"+String.format("%1$,.0f", b.notional));
			count++;
		}
		reportHtml = reportHtml.replace("{{brokerValue}}", rowshtml);
		
		rowshtml = "";
		// sort by volume for Broker
		Collections.sort(blockbrokerList, new Comparator<BlockBroker>() {
			@Override
			public int compare(BlockBroker block1, BlockBroker block2)
			{
				return (block2.shares - block1.shares) > 0 ? 1 : ((block2.shares - block1.shares) < 0 ? -1 : 0);
			}
		});		
		count = 1;
		for (BlockBroker b: blockbrokerList) {
			if (count > 10) { break; } 
			rowshtml += rowTemplate.replace("{{rank}}", count.toString()).replace("{{name}}", b.name).replace("{{numtrades}}", b.blockCount.toString()).replace("{{volume}}", NumberFormat.getNumberInstance(Locale.US).format(b.shares)).replace("{{value}}", "$"+String.format("%1$,.0f", b.notional));
			count++;
		}
		reportHtml = reportHtml.replace("{{brokerVolume}}", rowshtml);		
		
		rowshtml = "";
		// sort by value for Ticker
		Collections.sort(blocksecurityList, new Comparator<BlockSecurity>() {
			@Override
			public int compare(BlockSecurity block1, BlockSecurity block2)
			{
				return (block2.notional - block1.notional) > 0 ? 1 : ((block2.notional - block1.notional) < 0 ? -1 : 0);
			}
		});				
		count = 1;
		for (BlockSecurity b: blocksecurityList) {
			if (count > 10) { break; } 
			rowshtml += rowTemplate1.replace("{{rank}}", count.toString()).replace("{{name}}", b.ticker).replace("{{numtrades}}", b.blockCount.toString()).replace("{{scotiaRank}}", b.scotiaRankbyValue).replace("{{volume}}", NumberFormat.getNumberInstance(Locale.US).format(b.volume)).replace("{{value}}", "$"+String.format("%1$,.0f", b.notional));
			count++;
		}
		reportHtml = reportHtml.replace("{{tickerValue}}", rowshtml);			
		
		rowshtml = "";
		// sort by volume for Ticker
		Collections.sort(blocksecurityList, new Comparator<BlockSecurity>() {
			@Override
			public int compare(BlockSecurity block1, BlockSecurity block2)
			{
				return (block2.volume - block1.volume) > 0 ? 1 : ((block2.volume - block1.volume) < 0 ? -1 : 0);
			}
		});				
		count = 1;
		for (BlockSecurity b: blocksecurityList) {
			if (count > 10) { break; } 
			rowshtml += rowTemplate1.replace("{{rank}}", count.toString()).replace("{{name}}", b.ticker).replace("{{numtrades}}", b.blockCount.toString()).replace("{{scotiaRank}}", b.scotiaRankbyVolume).replace("{{volume}}", NumberFormat.getNumberInstance(Locale.US).format(b.volume)).replace("{{value}}", "$"+String.format("%1$,.0f", b.notional));
			count++;
		}
		reportHtml = reportHtml.replace("{{tickerVolume}}", rowshtml).replace("{{formattedDate}}", formattedDate).replace("{{emailSignature}}", Utilities.buildSignature(fullname));					
		
		
		try (PrintWriter out = new PrintWriter(fileSave)) {
			out.println(reportHtml);
//			System.out.print("Template Printed to: " + fileSave + "\n");
			//logger.info("Template Printed to: " + fileSave + "\n");
			
		} 
		catch (FileNotFoundException e) {
			//System.err.println("Error writing html to file: " + e.getMessage() + "\n");
			//logger.info("Error writing html to file: " + e.getMessage() + "\n");
		}				
		
		
	}
	
	public static void createR305Excel(Collection<BlockBroker> blockbroker, Collection<BlockSecurity> blocksecurity, HashMap<String, ArrayList<BlockDetails>> blockmap) {
		
	}

	public static void createBlockReportExcel(Collection<BlockBroker> blockbroker, Collection<BlockSecurity> blocksecurity, HashMap<String, ArrayList<BlockDetails>> blockmap) {
        
		ArrayList<BlockBroker> blockbrokerList = new ArrayList<BlockBroker>(blockbroker);
		ArrayList<BlockSecurity> blocksecurityList = new ArrayList<BlockSecurity>(blocksecurity);
		
		int tablenum = 0;
		
		
		//1. Create an Excel file
		String filelocation = "BlockTradeReport.xls";
        WritableWorkbook myFirstWbook = null;
        try {

            myFirstWbook = Workbook.createWorkbook(new File(filelocation));

            // create an Excel sheet
            WritableSheet excelSheet = myFirstWbook.createSheet("Blocktrades", 0);
            excelSheet.setColumnView(0, 10);
            excelSheet.setColumnView(1, 20);
            excelSheet.setColumnView(2, 10);
            excelSheet.setColumnView(3, 12);
            excelSheet.setColumnView(4, 15);
            excelSheet.setColumnView(5, 20);
            excelSheet.setColumnView(6, 8);

            WritableCellFormat TitleFormat = new WritableCellFormat();
            WritableFont font = new WritableFont(WritableFont.ARIAL, 8, WritableFont.BOLD, true);
            TitleFormat.setFont(font);
            
            WritableCellFormat HeaderFormat = new WritableCellFormat();
            HeaderFormat.setAlignment(Alignment.CENTRE);
            HeaderFormat.setBackground(Colour.GRAY_25);
            HeaderFormat.setWrap(true);
            font = new WritableFont(WritableFont.ARIAL, 8, WritableFont.BOLD, false);
            HeaderFormat.setFont(font);
            
            WritableCellFormat SummaryRowFormat = new WritableCellFormat();
            SummaryRowFormat.setAlignment(Alignment.CENTRE);
            SummaryRowFormat.setWrap(true);
            SummaryRowFormat.setBorder(Border.TOP, BorderLineStyle.MEDIUM, Colour.BLACK);
            font = new WritableFont(WritableFont.ARIAL, 8, WritableFont.BOLD, false);
            SummaryRowFormat.setFont(font);            
            
            WritableCellFormat CellFormat = new WritableCellFormat();
            CellFormat.setAlignment(Alignment.CENTRE);
            CellFormat.setWrap(true);
            //CellFormat.setBackground(Colour.GRAY_40_PERCENT);
            font = new WritableFont(WritableFont.ARIAL, 8, WritableFont.NO_BOLD, false);
            CellFormat.setFont(font);           
            
            
            
            
            
            //TABLE 1
            Label label = new Label(0, 0+tablenum*14, "Top 10 Brokers by Value", TitleFormat);
            excelSheet.addCell(label);

            label = new Label(0, 1+tablenum*14, "Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(1, 1+tablenum*14, "Broker Name", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(2, 1+tablenum*14, "#Trades", HeaderFormat);
            excelSheet.addCell(label);            
            label = new Label(3, 1+tablenum*14, "Volume", HeaderFormat);
            excelSheet.addCell(label);            
            label = new Label(4, 1+tablenum*14, "Value", HeaderFormat);
            excelSheet.addCell(label);    
            
            
    		// sort by value for Broker
    		Collections.sort(blockbrokerList, new Comparator<BlockBroker>() {
    			@Override
    			public int compare(BlockBroker block1, BlockBroker block2)
    			{
    				return (block2.notional - block1.notional) > 0 ? 1 : ((block2.notional - block1.notional) < 0 ? -1 : 0);
    			}
    		});
    		Integer count = 1;
    		for (BlockBroker b: blockbrokerList) {
    			if (count > 10) { break; } 
    			String rank = count.toString();
    			String name = getTruncatedName(b.name);
    			String numTrades = b.blockCount.toString();
    			String volume = NumberFormat.getNumberInstance(Locale.US).format(b.shares);
    			String value = "$"+String.format("%1$,.0f", b.notional);
    			
    			
                label = new Label(0, 2+tablenum*14+(count-1), rank, CellFormat);
                excelSheet.addCell(label);
                label = new Label(1, 2+tablenum*14+(count-1), name, CellFormat);
                excelSheet.addCell(label);
                label = new Label(2, 2+tablenum*14+(count-1), numTrades, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(3, 2+tablenum*14+(count-1), volume, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(4, 2+tablenum*14+(count-1), value, CellFormat);
                excelSheet.addCell(label);          			
                count++;
    		}		            
            
    		tablenum++; // increment # of tables already written, out of 4
        
            
            //TABLE 2
            label = new Label(0, 0+tablenum*14, "Top 10 Brokers by Volume", TitleFormat);
            excelSheet.addCell(label);

            label = new Label(0, 1+tablenum*14, "Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(1, 1+tablenum*14, "Broker Name", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(2, 1+tablenum*14, "#Trades", HeaderFormat);
            excelSheet.addCell(label);            
            label = new Label(3, 1+tablenum*14, "Volume", HeaderFormat);
            excelSheet.addCell(label);            
            label = new Label(4, 1+tablenum*14, "Value", HeaderFormat);
            excelSheet.addCell(label);    
            
            
    		// sort by volume for Broker
    		Collections.sort(blockbrokerList, new Comparator<BlockBroker>() {
    			@Override
    			public int compare(BlockBroker block1, BlockBroker block2)
    			{
    				return (block2.shares - block1.shares) > 0 ? 1 : ((block2.shares - block1.shares) < 0 ? -1 : 0);
    			}
    		});		
    		count = 1;
    		for (BlockBroker b: blockbrokerList) {
    			if (count > 10) { break; } 
    			String rank = count.toString();
    			String name = getTruncatedName(b.name);
    			String numTrades = b.blockCount.toString();
    			String volume = NumberFormat.getNumberInstance(Locale.US).format(b.shares);
    			String value = "$"+String.format("%1$,.0f", b.notional);
    			
    			
                label = new Label(0, 2+tablenum*14+(count-1), rank, CellFormat);
                excelSheet.addCell(label);
                label = new Label(1, 2+tablenum*14+(count-1), name, CellFormat);
                excelSheet.addCell(label);
                label = new Label(2, 2+tablenum*14+(count-1), numTrades, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(3, 2+tablenum*14+(count-1), volume, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(4, 2+tablenum*14+(count-1), value, CellFormat);
                excelSheet.addCell(label);          			
                count++;
    		}		            
            
    		tablenum++; // increment # of tables already written, out of 4    		
    		
    		
            //TABLE 3
            label = new Label(0, 0+tablenum*14, "Top 10 Tickers by Value", TitleFormat);
            excelSheet.addCell(label);

            label = new Label(0, 1+tablenum*14, "Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(1, 1+tablenum*14, "Ticker", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(2, 1+tablenum*14, "#Trades", HeaderFormat);
            excelSheet.addCell(label);      
            label = new Label(3, 1+tablenum*14, "Scotia Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(4, 1+tablenum*14, "Volume", HeaderFormat);
            excelSheet.addCell(label);    
            label = new Label(5, 1+tablenum*14, "Value", HeaderFormat);
            excelSheet.addCell(label);    
            
            
    		// sort by value for Ticker
    		Collections.sort(blocksecurityList, new Comparator<BlockSecurity>() {
    			@Override
    			public int compare(BlockSecurity block1, BlockSecurity block2)
    			{
    				return (block2.notional - block1.notional) > 0 ? 1 : ((block2.notional - block1.notional) < 0 ? -1 : 0);
    			}
    		});				
    		count = 1;
    		for (BlockSecurity b: blocksecurityList) {
    			if (count > 10) { break; } 
    			String rank = count.toString();
    			String name = b.ticker;
    			String numTrades = b.blockCount.toString();
    			String scotiaRankbyValue = b.scotiaRankbyValue;
    			String volume = NumberFormat.getNumberInstance(Locale.US).format(b.volume);
    			String value = "$"+String.format("%1$,.0f", b.notional);
    			
    			
                label = new Label(0, 2+tablenum*14+(count-1), rank, CellFormat);
                excelSheet.addCell(label);
                label = new Label(1, 2+tablenum*14+(count-1), name, CellFormat);
                excelSheet.addCell(label);
                label = new Label(2, 2+tablenum*14+(count-1), numTrades, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(3, 2+tablenum*14+(count-1), scotiaRankbyValue, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(4, 2+tablenum*14+(count-1), volume, CellFormat);
                excelSheet.addCell(label);
                label = new Label(5, 2+tablenum*14+(count-1), value, CellFormat);
                excelSheet.addCell(label);
                count++;
    		}		            
            
    		tablenum++; // increment # of tables already written, out of 4    		
    		    		
            
            //TABLE 4
            label = new Label(0, 0+tablenum*14, "Top 10 Tickers by Volume", TitleFormat);
            excelSheet.addCell(label);

            label = new Label(0, 1+tablenum*14, "Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(1, 1+tablenum*14, "Ticker", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(2, 1+tablenum*14, "#Trades", HeaderFormat);
            excelSheet.addCell(label);     
            label = new Label(3, 1+tablenum*14, "Scotia Rank", HeaderFormat);
            excelSheet.addCell(label);
            label = new Label(4, 1+tablenum*14, "Volume", HeaderFormat);
            excelSheet.addCell(label);            
            label = new Label(5, 1+tablenum*14, "Value", HeaderFormat);
            excelSheet.addCell(label);    
            
            // sort by volume ticker
    		Collections.sort(blocksecurityList, new Comparator<BlockSecurity>() {
    			@Override
    			public int compare(BlockSecurity block1, BlockSecurity block2)
    			{
    				return (block2.volume - block1.volume) > 0 ? 1 : ((block2.volume - block1.volume) < 0 ? -1 : 0);
    			}
    		});				
    		count = 1;
    		for (BlockSecurity b: blocksecurityList) {
    			if (count > 10) { break; } 
    			String rank = count.toString();
    			String name = b.ticker;
    			String numTrades = b.blockCount.toString();
    			String scotiaRankbyVolume = b.scotiaRankbyVolume;
    			String volume = NumberFormat.getNumberInstance(Locale.US).format(b.volume);
    			String value = "$"+String.format("%1$,.0f", b.notional);
    			
    			
                label = new Label(0, 2+tablenum*14+(count-1), rank, CellFormat);
                excelSheet.addCell(label);
                label = new Label(1, 2+tablenum*14+(count-1), name, CellFormat);
                excelSheet.addCell(label);
                label = new Label(2, 2+tablenum*14+(count-1), numTrades, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(3, 2+tablenum*14+(count-1), scotiaRankbyVolume, CellFormat);
                excelSheet.addCell(label);            
                label = new Label(4, 2+tablenum*14+(count-1), volume, CellFormat);
                excelSheet.addCell(label);     
                label = new Label(5, 2+tablenum*14+(count-1), value, CellFormat);
                excelSheet.addCell(label);
                count++;
    		}		              
    		
    		int rownumber = 2+tablenum*14+(count);
    		
            label = new Label(0, rownumber, "By Broker", TitleFormat);
            excelSheet.addCell(label);    		
            rownumber++;
    		
            Object[] keys = blockmap.keySet().toArray();
            ArrayList<Integer> BrokerIDs = new ArrayList<Integer>();
            for (Object key : keys) {
            	BrokerIDs.add(Integer.parseInt((String) key)); 
            }
            Collections.sort(BrokerIDs);
           
            
            for (Integer key : BrokerIDs) {
                label = new Label(0, rownumber, "Time", HeaderFormat);
                excelSheet.addCell(label);
                label = new Label(1, rownumber, "Ticker", HeaderFormat);
                excelSheet.addCell(label);
                label = new Label(2, rownumber, "Volume", HeaderFormat);
                excelSheet.addCell(label);            
                label = new Label(3, rownumber, "Price", HeaderFormat);
                excelSheet.addCell(label);            
                label = new Label(4, rownumber, "Value", HeaderFormat);
                excelSheet.addCell(label);     
                label = new Label(5, rownumber, "Broker Name", HeaderFormat);
                excelSheet.addCell(label);                  
                label = new Label(6, rownumber, "Broker ID", HeaderFormat);
                excelSheet.addCell(label);                  
                
                rownumber++;
                
                ArrayList<BlockDetails> blockList = blockmap.get(key.toString());
                Integer cumVol = 0;
                Double cumVal = 0.;
            	
                
                for (BlockDetails block: blockList) {
	
                	double price = block.tradePrice.doubleValue();
                	String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date (block.tradeTime));
                    label = new Label(0, rownumber, time, CellFormat);
                    excelSheet.addCell(label);
                    label = new Label(1, rownumber, block.ticker, CellFormat);
                    excelSheet.addCell(label);
                    label = new Label(2, rownumber, NumberFormat.getNumberInstance(Locale.US).format(block.tradeSize), CellFormat);
                    excelSheet.addCell(label);            
                    label = new Label(3, rownumber, "$"+String.format("%.2f", price), CellFormat);
                    excelSheet.addCell(label);            
                    label = new Label(4, rownumber, "$"+String.format("%1$,.0f", block.notional), CellFormat);
                    excelSheet.addCell(label);        
                    label = new Label(5, rownumber, getTruncatedName(block.brokerName), CellFormat);
                    excelSheet.addCell(label);                            
                    label = new Label(6, rownumber, key.toString(), CellFormat);
                    excelSheet.addCell(label);                            

                    rownumber++;                	
                		
                	cumVol += block.tradeSize;
                	cumVal += block.notional;
                }
                label = new Label(0, rownumber, "", SummaryRowFormat);
                excelSheet.addCell(label);
                label = new Label(1, rownumber, "", SummaryRowFormat);
                excelSheet.addCell(label);
                label = new Label(2, rownumber, NumberFormat.getNumberInstance(Locale.US).format(cumVol), SummaryRowFormat);
                excelSheet.addCell(label);            
                label = new Label(3, rownumber, "", SummaryRowFormat);
                excelSheet.addCell(label);            
                label = new Label(4, rownumber, "$"+String.format("%1$,.0f", cumVal), SummaryRowFormat);
                excelSheet.addCell(label);        
                label = new Label(5, rownumber, "", SummaryRowFormat);
                excelSheet.addCell(label);                            
                label = new Label(6, rownumber, "", SummaryRowFormat);
                excelSheet.addCell(label);                 
                
                rownumber++; 
                rownumber++; 
                
            
                
            }
            
    		// write the excel file
            myFirstWbook.write();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        } finally {

            if (myFirstWbook != null) {
                try {
                    myFirstWbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (WriteException e) {
                    e.printStackTrace();
                }
            }
		
		
        }
	}	
	

	// returns a hashmap with broker id is key, value is list of blocks associated with the broker
	public static HashMap<String, ArrayList<BlockDetails>> sortbyBrokerTimeTicker(ArrayList<BlockDetails> blocks) {
		
		HashMap<String, ArrayList<BlockDetails>> blockbyTimeBroker = new HashMap<String, ArrayList<BlockDetails>>();
		
		for (BlockDetails block: blocks) {
			String brokerID = block.brokerID.replaceAll("^0+", "");
			if (blockbyTimeBroker.containsKey(brokerID)) {
				ArrayList<BlockDetails> blockList = blockbyTimeBroker.get(brokerID);
				blockList.add(block);
			}
			else {
				ArrayList<BlockDetails> blockList = new ArrayList<BlockDetails>();
				blockList.add(block);
				blockbyTimeBroker.put(brokerID, blockList);
			}
		}
		
		for (String key : blockbyTimeBroker.keySet()) {
			ArrayList<BlockDetails> blockList = blockbyTimeBroker.get(key);
    		Collections.sort(blockList, new Comparator<BlockDetails>() {
    			@Override
    			public int compare(BlockDetails block2, BlockDetails block1)
    			{
    				return (block2.tradeTime - block1.tradeTime) > 0 ? 1 : ((block2.tradeTime - block1.tradeTime) < 0 ? -1 : 0);
    			}
    		});		
    		Collections.sort(blockList, new Comparator<BlockDetails>() {
    			@Override
    			public int compare(BlockDetails block2, BlockDetails block1)
    			{
    				return block2.ticker.compareTo(block1.ticker);
    			}
    		});
		}
		
		return blockbyTimeBroker;
	}
	
	
}
