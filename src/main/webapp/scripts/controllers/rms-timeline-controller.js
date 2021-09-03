// This is the ui entry point for the plugin

/* global define */

define(function (require) {
  'use strict';

  var modules = require('modules');
  var prefixer = require('scripts/plugin-url-prefixer');
  // We don't load lodash even if compiler claims it (less js to load)
  //var _ = require('lodash');
  //var moment = require('moment');
  var vis = require('vis');
  require('scripts/services/rms-timeline-service');

  modules.get('alien4cloud-rms-scheduler-plugin').controller('rmsTimelineController', ['$scope', 'breadcrumbsService', '$state', '$translate', 'rmsTimelineService',
    function ($scope, breadcrumbsService, $state, $translate, rmsTimelineService) {

      const deploymentId = $state.params.deploymentId;
      const body = angular.element(document).find('body');

      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploycurrent.rmstimeline',
        text: function(){
          return $translate.instant('RMS_SCHEDULER.MONITOR.MENU');
        },
        onClick: function(){
          $state.go('applications.detail.environment.deploycurrent.rmstimeline', {
            'deploymentId': deploymentId
          });
        }
      });

      // A unsuccesfull try to set local for timeline dates
      // var lang = $translate.use();
      // if (lang.length >= 2) {
      //   lang = $translate.use().substring(0, 2);
      // }
      // console.log('lang: ' + lang);
      // moment.locale(lang);

      // all the rules
      $scope.rules = [];
      // the current selected rule to display details
      $scope.currentRule = undefined;
      // true when we are currently polling data from backend
      $scope.polling = false;
      // true means poll periodically
      var autoRefresh = false;
      // inidicate if the poller is active or not
      // it can be active even if autoRefresh = false (when now is inside the current time range)
      $scope.pollerActive = false;
      // at startup, display all condition events
      $scope.displayPassedConditionsEvents = true;
      // such kind of events are only necessary for debug purpose or advanced users
      $scope.displayTriggerEvents = false;
      // for better visibility don't display condition labels by default
      $scope.displayEventInfo = false;
      // All available options for refresh rates
      $scope.refreshRates = [
        {'label': '1 s', 'value': 1000},
        {'label': '2 s', 'value': 2000},
        {'label': '5 s', 'value': 5000},
        {'label': '10 s', 'value': 10000},
        {'label': '60 s', 'value': 60000}
      ];
      // Dates for search form
      $scope.fromDate = undefined;
      $scope.toDate = undefined;

      // the reference to the poller 'job'
      var scheduledPoller;

      // initial and default 5 minutes ranges for the time window to monitor
      const defaultTimeRange = 5 * 60 * 1000;

      // a map to store rules by id (to get group id when receiving events)
      var ruleMap = {};

      // Configuration for the Timeline
      var options = {
        /*height: '300px',*/
        groupOrder: 'content',
        stackSubgroups: true,
        start: new Date(Date.now() - (defaultTimeRange * 2 / 3)),
        end: new Date(Date.now() + (defaultTimeRange / 3)),
        stack: false,
        showCurrentTime: true,
        editable: false,
        zoomMin: 1000 * 60,                   // one minute in milliseconds
        zoomMax: 1000 * 60 * 60 * 24 * 7,      // 7 days in milliseconds,
        /*rollingMode: {
          follow: true,
          offset: 0.5
        }*/
        tooltip: {
          followMouse: true
        }/*,
        locale: lang*/
      };
      // Create a Timeline
      var timelineContainer = document.getElementById('timeline');
      var timeline = new vis.Timeline(timelineContainer);
      // The dataset for timeline
      var items = new vis.DataSet({
        type: {start: 'Number', end: 'Number'}
      });
      timeline.setOptions(options);
      timeline.setItems(items);

      // FIXME: didn't find a better way to load CSS from plugin
      function loadCSS(filename) {
        var cssUrl = prefixer.prefix(filename);
        var file = document.createElement('link');
        file.setAttribute('rel', 'stylesheet');
        file.setAttribute('type', 'text/css');
        file.setAttribute('href', cssUrl);
        document.head.appendChild(file);
      }
      // FIXME: to avoid issues (ie. with fa) we unload the loaded CSSs specific to this plugin
      function unloadCSSs() {
        // search all link that contain our prefix and remove them
        var links = document.getElementsByTagName('link');
        const prefix = prefixer.prefix('');
        for (var i = 0; i < links.length; i++) {
          const link = links[i];
          if (link.attributes.href.value.indexOf(prefix) > -1) {
            link.parentNode.removeChild(link);
          }
        }
      }
      loadCSS('bower_components/vis/dist/vis.css');
      loadCSS('styles/main.css');

      //
      // -- private functions
      //

      // eventually poll events if not running
      // 'force' means order comes from end-user
      function schedulePoll(force) {
        if ($scope.polling) {
          return;
        }
        if (scheduledPoller) {
          clearTimeout(scheduledPoller);
        }
        var shoudlPoll = false;
        if (!autoRefresh) {
          // event if autoRefresh is false, if the current date is in the window we want to poll
          var timeWindow = timeline.getWindow();
          const now = Date.now();
          if (now > timeWindow.start && now < timeWindow.end) {
            shoudlPoll = true;
            $scope.pollerActive = true;
          } else {
            $scope.pollerActive = false;
          }
        }
        if (force || $scope.pollerActive) {
          doPoll();
        } else {
          // close the loop
          onPolled();
        }
      }

      function stopFollowNow() {
        var timeWindow = timeline.getWindow();
        const now = Date.now();
        var timeInterval = timeWindow.end - timeWindow.start;
        if (now < timeWindow.start || now > timeWindow.end - (timeInterval / 3)) {
          $scope.followTime = false;
          autoRefresh = false;
          $scope.pollerActive = false;
        }
        schedulePoll(true);
      }

      // Get events from backend using the timeline interval.
      function doPoll() {
        $scope.polling = true;
        //console.log('<<<<<<<<<< doPoll');
        body.addClass('timeline-waiting');   // set cursor to hourglass

        const timeWindow = timeline.getWindow();
        const from = timeWindow.start.getTime();
        const to = timeWindow.end.getTime();

        rmsTimelineService.getEventsByDate(
          {
            deploymentId: deploymentId
          },
          {
            fromDate: from,
            toDate: to,
            includeTriggerEvents: $scope.displayTriggerEvents
          },
          function (result) {
            if (_.undefined(result.error)) {
              //console.log(result.data.length + ' events');
              feedTimeLine(result.data);
            }
            //console.log('>>>>>>>>>>>> onPolled');
            onPolled();
          },
          function (error) {
            onPolled();
          }
        );

      }

      function onPolled() {
        $scope.polling = false;
        body.removeClass('timeline-waiting');
        scheduledPoller = setTimeout(function() {
          if ($scope.followTime) {
            startFollowNow();
          } else {
            schedulePoll();
            $scope.$digest();
          }
        }, $scope.refreshRate.value);
      }

      // Ensure the timeline follow the current date.
      function startFollowNow() {
        var timeWindow = timeline.getWindow();
        const now = Date.now();
        var timeInterval = timeWindow.end - timeWindow.start;
        if (now < timeWindow.start || now > timeWindow.end - (timeInterval / 3)) {
          // we want the past to feed 2/3 of the timeline width
          var end = new Date(Date.now() + (timeInterval / 3));
          var start = new Date(end - timeInterval);
          // finally set timeline window
          timeline.setWindow(start, end, schedulePoll);
        } else {
          schedulePoll();
        }
      }

      // Prepare the timeline by setting the groups (rules).
      function prepareTimeline(rules) {
        var groups = new vis.DataSet();
        // by provinding a subgroupOrder function we ensure subgroups are always ordered the same way (by id)
        var subgroupOrder = function (a, b) {
          return a.id - b.id;
        };

        for (var i = 0; i < rules.length; i++) {
          var rule = rules[i];
          // Subgroup 0 will be the time line (window time + actions)
          var subgroupStack = {0: false};
          // Subgroup 1+ for conditions
          for (var c = 0; c < rule.conditions.length; c++) {
            subgroupStack[c + 1] = false;
          }
          groups.add({id: i, content: rule.name, subgroupStack: subgroupStack, subgroupOrder: subgroupOrder});
          rule.idx = i;
          // we need a map id -> rule to get the rule idx when receiving events
          ruleMap[rule.id] = rule;
          $scope.rules.push(rule);
        }
        timeline.setGroups(groups);
        $scope.selectRule(0);
      }

      // pre-translated stuff for items titles
      const itemTitles = {
        'TimelineRuleConditionEvent.values.passed.true': $translate.instant('RMS_SCHEDULER.TimelineRuleConditionEvent.values.passed.true'),
        'TimelineRuleConditionEvent.values.passed.false': $translate.instant('RMS_SCHEDULER.TimelineRuleConditionEvent.values.passed.false'),
        'TimelineAction.title': $translate.instant('RMS_SCHEDULER.TimelineAction.title'),
        'TimelineEvent.title.SCHEDULED': $translate.instant('RMS_SCHEDULER.TimelineEvent.title.SCHEDULED')
      };

      // Feed the timeline while receiving events from polling.
      function feedTimeLine(events) {
        // all the ids we have received
        var eventSetIds = [];
        // all ids currently in the dataset
        var existingIds = items.getIds();
        events.forEach(function (event) {
          // define the groupId for the event
          const groupId = ruleMap[event.ruleId].idx;
          var item = {id: event.id, start: event.startTime, group: groupId, raw: event};

          var shouldAdd = true;


          if (event.type === 'TriggerEvent') {
            if ($scope.displayTriggerEvents) {
              item.subgroup = 0;
              item.type = 'point';
              item.title = event.status;
              item.content = '<i class="fa fa-flash"></i>';
              item.className = 'TriggerEvent TriggerEvent_' + event.status;
            } else {
              shouldAdd = false;
            }
          } else if (event.type === 'TimelineRuleConditionEvent') {
            // A smart icon for such events
            if (event.passed) {
              if (!$scope.displayPassedConditionsEvents) {
                shouldAdd = false;
              }
              item.content = '<i class="fa fa-check-circle"></i> ';
            } else {
              item.content = '<i class="fa fa-times-circle"></i> ';
            }
            if ($scope.displayEventInfo) {
              item.type = 'box';
              item.content += ruleMap[event.ruleId].conditions[event.conditionIdx];
              item.title = itemTitles['TimelineRuleConditionEvent.values.passed.' + event.passed];
            } else {
              item.type = 'point';
              item.title = ruleMap[event.ruleId].conditions[event.conditionIdx];
            }
            item.className = 'TimelineRuleConditionEvent TimelineRuleConditionEvent_' + event.passed;
            // for such kind of events, subgroupId is the conditionIxd + 1
            item.subgroup = event.conditionIdx + 1;
          } else {
            // time window and actions are in subgroup 0
            item.subgroup = 0;
            item.type = 'range';
            if (event.hasOwnProperty('endTime')) {
              item.end = event.endTime;
            } else {
              // unterminated stuffs will be drawn until now
              item.end = Date.now();
            }
            if (event.type === 'TimelineAction') {
              item.className = 'TimelineActionState TimelineActionState_' + event.state;
              item.title = event.name + itemTitles['TimelineAction.title'];
            } else if (event.type === 'TimelineWindow') {
              item.className = 'tl_window tl_window_' + event.obsolete;
            }
          }

          if (shouldAdd) {
            eventSetIds.push(event.id);
            // add or update item to the dataset, regarding it already exists or not
            if (_.indexOf(existingIds, item.id) === -1) {
              items.add(item);
            } else {
              items.update(item);
              if ($scope.pinnedTimelineAction && $scope.pinnedTimelineAction.id === item.id) {
                // Also update the info box describing actions
                $scope.pinnedTimelineAction = item.raw;
              }
            }
          }

        });
        // remove useless stuffs from the dataset
        var itemsToDelete = _.difference(existingIds, eventSetIds);
        items.remove(itemsToDelete);
      }

      //
      // -- timeline listeners
      //

      timeline.on('click', function (properties) {
        if (properties.hasOwnProperty('group')) {
          // when something is click and has a group, display the corresponding rule details
          $scope.currentRule = $scope.rules[properties.group];
          $scope.$digest();
        }
      });
      timeline.on('rangechanged', function (properties) {
        if (properties.byUser) {
          // when the user zooms or moves using mouse, we eventually stop to follow the line
          stopFollowNow();
        }
      });
      timeline.on('select', function (properties) {
        //console.log('select:' + JSON.stringify(properties));
        if (properties.items) {
          const item = items.get(properties.items[0]);
          if (item.raw.type === 'TimelineAction') {
            $scope.pinnedTimelineAction = item.raw;
            $scope.$digest();
          }
        }
      });

      //
      // -- scope functions
      //

      // Init the poller using the default settings
      $scope.initPoller = function() {
        $scope.followTime = true;
        //timeline.setOptions({showCurrentTime: true});
        autoRefresh = true;
        $scope.pollerActive = true;
        // Each 5s per default sounds good
        $scope.refreshRate = $scope.refreshRates[2];
        // Put 'now' at the 2/3 of the range
        var end = new Date(Date.now() + (defaultTimeRange / 3));
        var start = new Date(end - defaultTimeRange);
        timeline.setWindow(start, end, function() {
          schedulePoll();
        });
      };

      $scope.zoomIn = function () {
        timeline.zoomIn(0.2);
        stopFollowNow();
      };
      $scope.zoomOut = function () {
        timeline.zoomOut(0.2);
        stopFollowNow();
      };
      $scope.move = function (percentage) {
        var timeWindow = timeline.getWindow();
        var timeInterval = timeWindow.end - timeWindow.start;
        timeline.setWindow({
          start: timeWindow.start.valueOf() - timeInterval * percentage,
          end: timeWindow.end.valueOf() - timeInterval * percentage
        }, stopFollowNow);
      };
      $scope.datesChanged = function() {
        timeline.setWindow({
          start: $scope.fromDate.valueOf(),
          end: $scope.toDate.valueOf()
        }, stopFollowNow);
      };
      $scope.setFromDate = function(date) {
        $scope.fromDate = date;
        if (_.undefined($scope.toDate)) {
          $scope.toDate = date;
        }
        $scope.datesChanged();
      };
      $scope.setToDate = function(date) {
        $scope.toDate = date;
        if (_.undefined($scope.fromDate)) {
          $scope.fromDate = date;
        }
        $scope.datesChanged();
      };

      $scope.toggleFollowTime = function () {
        $scope.followTime = !$scope.followTime;
        if ($scope.followTime) {
          autoRefresh = true;
          $scope.pollerActive = true;
          startFollowNow();
        } else {
          autoRefresh = false;
          $scope.pollerActive = false;
          schedulePoll(true);
        }
      };
      $scope.refresh = function() {
        schedulePoll(true);
      };

      $scope.selectRule = function (selectedRuleIdx) {
        $scope.selectedRuleIdx = selectedRuleIdx;
        $scope.currentRule = $scope.rules[selectedRuleIdx];
      };

      // Redirect on display log state.
      $scope.displayLogs = function (executionId) {
        $state.go('applications.detail.environment.deploycurrent.logs', {
          'applicationId': $scope.application.id,
          'applicationEnvironmentId': $scope.environment.id,
          'executionId': executionId
        });
      };

      //
      // -- scopes listenner
      //

      $scope.$on('$destroy', function () {
        clearTimeout(scheduledPoller);
        unloadCSSs();
      });

      //
      // -- Now let's go !
      //

      // Get the rules associated to this deployment from backend
      rmsTimelineService.getRules({
        deploymentId: deploymentId
      }, function (result) {
        if (_.undefined(result.error)) {
          var rules = result.data;
          //console.log(rules.length + ' rules : ');
          if (rules.length > 0) {
            prepareTimeline(rules);
            $scope.initPoller();
          }
        }
      });

    }
  ]);

});
