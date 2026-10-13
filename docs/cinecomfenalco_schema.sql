-- =====================================================================
-- CineComfenalco - Script de base de datos (DDL + datos iniciales)
-- Requiere MySQL 8.0.16 o superior (antes de esa version ignora los CHECK).
-- Basado en el Documento 01 (version 0.1).
-- =====================================================================
--
-- GLOSARIO RAPIDO (las palabras que mas vas a ver):
--   PRIMARY KEY      = identificador unico de cada fila (la "cedula" de la fila).
--   AUTO_INCREMENT   = MySQL numera solo: 1, 2, 3...
--   NOT NULL         = el campo es obligatorio. Sin NOT NULL el campo puede quedar vacio (NULL).
--   DEFAULT          = valor que se usa si no se envia ninguno.
--   UNIQUE           = no puede repetirse el valor en dos filas.
--   FOREIGN KEY      = "llave foranea": un campo que apunta al id de otra tabla.
--                      Asi MySQL impide apuntar a algo que no existe.
--   ON DELETE RESTRICT = no deja borrar una fila si otra tabla todavia la usa.
--                      Es lo que protege el historial (ej: una pelicula con funciones).
--   CHECK            = regla que el dato debe cumplir (ej: precio >= 0).
--   ENUM             = campo que solo acepta una lista fija de valores.
--   DECIMAL(10,2)    = numero con 2 decimales (para dinero; nunca uses FLOAT/DOUBLE).
--   BIGINT           = numero entero grande (lo usamos para todos los id).
--   BOOLEAN          = verdadero/falso (TRUE = 1, FALSE = 0).
--
-- NOTAS GENERALES:
--   * Las fechas son DATETIME y se guardan en hora de Colombia (America/Bogota).
--     La aplicacion es quien pone la hora; la base de datos no la inventa.
--   * Todos los precios configurados se guardan SIN IVA. El IVA se suma al vender.
--   * RESTRICT protege a la tabla "padre": no deja borrar una pelicula, sala,
--     funcion, producto o usuario que ya tenga facturas.
--     OJO: eso NO impide editar o borrar filas dentro de las propias tablas de
--     factura. Esa proteccion es opcional y esta en la PARTE 8.
--   * Algunas reglas NO se pueden poner en la base de datos y las valida la
--     aplicacion (Spring Boot). Estan marcadas con "OJO APP".
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0. CREAR LA BASE DE DATOS
-- (En Railway la base de datos ya viene creada: borra estas dos lineas
--  y ejecuta el resto sobre la base de datos que te den.)
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS cinecomfenalco
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE cinecomfenalco;

-- Para que los acentos (Accion -> Acción) se guarden bien.
SET NAMES utf8mb4;


-- ---------------------------------------------------------------------
-- (OPCIONAL) REINICIAR TODO EN DESARROLLO
-- Descomenta estas lineas SOLO si quieres borrar las tablas y empezar de cero.
-- Se borra en orden inverso porque unas tablas dependen de otras.
-- ---------------------------------------------------------------------
-- DROP TABLE IF EXISTS factura_producto;
-- DROP TABLE IF EXISTS factura_asiento;
-- DROP TABLE IF EXISTS factura;
-- DROP TABLE IF EXISTS funcion;
-- DROP TABLE IF EXISTS usuario;
-- DROP TABLE IF EXISTS configuracion;
-- DROP TABLE IF EXISTS producto;
-- DROP TABLE IF EXISTS pelicula;
-- DROP TABLE IF EXISTS genero;
-- DROP TABLE IF EXISTS asiento;
-- DROP TABLE IF EXISTS sala;
-- DROP TABLE IF EXISTS tipo_sala;


-- =====================================================================
-- PARTE 1: TABLAS DE CATALOGO (datos que casi no cambian)
-- =====================================================================

