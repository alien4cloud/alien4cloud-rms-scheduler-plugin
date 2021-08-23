// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var states = require('states');
  var plugins = require('plugins');
  var prefixer = require('scripts/plugin-url-prefixer');
  //var moment = require('moment');

  require('scripts/controllers/rms-dsl-controller');
  require('scripts/controllers/rms-timeline-controller');
  require('scripts/controllers/rms-timeline-entry-controller');

  states.state('admin.rmsscheduler', {
    url: '/rmsscheduler',
    templateUrl: prefixer.prefix('views/rms_dsl_editor.html'),
    controller: 'rmsDslController',
    menu: {
      id: 'am.admin.rmsscheduler',
      state: 'admin.rmsscheduler',
      key: 'RMS_SCHEDULER.DSL_EDITOR.MENU',
      icon: 'fa fa-clock-o',
      priority: 9900,
      roles: ['ADMIN']
    }
  });

  states.state('applications.detail.environment.deploycurrent.rmstimeline', {
    url: '/rmstimeline/:deploymentId',
    templateUrl: prefixer.prefix('views/rms_timeline_viewer.html'),
    controller: 'rmsTimelineController',
    state: 'applications.detail.environment.deploycurrent.rmstimeline'
  });

  states.state('applications.detail.environment.deploycurrent.rmstimelineentry', {
    url: '/rmstimelineentry',
    template: '',
    controller: 'rmsTimelineEntryController',
    menu: {
      id: 'applications.detail.environment.deploycurrent.rmstimelineentry',
      state: 'applications.detail.environment.deploycurrent.rmstimelineentry',
      key: 'RMS_SCHEDULER.MONITOR.MENU',
      icon: 'fa fa-clock-o',
      priority: 500
    }
  });

  plugins.registerTranslations(prefixer.prefix('data/languages/rmsscheduler-'));

});
