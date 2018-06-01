var querystring = require('querystring');
var https = require('http');

var host = 't65-w7-eqcash';
var port = '9001';

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


var blockquery = {
        tickers: [],
        types: ['NORMAL'],
        brokers: [],
        sectors: ['Materials'],
        subIndustries: null,
        starttime: null,
        endtime: null,
        sortByVolume: false,
        indices: []
    };


function aggregateBlockList() {
  // emails mid day block data from flow
  port = '9001';

      performRequest('/block-list', 'POST', blockquery, function(series) {
          var blocks = series; 
          blockList = [];
          for (var i = 0; i < blocks.length; i++) {
              blockList.push({brokerName: blocks[i].brokerName, notional: blocks[i].notional, tradeSize: blocks[i].tradeSize, ticker: blocks[i].ticker, brokerID: blocks[i].brokerID, tradePrice: parseFloat(Math.round(blocks[i].tradePrice * 100) / 100).toFixed(2), tradeTime: blocks[i].tradeTime});
          }
          console.log('Sending create midday mining block report and email request to chalk ...\n')
          port = '9002';
          performRequest('/send-morning-BlockReport', 'POST', {fullname: 'MiningBlockReportEmail', blocks: blockList, isFlow: true}, function() {
             
          });

      }); 

}

aggregateBlockList();







