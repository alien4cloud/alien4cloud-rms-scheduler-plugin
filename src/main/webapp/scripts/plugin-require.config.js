require.config({
  baseUrl: './',
  paths: {
    'alien4cloud-rms-scheduler-plugin': 'scripts/plugin',
    /* 'moment': 'bower_components/moment/min/moment-with-locales.min', */
    'vis': 'bower_components/vis/dist/vis.min'
    /* We don't load lodash even if compiler claims it (less js to load) */
    /*,'lodash': 'bower_components/lodash/lodash'*/
  },
  shim: {
  }
});
