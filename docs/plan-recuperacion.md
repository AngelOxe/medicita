# Plan de recuperación — 17 al 24 de noviembre de 2026

> BORRADOR PENDIENTE DE CONCILIAR.
> Este documento no confirma tareas realizadas ni decisiones autorizadas.
> Ajustes pendientes: Ángel ya inició el backend de servicios;
> detener las actividades del VPS sigue pendiente de decisión;
> no se ha comprobado conectividad por el puerto 2525;
> MediCita tendrá un solo médico, sin selector al reservar.


Punto de partida (17 nov): autenticación completa (backend y frontend), base de datos, entidades, layout y componentes UI. Faltan el VPS, la recuperación de contraseña, servicios, horarios, citas, comunicación (correo/SMS/WhatsApp), usuarios, dashboard y Figma navegable.

Ángel = A (backend/datos/infra) · Karla = B (frontend/proceso). Ambos tocan backend y frontend.

## Reglas diarias (no se negocian)

- Junta de 15 min al empezar. Mover tarjetas del tablero **antes** de programar.
- Mínimo **2 a 3 commits propios por día**, pequeños y descriptivos, hechos conforme se avanza (no en lote al final).
- Una rama por tarea; Pull Request enlazado al issue y revisado por el otro. Nada directo a `main`.
- Antes de cada push: `git status` y comprobar que `application.properties` no aparece.
- Cada día termina con una **evidencia** (captura, respuesta de endpoint o pantalla funcionando).
- Si una tarea bloquea más de 45 minutos: se anota, se aplica la solución mínima y se pide ayuda.

## Plan día por día

### Mar 17 (hoy) — desbloquear lo que no es código

| | Tarea | Listo cuando |
|---|---|---|
| **A** | `GlobalExceptionHandler`: agregar 404 (recurso no encontrado), 422 (regla de negocio) y 23P01 (anti-traslape). Abrir en `SecurityConfig` las rutas que se vayan creando | Cada código devuelve el JSON estándar |
| **A** | `forgot-password` y `reset-password` (token aleatorio, se guarda su hash, expira, uso único; el enlace sale en el log mientras no haya correo) | Flujo completo probado en Bruno |
| **A** | **VPS**: apagar/deshabilitar las apps viejas (sin borrarlas), PostgreSQL, usuario `medicita`, `ufw`, systemd, JAR | `/api/salud` responde en el servidor |
| **B** | **Enviar el mensaje a la profesora** (PostgreSQL, fecha y hora de demo, Twilio trial, plan B del puerto 25). Guardar la respuesta como evidencia | Mensaje enviado |
| **B** | **Comprar el dominio** y dárselo a A para el DNS | Dominio con acceso al DNS |
| **B** | CRUD de **servicios** (backend): DTOs con Bean Validation, GET paginado con `search` y `activo` (Specification), POST/PUT/DELETE para ADMIN y MEDICO | PR revisado por A |
| **B** | Pantallas **Recuperar** y **Restablecer** | Errores bajo los inputs; mensaje de éxito |
| Ambos | Actualizar el Excel y el tablero; unificar `git config user.name` | Tablero al día |

### Mié 18 — servicios de punta a punta + horarios

| | Tarea | Listo cuando |
|---|---|---|
| **A** | **Horarios** (con `servicioIds`, relación N:M, `@EntityGraph`) y **Bloqueos** (fin > inicio) | Un horario guarda y devuelve sus servicios en una sola consulta |
| **A** | DNS: registro A hacia el VPS; segundo despliegue del JAR | `nslookup` devuelve la IP |
| **B** | Pantalla **Servicios** (tabla con paginación del servidor, búsqueda con debounce de 400 ms, filtro, formulario validado, modal de eliminar, loading y estado vacío) | Cambiar de página o filtro dispara una petición nueva (verlo en DevTools) |
| **B** | **Nginx** + build estático en el VPS + `deploy/deploy.sh` (por HTTP mientras no haya dominio) | La web abre desde otro dispositivo |
| **B** | Figma: **logo** original y **página de paleta** justificada (30-45 min) | Ambos en Figma |

