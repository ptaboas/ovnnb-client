#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_fd725cdc679b_key -iv $encrypted_fd725cdc679b_iv -in ./.travis/codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

version=${TRAVIS_TAG%v*}
echo "Release version $version"
mvn versions:set -DnewVersion=$version -DgenerateBackupPoms=false