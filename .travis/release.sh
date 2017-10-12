#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_fd725cdc679b_key -iv $encrypted_fd725cdc679b_iv -in ./.travis/codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

VERSION=$(echo "$TRAVIS_TAG" | sed 's/^v//g')
echo "Release version $VERSION"
mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
mvn --settings=./.travis/settings.xml jar:jar source:jar-no-fork javadoc:jar gpg:sign deploy:deploy