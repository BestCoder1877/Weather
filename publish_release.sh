#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GitHub Actions must provide GH_TOKEN}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"

apk_path="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$apk_path" ]]; then
  echo "Release APK not found: $apk_path" >&2
  exit 1
fi

latest_tag_num=0
while IFS= read -r tag; do
  version="${tag#v}"
  if [[ "$version" =~ ^[0-9]+$ ]] && (( version > latest_tag_num )); then
    latest_tag_num=$version
  fi
done < <(git tag --list 'v*')

version_tag="v$((latest_tag_num + 1))"
gh release create "$version_tag" "$apk_path" \
  --repo "$GITHUB_REPOSITORY" \
  --target "$GITHUB_SHA" \
  --title "Release ${version_tag}" \
  --notes "Automated release for commit ${GITHUB_SHA}."

git tag -f rolling "$GITHUB_SHA"
git push origin refs/tags/rolling --force

if gh release view rolling --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  gh release edit rolling \
    --repo "$GITHUB_REPOSITORY" \
    --title "Rolling Release" \
    --notes "Tracks the latest successful build (${version_tag}, ${GITHUB_SHA})."
  gh release upload rolling "$apk_path" \
    --repo "$GITHUB_REPOSITORY" \
    --clobber
else
  gh release create rolling "$apk_path" \
    --repo "$GITHUB_REPOSITORY" \
    --target "$GITHUB_SHA" \
    --title "Rolling Release" \
    --notes "Tracks the latest successful build (${version_tag}, ${GITHUB_SHA})."
fi