**Punto de control (noche):** el sitio abre desde otro dispositivo y `GlobalExceptionHandler` cubre 400, 401, 403, 404, 422 y 500.

### Jue 19 — el núcleo: citas (lo que más puntúa)

| | Tarea | Listo cuando |
|---|---|---|
| **A** | **Disponibilidad**: `GET /api/disponibilidad` (horario que ofrece el servicio, menos bloqueos y citas confirmadas, solo horas futuras, zona America/Mexico_City) | Horas libres coherentes en 3 días distintos |
| **A** | **Crear cita**: fecha futura, dentro de horario, sin bloqueo, sin traslape; `fin = inicio + duración`; `precio_reservado`; `@Transactional`; historial; SQLState 23P01 → 422 | 201 válida; empalme 422 con mensaje claro |
| **A** | **Cancelar** (paciente solo propias y con 2 h o más) y **cambiar estado** (completada / no asistió) | 422 con menos de 2 h; ajena da 403/404 |
| **B** | **HTTPS** con Certbot (si el DNS ya propagó) | https://dominio abre con candado |
| **B** | Pantalla **Reservar** (médico, servicio, calendario, horarios, motivo) con modal de confirmación | Reserva real desde la UI |

**Punto de control (noche):** HTTPS funcionando y una reserva creada desde la pantalla.
**Decisión de hoy:** ¿qué respondió la profesora sobre el correo? Si no hay respuesta, se aplica el plan B (ver más abajo).

### Vie 20 — listados, correo y Mis citas

| | Tarea | Listo cuando |
|---|---|---|
| **A** | `GET /api/citas/mias` y `GET /api/citas` (filtros `search`, `estado`, `fecha`; paginados; `@EntityGraph`) | Una consulta por página en los logs SQL |
| **A** | Prueba de **dos reservas simultáneas** al mismo horario | Una da 201 y la otra 422; captura en `docs/evidencias` |
| **A** | `MailService` con `JavaMailSender`, `@Async` y `try/catch`; correos de bienvenida, confirmación, cancelación y recuperación (en log hasta tener Postfix) | Los correos salen en el log al hacer acciones reales |
| **B** | **Mis citas** (paginada, pestañas, cancelar con modal, mensaje si faltan menos de 2 h, error 422 visible) | Cancelación válida e inválida se ven bien |
| **B** | **Gestión de citas** según Figma (filtros del servidor, paginación, acciones con modal) | Se parece al Figma |

### Sáb 21 (jornada larga) — comunicación real

| | Tarea | Listo cuando |
|---|---|---|
| **A** | **Postfix + OpenDKIM + SPF + DKIM + DMARC (p=none)**; PTR renombrando el droplet. O plan B con evidencia | Correo con `spf=pass` y `dkim=pass` en Gmail ("Mostrar original"), o evidencia completa |
| **A** | `UsuarioController` (ADMIN): listado paginado con búsqueda y rol, crear, editar, desactivar (borrado lógico); `PUT /api/perfil`; `GET /api/dashboard/resumen` | Paciente y médico reciben 403 en usuarios |
| **B** | `NotificacionService` con Twilio (`@Async`): reserva → WhatsApp, cancelación → SMS; claves solo en `application.properties` | Mensajes recibidos en el celular desde la app |
| **B** | Teléfono en registro y perfil (validado en front y back); pantallas **Dashboard** y **Perfil** | Datos reales del endpoint de resumen |

### Dom 22 — pantallas que faltan y calidad

| | Tarea | Listo cuando |
|---|---|---|
| **A** | Pantallas **Usuarios** y **Horarios/Bloqueos** (tablas paginadas del servidor, modales) | PR revisado por B |
| **A** | Pruebas de reglas: traslape, límite exacto (10:30 tras 10:00-10:30), regla de 2 h, permisos, token revocado. Revisar N+1 | Lista de pruebas con resultado esperado = obtenido |
| **B** | **Colección de Bruno** completa (200, 400, 401, 403, 404 y 422) con environments `local` y `vps` sin secretos | Corre completa contra el VPS |
| **B** | **Responsivo** (375 px y 1280 px), loading / vacío / error de red con botón de reintentar; buscar `alert(` y `confirm(` en el código | La búsqueda da cero resultados |
| **B** | Figma: **prototipo navegable** (3 recorridos: paciente, médico, admin) y link público | El link abre sin iniciar sesión |

