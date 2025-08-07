# Funcionalidades de Cámara y Análisis de Plantas - Cherry App

## Nuevas Funcionalidades Implementadas

### 1. Pantalla de Análisis de Plantas
- **Acceso**: Presiona "📸 Tomar foto" en la pantalla principal
- **Funcionalidades**:
  - "📸 Abrir Cámara Personalizada": Abre la cámara mejorada
  - "🖼️ Ver Galería de la App": Navega a la galería interna

### 2. Cámara Personalizada Mejorada
- **Selección de Resolución**: Dropdown con opciones:
  - VGA (640x480)
  - 1MP (1280x720)
  - 2MP (1920x1080)
- **Vista Previa Post-Captura**: Nueva pantalla con opciones:
  - "Guardar en Galería del Dispositivo"
  - "Eliminar y Reintentar"
  - "Mandar a Analizar"

### 3. Análisis de Plantas
- **Envío al Servidor**: URL: `http://192.168.1.100:5000/analizar_planta`
- **Manejo de Errores**: Si falla la conexión:
  - Muestra mensaje de error
  - Ofrece guardar foto en galería de la app para análisis posterior
- **Visualización de Resultados**: Nueva pantalla para mostrar JSON del análisis

### 4. Galería Mejorada
- **Botón "Enviar a Analizar"**: En vista completa de cada foto
- **Análisis Directo**: Envía foto al servidor sin pasar por cámara
- **Indicador de Carga**: Durante el proceso de análisis

### 5. Carga de JSON Local Mejorada
- **Manejo Robusto**: Soporta múltiples formatos:
  - Array de objetos: `[{...}, {...}]`
  - Objeto con array: `{"datos": [{...}, {...}]}`
  - Objeto simple: `{"temperatura": "...", "humedad": "...", "luminosidad": "..."}`
- **Mejor Manejo de Errores**: Logs detallados para depuración

## Flujo de Uso

### Análisis de Nueva Planta
1. Presiona "📸 Tomar foto" → Pantalla de Análisis
2. Presiona "📸 Abrir Cámara Personalizada"
3. Selecciona resolución deseada
4. Toma la foto
5. En vista previa, elige:
   - **Guardar**: En galería del dispositivo
   - **Reintentar**: Volver a cámara
   - **Analizar**: Enviar al servidor

### Análisis de Foto Existente
1. Ve a "🖼️ Ver galería de fotos"
2. Selecciona una foto
3. Presiona el botón "Enviar a Analizar" (ícono de envío)
4. Visualiza resultados del análisis

### Carga de Datos Locales
1. Presiona "📂 Abrir archivo JSON local"
2. Selecciona archivo JSON
3. Los datos se muestran en pantalla dedicada

## Permisos Requeridos
- `CAMERA`: Para usar la cámara
- `READ_EXTERNAL_STORAGE`: Para acceder a archivos (Android < 13)
- `WRITE_EXTERNAL_STORAGE`: Para guardar en galería (Android < 29)
- `READ_MEDIA_IMAGES`: Para acceder a galería (Android 13+)

## Estructura de Archivos
- `MainActivity.kt`: Lógica principal y navegación
- `CameraScreen.kt`: Cámara personalizada mejorada
- `ejemplo_datos.json`: Archivo de ejemplo para pruebas

## Notas Técnicas
- **Resoluciones**: Configuradas para diferentes calidades de imagen
- **Manejo de Errores**: Snackbars para feedback al usuario
- **Navegación**: Sistema basado en estados con BackHandler
- **Almacenamiento**: Fotos guardadas en directorio interno de la app
- **Servidor**: Endpoint configurado para recibir imágenes JPEG 