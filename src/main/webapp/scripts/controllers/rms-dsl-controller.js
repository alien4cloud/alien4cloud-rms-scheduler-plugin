define(function (require) {
  'use strict';

  var modules = require('modules');
  // We don't load lodash even if compiler claims it (less js to load)
  //var _ = require('lodash');

  require('scripts/services/rms-dsl-service');

  modules.get('alien4cloud-rms-scheduler-plugin').controller('rmsDslController', ['$scope', 'rmsDslService',
    function ($scope, rmsDslService) {

      var aceEditor;
      $scope.saving = false;
      $scope.errorsExist = false;
      $scope.aceLoaded = function (_editor) {
        aceEditor = _editor;
        _editor.commands.addCommand({
          name: 'save',
          bindKey: {win: 'Ctrl-S', mac: 'Command-S'},
          exec: function () {
            $scope.saveDsl();
          }
        });
      };

      rmsDslService.get({}, function (success) {
        var content = success.data;
        $scope.editorContent = {old: content, new: content};
      });

      $scope.saveDsl = function () {
        var content = aceEditor.getSession().getDocument().getValue();
        $scope.saving = true;
        rmsDslService.save({}, content, function (response) {
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
      };

      function getAnnotations(messages) {
        var annotations = [];
        _.each(messages, function (message) {
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


});
