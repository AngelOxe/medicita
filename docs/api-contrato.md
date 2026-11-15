# Contrato de API — MediCita

Estado: propuesta inicial para coordinar backend y frontend.
Solo GET /api/salud está implementado y verificado.
Las demás rutas describen el comportamiento que se implementará.

## 1. Alcance

- Un consultorio y un solo médico.
- Roles: ADMIN, MEDICO y PACIENTE.
- El paciente selecciona servicio, fecha y hora; no selecciona médico.
- Una reserva válida se confirma automáticamente.
- No se permiten empalmes entre citas confirmadas.
- El paciente puede cancelar sus propias citas si faltan al menos 2 horas.
- ADMIN y MEDICO pueden crear citas para un paciente.
- Sin reprogramación, pagos, expediente clínico ni recetas.

## 2. Convenciones

Base local de Ángel: http://localhost:8085/api
Base del VPS: pendiente de configurar.

Content-Type para cuerpos JSON: application/json

Las rutas protegidas recibirán:
Authorization: Bearer <token>

Roles en mayúsculas: ADMIN, MEDICO, PACIENTE.
Estados de cita en minúsculas:
confirmada, completada, cancelada, no_asistio.

Fechas: YYYY-MM-DD.
Horas de horarios semanales: HH:mm:ss.
Instantes: ISO 8601 con desplazamiento, por ejemplo:
2026-11-23T09:00:00-06:00.

Zona de negocio: America/Mexico_City.
Los instantes pueden devolverse con otro desplazamiento equivalente;
el frontend los mostrará en la zona del consultorio.

Los identificadores son números, salvo jti, que es una cadena.
No devolver entidades JPA directamente ni incluir passwordHash en respuestas.

## 3. Salud

GET /salud
Acceso: público.
Respuesta 200:

{
  "estado": "OK",
  "aplicacion": "MediCita"
}

Comprueba que la API responde; no prueba todos los servicios externos.

## 4. Autenticación

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| POST | /auth/register | Público | 201, usuario creado |
| POST | /auth/login | Público | 200, token y usuario |
| POST | /auth/logout | Autenticado | 200, token invalidado |
| POST | /auth/forgot-password | Público | 200, mensaje genérico |
| POST | /auth/reset-password | Público | 200, contraseña actualizada |

Registro: siempre crea un PACIENTE. No acepta rol enviado por el cliente.

Cuerpo de register:
{
  "nombre": "Paciente de prueba",
  "email": "paciente@example.com",
  "telefono": "+525512345678",
  "password": "Ejemplo#2026"
}

Los datos del ejemplo son ilustrativos; no enviar mensajes a ese número.

Contraseña: mínimo 8 caracteres, una mayúscula, un número
y un carácter especial. Validar en frontend y backend.
Teléfono: formato internacional, signo + y entre 8 y 15 dígitos.

Cuerpo de login:
{
  "email": "admin@medicita.test",
  "password": "Admin#2026"
}

Respuesta de login:
{
  "token": "<jwt>",
  "tipo": "Bearer",
  "expiraEn": "<instante ISO 8601>",
  "usuario": {
    "id": 1,
    "nombre": "Administrador MediCita",
    "email": "admin@medicita.test",
    "telefono": "0000000000",
    "avatarUrl": null,
    "rol": "ADMIN",
    "activo": true
  }
}

El teléfono 0000000000 pertenece a los seeds y no sirve para envíos.
Antes de probar notificaciones, actualizarlo con un número autorizado.

Registro devuelve el usuario creado; luego se inicia sesión mediante login.
Logout no requiere cuerpo; utiliza el JWT del encabezado.
Forgot-password recibe: {"email":"paciente@example.com"}.
Su respuesta no debe revelar si el correo existe.
Reset-password recibe: {"token":"<token>","password":"Nueva#2026"}.

Respuesta de mensaje:
{"message":"Operación realizada"}

## 5. Perfil

GET /perfil
PUT /perfil
Acceso: cualquier usuario autenticado, solo su propio perfil.

PUT recibe:
{
  "nombre": "Nombre actualizado",
  "telefono": "+525512345678"
}

Respuesta 200: mismo formato de usuario de la respuesta de login.
No permite cambiar rol, estado, contraseña ni correo mediante este endpoint.
El avatar puede ser null; el frontend muestra iniciales por defecto.

## 6. Servicios

| Método | Ruta | Acceso |
|---|---|---|
| GET | /servicios | Autenticados; paciente solo ve activos |
| GET | /servicios/{id} | Autenticados; paciente solo ve activos |
| POST | /servicios | ADMIN y MEDICO |
| PUT | /servicios/{id} | ADMIN y MEDICO |
| DELETE | /servicios/{id} | ADMIN y MEDICO |

