FROM eclipse-temurin:17-jdk-alpine

RUN apk add --no-cache \
    git \
    bash \
    curl \
    zip \
    unzip \
    openssh \
    sed

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
ENV ANDROID_SDK_ROOT=$ANDROID_HOME

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -sL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --install \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "platform-tools" && \
    echo "y" | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY app ./app

RUN chmod +x gradlew

# Patch build.gradle.kts to use stable SDK 34 for CI build
RUN sed -i 's/compileSdk { version = release(37) }/compileSdk = 34/' app/build.gradle.kts && \
    sed -i 's/targetSdk = 37/targetSdk = 34/' app/build.gradle.kts

# Build the release APK
RUN ./gradlew :app:assembleRelease

# Run the deployment script
COPY deploy_to_github.sh .
RUN chmod +x deploy_to_github.sh
RUN ./deploy_to_github.sh