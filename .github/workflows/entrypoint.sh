#!/bin/bash

hub clone https://github.com/Semper-Viventem/MetaRadar.git
hub checkout ${${REPO_BRANCH}:-master}
VERSION_NAME=`grep -oP 'versionName = "\K(.*?)(?=")' ./${APP_FOLDER}/build.gradle.kts`
hub release create -a ./${APP_FOLDER}/build/outputs/apk/BUILD_TYPE/*-BUILD_TYPE.apk -m "${RELEASE_TITLE} - v${VERSION_NAME}" $(date +%Y%m%d%H%M%S)
