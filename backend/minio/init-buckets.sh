#!/bin/sh
set -eu

MINIO_ALIAS=local
MINIO_URL=http://minio:9000

if [ -z "${MINIO_ROOT_USER:-}" ] || [ -z "${MINIO_ROOT_PASSWORD:-}" ]; then
  echo "MINIO_ROOT_USER and MINIO_ROOT_PASSWORD must be set"
  exit 1
fi

if [ -z "${MINIO_BUCKET_FOOD:-}" ] || [ -z "${MINIO_BUCKET_USER:-}" ]; then
  echo "MINIO_BUCKET_FOOD and MINIO_BUCKET_USER must be set"
  exit 1
fi

echo "Waiting for MinIO to become ready..."
until mc alias set "$MINIO_ALIAS" "$MINIO_URL" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
  sleep 2
done

echo "Creating buckets if they do not already exist..."
mc mb --ignore-existing "$MINIO_ALIAS/$MINIO_BUCKET_FOOD"
mc mb --ignore-existing "$MINIO_ALIAS/$MINIO_BUCKET_USER"

echo "MinIO buckets are ready."
