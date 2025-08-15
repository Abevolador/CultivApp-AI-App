# Nueva Funcionalidad: API REST para CherryApp

## Resumen de Cambios

Se ha transformado completamente la funcionalidad del botón "📡 Cargar datos desde red" para implementar una integración completa con API REST que se comunica con MongoDB.

## Nuevas Características

### 1. Configuración de API REST
- **Pantalla de Configuración**: Nueva pantalla para configurar la URL base de la API
- **Validación de Conexión**: Health check automático para verificar que el servidor esté funcionando
- **Persistencia**: La configuración se guarda localmente usando DataStore

### 2. Selección de Plantas/Sensores
- **Lista Dinámica**: Obtiene plantas/sensores desde la API
- **Interfaz Intuitiva**: Cards seleccionables con información detallada
- **Manejo de Errores**: Indicadores de carga y mensajes de error claros

### 3. Gestión de Conjuntos de Datos
- **Filtrado por Planta**: Muestra solo los datos de la planta seleccionada
- **Metadatos**: Información de fecha y descripción de cada conjunto
- **Navegación Fluida**: Transición suave entre pantallas

### 4. Visualización de Gráficos
- **Gráficos Interactivos**: Visualización de datos de temperatura, humedad y luminosidad
- **Filtros**: Opciones para filtrar por período y parámetros
- **Diseño Responsivo**: Gráficos que se adaptan al tamaño de pantalla

## Flujo de Usuario

1. **Pantalla Principal** → Botón "⚙️ Configurar API del Huerto"
2. **Configuración API** → Ingresar URL base y validar conexión
3. **Selección Planta** → Elegir planta/sensor del huerto
4. **Lista de Datos** → Seleccionar conjunto de datos específico
5. **Gráficos** → Visualizar datos en gráficos interactivos

## Archivos Nuevos Creados

### Modelos de Datos
- `app/src/main/java/com/cucei/cherryapp/data/ApiModels.kt` - Modelos para respuestas de API
- `app/src/main/java/com/cucei/cherryapp/data/DataStoreRepository.kt` - Persistencia de configuración

### Red y API
- `app/src/main/java/com/cucei/cherryapp/network/ApiService.kt` - Interfaz de Retrofit
- `app/src/main/java/com/cucei/cherryapp/network/RetrofitClient.kt` - Configuración de Retrofit

### Pantallas UI
- `app/src/main/java/com/cucei/cherryapp/ui/ApiConfigScreen.kt` - Configuración de API
- `app/src/main/java/com/cucei/cherryapp/ui/PlantSelectionScreen.kt` - Selección de plantas
- `app/src/main/java/com/cucei/cherryapp/ui/DataListScreen.kt` - Lista de conjuntos de datos
- `app/src/main/java/com/cucei/cherryapp/ui/ChartsScreen.kt` - Visualización de gráficos

### Documentación
- `README_API_ENDPOINTS.md` - Documentación completa de endpoints
- `app/ejemplo_datos_api.json` - Datos de ejemplo para pruebas

## Dependencias Agregadas

```kotlin
// Retrofit para API REST
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// DataStore para persistencia
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Gráficos
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## Configuración del Servidor

Para que la aplicación funcione correctamente, necesitas implementar un servidor API REST con los siguientes endpoints:

### Endpoints Requeridos:
1. `GET /health` - Health check
2. `GET /plantas` - Lista de plantas/sensores
3. `GET /datos/{plantaId}` - Conjuntos de datos por planta
4. `GET /datos_detalle/{archivoId}` - Datos detallados para gráficos

Ver `README_API_ENDPOINTS.md` para documentación completa.

## Estructura de Base de Datos MongoDB

### Colecciones:
- `plantas` - Información de plantas/sensores
- `conjuntos_datos` - Metadatos de conjuntos de datos
- `registros_sensores` - Datos de sensores (temperatura, humedad, luminosidad)

## Características Técnicas

### Manejo de Errores
- Validación de conexión de red
- Manejo de errores HTTP (404, 500, etc.)
- Mensajes de error descriptivos
- Reintentos automáticos

### Performance
- Carga asíncrona con coroutines
- Caché de configuración local
- Timeouts configurables
- Logging de red para debugging

### UX/UI
- Indicadores de carga
- Estados de error claros
- Navegación intuitiva
- Diseño consistente con Material 3

## Pruebas

### Datos de Ejemplo
El archivo `app/ejemplo_datos_api.json` contiene datos de ejemplo que puedes usar para probar la aplicación.

### Servidor de Prueba
Puedes usar herramientas como:
- **JSON Server**: Para crear un servidor de prueba rápido
- **Mockoon**: Para simular endpoints de API
- **Postman**: Para probar endpoints

### Ejemplo con JSON Server:
```bash
# Instalar JSON Server
npm install -g json-server

# Crear archivo db.json con los datos de ejemplo
# Ejecutar servidor
json-server --watch db.json --port 5000
```

## Migración desde la Versión Anterior

### Cambios en MainActivity.kt:
- Eliminada lógica antigua de "cargar datos desde red"
- Agregadas nuevas pantallas al sealed class `Pantalla`
- Integrada navegación entre nuevas pantallas
- Mantenida funcionalidad existente (cámara, galería, análisis)

### Compatibilidad:
- Todas las funcionalidades existentes siguen funcionando
- No hay cambios en la estructura de archivos existentes
- La nueva funcionalidad es completamente independiente

## Próximos Pasos

1. **Implementar Servidor**: Crear el servidor API REST con MongoDB
2. **Pruebas**: Probar todos los endpoints con datos reales
3. **Optimización**: Mejorar performance y manejo de errores
4. **Gráficos Avanzados**: Implementar gráficos más sofisticados
5. **Filtros**: Agregar más opciones de filtrado y búsqueda

## Soporte

Para dudas o problemas:
1. Revisar `README_API_ENDPOINTS.md` para detalles de implementación
2. Verificar logs de red en Android Studio
3. Probar endpoints individualmente con Postman o curl
4. Revisar configuración de CORS en el servidor 