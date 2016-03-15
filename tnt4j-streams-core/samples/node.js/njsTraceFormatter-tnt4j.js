var Formatter = require('njstrace/lib/formatter.js'),
    request = require('request');

function Tnt4jStreamsFormatter() {
}

require('util').inherits(Tnt4jStreamsFormatter, Formatter);


// Implement the onEntry method
Tnt4jStreamsFormatter.prototype.onEntry = function (args) {
    request({
        url: "http://localhost:9595",
        method: "POST",
        json: true,
        headers: {
            "content-type": "application/json",
        },
        body: args
    }, function (error, response, body) {
        if (error && response.statusCode !== 200) {
            console.log("error: " + error)
        }
    });
    // console.log('Got call TNT4J to %s@%s::%s, num of args: %s, stack location: %s',
    // args.name, args.file, args.line, args.args.length, args.stack.length);
};

// Implement the onExit method
Tnt4jStreamsFormatter.prototype.onExit = function (args) {
    request({
        url: "http://localhost:9595",
        method: "POST",
        json: true,
        headers: {
            "content-type": "application/json",
        },
        body: args
    }, function (error, response, body) {
        if (error && response.statusCode !== 200) {
            console.log("error: " + error)
        }
    });
    // console.log('TNT4J Exit from %s@%s::%s, had exception: %s, exit line: %s, execution time: %s, has return value: %s',
    // args.name, args.file, args.line, args.exception, args.retLine, args.span, args.returnValue !== null);
};

module.exports.Tnt4jStreamsFormatter = Tnt4jStreamsFormatter;