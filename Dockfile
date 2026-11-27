FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache \
    git \
    bash \
    curl \
    zip \
    unzip \
    openssh
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
ENV ANDROID_SDK_ROOT=$ANDROID_HOME
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -sL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip
RUN yes | $ANDROID_HOME/cmdline-tools/latest/sdkmanager --install \
    "platforms;android-37" \
    "build-tools;33.0.2" \
    "platform-tools" && \
    echo "y" | $ANDROID_HOME/cmdline-tools/latest/sdkmanager --licenses
WORKDIR /workspace
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY app ./app
RUN chmod +x gradlew
RUN ./gradlew :app:assembleRelease
RUN latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "0") && \
    echo "Latest existing tag: $latest_tag" && \
    new_tag_num=$((latest_tag + 1)) && \
    new_tag="v${new_tag_num}" && \
    echo "New tag will be: $new_tag" && \
    git config user.name "dockploy-ci" && \
    git config user.email "dockploy-ci@users.noreply.github.com" && \
    git tag -a "$new_tag" -m "Auto-generated release tag by Dockploy CI/CD" && \
    git push origin "$new_tag" && \
    echo "Tag $new_tag pushed to origin" && \
    commit_sha=$(git rev-parse HEAD) && \
    echo "Commit: $commit_sha" && \
    curl -s -X POST \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      https://api.github.com/repos/BestCoder1877/Weather/releases \
      -d "{
        \"tag_name\": \"${new_tag}\",
        \"name\": \"Release ${new_tag}\",
        \"body\": \"### Auto-generated Release\\n\\nThis release was automatically created by the Dockploy CI/CD pipeline.\\n\\n**Commit**: \`${commit_sha}\`\\n**Generated**: \$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
        \"draft\": false,
        \"prerelease\": false
      }" && \
    echo "GitHub release ${new_tag} created successfully" && \

    # Upload APK as release asset
    apk_path="app/build/outputs/apk/release/app-release.apk" && \
    release_id=$(curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
      https://api.github.com/repos/BestCoder1877/Weather/releases \
      | jq -r ".[] | select(.tag_name == \"${new_tag}\") | .id") && \
    if [ -n "$release_id" ] && [ "$release_id" != "null" ]; then
      curl -s -X POST \
        -H "Authorization: token ${GITHUB_TOKEN}" \
        -H "Content-Type: application/octet-stream" \
        --data-binary "@${apk_path}" \
        "https://uploads.github.com/repos/BestCoder1877/Weather/releases/${release_id}/assets?name=app-release.apk" && \
      echo "APK uploaded as release asset" || echo "Warning: APK upload may have failed"
    else
      echo "Could not find release ID, skipping asset upload"
    fi && \

    echo "=== Dockploy CI/CD Complete ===" && \
    echo "Tag: $new_tag" && \
    echo "APK built and released to GitHub"
CMD ["echo", "Dockploy CI/CD pipeline ready - run with appropriate GITHUB_TOKEN and GITHUB_REPOSITORY"]
