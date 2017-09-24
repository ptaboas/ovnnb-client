#!/usr/bin/env bash

#setup_git() {
#  git config --global user.email "travis@travis-ci.org"
#  git config --global user.name "Travis CI"
#}
#
#commit_changes() {
#  git add --all
#  git commit --message "$1"
#}
#
#upload_files() {
#  git remote add origin https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git
#  git push --quiet origin master
#}
#
#increment_version () { 
#	a=( ${1//./ } )
#	((a[2]++))
# 	DEVELOPMENT_VERISON="${a[0]}.${a[1]}.${a[2]}-SNAPSHOT"
# 	mvn versions:set -DnewVersion=$DEVELOPMENT_VERISON -DgenerateBackupPoms=false
#}

openssl aes-256-cbc -K $encrypted_85c50a6b4452_key -iv $encrypted_85c50a6b4452_iv -in ./.travis/codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

mvn help:evaluate -Dexpression=project.version
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -v '\[' | sed 's/-SNAPSHOT$//')
echo "Release version is $RELEASE_VERSION"
mvn versions:set -DnewVersion=$RELEASE_VERSION -DgenerateBackupPoms=false
mvn --settings=./.travis/settings.xml jar:jar source:jar-no-fork javadoc:jar gpg:sign deploy:deploy

#setup_git
#commit_changes "Travis: Set release version"
#upload_files
