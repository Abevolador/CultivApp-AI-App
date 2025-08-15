# Endpoints de API REST para CherryApp

Esta documentación describe los endpoints que necesitas implementar en tu servidor API REST para que funcione correctamente con la aplicación CherryApp.

## Base URL
La aplicación permite configurar una URL base (ej: `http://192.168.1.100:5000/api`)

## Endpoints Requeridos

### 1. Health Check
**GET** `/health`

Endpoint para validar que el servidor está funcionando correctamente.

**Respuesta esperada:**
```json
{
  "status": "ok",
  "message": "Servidor funcionando correctamente"
}
```

**Códigos de respuesta:**
- `200 OK`: Servidor funcionando
- `500 Internal Server Error`: Error del servidor

---

### 2. Listar Plantas/Sensores
**GET** `/plantas`

Obtiene la lista de todas las plantas o sensores disponibles en el huerto.

**Respuesta esperada:**
```json
[
  {
    "id": "sensor_001",
    "nombre": "Tomate Cherry 1",
    "descripcion": "Sensor de temperatura y humedad para tomate cherry"
  },
  {
    "id": "sensor_002", 
    "nombre": "Menta Piperita",
    "descripcion": "Sensor para planta de menta"
  }
]
```

**Códigos de respuesta:**
- `200 OK`: Lista de plantas obtenida
- `404 Not Found`: No se encontraron plantas
- `500 Internal Server Error`: Error del servidor

---

### 3. Listar Conjuntos de Datos por Planta
**GET** `/datos/{plantaId}`

Obtiene la lista de conjuntos de datos disponibles para una planta específica.

**Parámetros de ruta:**
- `plantaId` (string): ID de la planta/sensor

**Respuesta esperada:**
```json
[
  {
    "archivoId": "datos_2023_10_26",
    "fecha": "2023-10-26",
    "descripcion": "Datos del 26 de Octubre",
    "plantaId": "sensor_001"
  },
  {
    "archivoId": "datos_2023_10_25",
    "fecha": "2023-10-25", 
    "descripcion": "Datos del 25 de Octubre",
    "plantaId": "sensor_001"
  }
]
```

**Códigos de respuesta:**
- `200 OK`: Lista de conjuntos de datos obtenida
- `404 Not Found`: Planta no encontrada o sin datos
- `500 Internal Server Error`: Error del servidor

---

### 4. Obtener Datos Detallados
**GET** `/datos_detalle/{archivoId}`

Obtiene los datos detallados de un conjunto específico para generar gráficos.

**Parámetros de ruta:**
- `archivoId` (string): ID del conjunto de datos

**Respuesta esperada:**
```json
{
  "plantaId": "sensor_001",
  "archivoId": "datos_2023_10_26",
  "registros": [
    {
      "fecha": "2023-10-26T08:00:00",
      "temperatura": 25.5,
      "humedad": 65.2,
      "luminosidad": 1200.0,
      "timestamp": 1698314400000
    },
    {
      "fecha": "2023-10-26T09:00:00", 
      "temperatura": 26.1,
      "humedad": 63.8,
      "luminosidad": 1350.0,
      "timestamp": 1698318000000
    }
  ]
}
```

**Códigos de respuesta:**
- `200 OK`: Datos detallados obtenidos
- `404 Not Found`: Conjunto de datos no encontrado
- `500 Internal Server Error`: Error del servidor

---

### 5. Obtener Datos Detallados con Filtros (Opcional)
**GET** `/datos_detalle`

Versión alternativa del endpoint anterior que permite filtros adicionales.

**Parámetros de consulta:**
- `plantaId` (string, requerido): ID de la planta
- `archivoId` (string, requerido): ID del conjunto de datos
- `dias` (integer, opcional): Número de días a filtrar

**Ejemplo de URL:**
```
GET /datos_detalle?plantaId=sensor_001&archivoId=datos_2023_10_26&dias=7
```

**Respuesta:** Igual que el endpoint anterior.

