-- Los datos de prueba traian el telefono 0000000000, que no cumple el formato internacional
-- que exige la API (+ y de 8 a 15 digitos). Se reemplaza por numeros ficticios validos y distintos.
-- No son numeros reales: para recibir mensajes en una demo se cambia el telefono de la cuenta
-- desde el perfil o directamente en la base del servidor, nunca en este archivo.
UPDATE usuarios
SET telefono = '+52550' || lpad(id::text, 7, '0')
WHERE telefono = '0000000000';
