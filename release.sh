#!/bin/bash
# Clean is necessary because of the twirl templates. They won't be recompiled (even if changed) unless they've been deleted.
sbt clean assembly 

rm -rf release

mkdir -p release/
rsync -a ./scripts ./release/
rsync -a ./src/test/resources/ ./release/
find ./target/scala-2.13/ -name '*.jar' -exec mv {} ./release/ \;
cd release
JARFILE=*.jar
mkdir uppall_files_success
for f in examples/*.conf
do 
    OUT=./uppall_files_success/$(basename ${f%.*}).xml
    java -jar $JARFILE -m ${f} -o $OUT --verify 
done

mkdir uppall_files_mistakes
for f in common_mistakes/*.conf
do 
    OUT=./uppall_files_mistakes/$(basename ${f%.*}).xml
    java -jar $JARFILE -m ${f} -o $OUT --verify --trace
done