-- ---------------------------------------------------------------------
-- tipo_sala: categorias de sala (ej: 2D, 3D). Cada una tiene un recargo.
-- ---------------------------------------------------------------------
CREATE TABLE tipo_sala (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    nombre  VARCHAR(50)   NOT NULL,
    -- Monto fijo que se suma al precio base de la entrada (SIN IVA).
    recargo DECIMAL(10,2) NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    CONSTRAINT uq_tipo_sala_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_sala_recargo CHECK (recargo >= 0)
);

-- ---------------------------------------------------------------------
-- sala: espacio fisico donde se proyectan las funciones.
-- ---------------------------------------------------------------------
CREATE TABLE sala (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(50) NOT NULL,
    tipo_sala_id BIGINT      NOT NULL,
    -- TRUE = activa, FALSE = deshabilitada (RN-09: no recibe funciones nuevas).
    activa       BOOLEAN     NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    CONSTRAINT uq_sala_nombre UNIQUE (nombre),
    -- No se puede borrar un tipo de sala que alguna sala este usando.
    CONSTRAINT fk_sala_tipo_sala FOREIGN KEY (tipo_sala_id)
        REFERENCES tipo_sala (id) ON DELETE RESTRICT
);
-- OJO APP: no deshabilitar una sala que tenga funciones vigentes (RN-10).

-- ---------------------------------------------------------------------
-- asiento: los 80 asientos que existen. TODAS las salas usan los mismos
-- (filas A-H, numeros 1-10), por eso esta tabla no tiene sala_id (RN-22).
-- ---------------------------------------------------------------------
CREATE TABLE asiento (
    id     BIGINT           NOT NULL AUTO_INCREMENT,
    fila   CHAR(1)          NOT NULL,
    numero TINYINT UNSIGNED NOT NULL,

    PRIMARY KEY (id),
    -- No puede haber dos veces el asiento "A1".
    CONSTRAINT uq_asiento_fila_numero UNIQUE (fila, numero),
    CONSTRAINT ck_asiento_fila   CHECK (fila BETWEEN "A" AND "H"),
    CONSTRAINT ck_asiento_numero CHECK (numero BETWEEN 1 AND 10)
);

-- ---------------------------------------------------------------------
-- genero: catalogo pequeno de generos de pelicula.
-- ---------------------------------------------------------------------
CREATE TABLE genero (
    id     BIGINT      NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_genero_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- configuracion: UNA SOLA FILA con el precio base de la entrada y el IVA.
-- El ADMIN las modifica con UPDATE. Nunca se agregan mas filas.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion (
    id                 TINYINT       NOT NULL,
    -- Precio base de una entrada, SIN IVA (el recargo de la sala se suma aparte).
    precio_base_entrada DECIMAL(10,2) NOT NULL,
    -- Porcentaje de IVA. Ejemplo: 19.00 significa 19 %.
    iva_porcentaje     DECIMAL(5,2)  NOT NULL,

    PRIMARY KEY (id),
    -- Truco: solo se permite id = 1, asi es imposible tener dos configuraciones.
    CONSTRAINT ck_configuracion_una_fila CHECK (id = 1),
    CONSTRAINT ck_configuracion_precio   CHECK (precio_base_entrada >= 0),
    CONSTRAINT ck_configuracion_iva      CHECK (iva_porcentaje BETWEEN 0 AND 100)
);


-- =====================================================================
-- PARTE 2: PELICULAS Y PRODUCTOS
-- =====================================================================

-- ---------------------------------------------------------------------
-- pelicula
-- ---------------------------------------------------------------------
CREATE TABLE pelicula (
    id               BIGINT            NOT NULL AUTO_INCREMENT,
    titulo           VARCHAR(150)      NOT NULL,
    sinopsis         TEXT              NOT NULL,
    duracion_minutos SMALLINT UNSIGNED NOT NULL,
    genero_id        BIGINT            NOT NULL,
    clasificacion    ENUM("0+", "7+", "15+", "18+") NOT NULL,
    -- Solo guardamos la direccion (URL o ruta) de la imagen, nunca la imagen.
    imagen_url       VARCHAR(500)      NOT NULL,
    -- TRUE = sale en cartelera. FALSE = retirada (asi se "borra" una pelicula con historial).
    activa           BOOLEAN           NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    CONSTRAINT ck_pelicula_duracion CHECK (duracion_minutos > 0),
    CONSTRAINT fk_pelicula_genero FOREIGN KEY (genero_id)
        REFERENCES genero (id) ON DELETE RESTRICT
);
-- OJO APP: no desactivar ni cambiar la duracion de una pelicula con funciones vigentes (RN-29, RN-30).

-- ---------------------------------------------------------------------
-- producto: confiteria. No hay inventario (no se lleva stock).
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100)  NOT NULL,
    descripcion VARCHAR(255)  NOT NULL,
    -- Precio SIN IVA.
    precio      DECIMAL(10,2) NOT NULL,
    imagen_url  VARCHAR(500)  NOT NULL,
    -- FALSE = producto retirado: no se vende mas, pero las facturas viejas lo conservan.
    disponible  BOOLEAN       NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0)
);


