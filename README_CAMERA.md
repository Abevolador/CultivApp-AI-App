# Cámara Personalizada - Cherry App

## Funcionalidades

### 🎯 Características Principales

1. **Vista Previa Cuadrada**: La cámara muestra una vista previa cuadrada en tiempo real
2. **Selector de Calidad**: 3 opciones de resolución (1MP, 2MP, 5MP)
3. **Destino de Foto**: Elegir entre guardar en galería o enviar al servidor
4. **Fotos Cuadradas**: Todas las fotos se procesan para ser cuadradas
5. **Interfaz Intuitiva**: Controles fáciles de usar en la parte inferior

### 📱 Cómo Usar

1. **Abrir la Cámara**:
   - Presiona "📸 Tomar foto" en el menú principal
   - Selecciona "📱 Cámara personalizada"

2. **Configurar Calidad**:
   - Toca el ícono de configuración (⚙️) en la barra superior
   - Selecciona la calidad deseada (1MP, 2MP, 5MP)

3. **Elegir Destino**:
   - **📱 Galería**: Guarda la foto en la galería de la app
   - **🌐 Servidor**: Envía la foto al servidor (simulado)

4. **Tomar Foto**:
   - Presiona el botón circular grande
   - La foto se procesa automáticamente

### 🔧 Especificaciones Técnicas

#### Resoluciones Disponibles:
- **1MP**: 1024x1024 píxeles
- **2MP**: 1448x1448 píxeles  
- **5MP**: 2304x2304 píxeles

#### Procesamiento de Imagen:
- Recorte automático a formato cuadrado
- Redimensionamiento a la resolución seleccionada
- Compresión JPEG con calidad 90%
- Optimización de memoria

### 🎨 Interfaz de Usuario

- **Vista Previa**: Cuadrada con bordes redondeados
- **Controles**: Panel semitransparente en la parte inferior
- **Indicadores**: Calidad actual y estado de captura
- **Feedback Visual**: Indicador de carga durante la captura

### 🔒 Permisos Requeridos

- `CAMERA`: Para acceder a la cámara del dispositivo
- `WRITE_EXTERNAL_STORAGE`: Para guardar fotos (Android < 29)

### 🚀 Próximas Mejoras

- [ ] Detección automática de resoluciones disponibles
- [ ] Filtros y efectos en tiempo real
- [ ] Modo nocturno para la interfaz
- [ ] Configuración de calidad por defecto
- [ ] Historial de fotos tomadas 