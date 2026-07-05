#!/bin/bash
# Bump version across Ventus project files

set -e

if [ -z "$1" ]; then
  echo "Usage: ./scripts/bump-version.sh <new-version>"
  echo "Example: ./scripts/bump-version.sh 1.5.0"
  exit 1
fi

NEW_VERSION="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "🔄 Bumping version to $NEW_VERSION..."

update_file() {
  local file=$1
  local version=$2

  if [ ! -f "$file" ]; then
    echo "⚠️  File not found: $file"
    return
  fi

  if [[ $file == *.md ]]; then
    sed -i "s/^\*\*Version\*\*: [^*]*/\*\*Version\*\*: $version/" "$file"
  elif [[ $file == *.kts ]]; then
    # Increment versionCode by 1 and set versionName to the new version
    CURRENT_CODE=$(grep -oP 'versionCode = \K[0-9]+' "$file")
    NEW_CODE=$((CURRENT_CODE + 1))
    sed -i "s/versionCode = [0-9]*/versionCode = $NEW_CODE/" "$file"
    sed -i "s/versionName = \"[^\"]*\"/versionName = \"$version\"/" "$file"
  fi

  echo "✓ Updated: $file"
}

update_file "$PROJECT_ROOT/CLAUDE.md" "$NEW_VERSION"
update_file "$PROJECT_ROOT/app/build.gradle.kts" "$NEW_VERSION"

echo "✅ Version bumped to $NEW_VERSION"
echo ""
echo "Next steps:"
echo "  1. Commit changes: git add . && git commit -m 'chore: bump to v$NEW_VERSION' && git push"
echo "  2. Tag: git tag v$NEW_VERSION && git push origin tag v$NEW_VERSION"
