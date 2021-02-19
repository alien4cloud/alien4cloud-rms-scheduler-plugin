/* global define */

'use strict';

define(function (require) {
  var modules = require('modules');

  modules.get('alien4cloud-rms-scheduler-plugin', ['ngResource']).factory('rmsDslService', ['$resource',
    function($resource) {
      return $resource('rest/rmsscheduler/dsl');
    }
  ]);
});
