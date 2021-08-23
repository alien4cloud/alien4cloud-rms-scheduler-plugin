/* global define */

'use strict';

define(function (require) {
  var modules = require('modules');

  modules.get('alien4cloud-rms-scheduler-plugin', ['ngResource']).factory('rmsTimelineService', ['$resource',
    function($resource) {

      var rules = $resource('/rest/rmsscheduler/timeline/rules/:deploymentId', {}, {
        'get': {
          method: 'GET'
        }
      });
      var events = $resource('/rest/rmsscheduler/timeline/events/:deploymentId', {}, {
        'get': {
          method: 'GET'
        },
        'byDate': {
          method: 'POST'
        }
      });
      var sessions = $resource('/rest/rmsscheduler/timeline/sessions/:deploymentId', {}, {
        'get': {
          method: 'GET'
        }
      });
      return {
        'getSession': sessions.get,
        'getRules': rules.get,
        'getEvents': events.get,
        'getEventsByDate': events.byDate,
      };
    }
  ]);
});
