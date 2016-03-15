# Introduction 

Single page application using AngularJS tracking and analytics offers great challenge for developers. JKoolCloud offers
Angulartics plugin to deal this with easy.

# Installation

### npm

```shell
npm install tnt4j-angulatics-plugin --save
```

### manual

Put 'angulartics-tnt4j.js' under your script diretory

Then add angulartics-tnt4j.js to your page.

```html
    <script src="js/angulartics-tnt4j.js"></script>
```

# Simple use tutorial

This simple use tutorial based on Google's phonecat tutorial, this one extends AngularJS tutorial by adding
Angulartics - analytics for AngularJS applications.

## Prerequisites

* NodeJS
* Bower

If you dont have these, you can simply fallow instructions on (https://docs.angularjs.org/tutorial#get-started)
	
## Step 1
	
Clone angular-phonecat tutorial app.
	
`git clone --depth=14 https://github.com/angular/angular-phonecat.git`

## Step 2

Install all dependencies.
	
* `npm install`
* `bower install`

## Step 3

Install angulartics

```
bower install angulartics
````

## Step 4

Put 'angulartics-tnt4j.js' under your script diretory './js'
	
## Step 5
	
'cd angular-phonecat\app\' and edit `index.html` to add `angulartics-tnt4j` and `angulartics` scripts.
	
```html
    <script src="js/angulartics-tnt4j.js"></script>
    <script src="bower_components/angulartics/dist/angulartics.min.js"></script>
```
	
## Step 6

`cd js` and edit `app.js` to add `angulartics-tnt4j` and `angulartics` dependencies to existing app.

	
## Step 7

Run TNT4J-streams application with configuration provided in this folder. Ensure your JKoolCloud tokens configured in
`tnt4j.properties`
	
## Step 8
	
Allow CORS access:
* Edit `package.json`

```json
  "start": "http-server -a 0.0.0.0 -p 8000 --cors",
```

* Allow CORS in browser:

    I.E. in Chrome add plugin (https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi)
	
## Step 9
	
Run included web-server 'npm start' and access `http://localhost:8000/app/`
	
Now all angular Routed pages are tracked.
	
	
## Step 10

Assume you want to add one's every image click:
Edit phone-detail.html
	
and add `analytics-on="click" analytics-event="Image View"` to `img`
	
```html
    <ul class="phone-thumbs">
        <li ng-repeat="img in phone.images">
            <img ng-src="{{img}}" ng-click="setImage(img)" analytics-on="click" analytics-event="Image View">
        </li>
    </ul>
```
