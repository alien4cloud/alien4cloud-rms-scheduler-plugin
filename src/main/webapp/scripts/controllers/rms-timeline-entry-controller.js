// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var modules = require('modules');
  // We don't load lodash even if compiler claims it (less js to load)
  //var _ = require('lodash');
  require('scripts/services/rms-timeline-service');

  modules.get('alien4cloud-rms-scheduler-plugin').controller('rmsTimelineEntryController', ['$scope', 'toaster', '$state', '$window', '$translate', 'rmsTimelineService', 'applicationServices',
    function ($scope, toaster, $state, $window, $translate, rmsTimelineService, applicationServices) {

      applicationServices.getActiveDeployment.get({
        applicationId: $scope.application.id,
        applicationEnvironmentId: $scope.environment.id
      }, undefined, function (success) {
        if (_.defined(success.data)) {
          const deploymentId = success.data.id;
          //console.log('activeDeploymentId : ' + $scope.activeDeploymentId);
          rmsTimelineService.getSession({
            deploymentId: deploymentId
          }, function (result) {
            if (_.undefined(result.error)) {
              const hasSession = result.data;
              if (hasSession) {
                $state.go('applications.detail.environment.deploycurrent.rmstimeline', {
                  'deploymentId': deploymentId
                });
              } else {
                var titleError = $translate.instant('RMS_SCHEDULER.MONITOR.NoSession.Title');
                var bodyError = $translate.instant('RMS_SCHEDULER.MONITOR.NoSession.Content');
                toaster.pop('note', titleError, bodyError, 10000, 'trustedHtml',null);
                $window.history.back();
              }
            }
          });
        }
      });
    }
  ]);

});
