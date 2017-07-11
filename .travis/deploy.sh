#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ]; then
	mvn coveralls:report 
    mvn jar:jar deploy:deploy --settings=./.travis/settings.xml
fi
