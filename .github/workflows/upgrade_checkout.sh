#!/bin/bash

for file in *; do
  if [[ -f "$file" ]]; then
    # -? makes the hyphen optional
    # [[:space:]]* catches any spaces before or after the optional hyphen
    sed -i '' -E 's/^([[:space:]]*-?[[:space:]]*uses: actions\/checkout).*$/\1@de0fac2e4500dabe0009e67214ff5f5447ce83dd # v6.0.2/' "$file"
  fi
done

echo "Replacement complete!"