## Intercepting AJAX calls 

Sometimes, especially when AJAX call fails on client side you wan't to know.
In order to have jKoolCloud logged such events one may need to intercept all AJAX calls

## Installation

Put `ajax-interceptor-tnt4j.js` to your script directory.

Then add tnt4j-ajax-interceptor.js to your page.

```xml
  <script src="js/ajax-interceptor-tnt4j.js"></script>
```

One would prefer manual installation, copy `ajax-interceptor-tnt4j.js` to your scripts folder and add it to
your page.


After installing this interceptor 

## How it works

'tnt4j-ajax-interceptor' intercepts all `XMLHttpRequest` invocations and calculates every ajax query invocations
time, as accepts errors or abort conditions. As soon as the data is collected it streams to TNT4J-Stream http input.