### Lun 23 — congelación y QA

**Congelación funcional a mediodía.** Desde ahí solo se corrigen errores.

| | Tarea | Listo cuando |
|---|---|---|
| **A** | QA en el VPS con los 3 roles (Anexo B); diagrama ER y `docs/sql/medicita_respaldo.sql` con `pg_dump`; revisión de secretos en el historial | Cero errores P0; ningún secreto en `git log -p` |
| **B** | **README completo** (descripción, tecnologías, instalación, credenciales, ER, URLs, links, capturas por rol); QA en móvil; verificar commits con `git shortlog -sn` | Checklist 11.4 completo |
| Ambos | **Ensayo de demo** en la URL real: cada uno explica el código del otro y responde 5 preguntas orales. Volver a unirse al sandbox de WhatsApp | Demo de punta a punta sin trabarse |

### Mar 24 — entrega

- Verificar HTTPS, `/api/salud`, `systemctl status medicita`, certificado vigente y sandbox de WhatsApp.
- Revisar repo y tablero en ventana de incógnito.
- Texto para Classroom: nombres completos, número de equipo, repo, URL HTTPS y URL base de la API.
- Demo.

## Puntos de control

| Cuándo | Qué debe estar |
|---|---|
| Mié 18, noche | Sitio abierto desde otro dispositivo; errores 400/401/403/404/422/500 en JSON |
| Jue 19, noche | HTTPS + reserva real + 422 por empalme |
| Sáb 21, noche | Correo, SMS y WhatsApp reales (o evidencia del plan B) |
| Lun 23, mediodía | Congelación; solo errores |

## Qué recortar si hay atraso (en este orden)

1. Gráfica semanal del dashboard.
2. Pantalla Configuración.
3. Catálogo de servicios para el paciente (queda solo dentro de Reservar).
4. Calendario visual de bloqueos (basta una tabla con formulario).
5. Cambio de contraseña desde el perfil (basta nombre y teléfono).
6. Botón "Nueva cita" del personal en pantalla (el backend ya lo permite).

**No se recorta nunca:** seguridad por rol, validaciones en front y back, paginación y filtros del servidor, anti-traslape, Bruno, Figma navegable, repo y tablero con commits de ambos, despliegue con HTTPS, y correo / SMS / WhatsApp.

## Plan B del correo (decidir el jue 19)

DigitalOcean bloquea la salida por los puertos 25, 465 y 587; solo el 2525 y el 443 están abiertos. Postfix directo no podrá entregar a Gmail desde este VPS.

1. Dejar configurados Postfix, OpenDKIM, SPF, DKIM y DMARC de todos modos.
2. Reunir evidencia: prueba de puertos (25/465/587 con tiempo agotado vs 443 abierto), `mail.log`, `dig TXT`, `opendkim-testkey` y el ticket a DigitalOcean.
3. Pedir a la profesora una de dos: autorizar un relé por el 2525, o aceptar la configuración con evidencia.
4. Si no hay respuesta el jueves: evaluar cambiar a un proveedor que permita el puerto 25 saliente (verificarlo antes de pagar).

## Distribución de carga

| | A (Ángel) | B (Karla) |
|---|---|---|
| Backend | Errores, recuperación de contraseña, horarios, disponibilidad, citas, correo, usuarios, perfil, dashboard | Servicios, Twilio |
| Frontend | Usuarios, Horarios/Bloqueos | Recuperar, Servicios, Reservar, Mis citas, Gestión de citas, Dashboard, Perfil, responsivo |
| Infra | VPS, Postfix, DNS | Nginx, HTTPS, deploy.sh |
| Proceso | Secretos, ER, respaldo SQL, pruebas | Mensaje a la profesora, Figma, Bruno, README, Excel |
