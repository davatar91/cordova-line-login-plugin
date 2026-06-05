#!/usr/bin/env node

'use strict';

const fs = require('fs');
const path = require('path');
const xcode = require('xcode');

module.exports = function (context) {
  const projectRoot = context.opts.projectRoot;
  const iosPath = path.join(projectRoot, 'platforms', 'ios');

  if (!fs.existsSync(iosPath)) {
    return;
  }

  const projectDir = fs.readdirSync(iosPath).find((entry) => entry.endsWith('.xcodeproj'));
  if (!projectDir) {
    return;
  }

  const projectName = projectDir.replace(/\.xcodeproj$/, '');
  if (projectName === 'App') {
    return;
  }

  const pbxprojPath = path.join(iosPath, projectDir, 'project.pbxproj');
  const legacyProjectPath = path.join(iosPath, projectName);
  const bridgingHeaderPath = fs.existsSync(path.join(legacyProjectPath, 'Bridging-Header.h'))
    ? '"$(PROJECT_DIR)/$(PROJECT_NAME)/Bridging-Header.h"'
    : null;

  const xcodeProject = xcode.project(pbxprojPath);
  xcodeProject.parseSync();

  const COMMENT_KEY = /_comment$/;
  const buildConfigs = xcodeProject.pbxXCBuildConfigurationSection();

  for (const configName in buildConfigs) {
    if (COMMENT_KEY.test(configName)) {
      continue;
    }

    const buildConfig = buildConfigs[configName];
    xcodeProject.updateBuildProperty('SWIFT_VERSION', '5.0', buildConfig.name);
    xcodeProject.updateBuildProperty('ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES', 'YES', buildConfig.name);

    if (bridgingHeaderPath) {
      xcodeProject.updateBuildProperty('SWIFT_OBJC_BRIDGING_HEADER', bridgingHeaderPath, buildConfig.name);
    }
  }

  fs.writeFileSync(pbxprojPath, xcodeProject.writeSync());
};