-- =====================================================================
-- PARTE 3: USUARIOS (taquilleros, administradores y super administrador)
-- =====================================================================

-- ---------------------------------------------------------------------
-- usuario: una sola tabla para los tres roles.
--   TAQUILLERO  -> no tiene username ni contrasena (solo elige su nombre).
--   ADMIN       -> tiene username y contrasena.
--   SUPER_ADMIN -> tiene username y contrasena (solo maneja administradores).
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100) NOT NULL,
    rol           ENUM("TAQUILLERO", "ADMIN", "SUPER_ADMIN") NOT NULL,
    -- Para iniciar sesion. Queda vacio (NULL) en los taquilleros.
    username      VARCHAR(50)  NULL,
    -- NUNCA la contrasena en texto: aqui va el hash (ej: BCrypt, mide 60 caracteres).
    password_hash VARCHAR(255) NULL,
    -- FALSE = desactivado: no puede iniciar sesion ni vender, pero su historial se conserva.
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),
    -- El nombre no se repite dentro del mismo rol (dos taquilleros no pueden llamarse igual).
    -- Nota: tambien aplica a ADMIN y SUPER_ADMIN; es un poco mas estricto que el
    -- documento, que solo lo exige a los taquilleros.
    CONSTRAINT uq_usuario_rol_nombre UNIQUE (rol, nombre),
    -- El username no se repite (varias filas con NULL si estan permitidas).
    CONSTRAINT uq_usuario_username UNIQUE (username),
    -- Regla: taquillero SIN credenciales; admin y super admin CON credenciales.
    CONSTRAINT ck_usuario_credenciales CHECK (
        (rol = "TAQUILLERO" AND username IS NULL AND password_hash IS NULL)
        OR
        (rol IN ("ADMIN", "SUPER_ADMIN") AND username IS NOT NULL AND password_hash IS NOT NULL)
    )
);
-- OJO APP: un SUPER_ADMIN no puede vender (RN-26); la tabla factura solo debe
--          referenciar usuarios con rol TAQUILLERO o ADMIN.


-- =====================================================================
-- PARTE 4: FUNCIONES
-- =====================================================================

-- ---------------------------------------------------------------------
-- funcion: una pelicula en una sala, a una hora.
-- OJO: la funcion NO guarda precio. El precio de la entrada se calcula al
-- vender (precio base + recargo del tipo de sala + IVA) y queda en la factura.
-- ---------------------------------------------------------------------
CREATE TABLE funcion (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    pelicula_id BIGINT   NOT NULL,
    sala_id     BIGINT   NOT NULL,
    -- Cuando empieza.
    inicio      DATETIME NOT NULL,
    -- Cuando termina la pelicula = inicio + duracion (lo calcula la app).
    fin         DATETIME NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT ck_funcion_horas CHECK (fin > inicio),
    -- No se puede borrar una pelicula ni una sala que tengan funciones.
    CONSTRAINT fk_funcion_pelicula FOREIGN KEY (pelicula_id)
        REFERENCES pelicula (id) ON DELETE RESTRICT,
    CONSTRAINT fk_funcion_sala FOREIGN KEY (sala_id)
        REFERENCES sala (id) ON DELETE RESTRICT
);

