# Introduction 

Single page application using AngularJS tracking and analytics offers great challenge for developers. JKoolCloud offers
Angulartics plugin to deal this with easy.

# Install

### npm

```shell
npm install angulartics-tnt4j --save
```

### Bower

To install Angulartics core module:
```shell
bower install angulartics-tnt4j  --save
```

### Manual 

Put 'angulartics-tnt4j.js' under your script diretory


#Simple use tutorial 

This simple use tutorial based on Google's phonecat tutorial, this one extends AngularJS tutorial by adding
Angulartics - analytics for AngularJS applications.

#Prerequisites

* NodeJS
* Bower

If you dont have these, you can simply fallow instructions on https://docs.angularjs.org/tutorial#get-started
	
## Step 1
	
Clone angular-phonecat tutorial app.
	
`git clone --depth=14 https://github.com/angular/angular-phonecat.git`

## Step 2

Install all dependencies.
	
* `npm install`
* `bower install`
	
## Step 3
	
'cd angular-phonecat\app\' and edit `index.html` to add `angulartics-tnt4j` and `angulartics` scripts.
	
```xml
    <script src="js/angulartics-tnt4j.js"></script>
	<script src="bower_components/angulartics/dist/angulartics.min.js"></script>
```
	
## Step 4

`cd js` and edit `app.js` to add `angulartics-tnt4j` and `angulartics` dependencies to existing app.

	
## Step 5

Run TNT4J-streams application with configuration provided in this folder. Ensure your JKollCloud tokens configured in
`tnt4j.properties`
	
## Step 6
	
Allow CORS access:
* Edit `package.json`

    "start": "http-server -a 0.0.0.0 -p 8000 --cors",

* Allow CORS in browser:

	I.E. in Chrome add plugin "https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi"
	
## Step 7
	
Run included web-server 'npm start' and access `http://localhost:8000/app/`
	
Now all angular Routed pagel are tracked
	
	
## Step 8

Assume you want to add one's every image click:
Edit phone-detail.html
	
and add  `analytics-on="click" analytics-event="Image View"` to `img`
	
```html
    <ul class="phone-thumbs">
        <li ng-repeat="img in phone.images">
            <img ng-src="{{img}}" ng-click="setImage(img)" analytics-on="click" analytics-event="Image View">
        </li>
    </ul>
```

	