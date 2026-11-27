#!/bin/bash
set -eu

REPO="BestCoder1877/Weather"

if [ -z "${GITHUB_TOKEN:-}" ]; then
  echo "GITHUB_TOKEN is required" >&2
  exit 1
fi

latest_tag=$(curl -fsS \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/git/matching-refs/tags/v" |
  jq -r '[.[].ref | split("/")[-1] | ltrimstr("v") | select(test("^[0-9]+$")) | tonumber] | max // 0')

new_tag_num=$((latest_tag + 1))
new_tag="v${new_tag_num}"

commit_sha=$(curl -fsS \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/commits/master" |
  jq -r '.sha')

release_response=$(curl -fsS -X POST \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/releases" \
  -d "{
    \"tag_name\": \"${new_tag}\",
    \"target_commitish\": \"master\",
    \"name\": \"Release ${new_tag}\",
    \"body\": \"### Auto-generated Release\\n\\nThis release was automatically created by the Dockploy CI/CD pipeline.\\n\\n**Commit**: \\\`${commit_sha}\\\`\\n**Generated**: \$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"draft\": false,
    \"prerelease\": false
}")

apk_path="app/build/outputs/apk/release/app-release.apk"
if [ -f "$apk_path" ]; then
  release_id=$(printf '%s' "$release_response" | jq -r '.id')
  if [ -n "$release_id" ] && [ "$release_id" != "null" ]; then
    curl -fsS -X POST \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Content-Type: application/octet-stream" \
      --data-binary "@${apk_path}" \
      "https://uploads.github.com/repos/${REPO}/releases/${release_id}/assets?name=app-release.apk"
  fi
fi

rolling_release_id=$(curl -sS \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/releases/tags/rolling" |
  jq -r '.id // empty')

if [ -n "${rolling_release_id:-}" ]; then
  curl -fsS -X DELETE \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${REPO}/releases/${rolling_release_id}" >/dev/null
fi

curl -fsS -X DELETE \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/git/refs/tags/rolling" >/dev/null 2>&1 || true

curl -fsS -X POST \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/git/refs" \
  -d "{\"ref\":\"refs/tags/rolling\",\"sha\":\"${commit_sha}\"}" >/dev/null

rolling_response=$(curl -fsS -X POST \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/${REPO}/releases" \
  -d "{
    \"tag_name\": \"rolling\",
    \"target_commitish\": \"${commit_sha}\",
    \"name\": \"Rolling Release\",
    \"body\": \"### Rolling Release\\n\\nTracks the latest successful build.\\n\\n**Version**: \`${new_tag}\`\\n**Commit**: \\\`${commit_sha}\\\`\",
    \"draft\": false,
    \"prerelease\": false
}")

if [ -f "$apk_path" ]; then
  rolling_id=$(printf '%s' "$rolling_response" | jq -r '.id')
  if [ -n "$rolling_id" ] && [ "$rolling_id" != "null" ]; then
    curl -fsS -X POST \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Content-Type: application/octet-stream" \
      --data-binary "@${apk_path}" \
      "https://uploads.github.com/repos/${REPO}/releases/${rolling_id}/assets?name=app-release.apk"
  fi
fi
