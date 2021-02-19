'use strict';

// Renames files for browser caching purposes
module.exports = {
  dist: {
    files: {
      src: ['<%= yeoman.dist %>/scripts/{,*/}*.js', '<%= yeoman.dist %>/styles/{,*/}*.css',
        // '<%= yeoman.dist %>/images/{,*/}*.{png,jpg,jpeg,gif,webp,svg}',
        '<%= yeoman.dist %>/styles/fonts/*'
      ]
    }
  }
};