// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';
  
  var states = require('states');
  var modules = require('modules');
  var plugins = require('plugins');
  var prefixer = require('scripts/plugin-url-prefixer');

  require('scripts/rms-dsl-service.js');

  modules.get('alien4cloud-rms-scheduler-plugin').controller('RMSSchedulerController', ['$scope', 'rmsDslService',
    function($scope, rmsDslService) {

      var aceEditor;
      $scope.saving = false;
      $scope.errorsExist = false;
      $scope.aceLoaded = function(_editor){
        aceEditor = _editor;
        _editor.commands.addCommand({
          name: 'save',
          bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
          exec: function() {
            $scope.saveDsl();
          }
        });
      };

      rmsDslService.get({}, function(success) {
        var content = success.data;
        $scope.editorContent = {old:content, new:content};
      });

      $scope.saveDsl = function() {
        var content = aceEditor.getSession().getDocument().getValue();
        $scope.saving = true;
        rmsDslService.save({}, content, function(response) {
          $scope.saving = false;
          aceEditor.getSession().setAnnotations([]);
          if (response.error !== null) {
            var annotations = getAnnotations(response.data);
            aceEditor.getSession().setAnnotations(annotations);
            $scope.errorsExist = true;
          } else {
            $scope.editorContent.old = $scope.editorContent.new;
            $scope.errorsExist = false;
          }
        });
      }

      function getAnnotations(messages){
        var annotations = [];
        _.each(messages, function(message){
          annotations.push({
            row: message.line - 1,
            column: message.column,
            html: message.text,
            type: 'error'
          });
        });
        return annotations;
      }
    }
  ]);

  var templateUrl = prefixer.prefix('views/rms_dsl_editor.html');
  // register plugin state

  states.state('admin.rmsscheduler', {
    url: '/rmsscheduler',
    templateUrl: templateUrl ,
    controller: 'RMSSchedulerController',
    menu: {
      id: 'am.admin.rmsscheduler',
      state: 'admin.rmsscheduler',
      key: 'RMS_SCHEDULER.MENU',
      icon: 'fa fa-clock-o',
      priority: 9900,
      roles: ['ADMIN']
    }
  });

  plugins.registerTranslations(prefixer.prefix('data/languages/rmsscheduler-'));


});
