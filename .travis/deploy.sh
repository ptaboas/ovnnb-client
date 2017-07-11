#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ]; then
	bash <(curl -s https://codecov.io/bash)
	mvn coveralls:report
	mvn jar:jar deploy:deploy --settings=./.travis/settings.xml
fi
