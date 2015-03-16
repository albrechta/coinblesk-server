'use strict';

/**
 * LoginController
 * @constructor
 */

function LoginController($scope, $location, userAccountFactory) {
	
	$scope.login = function (credentials) {
    	$scope.credentials = {
    		    username: '',
    		    password: ''
    	};
    	
    	if(credentials.username == '' || credentials.password == '') {
    		//do nothing
    	}else {    		
    		$scope.$emit('event:loginRequest', credentials);
    		$location.path('/home');
    	}
    };
};