---

## Estructura de Base de Datos MongoDB

### Colección: `plantas`
```javascript
{
  "_id": ObjectId,
  "id": "sensor_001",
  "nombre": "Tomate Cherry 1", 
  "descripcion": "Sensor de temperatura y humedad para tomate cherry",
  "fecha_creacion": ISODate("2023-10-01T00:00:00Z")
}
```

### Colección: `conjuntos_datos`
```javascript
{
  "_id": ObjectId,
  "archivoId": "datos_2023_10_26",
  "fecha": "2023-10-26",
  "descripcion": "Datos del 26 de Octubre",
  "plantaId": "sensor_001",
  "fecha_creacion": ISODate("2023-10-26T00:00:00Z")
}
```

### Colección: `registros_sensores`
```javascript
{
  "_id": ObjectId,
  "plantaId": "sensor_001",
  "archivoId": "datos_2023_10_26",
  "fecha": "2023-10-26T08:00:00",
  "temperatura": 25.5,
  "humedad": 65.2,
  "luminosidad": 1200.0,
  "timestamp": 1698314400000,
  "fecha_creacion": ISODate("2023-10-26T08:00:00Z")
}
```

## Consideraciones de Implementación

### 1. Manejo de Errores
- Siempre devuelve códigos HTTP apropiados
- Incluye mensajes de error descriptivos en el cuerpo de la respuesta
- Maneja excepciones de base de datos

### 2. CORS
Si tu servidor está en un dominio diferente, asegúrate de configurar CORS:
```javascript
// Ejemplo con Express.js
app.use(cors({
  origin: '*', // O especifica el dominio de tu app
  methods: ['GET', 'POST', 'PUT', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
```

### 3. Validación
- Valida que los IDs de plantas existan antes de devolver datos
- Valida el formato de fechas
- Maneja casos donde no hay datos

### 4. Performance
- Usa índices en MongoDB para consultas frecuentes
- Considera paginación para grandes conjuntos de datos
- Implementa caché si es necesario

### 5. Seguridad
- Valida y sanitiza todas las entradas
- Considera autenticación si es necesario
- Limita el tamaño de las respuestas

## Ejemplo de Implementación con Node.js/Express

```javascript
const express = require('express');
const { MongoClient } = require('mongodb');
const app = express();

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', message: 'Servidor funcionando correctamente' });
});

// Listar plantas
app.get('/plantas', async (req, res) => {
  try {
    const plantas = await db.collection('plantas').find({}).toArray();
    res.json(plantas);
  } catch (error) {
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// Listar conjuntos de datos por planta
app.get('/datos/:plantaId', async (req, res) => {
  try {
    const { plantaId } = req.params;
    const conjuntos = await db.collection('conjuntos_datos')
      .find({ plantaId })
      .sort({ fecha: -1 })
      .toArray();
    res.json(conjuntos);
  } catch (error) {
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

// Obtener datos detallados
app.get('/datos_detalle/:archivoId', async (req, res) => {
  try {
    const { archivoId } = req.params;
    const registros = await db.collection('registros_sensores')
      .find({ archivoId })
      .sort({ timestamp: 1 })
      .toArray();
    
    const conjunto = await db.collection('conjuntos_datos')
      .findOne({ archivoId });
    
    res.json({
      plantaId: conjunto.plantaId,
      archivoId: conjunto.archivoId,
      registros: registros
    });
  } catch (error) {
    res.status(500).json({ error: 'Error interno del servidor' });
  }
});

app.listen(5000, () => {
  console.log('Servidor corriendo en puerto 5000');
});
```

## Pruebas

Puedes probar los endpoints usando curl o Postman:

```bash
# Health check
curl http://localhost:5000/api/health

# Listar plantas
curl http://localhost:5000/api/plantas

# Listar datos de una planta
curl http://localhost:5000/api/datos/sensor_001

# Obtener datos detallados
curl http://localhost:5000/api/datos_detalle/datos_2023_10_26
``` 