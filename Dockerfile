# Pull base image
ARG BASE_IMAGE_TAG
FROM openjdk:${BASE_IMAGE_TAG:-11.0.2-jdk-oraclelinux7}

# Env variables
ARG SCALA_VERSION
ENV SCALA_VERSION ${SCALA_VERSION:-2.13.4}
ARG SBT_VERSION
ENV SBT_VERSION ${SBT_VERSION:-1.4.3}
ARG USER_ID
ENV USER_ID ${USER_ID:-1001}
ARG GROUP_ID 
ENV GROUP_ID ${GROUP_ID:-1001}

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.rpm https://bintray.com/sbt/rpm/download_file?file_path=sbt-$SBT_VERSION.rpm && \
  rpm -i sbt-$SBT_VERSION.rpm && \
  rm sbt-$SBT_VERSION.rpm && \
  yum -y update && \
  yum -y install sbt

# Install Scala
## Piping curl directly in tar
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /usr/share && \
  mv /usr/share/scala-$SCALA_VERSION /usr/share/scala && \
  chown -R root:root /usr/share/scala && \
  chmod -R 755 /usr/share/scala && \
  ln -s /usr/share/scala/bin/scala /usr/local/bin/scala

# Add and use user sbtuser
RUN groupadd --gid $GROUP_ID sbtuser && useradd --gid $GROUP_ID --uid $USER_ID sbtuser --shell /bin/bash
RUN chown -R sbtuser:sbtuser /opt
# RUN mkdir /home/sbtuser && chown -R sbtuser:sbtuser /home/sbtuser
RUN mkdir /logs && chown -R sbtuser:sbtuser /logs
USER sbtuser

# Switch working directory
WORKDIR /home/sbtuser  

# Prepare sbt (warm cache)
RUN \
  sbt sbtVersion && \
  mkdir -p project && \
  echo "scalaVersion := \"${SCALA_VERSION}\"" > build.sbt && \
  echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
  echo "case object Temp" > Temp.scala && \
  sbt compile && \
  rm -r project && rm build.sbt && rm Temp.scala && rm -r target

# Link everything into root as well
# This allows users of this container to choose, whether they want to run the container as sbtuser (non-root) or as root
USER root
RUN \
  ln -s /home/sbtuser/.cache /root/.cache && \
  ln -s /home/sbtuser/.ivy2 /root/.ivy2 && \
  ln -s /home/sbtuser/.sbt /root/.sbt

ADD /uppaal-bin /usr/bin
COPY run_tests.sh /usr/bin/run_tests.sh


# Switch working directory back to root
## Users wanting to use this container as non-root should combine the two following arguments
## -u sbtuser
## -w /home/sbtuser
WORKDIR /root

# copy all the files to the container
COPY . .
RUN chmod +x /usr/bin/run_tests.sh


CMD ["sh", "./usr/bin/run_tests.sh"]
