# Nueva Pantalla de Análisis de Plantas

## Descripción

Se ha implementado una nueva funcionalidad que incluye dos pantallas principales:

1. **Pantalla de Análisis con Barra de Carga**: Simula el proceso de análisis de IA
2. **Pantalla de Resultados**: Muestra los resultados del análisis de manera visual y organizada

## Características Implementadas

### Pantalla de Análisis (`AnalisisScreen`)

- **Barra de progreso animada**: Simula el proceso de análisis durante 3 segundos
- **Mensajes dinámicos**: Muestra diferentes etapas del análisis:
  - "Preparando imagen..."
  - "Analizando características..."
  - "Procesando con IA..."
  - "Evaluando salud de la planta..."
  - "Generando recomendaciones..."
- **Interfaz bloqueada**: Previene interacciones durante el análisis
- **Visualización de la imagen**: Muestra la imagen que se está analizando
- **Diseño moderno**: Gradientes y animaciones suaves

### Pantalla de Resultados (`ResultadosAnalisisScreen`)

- **Título principal**: "RESULTADOS DEL ANÁLISIS" en negrita
- **Imagen analizada**: Muestra la foto tomada en tamaño similar al preview
- **Estado de la planta**: Indica "Estado de la planta: Saludable"
- **Nivel de confianza**: Muestra "Confianza: 90%"
- **Sección de recomendaciones**: Lista tres recomendaciones específicas:
  1. "Continúa monitoreando tu planta para detectar cualquier cambio o signo de plagas."
  2. "Asegúrate de que la planta tenga suficiente luz solar y un riego adecuado para un crecimiento óptimo."
  3. "Aplica fertilizante según las indicaciones para asegurar una nutrición adecuada."

## Flujo de Usuario

1. El usuario toma una foto con la cámara
2. En la vista previa, presiona el botón "Analizar"
3. Se muestra la pantalla de análisis con barra de carga (3 segundos)
4. Al completarse, se muestra la pantalla de resultados
5. El usuario puede regresar usando el botón de flecha atrás

## Variables de Estado Agregadas

```kotlin
// Nuevas variables para la pantalla de análisis con barra de carga
var showAnalisisScreen by remember { mutableStateOf(false) }
var analisisProgress by remember { mutableStateOf(0f) }
var analisisMessage by remember { mutableStateOf("Iniciando análisis...") }
var imagenAnalizada by remember { mutableStateOf<File?>(null) }
```

## Funciones Composable Creadas

### `AnalisisScreen`
- Maneja la pantalla de análisis con barra de carga
- Parámetros: `progress`, `message`, `imagen`

### `ResultadosAnalisisScreen`
- Maneja la pantalla de resultados del análisis
- Parámetros: `imagen`, `onBack`

### `RecommendationItem`
- Componente reutilizable para mostrar recomendaciones
- Parámetros: `text`

## Características de Diseño

- **Responsive**: Se adapta a diferentes tamaños de pantalla
- **Material Design 3**: Utiliza los colores y componentes del tema
- **Accesibilidad**: Incluye contentDescription para lectores de pantalla
- **Navegación intuitiva**: Botones de regreso claros
- **Feedback visual**: Animaciones y transiciones suaves

## Integración

La nueva funcionalidad se integra perfectamente con el flujo existente:
- No interfiere con otras pantallas
- Mantiene la consistencia visual
- Utiliza el mismo sistema de navegación
- Preserva todas las funcionalidades existentes

## Notas Técnicas

- La simulación de análisis dura exactamente 3 segundos
- Se procesa la imagen con rotación aplicada
- Se maneja correctamente la memoria de bitmaps
- Incluye manejo de errores robusto
- Compatible con temas claro y oscuro
