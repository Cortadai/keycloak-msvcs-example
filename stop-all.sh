#!/bin/bash

# Script para detener todos los microservicios
# Uso: ./stop-all.sh

echo "================================================"
echo "  Deteniendo Microservicios"
echo "================================================"
echo ""

# Leer PIDs del archivo
if [ -f pids.txt ]; then
    PIDS=$(cat pids.txt)
    echo "PIDs encontrados: $PIDS"
    echo ""

    for PID in $PIDS; do
        if ps -p $PID > /dev/null; then
            echo "Deteniendo proceso $PID..."
            kill $PID
        else
            echo "Proceso $PID ya no está corriendo"
        fi
    done

    rm pids.txt
    echo ""
    echo "✓ Todos los servicios detenidos"
else
    echo "Archivo pids.txt no encontrado"
    echo "Deteniendo por puerto..."
    echo ""

    # Detener por puerto
    for PORT in 8888 8761 8081 8082 8083 8084; do
        PID=$(lsof -ti:$PORT)
        if [ ! -z "$PID" ]; then
            echo "Deteniendo servicio en puerto $PORT (PID: $PID)..."
            kill $PID
        fi
    done

    echo ""
    echo "✓ Servicios detenidos"
fi

echo ""
echo "================================================"
