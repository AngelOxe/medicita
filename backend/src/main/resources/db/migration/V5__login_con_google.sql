-- Inicio de sesion con Google.
-- 1) google_sub es el identificador estable que Google asigna a cada cuenta. Una cuenta creada o
--    vinculada con Google lo guarda; las cuentas normales lo dejan vacio.
-- 2) Google no entrega telefono, asi que la columna deja de ser obligatoria. El paciente lo completa
--    despues desde su perfil para recibir mensajes. Sin telefono, simplemente no se le envian SMS.
ALTER TABLE usuarios
    ADD COLUMN google_sub VARCHAR(255);

ALTER TABLE usuarios
    ALTER COLUMN telefono DROP NOT NULL;

-- Una cuenta de Google solo puede estar vinculada a un usuario.
CREATE UNIQUE INDEX usuarios_google_sub_unico
    ON usuarios (google_sub)
    WHERE google_sub IS NOT NULL;
