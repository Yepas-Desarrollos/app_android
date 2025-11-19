#!/bin/bash
# Script de verificación de errores de Hilt

echo "🔍 Verificando errores de compilación..."
echo ""

# Lista de archivos críticos a verificar
FILES=(
    "app/src/main/java/mx/checklist/MainActivity.kt"
    "app/src/main/java/mx/checklist/ui/AppNavHost.kt"
    "app/src/main/java/mx/checklist/ui/vm/AuthViewModel.kt"
    "app/src/main/java/mx/checklist/ui/vm/RunsViewModel.kt"
    "app/src/main/java/mx/checklist/ui/vm/AdminViewModel.kt"
    "app/src/main/java/mx/checklist/ui/vm/AssignmentViewModel.kt"
    "app/src/main/java/mx/checklist/ui/vm/ChecklistStructureViewModel.kt"
)

echo "📋 Archivos críticos a verificar:"
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file (NO ENCONTRADO)"
    fi
done

echo ""
echo "🏗️  Compilando proyecto..."
./gradlew compileDebugKotlin

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ¡COMPILACIÓN EXITOSA!"
    echo ""
    echo "📊 Resumen de cambios:"
    echo "  - Dependencia: androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03"
    echo "  - Importación: androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel"
    echo "  - Sintaxis: hiltViewModel<TuViewModel>()"
else
    echo ""
    echo "❌ La compilación falló. Revisa los errores arriba."
    exit 1
fi

