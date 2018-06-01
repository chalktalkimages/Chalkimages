var querystring = require('querystring');
var https = require('http');

var host = 't65-w7-eqcash';
var port = '8001';

function performRequest(endpoint, method, data, success) {
  var dataString = JSON.stringify(data);
  var headers = {};
  
  if (method == 'GET') {
    endpoint += '?' + querystring.stringify(data);
  }
  else {
    headers = {
      'Content-Type': 'application/json',
      //'Content-Length': dataString.length
    };
  }
  var options = {
    host: host,
    port: port,
    path: endpoint,
    method: method,
    headers: headers
  };

  var req = https.request(options, function(res) {

    if (port != '9002') {
      res.setEncoding('utf8');

      var responseString = '';

      res.on('data', function(data) {
        responseString += data;
      });

      res.on('end', function() {
        //console.log(responseString);
        var responseObject = JSON.parse(responseString);
        success(responseObject);
      });
    }
  });

  req.write(dataString);
  req.end();
}

// query Object for order tracker block search
var queryObject = {brokers: [], ticker: ''};

var lastYear = new Date();
lastYear.setHours(0,0,0,0);
lastYear.setFullYear(lastYear.getFullYear()-1);
lastYear.setMonth(0);
lastYear.setDate(1);

queryObject.startdate = lastYear;
queryObject.enddate = '';
queryObject.filters = [0, 1, 9, 11, 12, 13, 14, 17, 18 ];
queryObject.aggregate = false;
queryObject.user = 'bliu';


function atBlockList() {
  // get data from SOT
  queryObject.liabilityTraders = [];
  port = '8001';
  
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
	var list = responseData.blocks;
	var blockList = [];
	for (var i = 0; i < list.length; i++) {
	  blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate, trader: '', analyst: list[i].analyst});
	}
	queryObject.liabilityTraders = ["Ian.Reston@SCIT.CA"];
	performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
		list = responseData.blocks;
		for (var i = 0; i < list.length; i++) {
			blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate, trader: 'Ian Reston', analyst: ''});
		}
		queryObject.liabilityTraders = ["Colin.Thacker@SCIT.CA"];
		performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
			list = responseData.blocks;
			for (var i = 0; i < list.length; i++) {
				blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate, trader: 'Colin Thacker', analyst: ''});
			}
			queryObject.liabilityTraders = ["Paul.OHea@SCIT.CA"];
			performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
				list = responseData.blocks;
				for (var i = 0; i < list.length; i++) {
					blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate, trader: 'Paul OHea', analyst: ''});
				}
				queryObject.liabilityTraders = ["Scott.Wigle@SCIT.CA"];
				performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
					list = responseData.blocks;
					for (var i = 0; i < list.length; i++) {
						blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate, trader: 'Scott Wigle', analyst: ''});
					}
					//host = 't68-w7-bliu';
					console.log('Sending (R305) analyst trader block report and email request to chalk ...\n')
					port = '9002';
					// send chalk server request
					performRequest('/send-trader-analyst-BlockReport', 'POST', {fullname: 'R305 Analyst Trader Report', blocks: blockList, isFlow: false}, function(responseData) {
					});
				});
			});
		});
	});
	
	
  });  

}

atBlockList();