-- Indice: hace rapida la busqueda "funciones de una sala por fecha".
CREATE INDEX idx_funcion_sala_inicio ON funcion (sala_id, inicio);

-- OJO APP (muy importante): MySQL no puede impedir que dos funciones se
-- solapen en la misma sala. La aplicacion debe revisarlo dentro de una
-- transaccion, bloqueando primero la fila de la sala:
--
--     SELECT id FROM sala WHERE id = ? FOR UPDATE;
--
-- y luego buscar funciones de esa sala que choquen. Hay choque cuando:
--
--     inicio_existente < fin_nueva + 15 minutos
--     Y  fin_existente + 15 minutos > inicio_nueva
--
-- (los 15 minutos son la limpieza, RN-07 y RN-08).
-- Tambien valida en la app: inicio no puede estar en el pasado (RN-06) y la
-- sala y la pelicula deben estar activas.


-- =====================================================================
-- PARTE 5: FACTURAS (cada factura ES la compra: son lo mismo)
-- =====================================================================
-- Como una compra solo existe si el pago fue aprobado, y siempre genera una
-- factura, guardamos las dos cosas juntas. Una factura tiene:
--   - sus datos generales (tabla factura)
--   - los asientos vendidos (tabla factura_asiento)
--   - los productos vendidos (tabla factura_producto)
--
-- REGLA DE ORO: una factura NUNCA se modifica ni se borra (RN-16).
-- Por eso copiamos los precios y los nombres en texto: si manana cambia el
-- titulo de una pelicula o el precio de un producto, la factura vieja no cambia.
-- OJO APP: la aplicacion no debe tener ningun UPDATE ni DELETE sobre estas 3 tablas.

-- ---------------------------------------------------------------------
-- factura
-- ---------------------------------------------------------------------
CREATE TABLE factura (
    -- El "numero de factura" es este id (la app lo muestra con ceros: 000123).
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    fecha_hora        DATETIME      NOT NULL,

    -- Quien vendio. Guardamos el id Y el nombre copiado (por si luego cambia el nombre).
    usuario_id        BIGINT        NOT NULL,
    vendedor_nombre   VARCHAR(100)  NOT NULL,

    -- Datos opcionales del cliente (pueden quedar vacios).
    cliente_nombre    VARCHAR(100)  NULL,
    cliente_documento VARCHAR(30)   NULL,
    cliente_correo    VARCHAR(150)  NULL,

    metodo_pago       ENUM("EFECTIVO", "TARJETA") NOT NULL,

    -- Datos de la funcion. Si la factura es SOLO de confiteria, los 4 quedan NULL.
    funcion_id        BIGINT        NULL,
    pelicula_titulo   VARCHAR(150)  NULL,   -- copia del titulo
    sala_nombre       VARCHAR(50)   NULL,   -- copia del nombre de la sala
    funcion_inicio    DATETIME      NULL,   -- copia de la hora de inicio

    -- Totales. La relacion es: total = subtotal + iva.
    iva_porcentaje    DECIMAL(5,2)  NOT NULL,   -- IVA que se aplico en esta venta
    subtotal          DECIMAL(12,2) NOT NULL,   -- suma de precios SIN IVA
    iva               DECIMAL(12,2) NOT NULL,   -- total - subtotal
    total             DECIMAL(12,2) NOT NULL,   -- suma de precios CON IVA

    PRIMARY KEY (id),

    -- Esta llave unica extra (id + funcion_id) sirve para que factura_asiento
    -- pueda comprobar que sus asientos son de la MISMA funcion de la factura.
    CONSTRAINT uq_factura_id_funcion UNIQUE (id, funcion_id),

    CONSTRAINT ck_factura_totales CHECK (
        subtotal >= 0 AND iva >= 0 AND total = subtotal + iva
    ),
    CONSTRAINT ck_factura_iva_porcentaje CHECK (iva_porcentaje BETWEEN 0 AND 100),

    -- Los 4 datos de la funcion van juntos: o todos NULL o todos con valor.
    CONSTRAINT ck_factura_datos_funcion CHECK (
        (funcion_id IS NULL AND pelicula_titulo IS NULL
            AND sala_nombre IS NULL AND funcion_inicio IS NULL)
        OR
        (funcion_id IS NOT NULL AND pelicula_titulo IS NOT NULL
            AND sala_nombre IS NOT NULL AND funcion_inicio IS NOT NULL)
    ),

    -- No se puede borrar un usuario ni una funcion que tengan facturas.
    CONSTRAINT fk_factura_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_factura_funcion FOREIGN KEY (funcion_id)
        REFERENCES funcion (id) ON DELETE RESTRICT
);

