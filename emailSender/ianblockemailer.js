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

var yesterday = new Date();
yesterday.setHours(0,0,0,0);
if (yesterday.getDay() == 1) // Monday
    yesterday.setDate(yesterday.getDate() - 3);
else
    yesterday.setDate(yesterday.getDate() - 1);

queryObject.startdate = yesterday;
queryObject.enddate = yesterday;
queryObject.filters = [0, 1, 8, 9, 11, 12, 13, 14, 17, 18 ];

queryObject.aggregate = false;
queryObject.user = 'bliu';


function aggregateBlockList() {
  // Aggregate block list
  // get data from OT
  queryObject.liabilityTraders = [];
  port = '8001';
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
      var list = responseData.blocks;
      var blockList = [];
      for (var i = 0; i < list.length; i++) {
          blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate});
          
     }
      console.log('Sending create aggregate block report and email request to chalk ...\n')
      port = '9002';
      // send chalk server request
      performRequest('/send-morning-BlockReport', 'POST', {fullname: 'AllBlockReportEmail', blocks: blockList, isFlow: false}, function(responseData) {
      });
  });  

}


function IanBlockList() {
    // Ian block list
  // get data from OT
  queryObject.liabilityTraders = ['Ian.Reston@SCIT.CA'];
  port = '8001';
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
      var list = responseData.blocks;
      var blockList = [];
      for (var i = 0; i < list.length; i++) {
          blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate});
          
     }
      console.log('Sending create Ian\'s block report and email request to chalk ...\n')
      port = '9002';
      // send chalk server request
      performRequest('/send-morning-BlockReport', 'POST', {fullname: 'IanBlockReportEmail', blocks: blockList, isFlow: false}, function(responseData) {
      });
  });
}

function PaulBlockList() {
    // Paul block list
  // get data from OT
  queryObject.liabilityTraders = ['Paul.OHea@SCIT.CA'];
  port = '8001';
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
      var list = responseData.blocks;
      var blockList = [];
      for (var i = 0; i < list.length; i++) {
          blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate});
          
     }
      console.log('Sending create Paul\'s block report and email request to chalk ...\n')
      port = '9002';
      // send chalk server request
      performRequest('/send-morning-BlockReport', 'POST', {fullname: 'PaulBlockReportEmail', blocks: blockList, isFlow: false}, function(responseData) {
      });
  });
}

function ScottBlockList() {
    // Scott block list
  // get data from OT
  queryObject.liabilityTraders = ['Scott.Wigle@SCIT.CA'];
  port = '8001';
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
      var list = responseData.blocks;
      var blockList = [];
      for (var i = 0; i < list.length; i++) {
          blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate});
          
     }
      console.log('Sending create Scott\'s block report and email request to chalk ...\n')
      port = '9002';
      // send chalk server request
      performRequest('/send-morning-BlockReport', 'POST', {fullname: 'ScottBlockReportEmail', blocks: blockList, isFlow: false}, function(responseData) {
      });
  });
}


function ColinBlockList() {
  // Colin block list
  // get data from OT
  queryObject.liabilityTraders = ['Colin.Thacker@SCIT.CA'];
  port = '8001';
  performRequest('/search-block-orders', 'POST', queryObject, function (responseData) {
      var list = responseData.blocks;
      var blockList = [];
      for (var i = 0; i < list.length; i++) {
          blockList.push({brokerName: list[i].broker, notional: list[i].value, tradeSize: list[i].volume, ticker: list[i].security, brokerID: String(list[i].brokerid), tradePrice: parseFloat(Math.round(list[i].avgpx * 100) / 100).toFixed(2), tradeTime: list[i].startdate});
          
     }
      console.log('Sending create Colin\'s block report and email request to chalk ...\n')
      port = '9002';
      // send chalk server request
      performRequest('/send-morning-BlockReport', 'POST', {fullname: 'ColinBlockReportEmail', blocks: blockList, isFlow: false}, function(responseData) {
      });
  });  
}



IanBlockList();







