#!/bin/bash
# Version minimaliste

echo "🔨 Build du projet..."
mvn clean package

echo "📤 Déploiement vers Tomcat..."
cp target/frame.war ~/Documents/apache-tomcat-10.1.46/webapps/

echo "✅ Terminé! http://localhost:8080/frame/"