Listado:
GET /servicios?page=0&size=10&search=consulta&activo=true

POST/PUT reciben:
{
  "nombre": "Consulta general",
  "descripcion": "Consulta de medicina general",
  "duracionMin": 30,
  "precio": 400.00,
  "activo": true
}

Respuesta: los mismos campos más id.
POST devuelve 201; PUT y GET de detalle, 200.
DELETE desactiva el servicio y devuelve 204 sin cuerpo.
No elimina citas históricas.
Un paciente no puede consultar servicios inactivos.

## 7. Horarios y bloqueos

Acceso: ADMIN y MEDICO.

| Método | Ruta |
|---|---|
| GET | /horarios |
| GET | /horarios/{id} |
| POST | /horarios |
| PUT | /horarios/{id} |
| DELETE | /horarios/{id} |
| GET | /bloqueos |
| GET | /bloqueos/{id} |
| POST | /bloqueos |
| PUT | /bloqueos/{id} |
| DELETE | /bloqueos/{id} |

Listado de horarios:
GET /horarios?page=0&size=10&diaSemana=1&activo=true

POST/PUT de horario:
{
  "diaSemana": 1,
  "horaInicio": "09:00:00",
  "horaFin": "13:00:00",
  "activo": true,
  "servicioIds": [1, 2]
}

Día: 1=lunes hasta 7=domingo.
La hora final debe ser posterior a la inicial, dentro del mismo día.
Respuesta: id, diaSemana, horaInicio, horaFin, activo y servicios.
servicios es una lista de objetos con id y nombre.

Listado de bloqueos:
GET /bloqueos?page=0&size=10&desde=2026-11-23&hasta=2026-11-30

desde y hasta son fechas inclusivas en la zona del consultorio.
Devolver bloqueos que se crucen con ese intervalo de días.

POST/PUT de bloqueo:
{
  "inicio": "2026-11-23T12:00:00-06:00",
  "fin": "2026-11-23T13:00:00-06:00",
  "motivo": "Ausencia del médico"
}

Respuesta: los mismos campos más id.
POST devuelve 201; GET de detalle y PUT, 200.
DELETE de horario desactiva; DELETE de bloqueo elimina.
Ambos devuelven 204 sin cuerpo.

No permitir cambios de agenda que dejen citas futuras confirmadas
sin cobertura de horario/servicio o dentro de un bloqueo: devolver 422.
Cancelar primero las citas afectadas cuando corresponda.

## 8. Disponibilidad

GET /disponibilidad?servicioId=1&fecha=2026-11-23
Acceso: cualquier usuario autenticado.

Respuesta 200:
{
  "fecha": "2026-11-23",
  "servicioId": 1,
  "zonaHoraria": "America/Mexico_City",
  "slots": [
    {
      "inicio": "2026-11-23T09:00:00-06:00",
      "fin": "2026-11-23T09:30:00-06:00"
    }
  ]
}

Sin disponibilidad: slots será [].
Solo devolver intervalos futuros que:
- correspondan a un horario activo que ofrezca el servicio;
- permitan completar la duración del servicio;
- no se crucen con bloqueos ni citas confirmadas.

El frontend envía exactamente el inicio elegido.
El backend vuelve a validar la disponibilidad al reservar.

## 9. Citas

| Método | Ruta | Acceso |
|---|---|---|
| POST | /citas | PACIENTE para sí; ADMIN/MEDICO para un paciente |
| GET | /citas/mias | PACIENTE |
| GET | /citas | ADMIN y MEDICO |
| GET | /citas/{id} | Propietario, ADMIN o MEDICO |
| PATCH | /citas/{id}/cancelar | Propietario, ADMIN o MEDICO |
| PATCH | /citas/{id}/estado | ADMIN y MEDICO |

Reserva del paciente:
{
  "servicioId": 1,
  "inicio": "2026-11-23T09:00:00-06:00"
}

ADMIN/MEDICO agregan pacienteId al cuerpo.
Un paciente no puede indicar otro pacienteId.
El backend calcula fin, precioReservado y estado; no los acepta del cliente.

Respuesta de cita:
{
  "id": 25,
  "paciente": {"id": 3, "nombre": "Ana Lopez"},
  "servicio": {"id": 1, "nombre": "Consulta general"},
  "inicio": "2026-11-23T09:00:00-06:00",
  "fin": "2026-11-23T09:30:00-06:00",
  "precioReservado": 400.00,
  "estado": "confirmada",
  "creadoEn": "<instante ISO 8601>"
}

