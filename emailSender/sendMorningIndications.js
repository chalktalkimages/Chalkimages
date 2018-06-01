var querystring = require('querystring');
var https = require('http');

var host = 't65-w7-eqcash';
var port = '8001';

function performRequest(endpoint, method, data, success) {
    var dataString = JSON.stringify(data);
    var headers = {};

    if (method == 'GET') {
        endpoint += '?' + querystring.stringify(data);
    } else {
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

function sendEmail() {
    console.log('Sending morning indications email request...\n')
    performRequest('/ui-event', 'POST',  { buildIOIEmails: '' }, function(responseData) {});
}

sendEmail();