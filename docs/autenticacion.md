# Autenticación de MediCita

## Estado del módulo

Implementado y probado localmente:

- Registro público de pacientes.
- Inicio de sesión con correo y contraseña.
- Contraseñas almacenadas mediante BCrypt.
- Generación y validación de tokens JWT.
- Consulta del perfil del usuario autenticado.
- Cierre de sesión con revocación del token utilizado.
- Respuestas JSON para errores de validación y seguridad.
- Restricción de las rutas /api/usuarios y /api/usuarios/** al rol ADMIN.

La restricción de usuarios no implica que su CRUD esté implementado.

## Ejecución local

Requisitos: Java 21 y PostgreSQL.

Desde backend:

```powershell
.\mvnw.cmd spring-boot:run
```

La configuración local debe incluir la conexión a PostgreSQL y:

```properties
server.port=8085
app.jwt.secret=CLAVE_PRIVADA_BASE64
app.jwt.expiration-minutes=60
app.jwt.issuer=medicita
```

La clave JWT debe generarse con al menos 32 bytes aleatorios.
No utilizar literalmente el marcador CLAVE_PRIVADA_BASE64.

application.properties contiene configuración privada y no debe subirse
al repositorio. Utilizar application.properties.example como referencia.

URL local: http://localhost:8085

## Endpoints disponibles

| Método | Ruta | Acceso | Resultado correcto |
|---|---|---|---|
| GET | /api/salud | Público | 200 |
| POST | /api/auth/register | Público | 201 |
| POST | /api/auth/login | Público | 200 |
| GET | /api/perfil | JWT válido | 200 |
| POST | /api/auth/logout | JWT válido | 200 |

## Registro

POST /api/auth/register

```json
{
  "nombre": "Paciente de prueba",
  "email": "paciente@example.com",
  "telefono": "+525500000000",
  "password": "Paciente#2026"
}
```

Datos ficticios para pruebas; el registro todavía no envía mensajes.

Reglas:

- Nombre obligatorio, máximo 120 caracteres.
- Correo obligatorio, formato válido y máximo 254 caracteres.
- El correo se guarda en minúsculas y sin espacios exteriores.
- El correo debe ser único, sin distinguir mayúsculas.
- Teléfono con + y entre 8 y 15 dígitos; el primero no puede ser cero.
- Contraseña de al menos 8 caracteres, con mayúscula, número y símbolo.
- Contraseña de máximo 72 bytes UTF-8.
- La contraseña no se recorta ni se transforma.
- El backend asigna siempre el rol PACIENTE y activa la cuenta.

Respuesta 201: datos públicos del usuario, sin contraseña ni hash.
El registro no inicia sesión automáticamente.

Correo duplicado: 422.
Datos inválidos: 400.

## Login

POST /api/auth/login

```json
{
  "email": "paciente@example.com",
  "password": "Paciente#2026"
}
```

Respuesta 200:

```json
{
  "token": "JWT_GENERADO",
  "tipo": "Bearer",
  "expiraEn": "FECHA_ISO_8601",
  "usuario": {
    "id": 13,
    "nombre": "Paciente de prueba",
    "email": "paciente@example.com",
    "telefono": "+525500000000",
    "avatarUrl": null,
    "rol": "PACIENTE",
    "activo": true
  }
}
```

Los valores son ilustrativos; el ID y la expiración dependen del registro
y del momento de inicio de sesión.

Credenciales incorrectas o cuenta deshabilitada: 401.

## Perfil

GET /api/perfil

Enviar el encabezado:

```text
Authorization: Bearer JWT_GENERADO
```

El usuario se identifica mediante el token. No se recibe un ID del cliente.

La respuesta contiene los datos públicos del usuario autenticado.

## Logout

POST /api/auth/logout

Enviar Authorization con el token. No requiere cuerpo.

Respuesta:

```json
{
  "message": "Sesion cerrada correctamente"
}
```

El backend guarda el identificador jti y la expiración en tokens_revocados.

Reutilizar ese token devuelve 401, aunque todavía no haya vencido.
Otros tokens del mismo usuario no se revocan mediante esta operación.

El frontend debe eliminar su copia del token al cerrar sesión.

## Seguridad y errores

El filtro comprueba:

- Firma, expiración y claims requeridos del JWT.
- Ausencia del jti en tokens_revocados.
- Existencia y estado activo del usuario.

Los permisos se obtienen del rol actual en PostgreSQL.

Sin token, las rutas protegidas responden 401.
Un encabezado Authorization inválido se rechaza con 401, incluso en rutas
públicas; evitar enviar tokens antiguos en login y registro.

Formato de error:

```json
{
  "timestamp": "FECHA_ISO_8601",
  "status": 400,
  "message": "Datos invalidos",
  "path": "/api/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "message": "El correo no tiene un formato valido"
    }
  ]
}
```

| Código | Significado |
|---|---|
| 400 | Datos o JSON inválidos |
| 401 | Autenticación ausente o inválida |
| 403 | Acceso prohibido |
| 422 | Correo duplicado |
| 500 | Error interno; detalles no expuestos al cliente |

## Integración con el frontend

- Consumir los endpoints anteriores y respetar los nombres de sus campos.
- Enviar el JWT en Authorization para perfil y logout.
- Usar fieldErrors para mostrar errores junto a los campos del formulario.
- Ante un 401 en una ruta protegida, solicitar un nuevo inicio de sesión.
- Un 403 indica falta de permisos; no debe interpretarse como token vencido.
- Ocultar opciones por rol en la interfaz no sustituye la seguridad del backend.

Pendiente: configurar y verificar CORS o un proxy de Vite para las
peticiones del navegador entre localhost:5173 y localhost:8085.
Las pruebas con PowerShell no verifican CORS.

## Verificación realizada

Pruebas manuales:

- Login correcto y rechazo de contraseña incorrecta.
- Salud pública disponible.
- Perfil accesible con JWT válido.
- Perfil rechazado sin token o con token falso.
- Logout y rechazo posterior del token revocado.
- Registro exitoso con rol PACIENTE.
- Rechazo de correo duplicado.
- Rechazo de datos inválidos.
- Login y perfil del paciente recién registrado.

Pruebas automáticas:

```powershell
cd backend
.\mvnw.cmd test
```

Resultado confirmado: 8 pruebas, 0 fallos, 0 errores y 0 omitidas.

Las pruebas de roles utilizan usuarios simulados y un controlador exclusivo
de pruebas. No sustituyen las pruebas de JWT ni implementan el CRUD de usuarios.

## Colección Bruno

Abrir la carpeta bruno como colección y seleccionar el entorno Local.

Con el backend encendido, ejecutar en orden:

1. Salud: 200.
2. Registro: 201.
3. Login: 200.
4. Perfil: 200.
5. Logout: 200.
6. Perfil con token revocado: 401.

Recorrido verificado localmente.

Cada ejecución completa crea un paciente ficticio nuevo.
El login conserva el JWT como variable temporal para las peticiones siguientes.
No guardar tokens reales en los archivos de la colección.





## Pendientes

- Integración y prueba desde el frontend.
- Permisos específicos de citas, servicios y horarios al implementar sus rutas.
- Recuperación de contraseña.
- Despliegue y configuración del entorno del VPS.