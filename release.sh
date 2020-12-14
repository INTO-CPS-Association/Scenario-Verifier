#!/bin/bash
# Clean is necessary because of the twirl templates. They won't be recompiled (even if changed) unless they've been deleted.
sbt clean assembly 

rm -rf release

mkdir -p release/
rsync -a ./scripts ./release/
rsync -a ./src/test/resources/ ./release/
find ./target/scala-2.13/ -name '*.jar' -exec mv {} ./release/ \;
