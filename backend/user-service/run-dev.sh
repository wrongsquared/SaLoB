#!/usr/bin/env bash
cd "$(dirname "$0")" || exit
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
