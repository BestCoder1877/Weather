FROM eclipse-temurin:25-jdk
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    bash \
    curl \
    zip \
    unzip \
    openssh-client \
    ca-certificates \
    jq \
    sed \
    && rm -rf /var/lib/apt/lists/*
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
ENV ANDROID_SDK_ROOT=$ANDROID_HOME
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -sL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --install \
    "platforms;android-37.0" \
    "build-tools;36.0.0" \
    "platform-tools" && \
    echo "y" | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
WORKDIR /workspace
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY app ./app
RUN chmod +x gradlew
RUN ./gradlew :app:assembleRelease
COPY deploy_to_github.sh .
RUN chmod +x deploy_to_github.sh
RUN ./deploy_to_github.sh
