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

wait_for_mysql() {
    echo "⏳ Waiting for MySQL to become ready on port 3306..."
    for i in {1..30}; do
        if docker exec pds_mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            echo "✅ MySQL is ready and responding!"
            return 0
        fi
        sleep 2
    done
    echo "⚠️ Timed out waiting for MySQL."
    return 1
}

case "$ACTION" in
    docker-db)
        echo "🐳 Starting MySQL 8.0 container via Docker Compose..."
        docker compose up -d
        wait_for_mysql
        ;;
    run)
        # Check if port 3306 is open; if not, spin up Docker container automatically
        if ! nc -z 127.0.0.1 3306 2>/dev/null; then
            echo "ℹ️ MySQL is not running on port 3306. Starting Docker container..."
            docker compose up -d
            wait_for_mysql
        fi
        echo "🚀 Starting PDS Logistics Decision Support System..."
        mvn javafx:run
        ;;
    test)
        echo "🧪 Running JUnit 5 test suite..."
        mvn test
        ;;
    compile)
        echo "🔨 Compiling project..."
        mvn compile
        ;;
    package)
        echo "📦 Packaging JAR..."
        mvn package -DskipTests
        ;;
    db-init)
        if command -v mysql >/dev/null 2>&1; then
            echo "🗄️ Initializing MySQL database via local mysql CLI..."
            mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS pds_logistics;"
            mysql -u root -p pds_logistics < src/main/resources/sql/schema.sql
            mysql -u root -p pds_logistics < src/main/resources/sql/seed_data.sql
            echo "✅ Database initialized and seeded successfully!"
        else
            echo "ℹ️ Local 'mysql' CLI not found. Using Docker Compose container..."
            docker compose up -d
            wait_for_mysql
            echo "✅ Docker MySQL container initialized with schema and seed data!"
        fi
        ;;
    *)
        echo "Usage: ./run.sh [run|test|compile|package|db-init|docker-db]"
        exit 1
        ;;
esac