-- Indice: hace rapidos los reportes por fecha (hoy, 7 dias, mes, rango).
CREATE INDEX idx_factura_fecha ON factura (fecha_hora);

-- ---------------------------------------------------------------------
-- factura_asiento: una fila por cada asiento vendido (es la "entrada").
-- ---------------------------------------------------------------------
CREATE TABLE factura_asiento (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    factura_id     BIGINT        NOT NULL,
    funcion_id     BIGINT        NOT NULL,
    asiento_id     BIGINT        NOT NULL,
    -- Precio de ESTA entrada en el momento de la venta.
    -- sin IVA = precio base + recargo del tipo de sala; final = sin IVA + IVA (2 decimales).
    precio_sin_iva DECIMAL(10,2) NOT NULL,
    precio_final   DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (id),

    -- *** ESTA ES LA REGLA ANTI DOBLE VENTA ***
    -- Un mismo asiento no puede venderse dos veces en la misma funcion.
    -- Si dos taquilleros compran el mismo asiento a la vez, MySQL deja pasar
    -- al primero y le da error al segundo (la app muestra "asiento ya vendido").
    -- OJO APP: inserta los asientos SIEMPRE ordenados por asiento_id. Si dos ventas
    -- insertan los mismos asientos en distinto orden, MySQL puede dar error de
    -- "deadlock"; la app debe tratarlo igual que "asiento ya vendido".
    CONSTRAINT uq_factura_asiento_funcion_asiento UNIQUE (funcion_id, asiento_id),

    CONSTRAINT ck_factura_asiento_precios CHECK (
        precio_sin_iva >= 0 AND precio_final >= 0
    ),

    -- Esta llave foranea compuesta asegura que la funcion del asiento sea la
    -- misma funcion de la factura (no se puede mezclar asientos de otra funcion).
    CONSTRAINT fk_factura_asiento_factura FOREIGN KEY (factura_id, funcion_id)
        REFERENCES factura (id, funcion_id) ON DELETE RESTRICT,
    CONSTRAINT fk_factura_asiento_asiento FOREIGN KEY (asiento_id)
        REFERENCES asiento (id) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- factura_producto: una fila por cada producto de confiteria vendido.
-- ---------------------------------------------------------------------
CREATE TABLE factura_producto (
    id              BIGINT            NOT NULL AUTO_INCREMENT,
    factura_id      BIGINT            NOT NULL,
    producto_id     BIGINT            NOT NULL,
    -- Copia del nombre del producto en el momento de la venta.
    producto_nombre VARCHAR(100)      NOT NULL,
    cantidad        SMALLINT UNSIGNED NOT NULL,
    -- Precio de UNA unidad en el momento de la venta.
    precio_sin_iva  DECIMAL(10,2)     NOT NULL,
    precio_final    DECIMAL(10,2)     NOT NULL,

    PRIMARY KEY (id),
    -- Un producto aparece una sola vez por factura (si compra 3, cantidad = 3).
    CONSTRAINT uq_factura_producto UNIQUE (factura_id, producto_id),
    CONSTRAINT ck_factura_producto_cantidad CHECK (cantidad >= 1),
    CONSTRAINT ck_factura_producto_precios CHECK (
        precio_sin_iva >= 0 AND precio_final >= 0
    ),
    CONSTRAINT fk_factura_producto_factura FOREIGN KEY (factura_id)
        REFERENCES factura (id) ON DELETE RESTRICT,
    CONSTRAINT fk_factura_producto_producto FOREIGN KEY (producto_id)
        REFERENCES producto (id) ON DELETE RESTRICT
);
-- OJO APP: una factura no puede quedar vacia (RN-04): debe tener al menos un
--          asiento o un producto. La base de datos no puede comprobarlo.


-- =====================================================================
-- PARTE 6: DATOS INICIALES
-- =====================================================================

-- ---------------------------------------------------------------------
-- Los 80 asientos: 8 filas (A-H) x 10 numeros (1-10).
-- Se arma combinando cada letra con cada numero (CROSS JOIN).
-- ---------------------------------------------------------------------
INSERT INTO asiento (fila, numero)
SELECT f.fila, n.numero
FROM (
    SELECT "A" AS fila UNION ALL SELECT "B" UNION ALL SELECT "C" UNION ALL SELECT "D"
    UNION ALL SELECT "E" UNION ALL SELECT "F" UNION ALL SELECT "G" UNION ALL SELECT "H"
) AS f
CROSS JOIN (
    SELECT 1 AS numero UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10
) AS n
ORDER BY f.fila, n.numero;

-- ---------------------------------------------------------------------
-- Generos (puedes agregar o quitar con INSERT/DELETE; no hay pantalla para esto en el MVP).
-- ---------------------------------------------------------------------
INSERT INTO genero (nombre) VALUES
    ("Acción"), ("Animación"), ("Aventura"), ("Ciencia ficción"),
    ("Comedia"), ("Documental"), ("Drama"), ("Fantasía"),
    ("Musical"), ("Romance"), ("Suspenso"), ("Terror");

-- ---------------------------------------------------------------------
-- Tipos de sala de EJEMPLO. El ADMIN los edita luego desde el sistema.
-- ---------------------------------------------------------------------
INSERT INTO tipo_sala (nombre, recargo) VALUES
    ("2D", 0),
    ("3D", 4000);

-- ---------------------------------------------------------------------
-- Configuracion inicial (valores de EJEMPLO). El ADMIN los cambia luego.
-- Es obligatorio que exista esta fila con id = 1.
-- ---------------------------------------------------------------------
INSERT INTO configuracion (id, precio_base_entrada, iva_porcentaje)
VALUES (1, 12000, 19.00);

-- ---------------------------------------------------------------------
-- Cuenta SUPER_ADMIN (RF-09).
-- NO escribimos la contrasena en este archivo: este script se sube a GitHub
-- y una contrasena (aunque sea un hash) no debe estar en el repositorio.
--
-- Pasos:
--   1. Genera un hash BCrypt de la contrasena que elijas con una pequena clase
--      Java en tu proyecto (BCryptPasswordEncoder.encode("tu-contrasena")).
--   2. Descomenta el INSERT de abajo, pega el hash y ejecutalo UNA vez.
-- ---------------------------------------------------------------------
-- INSERT INTO usuario (nombre, rol, username, password_hash)
-- VALUES ("Super Administrador", "SUPER_ADMIN", "superadmin", "PEGA_AQUI_EL_HASH_BCRYPT");


-- =====================================================================
-- PARTE 7: REGLAS DEL DOCUMENTO QUE VALIDA LA APLICACION (no la base de datos)
-- =====================================================================
-- MySQL no puede comprobar estas reglas por si solo. Spring Boot debe hacerlo
-- (dentro de una transaccion cuando modifica datos):
--
--   RN-04   Una factura no puede quedar vacia: al menos un asiento o un producto.
--   RN-06   Una funcion no puede empezar en el pasado.
--   RN-07/08 Funciones sin solaparse, con 15 minutos de limpieza (ver tabla funcion).
--   RN-09   La sala y la pelicula de una funcion nueva deben estar activas.
--   RN-10   No deshabilitar una sala con funciones vigentes (fin todavia no ocurrio).
--   RN-21   Solo se venden entradas de funciones vigentes (hasta que termine).
--   RN-23   Precio de entrada sin IVA = precio base + recargo del tipo de sala.
--   RN-24   El precio final = precio sin IVA x (1 + IVA), con 2 decimales.
--   RN-31   total = suma de lineas; subtotal = suma de precios sin IVA;
--           iva = total - subtotal. (La base de datos solo comprueba total = subtotal + iva.)
--   RN-26   Un SUPER_ADMIN no puede vender (factura.usuario_id no debe ser SUPER_ADMIN).
--   RN-25   Un usuario desactivado no vende ni inicia sesion.
--   RN-28   Una funcion con ventas no cambia de pelicula, sala, fecha ni hora.
--   RN-29/30 Una pelicula con funciones vigentes no cambia de duracion ni se desactiva.
--   RN-16   Las facturas no se modifican (ver PARTE 8 para protegerlas tambien aqui).
--   Funcion: fin = inicio + duracion de la pelicula (la calcula la app).
--   Factura: si tiene asientos, funcion_id no puede ser NULL (y al reves: si tiene
--            funcion_id debe tener al menos un asiento).
--   Correo del cliente: validar que tenga formato de correo.
--   Configuracion: nunca borrar la fila id = 1; solo hacer UPDATE.


-- =====================================================================
-- PARTE 8 (OPCIONAL): PROTEGER LAS FACTURAS CONTRA UPDATE Y DELETE
-- =====================================================================
-- Sin esto, un error de programacion podria editar o borrar una factura
-- (o quitar un asiento vendido, dejandolo libre para venderse otra vez).
-- Un "trigger" es una regla que MySQL ejecuta solo antes de cada UPDATE/DELETE;
-- aqui simplemente lo rechaza con un mensaje. INSERT sigue funcionando normal.
-- Para activarlo, quita los "-- " del inicio de cada linea.
-- Tienes que tener permiso TRIGGER en la base de datos.
--
-- Ojo: con esto activado, en desarrollo no podras limpiar datos de prueba con
-- DELETE; usa DROP TABLE (el bloque de reinicio del inicio del script).
--
-- CREATE TRIGGER trg_factura_no_update BEFORE UPDATE ON factura
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Una factura no se puede modificar";
-- CREATE TRIGGER trg_factura_no_delete BEFORE DELETE ON factura
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Una factura no se puede borrar";
--
-- CREATE TRIGGER trg_factura_asiento_no_update BEFORE UPDATE ON factura_asiento
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Los asientos de una factura no se pueden modificar";
-- CREATE TRIGGER trg_factura_asiento_no_delete BEFORE DELETE ON factura_asiento
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Los asientos de una factura no se pueden borrar";
--
-- CREATE TRIGGER trg_factura_producto_no_update BEFORE UPDATE ON factura_producto
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Los productos de una factura no se pueden modificar";
-- CREATE TRIGGER trg_factura_producto_no_delete BEFORE DELETE ON factura_producto
--     FOR EACH ROW SIGNAL SQLSTATE "45000" SET MESSAGE_TEXT = "Los productos de una factura no se pueden borrar";


-- =====================================================================
-- PARTE 9: COMPROBACIONES (ejecutalas despues para verificar)
-- =====================================================================
-- SHOW TABLES;                                  -- deben aparecer 12 tablas
-- SELECT COUNT(*) FROM asiento;                 -- debe dar 80
-- SELECT * FROM configuracion;                  -- debe dar 1 fila
-- SELECT * FROM genero;                         -- 12 generos
