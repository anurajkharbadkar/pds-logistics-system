#!/usr/bin/env bash
# ==============================================================================
# PDS Logistics Optimization & Decision Support System – Runner Script
# ==============================================================================
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

# Ensure brew and java binaries are on PATH
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

ACTION="${1:-run}"

case "$ACTION" in
    run)
        echo "🚀 Starting PDS Logistics Decision Support System..."
        mvn clean javafx:run
        ;;
    test)
        echo "🧪 Running JUnit 5 test suite..."
        mvn test
        ;;
    compile)
        echo "🔨 Compiling project..."
        mvn clean compile
        ;;
    package)
        echo "📦 Packaging JAR..."
        mvn clean package -DskipTests
        ;;
    db-init)
        echo "🗄️ Initializing MySQL database (pds_logistics)..."
        echo "Please enter MySQL root password when prompted:"
        mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pds_logistics;"
        mysql -u root -p pds_logistics < src/main/resources/sql/schema.sql
        mysql -u root -p pds_logistics < src/main/resources/sql/seed_data.sql
        echo "✅ Database initialized and seeded successfully!"
        ;;
    *)
        echo "Usage: ./run.sh [run|test|compile|package|db-init]"
        exit 1
        ;;
esac
