#!/usr/bin/env bash

openssl aes-256-cbc -K $encrypted_fd725cdc679b_key -iv $encrypted_fd725cdc679b_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

mvn --settings=./.travis/settings.xml jar:jar source:jar-no-fork javadoc:jar gpg:sign deploy:deploy