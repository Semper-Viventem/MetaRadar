#!/bin/bash

ls -a
git config --global --add safe.directory /github/workspace
hub checkout ${REPO_BRANCH:-master}
VERSION_NAME=`grep -oP 'versionName = "\K(.*?)(?=")' ./${APP_FOLDER}/build.gradle.kts`
hub release create -a ./${APP_FOLDER}/build/outputs/apk/${BUILD_FLAVOR}/${BUILD_TYPE}/*-${BUILD_TYPE}.apk -m "${RELEASE_TITLE} - v${VERSION_NAME}" v${VERSION_NAME}${TAG_SUFFIX}
