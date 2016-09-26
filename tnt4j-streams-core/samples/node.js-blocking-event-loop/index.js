console.log('hello from Node.js');
JsTimedProcess = require('njstrace-timed-process')(10); // Timeout value
var Tnt4jStreamsFormatter = require('njstrace-tnt4j-formatter').Tnt4jStreamsFormatter;							// ################# Tnt4JFormatter
var njstrace = require('njstrace').inject({	formatter: new Tnt4jStreamsFormatter("true"), inspectArgs: false, files: ['**/*.js', '*.js', '!**/node_modules/**'],});

const calc = require('./app/fibionacci.js');


console.log(calc.fibonacci(15));
