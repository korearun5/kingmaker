#!/bin/bash
# deploy.sh

set -e

echo "🚀 Starting BetKing deployment..."

# Load environment variables
source .env

# Build the application
echo "📦 Building application..."
./mvnw clean package -DskipTests

# Build Docker image
echo "🐳 Building Docker image..."
docker-compose build

# Stop existing services
echo "🛑 Stopping existing services..."
docker-compose down

# Start services
echo "✅ Starting services..."
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 30

# Run database migrations (if any)
echo "🗄️ Running database migrations..."
# Add migration commands here

# Check application health
echo "🏥 Checking application health..."
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$HEALTH_CHECK" -eq 200 ]; then
    echo "🎉 Deployment successful! BetKing is now running."
    echo "📍 Application URL: http://localhost:8080"
    echo "🔧 Admin URL: http://localhost:8080/admin/dashboard"
else
    echo "❌ Deployment failed. Health check returned: $HEALTH_CHECK"
    exit 1
fi