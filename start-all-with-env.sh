#!/bin/bash

# ===============================================
# Script para iniciar todos los microservicios
# con soporte de variables de entorno desde .env
# ===============================================
# Uso: ./start-all-with-env.sh

echo "================================================"
echo "  Iniciando Microservicios con Keycloak"
echo "  (con variables de entorno)"
echo "================================================"
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ===============================================
# 1. CARGAR VARIABLES DE ENTORNO DESDE .env
# ===============================================
if [ -f .env ]; then
    echo -e "${BLUE}📄 Cargando variables de entorno desde .env...${NC}"

    # Exportar variables del archivo .env
    export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)

    echo -e "${GREEN}✓ Variables de entorno cargadas${NC}"
    echo ""
    echo "Variables configuradas:"
    echo "  KEYCLOAK_ISSUER_URI:     ${KEYCLOAK_ISSUER_URI:-[no configurado]}"
    echo "  KEYCLOAK_JWK_SET_URI:    ${KEYCLOAK_JWK_SET_URI:-[no configurado]}"
    echo "  JWT_AUDIENCE:            ${JWT_AUDIENCE:-[no configurado]}"
    echo "  EUREKA_URL:              ${EUREKA_URL:-[no configurado]}"
    echo ""
else
    echo -e "${YELLOW}⚠️  Archivo .env no encontrado${NC}"
    echo "Creando .env desde .env.example..."

    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✓ Archivo .env creado${NC}"
        echo -e "${YELLOW}⚠️  Por favor, edita .env con tus configuraciones y ejecuta de nuevo${NC}"
        exit 1
    else
        echo -e "${RED}❌ Archivo .env.example no encontrado${NC}"
        echo "Por favor, crea un archivo .env con las variables necesarias"
        exit 1
    fi
fi

# ===============================================
# 2. VALIDAR VARIABLES REQUERIDAS
# ===============================================
echo -e "${BLUE}🔍 Validando variables requeridas...${NC}"

REQUIRED_VARS=("KEYCLOAK_ISSUER_URI" "KEYCLOAK_JWK_SET_URI" "JWT_AUDIENCE" "EUREKA_URL")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${RED}❌ Faltan las siguientes variables de entorno:${NC}"
    for var in "${MISSING_VARS[@]}"; do
        echo "  - $var"
    done
    echo ""
    echo "Por favor, configura estas variables en el archivo .env"
    exit 1
fi

echo -e "${GREEN}✓ Todas las variables requeridas están configuradas${NC}"
echo ""

# ===============================================
# 3. FUNCIONES AUXILIARES
# ===============================================

# Función para verificar si un puerto está en uso
check_port() {
    port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
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
            echo -e " ${RED}✗ TIMEOUT${NC}"
            echo "❌ $service_name no inició en $max_wait segundos"
            echo "Ver logs en: logs/${service_name,,}.log"
            exit 1
        fi
        echo -n "."
        sleep 2
        waited=$((waited + 2))
    done

    echo -e " ${GREEN}✓${NC}"
}

# Crear directorio de logs si no existe
mkdir -p logs

# ===============================================
# 4. VERIFICAR KEYCLOAK
# ===============================================
echo "================================================"
echo "0. Verificando Keycloak..."
echo "================================================"

if ! check_port 8080; then
    echo -e "${RED}❌ Keycloak no está corriendo en puerto 8080${NC}"
    echo "Por favor, inicia Keycloak primero"
    echo ""
    echo "Para iniciar Keycloak (si está en Docker):"
    echo "  cd infrastructure"
    echo "  docker-compose up -d keycloak"
    exit 1
fi
echo -e "${GREEN}✓ Keycloak está corriendo${NC}"
echo ""

# ===============================================
# 5. INICIAR SERVICIOS
# ===============================================

# Config Server
echo "================================================"
echo "1. Iniciando Config Server (8888)..."
echo "================================================"

cd config-server || exit
mvn spring-boot:run > ../logs/config-server.log 2>&1 &
CONFIG_PID=$!
cd ..

wait_for_service "Config Server" 8888
echo ""

# Eureka Discovery Server
echo "================================================"
echo "2. Iniciando Eureka Discovery Server (8761)..."
echo "================================================"

cd discovery-server || exit
mvn spring-boot:run > ../logs/discovery-server.log 2>&1 &
EUREKA_PID=$!
cd ..

wait_for_service "Eureka" 8761
echo ""

# API Gateway
echo "================================================"
echo "3. Iniciando API Gateway (8081)..."
echo "================================================"

cd api-gateway || exit
mvn spring-boot:run > ../logs/api-gateway.log 2>&1 &
GATEWAY_PID=$!
cd ..

wait_for_service "Gateway" 8081
echo ""

# Microservicios (en paralelo)
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

# ===============================================
# 6. RESUMEN FINAL
# ===============================================

echo ""
echo "================================================"
echo "  ✅ TODOS LOS SERVICIOS INICIADOS"
echo "================================================"
echo ""
echo "🔐 Configuración de Seguridad:"
echo "  - Keycloak Issuer:    $KEYCLOAK_ISSUER_URI"
echo "  - JWT Audience:       $JWT_AUDIENCE"
echo ""
echo "🚀 Servicios corriendo:"
echo "  - Config Server:      http://localhost:8888"
echo "  - Eureka:             http://localhost:8761"
echo "  - API Gateway:        http://localhost:8081"
echo "  - User Service:       http://localhost:8082"
echo "  - Product Service:    http://localhost:8083"
echo "  - Order Service:      http://localhost:8084"
echo ""
echo "📄 Logs en directorio: ./logs/"
echo ""
echo "🛑 Para detener todos los servicios:"
echo "  ./stop-all.sh"
echo "  o"
echo "  kill $CONFIG_PID $EUREKA_PID $GATEWAY_PID $USER_PID $PRODUCT_PID $ORDER_PID"
echo ""

# Guardar PIDs
echo "$CONFIG_PID $EUREKA_PID $GATEWAY_PID $USER_PID $PRODUCT_PID $ORDER_PID" > pids.txt
echo "PIDs guardados en ./pids.txt"
echo ""

echo "📊 Para ver logs en tiempo real:"
echo "  tail -f logs/config-server.log"
echo "  tail -f logs/api-gateway.log"
echo "  tail -f logs/user-service.log"
echo ""
echo "================================================"
echo "  🚀 ¡Listo para probar!"
echo "================================================"
echo ""
echo "🔑 Próximo paso: obtén un JWT de Keycloak"
echo ""
echo "Ejemplo con curl:"
echo "curl -X POST $KEYCLOAK_ISSUER_URI/protocol/openid-connect/token \\"
echo "  -d \"client_id=mi-cliente\" \\"
echo "  -d \"username=user\" \\"
echo "  -d \"password=user\" \\"
echo "  -d \"grant_type=password\""
echo ""
