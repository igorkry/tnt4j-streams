# Why njsTrace with TNT4J streams

Beyond all advantages JKoolCloud offers, `njsTrace` has a nice ability to instrument and monitor your `node.js`
applications. `njsTrace` "hijacks" `node.js` `Module._compile()` method, and then calls to the original `Module._compile()`
with the instrumented code.

In this way you don't need to change your application code to have analytics in your application. 

# Installation

Install `tnt4j-njstrace-plugin` to your application with npm.

```cmd
npm install tnt4j-njstrace-plugin --save 
```

# Simple use tutorial

Put require in your code. As soon as the `inject` is called `njsTrace` start to work.
Since the instrumentation happens when the `Module._compile()` is called, only modules that are "required" after the
call to `njstrace.inject()` would get instrumented.

```js
var Tnt4jStreamsFormatter = require('../../../njsTraceFormatter-tnt4j.js').Tnt4jStreamsFormatter;
var njstrace = require('njstrace').inject({	formatter: new Tnt4jStreamsFormatter(), inspectArgs: false,
											files: ['**/*.js', '!**/node_modules/**',  '**/node_modules/http-server/**'],});
```

**NOTE:** You may want to deactivate argument inspection by `inspectArgs=false`, because you may start getting large amount
of data otherwise.

In this example instrumentation happens on http-server. 
Put everything you want to instrument in formatter argument;
For more information read the `njsTrace` [documentation](https://github.com/ValYouW/njsTrace).

