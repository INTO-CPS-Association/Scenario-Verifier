
# Use the official image as a parent image
FROM eclipse-temurin:11

# Set the working directory
WORKDIR /tools

ARG SBT_VERSION=1.9.7

# Install Z3 using apt-get
RUN apt-get update && apt-get install -y wget unzip

# Download Z3
RUN apt-get update && apt-get install -y z3

# Install sbt
RUN \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  cd && \
  rm -r /working/ && \
  sbt sbtVersion

# Copy everything in the current directory to the working directory
COPY uppaal_binary/uppaal64-4.1.24.zip /tools

# Unzip UPPAAL
RUN unzip uppaal64-4.1.24.zip -d /tools
RUN rm uppaal64-4.1.24.zip

# Rename UPPAAL folder
RUN mv /tools/uppaal64-4.1.24 /tools/uppaal
RUN chmod +x /tools/uppaal/bin-Linux/verifyta

# Add UPPAAL to path
ENV PATH="/tools/uppaal/bin-Linux:${PATH}"

# Add Z3 to path
ENV PATH="/bin/z3:${PATH}"

# Set the working directory
WORKDIR bin/