var tntDebug = true;
var tntInterceptErrors = true;
var tntInterceptAbortState = true;
var tntInterceptAll = true;
var tnt4jStreamsHost = "http://localhost:9595";

XMLHttpRequest.prototype.reallySend = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function (body) {
    console.log("Intercepted ajax	");
    var evently = {
        startOfLoading: Date.now(),
        endOfLoading: "",
        elapsedTime: "",
        contentSize: "",
        statuss: "",
        url: "",
        error: false,
        abort: false
    };

    this.addEventListener("loadstart", function () {
        evently.startOfLoading = Date.now();
        if (tntDebug) console.log("Loading " + evently.startOfLoading);
    }, false);

    this.addEventListener("load", function () {
        evently.endOfLoading = Date.now();
        evently.elapsedTime = evently.endOfLoading - evently.startOfLoading;
        evently.contentSize = this.response.length;
        if (tntDebug) console.log("Loaded" + evently);
        if (tntInterceptAll) tntSend(this);
    }, false);

    this.addEventListener("error", function () {
        evently.error = true;
        if (tntDebug) console.log("Error " + evently);
        if (tntInterceptAll || tntInterceptErrors) tntSend(this);
    }, false);

    this.addEventListener("abort", function () {
        evently.abort = true;
        if (tntDebug) console.log("Aborted " + evently);
        if (tntInterceptAll || tntInterceptAbortState) tntSend(this);
    }, false);

    var tntSend = function (request) {
        evently.statuss = request.status;
        evently.url = request.responseURL;
        var tnt = new XMLHttpRequest;
        if (tntDebug) console.log("TNT event" + JSON.stringify(evently));
        tnt.open("POST", tnt4jStreamsHost, true);
        tnt.reallySend(JSON.stringify(evently));
    }

    this.reallySend(body);
};