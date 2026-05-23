#!/usr/bin/env bash
cd "$(dirname "$0")"
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
