#!/bin/bash
set -e
REPO="BestCoder1877/Weather"
latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "0")
new_tag_num=$((latest_tag + 1))
new_tag="v${new_tag_num}"
git config user.name "dockploy-ci"
git config user.email "dockploy-ci@users.noreply.github.com"
git tag -a "$new_tag" -m "Auto-generated release tag by Dockploy CI/CD"
git push origin "$new_tag"
commit_sha=$(git rev-parse HEAD)
curl -s -X POST \
  -H "Authorization: token ${GITHUB_TOKEN}" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/$REPO/releases \
  -d "{
    \"tag_name\": \"${new_tag}\",
    \"name\": \"Release ${new_tag}\",
    \"body\": \"### Auto-generated Release\\n\\nThis release was automatically created by the Dockploy CI/CD pipeline.\\n\\n**Commit**: \\\`${commit_sha}\\\`\\n**Generated**: \$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"draft\": false,
    \"prerelease\": false
}"
apk_path="app/build/outputs/apk/release/app-release.apk"
if [ -f "$apk_path" ]; then
  release_id=$(curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
    https://api.github.com/repos/$REPO/releases |
    jq -r ".[] | select(.tag_name == \"${new_tag}\") | .id")
  if [ -n "$release_id" ] && [ "$release_id" != "null" ]; then
    curl -s -X POST \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Content-Type: application/octet-stream" \
      --data-binary "@${apk_path}" \
      "https://uploads.github.com/repos/$REPO/releases/${release_id}/assets?name=app-release.apk"
  fi
fi
