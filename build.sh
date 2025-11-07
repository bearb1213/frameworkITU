#!/bin/bash
set -euo pipefail

TARGET_DIR="target"
SRC_DIR="src"
LIB_DIR="lib"
JAR_NAME="frameWork.jar"
MAIN_CLASS="com.frame.Main"  # ⚠️ change ça selon ton projet

mkdir -p "$TARGET_DIR"

# Construire le classpath avec toutes les libs présentes dans lib/
CLASSPATH="."
if [ -d "$LIB_DIR" ]; then
  for jar in "$LIB_DIR"/*.jar; do
    [ -e "$jar" ] || continue
    CLASSPATH="$CLASSPATH:$jar"
  done
fi

echo "Classpath utilisé: $CLASSPATH"

SOURCES_FILE=$(mktemp)
trap 'rm -f "$SOURCES_FILE"' EXIT

find "$SRC_DIR" -name "*.java" > "$SOURCES_FILE"

if [ ! -s "$SOURCES_FILE" ]; then
  echo "Aucun fichier .java trouvé dans $SRC_DIR"
  exit 0
fi

javac -d "$TARGET_DIR" -cp "$CLASSPATH" @"$SOURCES_FILE"

echo "Compilation terminée. Création du JAR..."

# Création du JAR exécutable
cd "$TARGET_DIR"
jar cfe "$JAR_NAME" "$MAIN_CLASS" $(find . -type f -name "*.class")

echo "JAR créé : $TARGET_DIR/$JAR_NAME"

echo "pwd : "
pwd 

SOURCE_FILE="frameWork.jar"
DEST1="/home/mrtsila/Documents/S5/framework/frameworkITUTest/webapp/WEB-INF/lib"
DEST2="/home/mrtsila/Documents/S5/framework/frameworkITUTest/lib"

# # Créer les répertoires de destination s'ils n'existent pas
# mkdir -p "$DEST1"
# mkdir -p "$DEST2"

# Supprimer les fichiers existants puis copier
rm -f "$DEST1/$(basename "$SOURCE_FILE")"
rm -f "$DEST2/$(basename "$SOURCE_FILE")"

cp "$SOURCE_FILE" "$DEST1"
cp "$SOURCE_FILE" "$DEST2"

echo "JAR remplace : $DEST1 , $DEST2"