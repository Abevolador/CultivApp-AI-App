# Cherry App - Funcionalidades de Conexión a Base de Datos

## Nueva Pantalla de Conexión de Red

La aplicación Cherry ahora incluye una nueva pantalla para conectarse a bases de datos y servidores REST. Esta funcionalidad está diseñada para trabajar con MongoDB y otros servidores que proporcionen datos JSON.

### Características Principales

#### 1. **Conexión a Servidor**
- Campo de entrada para URL del servidor
- Historial de URLs utilizadas anteriormente
- Validación de URL antes de intentar conexión
- Manejo de errores de conexión

#### 2. **Carga de Datos JSON**
- **JSON Actual**: Carga el archivo JSON más reciente del servidor
- **Historial de Archivos**: Muestra una lista de todos los archivos JSON disponibles
- Organización por fecha (más reciente primero)
- Visualización de datos en formato de tarjetas

#### 3. **Visualización de Datos**
- Lista de registros con información de:
  - 🌡️ Temperatura
  - 💧 Humedad  
  - 🔆 Luminosidad
- Navegación intuitiva entre pantallas
- Estados de carga y error

#### 4. **Gráficos (En Desarrollo)**
- Selector de parámetros para graficar
- Gráficos de línea temporales
- Interfaz para elegir entre temperatura, humedad y luminosidad
- *Nota: Los gráficos están temporalmente deshabilitados hasta configurar completamente Vico*

### Cómo Usar

#### Paso 1: Acceder a la Pantalla de Conexión
1. Abre la aplicación Cherry
2. Toca el botón "📡 Cargar datos desde red"
3. Se abrirá la nueva pantalla de conexión

#### Paso 2: Conectar al Servidor
1. Ingresa la URL del servidor (ej: `http://192.168.1.100:5000`)
2. Elige una de las opciones:
   - **"Cargar JSON Actual"**: Para obtener el archivo más reciente
   - **"Ver Historial de Archivos"**: Para ver todos los archivos disponibles

#### Paso 3: Ver Datos
- Los datos se mostrarán en formato de tarjetas
- Cada registro muestra temperatura, humedad y luminosidad
- Puedes navegar entre diferentes archivos del historial

#### Paso 4: Gráficos (Próximamente)
- Toca el icono de gráfico en la pantalla de datos
- Selecciona el parámetro que quieres graficar
- Visualiza la evolución temporal de los datos

### Estructura de Datos Esperada

El servidor debe proporcionar datos en el siguiente formato JSON:

```json
[
  {
    "temperatura": "25°C",
    "humedad": "60%",
    "luminosidad": "80%"
  },
  {
    "temperatura": "26°C", 
    "humedad": "58%",
    "luminosidad": "85%"
  }
]
```

### Endpoints del Servidor

La aplicación espera los siguientes endpoints:

1. **JSON Actual**: `GET /datos` (o la URL completa que proporciones)
2. **Historial de Archivos**: `GET /historial`
3. **Archivo Específico**: `GET /archivo/{id}`

### Historial de URLs

La aplicación guarda automáticamente las URLs utilizadas para facilitar futuras conexiones. El historial se muestra en la pantalla de conexión y se puede seleccionar con un toque.

### Estados de la Aplicación

- **Conexión**: Pantalla principal para ingresar URL
- **Historial**: Lista de archivos disponibles
- **Visualización de Datos**: Muestra los registros JSON
- **Gráficos**: Diálogo para visualizar datos gráficamente

### Próximas Mejoras

1. **Gráficos Completos**: Integración completa con Vico para gráficos interactivos
2. **Actualización Automática**: Polling automático de datos nuevos
3. **Filtros**: Filtrar datos por fecha o rango
4. **Exportación**: Exportar datos a CSV o PDF
5. **Notificaciones**: Alertas cuando los valores superen umbrales

### Notas Técnicas

- La aplicación usa Retrofit para las llamadas HTTP
- Los datos se almacenan temporalmente en memoria
- El historial de URLs se guarda en SharedPreferences
- Manejo de errores de red y timeouts
- Interfaz responsive con Material Design 3

### Dependencias Agregadas

```gradle
// Retrofit para llamadas HTTP
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// Vico para gráficos (en configuración)
implementation("com.patrykandpatrick.vico:compose:1.13.1")
implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
implementation("com.patrykandpatrick.vico:core:1.13.1")
```

### Archivos Principales

- `NetworkConnectionScreen.kt`: Pantalla principal de conexión
- `NetworkViewModel.kt`: Lógica de negocio y estados
- `ApiService.kt`: Interfaz para llamadas HTTP
- `MainActivity.kt`: Integración con la aplicación principal 