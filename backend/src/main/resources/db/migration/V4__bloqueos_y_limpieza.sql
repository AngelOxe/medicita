-- 1. Los datos de prueba deben tener al menos diez bloqueos futuros.
-- Con el uso (pruebas, bloqueos eliminados o vencidos) pueden quedar menos, asi que se completan
-- hasta diez. Si ya hay diez o mas no se inserta nada. Dias laborables, de 12:00 a 13:00.
INSERT INTO bloqueos_horario (inicio, fin, motivo)
SELECT
    (dia + TIME '12:00') AT TIME ZONE 'America/Mexico_City',
    (dia + TIME '13:00') AT TIME ZONE 'America/Mexico_City',
    'Bloqueo de demostracion adicional ' || (i + 1)
FROM (
    SELECT
        i,
        (
            date_trunc('week', CURRENT_DATE)::date
            + 21
            + (i / 5) * 7
            + (i % 5)
        ) AS dia
    FROM generate_series(
        0,
        GREATEST(
            0,
            10 - (SELECT COUNT(*) FROM bloqueos_horario WHERE fin > CURRENT_TIMESTAMP)
        )::int - 1
    ) AS serie(i)
) AS fechas;

-- 2. Se eliminan los tokens que ya no sirven: enlaces de recuperacion usados o vencidos y
-- tokens de sesion revocados cuya fecha de expiracion ya paso (el JWT vencido se rechaza solo).
DELETE FROM password_reset_tokens
WHERE usado = TRUE OR expira_en < CURRENT_TIMESTAMP;

DELETE FROM tokens_revocados
WHERE expira_en < CURRENT_TIMESTAMP;
