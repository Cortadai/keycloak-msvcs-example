#!/bin/bash

# Script para iniciar todos los microservicios en el orden correcto
# Uso: ./start-all.sh

echo "================================================"
echo "  Iniciando Microservicios con Keycloak"
echo "================================================"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para verificar si un puerto está en uso
check_port() {
    port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        return 0
    else
        return 1
    fi
}

# Función para esperar a que un servicio esté listo
wait_for_service() {
    service_name=$1
    port=$2
    max_wait=60
    waited=0

    echo -n "Esperando a que $service_name esté listo..."

    while ! check_port $port; do
        if [ $waited -ge $max_wait ]; then
            echo -e "${RED}✗ TIMEOUT${NC}"
            echo "❌ $service_name no inició en $max_wait segundos"
            exit 1
        fi
        echo -n "."
        sleep 2
        waited=$((waited + 2))
    done

    echo -e " ${GREEN}✓${NC}"
}

# 0. Verificar que Keycloak está corriendo
echo "================================================"
echo "0. Verificando Keycloak..."
echo "================================================"

if ! check_port 8080; then
    echo -e "${RED}❌ Keycloak no está corriendo en puerto 8080${NC}"
    echo "Por favor, inicia Keycloak primero"
    exit 1
fi
echo -e "${GREEN}✓ Keycloak está corriendo${NC}"
echo ""

# 1. Config Server
echo "================================================"
echo "1. Iniciando Config Server (8888)..."
echo "================================================"

cd config-server || exit
mvn spring-boot:run > ../logs/config-server.log 2>&1 &
CONFIG_PID=$!
cd ..

wait_for_service "Config Server" 8888
echo ""

# 2. Eureka Discovery Server
echo "================================================"
echo "2. Iniciando Eureka Discovery Server (8761)..."
echo "================================================"

cd discovery-server || exit
mvn spring-boot:run > ../logs/discovery-server.log 2>&1 &
EUREKA_PID=$!
cd ..

wait_for_service "Eureka" 8761
echo ""

# 3. API Gateway
echo "================================================"
echo "3. Iniciando API Gateway (8081)..."
echo "================================================"

cd api-gateway || exit
mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
GATEWAY_PID=$!
cd ..

wait_for_service "Gateway" 8081
echo ""

# 4. Microservicios (en paralelo)
echo "================================================"
echo "4. Iniciando Microservicios..."
echo "================================================"

# User Service
echo "Iniciando User Service (8082)..."
cd user-service || exit
mvn spring-boot:run > ../logs/user-service.log 2>&1 &
USER_PID=$!
cd ..

# Product Service
echo "Iniciando Product Service (8083)..."
cd product-service || exit
mvn spring-boot:run > ../logs/product-service.log 2>&1 &
PRODUCT_PID=$!
cd ..

# Order Service
echo "Iniciando Order Service (8084)..."
cd order-service || exit
mvn spring-boot:run > ../logs/order-service.log 2>&1 &
ORDER_PID=$!
cd ..

# Esperar a que todos inicien
wait_for_service "User Service" 8082
wait_for_service "Product Service" 8083
wait_for_service "Order Service" 8084

echo ""
echo "================================================"
echo "  ✅ TODOS LOS SERVICIOS INICIADOS"
echo "================================================"
echo ""
echo "Servicios corriendo:"
echo "  - Config Server:      http://localhost:8888"
echo "  - Eureka:             http://localhost:8761"
echo "  - API Gateway:        http://localhost:8081"
echo "  - User Service:       http://localhost:8082"
echo "  - Product Service:    http://localhost:8083"
echo "  - Order Service:      http://localhost:8084"
echo ""
echo "Logs en directorio: ./logs/"
echo ""
echo "Para detener todos los servicios:"
echo "  kill $CONFIG_PID $EUREKA_PID $GATEWAY_PID $USER_PID $PRODUCT_PID $ORDER_PID"
echo ""
echo "PIDs guardados en ./pids.txt"
echo "$CONFIG_PID $EUREKA_PID $GATEWAY_PID $USER_PID $PRODUCT_PID $ORDER_PID" > pids.txt
echo ""
echo "Para ver logs en tiempo real:"
echo "  tail -f logs/config-server.log"
echo "  tail -f logs/api-gateway.log"
echo "  tail -f logs/user-service.log"
echo ""
echo "================================================"
echo "  🚀 ¡Listo para probar!"
echo "================================================"
echo ""
echo "Próximo paso: obtén un JWT de Keycloak"
echo ""
echo "curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \\"
echo "  -d \"client_id=mi-cliente\" \\"
echo "  -d \"username=user\" \\"
echo "  -d \"password=user\" \\"
echo "  -d \"grant_type=password\""
echo ""
