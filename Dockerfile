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

# Build the release APK
RUN ./gradlew :app:assembleRelease

# Run the deployment script
COPY deploy_to_github.sh .
RUN chmod +x deploy_to_github.sh
RUN ./deploy_to_github.sh