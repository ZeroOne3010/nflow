(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflowDefinition', []);

  m.controller('WorkflowDefinitionCtrl', function WorkflowDefinitionCtrl($scope, $rootScope, $routeParams,
                                                                         WorkflowDefinitions, WorkflowDefinitionStats, WorkflowStatsPoller) {

    var self = this;
    self.hasStatistics = false;
    self.definition = undefined;
    self.graph = undefined; // TODO no need to expose in view model?
    self.selectedNode = undefined; // TODO no need to expose in view model?

    self.nodeSelected = nodeSelected;
    self.startRadiator = startRadiator;
    self.savePng = savePng;
    self.saveSvg = saveSvg;

    initialize();

    function initialize() {
      // TODO handle errors
      WorkflowDefinitions.get({type: $routeParams.type},
        function (data) {
          var start = new Date().getTime();
          var definition = _.first(data);
          self.definition = definition;
          self.graph = workflowDefinitionGraph(definition);
          drawWorkflowDefinition(self.graph, 'dagreSvg', nodeSelectedCallBack, $rootScope.graph.css);
          updateStateExecutionGraph($routeParams.type);
          console.debug('Rendering dagre graph took ' +
          (new Date().getTime() - start) + ' msec');
        });

      // poller polls stats with fixed period
      WorkflowStatsPoller.start($routeParams.type);

      // poller broadcasts when events change
      $scope.$on('workflowStatsUpdated', function (scope, type) {
        if (type !== $routeParams.type) {
          return;
        }
        updateStateExecutionGraph(type);

      });
    }

    /** called when node is clicked */
    function nodeSelected(nodeId) {
      console.debug('Selecting node ' + nodeId);
      if (self.selectedNode) {
        unhiglightNode(self.graph, self.definition, self.selectedNode);
      }
      if (nodeId) {
        higlightNode(self.graph, self.definition, nodeId);
      }
      self.selectedNode = nodeId;
    }

    function startRadiator() {
      $rootScope.$broadcast('startRadiator');
    }

    // TODO move to service
    function processStats(definition, stats) {
      var totals = {
        executing: 0,
        queued: 0,
        sleeping: 0,
        nonScheduled: 0,
        totalActive: 0
      };
      if (!stats.stateStatistics) {
        stats.stateStatistics = {};
      }
      _.each(definition.states, function (state) {
        var name = state.name;

        state.stateStatistics = stats.stateStatistics[name] ? stats.stateStatistics[name] : {};

        state.stateStatistics.totalActive = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b;
        }, 0) - (state.stateStatistics.nonScheduled ? state.stateStatistics.nonScheduled : 0);

        _.each(['executing', 'queued', 'sleeping', 'nonScheduled', 'totalActive'], function (stat) {
          totals[stat] += state.stateStatistics[stat] ? state.stateStatistics[stat] : 0;
        });
      });
      definition.stateStatisticsTotal = totals;
      return stats;
    }

    // must use $apply() - event not managed by angular
    function nodeSelectedCallBack(nodeId) {
      $scope.$apply(function () {
        nodeSelected(nodeId);
      });
    }

    function updateStateExecutionGraph(type) {
      var stats = WorkflowStatsPoller.getLatest(type);
      if (!self.definition) {
        console.debug('Definition not loaded yet');
        return;
      }
      if (stats) {
        processStats(self.definition, stats);
        self.hasStatistics = drawStateExecutionGraph('statisticsGraph', stats.stateStatistics, self.definition, nodeSelectedCallBack);
      }
    }

    // download buttons
    function svgDataUrl() {
      var html = d3.select('#dagreSvg')
        .attr('version', 1.1)
        .attr('xmlns', 'http://www.w3.org/2000/svg')
        .node().outerHTML;
      return 'data:image/svg+xml;base64,' + btoa(html);
    }

    function downloadSvg(filename) {
      downloadDataUrl(svgDataUrl(), filename);
    }

    // TODO save as PNG doesn't work. due to css file?
    function savePng() {
      console.info('Save PNG');
      var selectedNode = self.selectedNode;
      nodeSelected(null);
      downloadImage(svgDataUrl(), self.definition.type + '.png', 'image/png');
      nodeSelected(selectedNode);
    }

    function saveSvg() {
      console.info('Save SVG');
      var selectedNode = self.selectedNode;
      nodeSelected(null);
      downloadSvg(self.definition.type + '.svg');
      nodeSelected(selectedNode);
    }

  });

})();
