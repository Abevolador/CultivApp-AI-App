# Cherry App - Solución de Problemas

## 🔧 Problemas de Ejecución

### Problema: La aplicación no se ejecuta en el emulador o dispositivo

#### Soluciones:

### 1. **Verificar Compilación**
```bash
./gradlew assembleDebug
```
- ✅ Si compila sin errores, el problema no es de código
- ❌ Si hay errores, revisar los logs y corregir

### 2. **Verificar Dispositivos Conectados**
```bash
adb devices
```
- Debe mostrar dispositivos conectados
- Si no hay dispositivos, conectar emulador o dispositivo físico

### 3. **Instalar APK Manualmente**
```bash
# Generar APK
./gradlew assembleDebug

# Instalar en dispositivo conectado
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. **Limpiar y Reconstruir**
```bash
./gradlew clean
./gradlew assembleDebug
```

### 5. **Verificar Permisos**
- La aplicación requiere permisos de cámara y almacenamiento
- Asegurarse de que el dispositivo/emulador tenga cámara habilitada

## 🚀 Instrucciones de Prueba

### Prueba Básica:
1. **Instalar la aplicación**
2. **Abrir la app** - Debe mostrar la pantalla principal con:
   - 🍒 Cherry (título)
   - 📂 Abrir archivo JSON local
   - 📡 Cargar datos desde red
   - 📸 Tomar foto
   - 🖼️ Ver galería de fotos

### Prueba de Funcionalidades:

#### 📂 Abrir archivo JSON local
- Toca el botón
- Selecciona un archivo JSON
- Debe mostrar los datos en una nueva pantalla

#### 📸 Tomar foto
- Toca el botón
- Selecciona "Personalizada" o "Nativa del Sistema"
- Toma una foto
- Guarda en la galería de la app

#### 🖼️ Ver galería de fotos
- Toca el botón
- Debe mostrar las fotos guardadas
- Toca una foto para verla en pantalla completa

## 🔍 Logs de Depuración

### Ver logs en tiempo real:
```bash
adb logcat | grep "CherryApp"
```

### Ver logs de errores:
```bash
adb logcat | grep -E "(FATAL|ERROR)"
```

## 📱 Configuración del Emulador

### Emulador Recomendado:
- **API Level**: 26 o superior
- **RAM**: 2GB o más
- **Cámara**: Habilitada
- **Almacenamiento**: 2GB o más

### Configuración de Cámara:
1. Abrir AVD Manager
2. Editar el emulador
3. En "Advanced Settings"
4. Habilitar "Camera" (Front y Back)

## 🛠️ Problemas Comunes

### 1. **App se cierra inmediatamente**
- Verificar logs con `adb logcat`
- Revisar permisos en el dispositivo
- Verificar que el emulador tenga cámara habilitada

### 2. **No se pueden tomar fotos**
- Verificar permisos de cámara
- Asegurarse de que el emulador tenga cámara configurada
- Probar con dispositivo físico

### 3. **No se pueden abrir archivos JSON**
- Verificar permisos de almacenamiento
- Asegurarse de que el archivo JSON sea válido
- Probar con archivos JSON simples

### 4. **Error de red**
- Verificar conexión a internet
- El endpoint `http://192.168.1.100:5000/datos` debe estar disponible
- O modificar la URL en el código

## 📋 Checklist de Verificación

### Antes de Probar:
- [ ] Proyecto compila sin errores
- [ ] Dispositivo/emulador conectado
- [ ] Permisos de cámara concedidos
- [ ] Permisos de almacenamiento concedidos
- [ ] Emulador con cámara habilitada

### Durante la Prueba:
- [ ] App se abre correctamente
- [ ] Pantalla principal se muestra
- [ ] Botones responden a toques
- [ ] Navegación entre pantallas funciona
- [ ] Funcionalidades básicas operan

### Después de la Prueba:
- [ ] Verificar logs sin errores críticos
- [ ] Confirmar que las funcionalidades principales funcionan
- [ ] Documentar cualquier problema encontrado

## 🆘 Contacto

Si los problemas persisten:
1. Revisar logs completos
2. Probar en dispositivo físico
3. Verificar configuración del entorno de desarrollo
4. Revisar versiones de Android Studio y SDK

## 📝 Notas Importantes

- La aplicación está diseñada para Android API 26+
- Requiere permisos de cámara y almacenamiento
- Funciona mejor en dispositivos físicos que en emuladores
- La funcionalidad de red requiere un servidor en `192.168.1.100:5000`

¡La aplicación debería funcionar correctamente siguiendo estas instrucciones! 🍒 