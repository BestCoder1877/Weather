#!/usr/bin/env bash
set -euo pipefail

REPO="BestCoder1877/Weather"
API="https://api.github.com/repos/${REPO}"
: "${GITHUB_TOKEN:?Set the GITHUB_TOKEN build-time secret in Dokploy}"

apk_path="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$apk_path" ]]; then
  echo "Release APK not found: $apk_path" >&2
  exit 1
fi

api() {
  curl -fsS \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" \
    "$@"
}

latest_tag_num=$(api "${API}/git/matching-refs/tags/v" |
  jq -r '[.[] | .ref | split("/")[-1] | ltrimstr("v") | select(test("^[0-9]+$")) | tonumber] | max // 0')
new_tag="v$((latest_tag_num + 1))"
commit_sha=$(api "${API}/commits/master" | jq -er '.sha')
generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)

version_release=$(jq -n \
  --arg tag "$new_tag" \
  --arg sha "$commit_sha" \
  --arg generated "$generated_at" \
  '{
    tag_name: $tag,
    target_commitish: "master",
    name: ("Release " + $tag),
    body: ("Automated release.\n\nCommit: `" + $sha + "`\nGenerated: " + $generated),
    draft: false,
    prerelease: false
  }')
version_release_response=$(api -X POST "${API}/releases" -d "$version_release")
version_release_id=$(printf '%s' "$version_release_response" | jq -er '.id')

upload_apk() {
  local release_id=$1
  local old_asset_id

  old_asset_id=$(api "${API}/releases/${release_id}/assets" |
    jq -r '[.[] | select(.name == "app-release.apk") | .id] | first // empty')
  if [[ -n "$old_asset_id" ]]; then
    api -X DELETE "${API}/releases/assets/${old_asset_id}" >/dev/null
  fi

  curl -fsS -X POST \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/octet-stream" \
    --data-binary "@${apk_path}" \
    "https://uploads.github.com/repos/${REPO}/releases/${release_id}/assets?name=app-release.apk" \
    >/dev/null
}

upload_apk "$version_release_id"

rolling_refs=$(api "${API}/git/matching-refs/tags/rolling")
rolling_ref=$(printf '%s' "$rolling_refs" |
  jq -r '.[] | select(.ref == "refs/tags/rolling") | .ref' | head -n 1)
if [[ -n "$rolling_ref" ]]; then
  api -X PATCH "${API}/git/refs/tags/rolling" \
    -d "$(jq -n --arg sha "$commit_sha" '{sha: $sha, force: true}')" >/dev/null
else
  api -X POST "${API}/git/refs" \
    -d "$(jq -n --arg sha "$commit_sha" '{ref: "refs/tags/rolling", sha: $sha}')" >/dev/null
fi

rolling_release_id=$(api "${API}/releases?per_page=100" |
  jq -r '[.[] | select(.tag_name == "rolling") | .id] | first // empty')
rolling_body="Tracks the latest successful build (${new_tag}, ${commit_sha})."
if [[ -n "$rolling_release_id" ]]; then
  api -X PATCH "${API}/releases/${rolling_release_id}" \
    -d "$(jq -n --arg body "$rolling_body" '{name: "Rolling Release", body: $body}')" >/dev/null
else
  rolling_release=$(jq -n \
    --arg sha "$commit_sha" \
    --arg body "$rolling_body" \
    '{
      tag_name: "rolling",
      target_commitish: $sha,
      name: "Rolling Release",
      body: $body,
      draft: false,
      prerelease: false
    }')
  rolling_response=$(api -X POST "${API}/releases" -d "$rolling_release")
  rolling_release_id=$(printf '%s' "$rolling_response" | jq -er '.id')
fi

upload_apk "$rolling_release_id"
printf 'Published %s and updated rolling release for %s\n' "$new_tag" "$commit_sha"
