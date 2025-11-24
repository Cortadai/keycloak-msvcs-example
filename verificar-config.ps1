# Script de Verificación del Config Server
# Verifica que todo está listo antes de arrancar

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Verificación de Config Server" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar directorio config-repo
Write-Host "[1/4] Verificando directorio config-repo..." -ForegroundColor Yellow

$configRepoPath = "C:\Users\david\Desktop\keycloak\microservices\infrastructure\config-repo"

if (Test-Path $configRepoPath) {
    Write-Host "  ✅ Directorio encontrado: $configRepoPath" -ForegroundColor Green

    # Listar archivos
    $files = Get-ChildItem -Path $configRepoPath -Filter "*.yml" | Select-Object -ExpandProperty Name
    Write-Host "  📄 Archivos encontrados:" -ForegroundColor Cyan
    foreach ($file in $files) {
        Write-Host "     - $file" -ForegroundColor White
    }
} else {
    Write-Host "  ❌ ERROR: Directorio NO encontrado: $configRepoPath" -ForegroundColor Red
    Write-Host "     Por favor verifica la ubicación del proyecto" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 2. Verificar archivo application.yml del Config Server
Write-Host "[2/4] Verificando application.yml del Config Server..." -ForegroundColor Yellow

$configServerYml = "C:\Users\david\Desktop\keycloak\microservices\config-server\src\main\resources\application.yml"

if (Test-Path $configServerYml) {
    Write-Host "  ✅ Archivo encontrado: application.yml" -ForegroundColor Green

    # Verificar que tiene el perfil native
    $content = Get-Content $configServerYml -Raw
    if ($content -match "active:\s*native") {
        Write-Host "  ✅ Perfil 'native' está activo" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  ADVERTENCIA: Perfil 'native' no encontrado" -ForegroundColor Yellow
        Write-Host "     El Config Server podría no arrancar correctamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ ERROR: application.yml no encontrado" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 3. Verificar Java
Write-Host "[3/4] Verificando Java..." -ForegroundColor Yellow

try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "  ✅ Java encontrado: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ❌ ERROR: Java no encontrado" -ForegroundColor Red
    Write-Host "     Instala Java 17+ antes de continuar" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 4. Verificar Maven
Write-Host "[4/4] Verificando Maven..." -ForegroundColor Yellow

$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
$mvnwCmd = Test-Path "C:\Users\david\Desktop\keycloak\microservices\config-server\mvnw.cmd"

if ($mvnCmd -or $mvnwCmd) {
    if ($mvnwCmd) {
        Write-Host "  ✅ Maven Wrapper encontrado (mvnw.cmd)" -ForegroundColor Green
    } else {
        Write-Host "  ✅ Maven encontrado (mvn)" -ForegroundColor Green
    }
} else {
    Write-Host "  ⚠️  ADVERTENCIA: Maven no encontrado" -ForegroundColor Yellow
    Write-Host "     Puedes usar el Maven Wrapper (mvnw) del proyecto" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ✅ VERIFICACIÓN COMPLETADA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Instrucciones para arrancar
Write-Host "📝 Siguiente paso: Arrancar el Config Server" -ForegroundColor Cyan
Write-Host ""
Write-Host "   cd config-server" -ForegroundColor White
Write-Host "   .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "   O si tienes Maven instalado:" -ForegroundColor Gray
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "   Verifica que arranca en: http://localhost:8888" -ForegroundColor Yellow
Write-Host ""
