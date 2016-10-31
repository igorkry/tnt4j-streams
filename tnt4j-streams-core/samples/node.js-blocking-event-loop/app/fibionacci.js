/**
 * 
 */

function fibonacci(n) {
		if (n < 2) {
			return 1;
		  }
		  else {
			var returnVal = fibonacci(n-2) + fibonacci(n-1);
			return returnVal;	
		  }
}

module.exports = {
		fibonacci : fibonacci,
	}