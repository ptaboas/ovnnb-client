#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_fd725cdc679b_key -iv $encrypted_fd725cdc679b_iv -in ./.travis/codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

echo "Release version $TRAVIS_TAG"
mvn versions:set -DnewVersion=$TRAVIS_TAG -DgenerateBackupPoms=false