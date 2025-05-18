#!/bin/bash

find . \
  -type f \
  ! -path "./.git/*" \
  ! -name "pom.xml" \
  ! -path "./target/*" \
  | sort | while read -r file; do
    echo "===== $file ====="
    cat "$file"
    echo
done