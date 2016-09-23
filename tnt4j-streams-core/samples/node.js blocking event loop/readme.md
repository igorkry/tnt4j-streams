# Introduction

This sample demonstrates how his Fibonacci number calculation written in node.js has abismal performance.

## Prerequisites

node.js needs to be installed in your system.

# Installing and running

To install:

1) This command get's all dependencies needed
```
npm install
```

2) To run provided sample run TNT4J-steams with provided parser configuration tnt-data-source.xml.
O simply start run.bat (run.sh in Linux)

3) run sample 
```
npm start
```

# Apply to other node.js software

You need to inject 3 objects: JsTimedProcess, Tnt4jStreamsFormatter and njsTrace.

JsTimedProcess comes with timeout option (in this case 10);
If you want to send only error traces to jKoolCloud use option "true" in Tnt4jStreamsFormatter;
njsTrace comes with filter witch files to instrument, and if function arguments should be inspected;


```
JsTimedProcess = require('./app/jsTimedProcess.js')(__10__);
var Tnt4jStreamsFormatter = require('./njsTraceFormatter-tnt4j.js').Tnt4jStreamsFormatter;	
var njstrace = require('./njstrace/njsTrace.js').inject({	formatter: new Tnt4jStreamsFormatter("__true__"), inspectArgs: false, files: [__'**/*.js', '*.js', '!**/node_modules/**'__],});
```

# Note

This sample uses enhanced njsTrace lib