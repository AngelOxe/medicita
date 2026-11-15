-- Datos ficticios para desarrollo y evaluacion.
-- Fechas relativas al momento en que se aplica esta migracion.
SET LOCAL TIME ZONE 'America/Mexico_City';

-- 1. Roles
INSERT INTO roles (nombre)
VALUES ('ADMIN'), ('MEDICO'), ('PACIENTE');

-- 2. Administrador y unico medico
INSERT INTO usuarios (
    nombre, email, password_hash, telefono, rol_id
)
VALUES
(
    'Administrador MediCita',
    'admin@medicita.test',
    '$2a$12$YeI0/Ur32mBBWAqnkngeDu0ZuflZPsYGZY/EEnCDSXOfH8lGEXGbG',
    '0000000000',
    (SELECT id FROM roles WHERE nombre = 'ADMIN')
),
(
    'Daniel Martinez',
    'daniel.martinez@medicita.test',
    '$2a$12$G2ScUpP.8IjyLnNIYi7X9uWapP/hxXwQneEy48xRen4BVVuCxWbqi',
    '0000000000',
    (SELECT id FROM roles WHERE nombre = 'MEDICO')
);

-- Telefonos ficticios: no utilizarlos para envios reales.
-- Los pacientes comparten una contrasena de evaluacion.
INSERT INTO usuarios (
    nombre, email, password_hash, telefono, rol_id
)
SELECT
    datos.nombre,
    datos.email,
    '$2a$12$twt4XRMVDodmKeMUj1gaz.DPvMT5dgyT84sQ74DwGP2Je/2WVGGcy',
    '0000000000',
    (SELECT id FROM roles WHERE nombre = 'PACIENTE')
FROM (
    VALUES
        ('Ana Lopez', 'ana.lopez@example.com'),
        ('Carlos Hernandez', 'carlos.hernandez@example.com'),
        ('Maria Garcia', 'maria.garcia@example.com'),
        ('Jose Perez', 'jose.perez@example.com'),
        ('Laura Ramirez', 'laura.ramirez@example.com'),
        ('Pedro Sanchez', 'pedro.sanchez@example.com'),
        ('Sofia Torres', 'sofia.torres@example.com'),
        ('Miguel Flores', 'miguel.flores@example.com'),
        ('Elena Cruz', 'elena.cruz@example.com'),
        ('Luis Mendoza', 'luis.mendoza@example.com')
) AS datos(nombre, email);

-- 3. Servicios del consultorio
INSERT INTO servicios (nombre, descripcion, duracion_min, precio)
VALUES
('Consulta general', 'Consulta de medicina general.', 30, 400.00),
('Primera consulta', 'Valoracion inicial del paciente.', 60, 600.00),
('Seguimiento', 'Seguimiento de una consulta previa.', 30, 350.00),
('Control de presion', 'Revision de presion arterial.', 15, 150.00),
('Control de glucosa', 'Revision y seguimiento de glucosa.', 20, 200.00),
('Revision de resultados', 'Revision de estudios del paciente.', 30, 300.00),
('Certificado medico', 'Valoracion para certificado medico.', 30, 300.00),
('Valoracion preventiva', 'Consulta de prevencion y orientacion.', 45, 500.00),
('Control de peso', 'Seguimiento general del peso corporal.', 30, 300.00),
('Curacion simple', 'Atencion de curaciones simples.', 30, 250.00);

-- 4. Diez horarios: lunes a viernes, manana y tarde
INSERT INTO horarios (dia_semana, hora_inicio, hora_fin)
SELECT dia, TIME '09:00', TIME '13:00'
FROM generate_series(1, 5) AS dias(dia);

INSERT INTO horarios (dia_semana, hora_inicio, hora_fin)
SELECT dia, TIME '15:00', TIME '19:00'
FROM generate_series(1, 5) AS dias(dia);

-- Todos los servicios se ofrecen en todos los horarios inicialmente.
-- La tabla puente permite restringirlos mas adelante.
INSERT INTO horario_servicio (horario_id, servicio_id)
SELECT h.id, s.id
FROM horarios h
CROSS JOIN servicios s;

