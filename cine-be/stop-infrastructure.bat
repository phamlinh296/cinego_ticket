@echo off
echo 🛑 Stopping CineGo Ticket Infrastructure...

echo 📦 Stopping Docker containers...
docker-compose down

echo 🧹 Cleaning up...
docker system prune -f

echo ✅ Infrastructure stopped successfully!
pause