POST devuelve 201.
GET de detalle y PATCH devuelven 200 con la cita.

Mis citas:
GET /citas/mias?page=0&size=10&tipo=proximas&estado=confirmada
tipo admite proximas o historial; si se omite, incluye ambas.
Próximas: confirmadas con inicio futuro.
Historial: el resto.

Agenda:
GET /citas?page=0&size=10&search=ana&estado=confirmada&fecha=2026-11-23
search busca por nombre del paciente.

Cancelar no necesita cuerpo.
Paciente: solo cita propia confirmada y con al menos 2 horas.
ADMIN/MEDICO: pueden cancelar una cita confirmada sin ese límite.

Cambio de estado:
{"estado":"completada"}
También admite no_asistio.
Solo desde confirmada y una vez alcanzada la hora de fin.

Cada creación o cambio de estado genera historial.
Empalme o regla de negocio incumplida: 422.
El acceso a citas ajenas de un paciente se rechaza.

## 10. Usuarios

Acceso exclusivo ADMIN.

| Método | Ruta |
|---|---|
| GET | /usuarios |
| GET | /usuarios/{id} |
| POST | /usuarios |
| PUT | /usuarios/{id} |
| DELETE | /usuarios/{id} |

Listado:
GET /usuarios?page=0&size=10&search=ana&rol=PACIENTE

POST recibe:
{
  "nombre": "Nombre",
  "email": "usuario@example.com",
  "telefono": "+525512345678",
  "password": "Ejemplo#2026",
  "rol": "PACIENTE"
}

PUT recibe nombre, telefono y activo.
No cambia correo, rol ni contraseña en esta primera versión.
Respuesta: formato de usuario de login, sin hash de contraseña.

POST devuelve 201; PUT y GET de detalle, 200.
DELETE desactiva y devuelve 204; conserva citas e historial.
No permitir desactivar al último administrador activo.
No permitir más de un usuario MEDICO activo.
Reactivar un médico también debe respetar esa regla.

## 11. Configuración y resumen

GET /configuracion: ADMIN y MEDICO.
PUT /configuracion: ADMIN.

Configuración:
{
  "id": 1,
  "nombre": "Consultorio MediCita",
  "telefono": "+525512345678",
  "direccion": "Dirección del consultorio",
  "zonaHoraria": "America/Mexico_City"
}

PUT recibe nombre, telefono y direccion.
La zona horaria se mantiene fija en esta versión.

GET /dashboard/resumen: ADMIN y MEDICO.
Respuesta:
{
  "citasHoy": 0,
  "proximasConfirmadas": 0,
  "pacientesActivos": 0,
  "completadas": 0,
  "porEstado": {
    "confirmada": 0,
    "completada": 0,
    "cancelada": 0,
    "no_asistio": 0
  }
}

citasHoy: citas cuya fecha de inicio sea hoy, de cualquier estado.
proximasConfirmadas: confirmadas con inicio futuro.
completadas y porEstado: totales de toda la base.
pacientesActivos: usuarios activos con rol PACIENTE.

## 12. Paginación

Todos los listados administrativos y de citas se paginan en el servidor.
page comienza en 0.
size por defecto: 10; máximo: 100.
Los filtros son opcionales salvo indicación contraria.

Respuesta común:
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}

Cambiar un filtro en React reinicia page a 0.
No descargar todos los registros para filtrar o paginar en el cliente.

## 13. Errores

Formato acordado:
{
  "timestamp": "<instante ISO 8601>",
  "status": 400,
  "message": "Datos inválidos",
  "path": "/api/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Debe ser un correo válido"
    }
  ]
}

fieldErrors será [] cuando no existan errores por campo.
El frontend coloca los errores bajo el input correspondiente.

| Código | Significado |
|---|---|
| 400 | Campos, parámetros o formato inválidos |
| 401 | Falta token, token inválido/vencido/revocado o login incorrecto |
| 403 | Usuario autenticado sin permiso |
| 404 | Recurso inexistente o no visible según la regla del módulo |
| 422 | Regla de negocio incumplida o conflicto de datos |
| 500 | Error interno inesperado, sin revelar detalles técnicos |

No devolver contraseñas, hashes, claves ni trazas internas.

## 14. Coordinación

- Este contrato requiere revisión de ambos antes de congelarlo.
- Karla puede preparar pantallas y datos simulados con estos formatos.
- Las rutas previstas no deben marcarse como implementadas hasta probarlas.
- Los cambios al contrato deben comunicarse antes de modificar frontend/backend.
- Seguridad JWT y manejo uniforme de errores se implementan en etapas posteriores.