-- 5. Configuracion unica
INSERT INTO configuracion_consultorio (
    id, nombre, telefono, direccion, zona_horaria
)
VALUES (
    1,
    'Consultorio MediCita',
    '0000000000',
    'Direccion ficticia para demostracion, Oaxaca',
    'America/Mexico_City'
);

-- 6. Diez bloqueos futuros, de 12:00 a 13:00.
-- Se reparten entre lunes y viernes de las proximas dos semanas.
WITH fechas AS (
    SELECT
        i,
        (
            date_trunc('week', CURRENT_DATE)::date
            + 7
            + (i / 5) * 7
            + (i % 5)
        ) AS dia
    FROM generate_series(0, 9) AS serie(i)
)
INSERT INTO bloqueos_horario (inicio, fin, motivo)
SELECT
    (dia + TIME '12:00') AT TIME ZONE 'America/Mexico_City',
    (dia + TIME '13:00') AT TIME ZONE 'America/Mexico_City',
    'Bloqueo de demostracion ' || (i + 1)
FROM fechas;

-- 7. Quince citas:
-- Diez pasadas, de las dos semanas anteriores.
-- Cinco confirmadas para la proxima semana.
-- Todas a las 09:00 y en dias laborables.
WITH pacientes AS (
    SELECT
        u.id,
        row_number() OVER (ORDER BY u.email) AS numero
    FROM usuarios u
    JOIN roles r ON r.id = u.rol_id
    WHERE r.nombre = 'PACIENTE'
),
catalogo AS (
    SELECT
        id,
        duracion_min,
        precio,
        row_number() OVER (ORDER BY nombre) AS numero
    FROM servicios
),
fechas AS (
    SELECT
        i,
        CASE
            WHEN i < 10 THEN
                date_trunc('week', CURRENT_DATE)::date
                - 14 + (i / 5) * 7 + (i % 5)
            ELSE
                date_trunc('week', CURRENT_DATE)::date
                + 7 + (i - 10)
        END AS dia,
        CASE
            WHEN i >= 10 THEN 'confirmada'
            WHEN i % 3 = 0 THEN 'completada'
            WHEN i % 3 = 1 THEN 'cancelada'
            ELSE 'no_asistio'
        END AS estado
    FROM generate_series(0, 14) AS serie(i)
),
datos AS (
    SELECT
        p.id AS paciente_id,
        s.id AS servicio_id,
        s.duracion_min,
        s.precio,
        f.estado,
        (f.dia + TIME '09:00')
            AT TIME ZONE 'America/Mexico_City' AS inicio
    FROM fechas f
    JOIN pacientes p ON p.numero = (f.i % 10) + 1
    JOIN catalogo s ON s.numero = (f.i % 10) + 1
)
INSERT INTO citas (
    paciente_id, servicio_id, inicio, fin,
    precio_reservado, estado, creado_en
)
SELECT
    paciente_id,
    servicio_id,
    inicio,
    inicio + duracion_min * INTERVAL '1 minute',
    precio,
    estado,
    inicio - INTERVAL '7 days'
FROM datos;

-- 8. Historial inicial: todas las citas nacieron confirmadas.
INSERT INTO historial_estados (
    cita_id, estado_anterior, estado_nuevo, cambiado_por, fecha
)
SELECT
    id, NULL, 'confirmada', paciente_id, creado_en
FROM citas;

-- Para las citas pasadas, registrar su cambio posterior.
INSERT INTO historial_estados (
    cita_id, estado_anterior, estado_nuevo, cambiado_por, fecha
)
SELECT
    c.id,
    'confirmada',
    c.estado,
    (SELECT id FROM usuarios WHERE email = 'admin@medicita.test'),
    CASE
        WHEN c.estado = 'cancelada'
            THEN c.inicio - INTERVAL '1 day'
        ELSE c.fin
    END
FROM citas c
WHERE c.estado <> 'confirmada';

-- password_reset_tokens y tokens_revocados empiezan vacias.
-- Se llenaran mediante las operaciones reales de autenticacion.