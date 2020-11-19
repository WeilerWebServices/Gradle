(function () {
	var app = angular.module("org.gradle.profiler.listener", [
		"org.gradle.profile.listener.processor"
	]);

	app.controller("MainController", function ($scope, gradleEnterpriseServer) {
		$scope.listenUrl = null;
		$scope.withCredentials = true;
		$scope.builds = [];
		$scope.buildProcessor = gradleEnterpriseServer(function (build) {
			$scope.$apply(() => {
				$scope.builds.unshift(build);
			});
		});
		$scope.listen = function () {
			$scope.buildProcessor.start($scope.listenUrl, "now", $scope.withCredentials);
		};
		$scope.process = function () {
        	const urlRegex = /(https?:\/\/\S+)\/s\/(\w+)/g;
        	const text = $scope.buildsToProcess;
        	while (true) {
        		const match = urlRegex.exec(text);
        		if (match === null) {
        			break;
        		}
        		$scope.buildProcessor.processBuild({
        			ignoreTags: true,
        			gradleEnterpriseServerUrl: match[1],
        			buildId: match[2]
        		}, $scope.withCredentials);
        	}
		};
	});
})();
