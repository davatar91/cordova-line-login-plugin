#!/usr/bin/env node

var fs = require("fs")
var os = require("os")

module.exports = function (context) {

  var rootPath = context.opts.projectRoot
  var platformPath = rootPath + "/platforms/android"
  var propertiesPath = platformPath + "/gradle.properties"
  var requiredProperties = {
    "android.useAndroidX": "true",
    "android.enableJetifier": "true"
  }

  if (!existsFile(propertiesPath)) {
    fs.writeFileSync(propertiesPath, "")
  }

  var text = fs.readFileSync(propertiesPath, "utf-8")
  Object.keys(requiredProperties).forEach(function (key) {
    var pattern = new RegExp("^" + key.replace(/\./g, "\\.") + "\\s*=", "m")
    if (!pattern.test(text)) {
      text = text.length === 0 ? key + "=" + requiredProperties[key] : text + os.EOL + key + "=" + requiredProperties[key]
    }
  })
  fs.writeFileSync(propertiesPath, text)

  function existsFile(path) {
    try {
      fs.statSync(path)
      return true
    } catch(err) {
      return false
    }
  }

}
