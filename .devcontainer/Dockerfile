FROM mcr.microsoft.com/devcontainers/java:17

# Install required packages
RUN apt update && apt install -y graphviz gnuplot curl zip unzip

# Switch to vscode user for installations
USER vscode

# Set up SDKMAN
ENV SDKMAN_DIR="/usr/local/sdkman"
RUN bash -c 'source "$SDKMAN_DIR/bin/sdkman-init.sh" \
    && echo "source \$SDKMAN_DIR/bin/sdkman-init.sh" >> ~/.bashrc \
    && sdk version'

# Install JBang
RUN curl -Ls https://sh.jbang.dev | bash -s - app setup
ENV PATH="/home/vscode/.jbang/bin:${PATH}"

# Add SDKMAN to PATH
ENV PATH="${SDKMAN_DIR}/candidates/java/current/bin:${PATH}"

# Verify installations
RUN bash -c 'source "$SDKMAN_DIR/bin/sdkman-init.sh" && sdk version && jbang version'
