FROM mcr.microsoft.com/devcontainers/java:21

# Install required packages
RUN apt update && apt install -y graphviz gnuplot curl zip unzip

# Switch to vscode user for installations
USER vscode
WORKDIR /home/vscode

# Install Jbang
RUN curl -sL https://sh.jbang.dev | bash -s - app setup


