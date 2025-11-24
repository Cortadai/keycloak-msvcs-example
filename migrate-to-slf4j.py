#!/usr/bin/env python3
"""
Script para migrar de System.out/System.err a SLF4J en archivos Java

Uso:
    python migrate-to-slf4j.py [--dry-run]

Este script:
1. Busca todos los archivos .java con System.out o System.err
2. Agrega el import de SLF4J si no existe
3. Agrega la declaración del logger si no existe
4. Reemplaza System.out.println() por log.info() o log.debug()
5. Reemplaza System.err.println() por log.error()
"""

import re
import os
import sys
from pathlib import Path

def has_slf4j_import(content):
    """Verifica si el archivo ya tiene el import de SLF4J"""
    return 'import org.slf4j.Logger' in content

def has_logger_declaration(content):
    """Verifica si el archivo ya tiene la declaración del logger"""
    return re.search(r'private\s+static\s+final\s+Logger\s+log\s*=', content) is not None

def get_class_name(content):
    """Extrae el nombre de la clase del archivo Java"""
    match = re.search(r'public\s+class\s+(\w+)', content)
    if match:
        return match.group(1)
    return None

def add_slf4j_import(content):
    """Agrega los imports de SLF4J después del package"""
    # Buscar el último import
    import_pattern = r'(import\s+[\w.]+;)'
    imports = list(re.finditer(import_pattern, content))

    if imports:
        last_import = imports[-1]
        insert_pos = last_import.end()

        slf4j_imports = '\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;'
        content = content[:insert_pos] + slf4j_imports + content[insert_pos:]

    return content

def add_logger_declaration(content, class_name):
    """Agrega la declaración del logger en la clase"""
    # Buscar el inicio de la clase
    class_pattern = rf'(@Configuration\s+)?public\s+class\s+{class_name}\s*{{'
    match = re.search(class_pattern, content)

    if match:
        insert_pos = match.end()
        logger_declaration = f'\n\n    private static final Logger log = LoggerFactory.getLogger({class_name}.class);'
        content = content[:insert_pos] + logger_declaration + content[insert_pos:]

    return content

def replace_system_out(content):
    """Reemplaza System.out.println() por log.info() o log.debug()"""

    # Patrones comunes de System.out.println
    patterns = [
        # System.out.println("texto");
        (r'System\.out\.println\("([^"]+)"\);', r'log.info("\1");'),

        # System.out.println("texto: " + variable);
        (r'System\.out\.println\("([^"]+):\s*"\s*\+\s*([^)]+)\);', r'log.info("\1: {}", \2);'),

        # System.out.println(variable);
        (r'System\.out\.println\(([^"]+)\);', r'log.info("{}",\1);'),
    ]

    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)

    return content

def replace_system_err(content):
    """Reemplaza System.err.println() por log.error()"""

    patterns = [
        # System.err.println("texto");
        (r'System\.err\.println\("([^"]+)"\);', r'log.error("\1");'),

        # System.err.println("texto: " + variable);
        (r'System\.err\.println\("([^"]+):\s*"\s*\+\s*([^)]+)\);', r'log.error("\1: {}", \2);'),

        # System.err.println(variable);
        (r'System\.err\.println\(([^"]+)\);', r'log.error("{}",\1);'),
    ]

    for pattern, replacement in patterns:
        content = re.sub(pattern, replacement, content)

    return content

def process_file(file_path, dry_run=False):
    """Procesa un archivo Java para migrar a SLF4J"""
    print(f"Procesando: {file_path}")

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # Verificar si el archivo tiene System.out o System.err
    if 'System.out' not in content and 'System.err' not in content:
        print(f"  ⏭️  Sin System.out/err - omitiendo")
        return False

    # Agregar imports si no existen
    if not has_slf4j_import(content):
        print(f"  ➕ Agregando imports de SLF4J")
        content = add_slf4j_import(content)

    # Agregar declaración del logger si no existe
    class_name = get_class_name(content)
    if class_name and not has_logger_declaration(content):
        print(f"  ➕ Agregando logger para clase {class_name}")
        content = add_logger_declaration(content, class_name)

    # Reemplazar System.out
    content = replace_system_out(content)

    # Reemplazar System.err
    content = replace_system_err(content)

    # Guardar si hay cambios
    if content != original_content:
        if not dry_run:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  ✅ Archivo actualizado")
        else:
            print(f"  🔍 [DRY-RUN] Se actualizaría")
        return True
    else:
        print(f"  ℹ️  Sin cambios necesarios")
        return False

def main():
    dry_run = '--dry-run' in sys.argv

    if dry_run:
        print("🔍 MODO DRY-RUN - No se modificarán archivos\n")

    # Buscar todos los archivos .java con System.out/err
    java_files = []
    for root, dirs, files in os.walk('.'):
        # Ignorar directorios de build
        if 'target' in root or '.git' in root:
            continue

        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                java_files.append(file_path)

    print(f"Encontrados {len(java_files)} archivos Java\n")

    # Procesar cada archivo
    updated_count = 0
    for file_path in java_files:
        if process_file(file_path, dry_run):
            updated_count += 1
        print()

    print(f"\n{'[DRY-RUN] ' if dry_run else ''}Archivos actualizados: {updated_count}/{len(java_files)}")

    if dry_run:
        print("\n💡 Ejecuta sin --dry-run para aplicar los cambios")

if __name__ == '__main__':
    main()
