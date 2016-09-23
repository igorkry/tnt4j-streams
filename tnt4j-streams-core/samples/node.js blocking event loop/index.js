console.log('hello from Node.js');
JsTimedProcess = require('./app/jsTimedProcess.js')(10);
var Tnt4jStreamsFormatter = require('./njsTraceFormatter-tnt4j.js').Tnt4jStreamsFormatter;							// ################# Tnt4JFormatter
var njstrace = require('./njstrace/njsTrace.js').inject({	formatter: new Tnt4jStreamsFormatter("true"), inspectArgs: false, files: ['**/*.js', '*.js', '!**/node_modules/**'],});

const calc = require('./app/fibionacci.js');

//const calc = require('./app/fibionacci_injected.js');


console.log(calc.fibonacci(15));
