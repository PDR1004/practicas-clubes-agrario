-- V2: restringe el formato de cuota.periodo a 'YYYY-MM'.
-- V1 declaro periodo como varchar(7) sin CHECK, por lo que aceptaba cualquier
-- cadena de hasta 7 caracteres ('hola', '2026/03', 'abcdefg').
-- El mes se valida en rango 01-12 para que el valor sea un periodo real.
-- Se agrega como migracion nueva (ADR-0009) y no editando V1, que ya fue aplicada.

ALTER TABLE cuota
    ADD CONSTRAINT ck_cuota_periodo_formato
    CHECK (periodo ~ '^[0-9]{4}-(0[1-9]|1[0-2])$');
