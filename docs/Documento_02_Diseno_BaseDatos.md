# CineComfenalco · Diseño de Base de Datos

**Documento 02 · Modelo relacional — v1.2-lite Académica (validación en Java)**

| Campo | Valor |
| ----- | ----- |
| Versión | 0.2-lite (simplificado) |
| Tipo | Diseño de base de datos + validación en código — referencia de implementación |
| Stack | MySQL 8+ · InnoDB · utf8mb4 · JPA/Hibernate · Bean Validation |
| Estado | Pendiente por aprobar|

> Idea central: la BD solo garantiza lo que Java no puede garantizar en concurrencia (`PK, FK, UNIQUE, NOT NULL`). Todo lo demás se valida en Java con `if` y anotaciones. Sin `CHECK`, sin `TRIGGER`, sin SQL procedural. Más fácil de entender, escribir y corregir. A cambio se acepta que la BD sola no se defiende de datos metidos a mano.

---

## 1. Propósito y qué se simplificó

1. Este documento es la única referencia para crear las tablas (`db/01_init.sql`), las entidades JPA y los servicios que validan.
2. Supuestos congelados:
   - **S1. Sin reservas.** No hay tabla de reserva ni estado `PENDIENTE`. Venta atómica o nada.
   - **S2. Factura autosuficiente para mostrar.** UI/PDF solo leen `factura + detalle_factura`. No reconstruyen con datos actuales.
   - **S3. `compra` = operativo, `factura` = lo que se muestra.** Se crean juntas en la misma transacción.
   - **S4. Nivel académico.** Se prioriza claridad sobre blindaje. Lo que en producción sería `CHECK/TRIGGER` aquí es un `if` en el servicio (ver §15).
3. Riesgo asumido por escrito: si alguien inserta por consola MySQL o hay un bug en Java, la BD lo acepta (ej. `precio=-50`). En producción v1.1 lo rechazaría. En este MVP lo evitamos con pruebas, no con la BD.

---

## 2. Decisiones (versión simple)

| # | Decisión v1.2-lite | Por qué |
| - | ------------------ | ------- |
| D-01 | Imagen = `imagen_url VARCHAR(500)` + `imagen_tipo VARCHAR(10)` (`LOCAL`/`URL`/null). Validado en Java. | Sin esto no se sabe si es ruta o link. |
| D-02 | Sin reservas, `estado_compra` solo `PAGADA, CANCELADA`. Fallo = `ROLLBACK`. | Elimina `PENDIENTE` y asientos fantasma. |
| D-03 | `asiento_vendido` con PK `(id_funcion, numero_asiento)`. Es el único cerrojo real. `detalle_compra` = historial. | Si dos venden el mismo asiento, la PK lanza error y Java responde “ocupado”. Esto NO se puede mover a Java, se queda en BD. |
| D-04 | Snapshot en factura: se copian nombres, precios y totales al vender. Después no cambian. | Permite mostrar facturas viejas aunque renombren película o cambien precio. Se hace en Java al construir la factura. |
| D-05 | IVA 19% incluido. `sub_total = ROUND(total/1.19,2)`, `iva = total - sub_total`. Cálculo en Java. | Sin redondeo hay descuadres de 1 centavo. |
| D-06 | Borrado lógico: `activo/disponible=FALSE` o `estado=CANCELADA/ANULADA`. Nunca `DELETE` con historia. Validado en Java (no hay trigger). | Trazabilidad sin SQL complejo. |
| D-07 | Sin BLOB, solo URL/ruta. | — |
| D-08 | Sin `compra.id_funcion`. La función sale de los detalles `ENTRADA`. Solo confitería = cero `ENTRADA`. | Evita contradicción `compra.funcion=5` con `detalle.funcion=9`. |
| D-09 | Factura 1:1 con compra (`UNIQUE(id_compra)`) y `numero VARCHAR(30) UNIQUE` (`FAC-YYYY-MM-DD-NNNN`). | 1 compra = 1 factura. Consecutivo diario con tabla simple §5.12. |
| D-10 | `configuracion_precio` una sola fila `id=1`. Respetado en Java (siempre `findById(1)`). | Sin `CHECK`, es convención. |
| D-11 | Asientos por `capacidad` (1..500). Rango validado en Java. Limitación: sin mapa ni sillas dañadas. | Simple para MVP. |
| D-12 | Enums en Java (`enum` + `VARCHAR` en BD), sin `ENUM` MySQL ni `CHECK`. | Agregar valor no exige `ALTER TABLE`. |
| D-13 | `funcion.inicio/fin DATETIME`. Validado en Java (`fin>​inicio`, no solape, no pasado). | Más fácil que `DATE+TIME` separados. |
| D-14 | `ddl-auto=validate`, SQL manual en Git, flujo pull §12.1. Prohibido `update`. | Las entidades no crean tablas. |
| D-15 | `taquillero.documento UNIQUE`, `sala.nombre UNIQUE`, `producto.nombre UNIQUE`. | Evita “dos Juan” y duplicados. Se queda en BD porque es `UNIQUE` simple. |
| D-16 | Auditoría mínima: `created_at` en tablas clave. Sin triggers. | Trazabilidad básica. |

---

## 3. Diagrama

```
tipo_sala ──< sala ──< funcion >── pelicula
                 │         │
                 │         ├──< detalle_compra [ENTRADA] (historial)
                 │         └──< asiento_vendido (ocupados, PK natural)
                 │
taquillero ──< compra ──< detalle_compra [ENTRADA|CONFITERIA] >── producto
                 │
                 └──── factura (1:1, LO QUE MUESTRA LA UI)
                        └──< detalle_factura

administrador ── configuracion_precio.updated_by
secuencia_factura_dia (consecutivo diario)
```

Lectura UI: `factura + detalle_factura`. Ocupación: `asiento_vendido WHERE id_funcion=?`.

---

## 4. Convenciones BD (solo lo simple)

- `InnoDB`, `utf8mb4`, `utf8mb4_0900_ai_ci`.
- PK `BIGINT UNSIGNED AUTO_INCREMENT`, salvo `asiento_vendido` y `secuencia_factura_dia` con PK natural.
- FK `BIGINT UNSIGNED`, `ON UPDATE CASCADE ON DELETE RESTRICT`, salvo `detalle_compra → compra ON DELETE CASCADE` y `asiento_vendido → compra ON DELETE CASCADE` (para limpiar pruebas).
- Dinero `DECIMAL(10,2)`. Boolean `BOOLEAN`. Operativa `DATETIME`, auditoría `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`.
- Enums = `VARCHAR` en BD + `enum` Java. Nada de `ENUM` MySQL, nada de `CHECK`, nada de `TRIGGER`.
- Si una validación no es `PK/FK/UNIQUE/NOT NULL`, va en Java (§15).

---

## 5. Tablas (BD simple + dónde se valida en Java)

### 5.1 `tipo_sala`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | BIGINT UNSIGNED PK AUTO_INCREMENT | BD |
| nombre | VARCHAR(50) NOT NULL UNIQUE | BD + `TipoSalaService.crear`: no vacío, no duplicado |
| recargo | DECIMAL(10,2) NOT NULL DEFAULT 0.00 | `TipoSalaService`: `>=0` |
| activa | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| created_at/updated_at | TIMESTAMP | auto |

### 5.2 `sala`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| nombre | VARCHAR(50) NOT NULL UNIQUE | BD + `SalaService`: no vacío |
| id_tipo_sala | BIGINT UNSIGNED NOT NULL FK → tipo_sala | BD + `SalaService`: tipo debe existir y estar activo |
| capacidad | INT NOT NULL | `SalaService`: `1..500`. No reducir si hay funciones futuras (query + `if`) |
| activa | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| created_at/updated_at | TIMESTAMP | auto |

### 5.3 `pelicula`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| titulo | VARCHAR(150) NOT NULL, INDEX | `PeliculaService`: no vacío |
| anio | SMALLINT NULL | `PeliculaService`: si viene, `1895..2100` |
| sinopsis | TEXT NULL | — |
| duracion_min | INT NOT NULL | `PeliculaService`: `1..600` |
| genero | VARCHAR(20) NOT NULL | `enum Genero` Java: `ACCION,COMEDIA,DRAMA,TERROR,CIENCIA_FICCION,ANIMACION,DOCUMENTAL,OTRO` |
| clasificacion | VARCHAR(10) NOT NULL | `enum Clasificacion`: `0_PLUS,7_PLUS,15_PLUS,18_PLUS` |
| imagen_url | VARCHAR(500) NULL | `PeliculaService`: si hay url, `imagen_tipo` obligatorio |
| imagen_tipo | VARCHAR(10) NULL | `if LOCAL/URL` |
| activa | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| created_at/updated_at | TIMESTAMP | auto |

### 5.4 `funcion`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| id_pelicula | FK NOT NULL → pelicula | BD + `FuncionService`: película activa |
| id_sala | FK NOT NULL → sala | BD + `FuncionService`: sala activa |
| inicio | DATETIME NOT NULL | `FuncionService`: no en pasado al crear |
| fin | DATETIME NOT NULL | `FuncionService`: `fin>inicio` y `fin == inicio+duracion_min` |
| precio_snapshot | DECIMAL(10,2) NOT NULL | `FuncionService`: `= base + recargo`, `>=0`. Congelado. |
| activa | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| created_at/updated_at | TIMESTAMP | auto |

Índices simples: `(id_sala, inicio)`, `(id_pelicula, inicio)`.
Solape + 15min limpieza: `FuncionService.programar()` hace `findSolape(idSala, inicio-15min, fin+15min)` y si encuentra algo lanza error. Limitación académica: con 2 programaciones exactamente simultáneas podría colarse un solape; en producción se usaría bloqueo. Para el MVP es aceptable.

### 5.5 `producto`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| nombre | VARCHAR(100) NOT NULL UNIQUE | BD + no vacío |
| descripcion | VARCHAR(500) NULL | — |
| precio | DECIMAL(10,2) NOT NULL | `ProductoService`: `>0` |
| imagen_url/imagen_tipo | VARCHAR(500)/VARCHAR(10) NULL | misma regla pareada que película |
| disponible | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| created_at/updated_at | TIMESTAMP | auto |

### 5.6 `taquillero`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| nombre | VARCHAR(100) NOT NULL | no vacío |
| documento | VARCHAR(20) NOT NULL UNIQUE | BD + no vacío. Selector de venta por documento. |
| activo | BOOLEAN NOT NULL DEFAULT TRUE | BD. Prohibido vender si `FALSE` |
| created_at | TIMESTAMP | auto |

### 5.7 `administrador`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| username | VARCHAR(50) NOT NULL UNIQUE | BD + `>=4` caracteres en Java |
| password_hash | VARCHAR(255) NOT NULL | BCrypt en Java, nunca plano. `longitud>=60` en Java. |
| nombre | VARCHAR(100) NULL | — |
| activo | BOOLEAN NOT NULL DEFAULT TRUE | BD |
| es_super_admin | BOOLEAN NOT NULL DEFAULT FALSE | `AdminService`: solo 1 en `TRUE` (query `countBySuperAdmin` + `if`). Sin columna generada. |
| created_at/updated_at | TIMESTAMP | auto |

### 5.8 `configuracion_precio`
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | BIGINT UNSIGNED PK | Java siempre usa `id=1`. No insertar otro. |
| precio_base_entrada | DECIMAL(10,2) NOT NULL | `ConfigService`: `>0` |
| iva_porcentaje | DECIMAL(5,2) NOT NULL DEFAULT 19.00 | `ConfigService`: `0..100` |
| updated_at | TIMESTAMP auto | auto |
| updated_by | BIGINT UNSIGNED NULL FK → administrador | quién cambió |

### 5.9 `compra` (operativa, no se muestra)
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| id_taquillero | FK NOT NULL → taquillero | BD + taquillero activo en Java |
| metodo_pago | VARCHAR(10) NOT NULL | `enum MetodoPago`: `EFECTIVO,TARJETA` |
| estado | VARCHAR(10) NOT NULL DEFAULT 'PAGADA' | `enum EstadoCompra`: `PAGADA,CANCELADA`. Sin `PENDIENTE`. |
| nombre_cliente/documento_cliente | VARCHAR(100)/VARCHAR(50) NULL | opcionales, no crean cliente |
| fecha_hora | DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP | auto |
| motivo_cancelacion | VARCHAR(255) NULL | `if CANCELADA exige motivo; si PAGADA debe ser null` |
| created_at/updated_at | TIMESTAMP | auto |

Sin `id_funcion`. Regla `no vacía` en `VentaService`: si no hay detalles → `throw` y `ROLLBACK`.

### 5.10 `detalle_compra` (historial)
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| id_compra | FK NOT NULL → compra ON DELETE CASCADE, INDEX | BD |
| tipo | VARCHAR(10) NOT NULL | `enum TipoDetalle` |
| id_funcion | FK NULL → funcion, INDEX | `VentaService`: `ENTRADA` exige no-null, `CONFITERIA` exige null |
| numero_asiento | INT NULL | `ENTRADA` exige `>=1` y `<=capacidad`; `CONFITERIA` exige null |
| id_producto | FK NULL → producto | `ENTRADA` null, `CONFITERIA` no-null y disponible |
| cantidad | INT NOT NULL DEFAULT 1 | `ENTRADA =1`, `CONFITERIA >0` |
| precio_unitario | DECIMAL(10,2) NOT NULL | `ENTRADA = funcion.precio_snapshot`, `CONFITERIA = producto.precio`, `>=0` |

BD además: `UNIQUE(id_funcion, numero_asiento)`. Los `(NULL,NULL)` de confitería no chocan. Es la segunda barrera además de `asiento_vendido`, y es solo un `UNIQUE` simple.

### 5.11 `asiento_vendido` (ocupados, cerrojo)
| Columna | Tipo BD |
| ------- | ------- |
| id_funcion | PK (1/2), FK → funcion |
| numero_asiento | PK (2/2) INT NOT NULL |
| id_compra | NOT NULL FK → compra ON DELETE CASCADE, INDEX |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP |

```sql
PRIMARY KEY (id_funcion, numero_asiento)
```
Solo `PAGADA` vigente. Mapa libre = `SELECT numero_asiento ... WHERE id_funcion=?`. Al anular se borran sus filas. Nunca `UPDATE`.

### 5.12 `secuencia_factura_dia` (consecutivo simple)
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| fecha | DATE PK | BD |
| ultimo_consecutivo | INT NOT NULL DEFAULT 0 | `VentaService`: `>=0`, `n=ultimo+1` |
| updated_at | TIMESTAMP auto | auto |

Lógica Java en transacción: `INSERT IGNORE (hoy,0)` → `SELECT ultimo` → `n=ultimo+1` → `UPDATE` → `numero=FAC-YYYY-MM-DD-NNNN`. Si dos facturan a la vez y chocan en `UNIQUE(numero)`, capturar y reintentar una vez. Limitación académica documentada.

### 5.13 `factura` (lo que muestra la UI, inmutable por convención)
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| id_compra | BIGINT UNSIGNED NOT NULL UNIQUE FK → compra | BD (1:1) |
| numero | VARCHAR(30) NOT NULL UNIQUE | BD + formato en Java |
| fecha_hora | DATETIME NOT NULL | auto (emisión) |
| estado | VARCHAR(10) NOT NULL DEFAULT 'ACTIVA' | `ACTIVA,ANULADA` en Java |
| motivo_anulacion | VARCHAR(255) NULL | `ANULADA` exige motivo |
| metodo_pago | VARCHAR(10) NOT NULL | copiado de compra |
| sub_total/iva/total | DECIMAL(10,2) NOT NULL | `total>0`, `total==sub_total+iva`, `sub_total=ROUND(total/1.19,2)` en Java |
| nombre_cliente/documento_cliente | VARCHAR NULL | copiado de compra |
| id_taquillero | FK NOT NULL → taquillero | BD (trazabilidad) |
| taquillero_nombre | VARCHAR(100) NOT NULL | snapshot para mostrar |
| id_funcion | FK NULL → funcion | null = solo confitería |
| pelicula_titulo/sala_nombre/tipo_sala_nombre | VARCHAR NULL | snapshots; null si solo confitería |
| funcion_inicio | DATETIME NULL | snapshot |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | auto, sin `updated_at` |

Inmutabilidad en Java: no existe `updateFactura()` ni endpoint `PUT /facturas`. Solo `anular()` que hace `ACTIVA→ANULADA`. Si alguien intenta otro cambio → `throw`.

Consulta UI: `findByNumero()` + `findByFacturaOrderById()`.

### 5.14 `detalle_factura` (líneas para mostrar)
| Columna | Tipo BD | Validado en |
| ------- | ------- | ----------- |
| id | PK | BD |
| id_factura | FK NOT NULL → factura, INDEX | BD |
| tipo | VARCHAR(10) NOT NULL | enum |
| cantidad | INT NOT NULL | `>0`, `ENTRADA=1` |
| precio_unitario | DECIMAL(10,2) NOT NULL | `>=0`, copiado de `detalle_compra` |
| subtotal_linea | DECIMAL(10,2) NOT NULL | `=cantidad*precio` en Java |
| numero_asiento | INT NULL | solo `ENTRADA` |
| pelicula_titulo/sala_nombre/tipo_sala_nombre | VARCHAR NULL | solo `ENTRADA` |
| funcion_inicio | DATETIME NULL | solo `ENTRADA` |
| id_producto | FK NULL → producto | solo `CONFITERIA` |
| producto_nombre | VARCHAR(100) NULL | solo `CONFITERIA` |
| descripcion_legible | VARCHAR(500) NOT NULL | texto listo UI: `Entrada · Dune 2 · Sala 3 (VIP) · 2026-09-10 19:00 · Asiento 12` |

---

## 6. Enums (solo Java)

```java
enum Genero { ACCION, COMEDIA, DRAMA, TERROR, CIENCIA_FICCION, ANIMACION, DOCUMENTAL, OTRO }
enum Clasificacion { PLUS_0, PLUS_7, PLUS_15, PLUS_18 } // en BD se guarda "0_PLUS..." como String
enum MetodoPago { EFECTIVO, TARJETA }
enum EstadoCompra { PAGADA, CANCELADA }
enum EstadoFactura { ACTIVA, ANULADA }
enum TipoDetalle { ENTRADA, CONFITERIA }
```
En entidad: `@Enumerated(EnumType.STRING)`.

---

## 7. Índices mínimos (solo lo simple)

- PKs + `UNIQUE`: `asiento_vendido(id_funcion,numero_asiento)`, `detalle_compra(id_funcion,numero_asiento)`, `factura(id_compra)`, `factura(numero)`, `administrador(username)`, `taquillero(documento)`, `sala(nombre)`, `producto(nombre)`, `tipo_sala(nombre)`.
- INDEX ayuda: `funcion(id_sala,inicio)`, `compra(id_taquillero,fecha_hora)`, `detalle_factura(id_factura)`.

---

## 8. Venta sin reservas (todo en Java, una transacción)

```java
@Transactional
public Factura vender(VentaDTO dto) {
  // 1. Sala y función activas
  Sala sala = salaRepo.findById(dto.idSala()).orElseThrow();
  if (!sala.isActiva()) throw new Negocio("SALA_INACTIVA");
  Funcion f = funcionRepo.findById(dto.idFuncion()).orElseThrow(); // null si solo confitería
  if (f != null) {
    if (!f.isActiva()) throw new Negocio("FUNCION_CANCELADA");
    if (LocalDateTime.now().isAfter(f.getInicio().plusMinutes(20))) throw new Negocio("VENTANA_CERRADA");
  }
  // 2. Asientos: rango, no duplicados en pedido, no ocupados
  for (int a : dto.asientos()) {
    if (a < 1 || a > sala.getCapacidad()) throw new Negocio("ASIENTO_FUERA_RANGO");
  }
  if (asientoRepo.existsOcupado(f.getId(), dto.asientos())) throw new Negocio("ASIENTO_OCUPADO");
  // 3. Productos disponibles
  // 4. Totales: ENTRADA=precio_snapshot, CONFITERIA=precio actual
  // 5. INSERT compra PAGADA + detalles (validar tipo/columnas §5.10 con ifs)
  // 6. INSERT asiento_vendido por cada asiento
  //    try { ... } catch (DataIntegrityViolationException e) { throw new Negocio("ASIENTO_OCUPADO"); }
  // 7. Consecutivo factura (§5.12) + snapshots + INSERT factura + detalles
  // 8. COMMIT automático; cualquier throw = ROLLBACK, no queda PENDIENTE
}
@Transactional
public void anular(Long idCompra, String motivo) {
  // compra PAGADA→CANCELADA + motivo, DELETE asiento_vendido de esa compra, factura ACTIVA→ANULADA + motivo
}
```

Disponibilidad para pintar mapa: `SELECT numero_asiento FROM asiento_vendido WHERE id_funcion=?`.

---

## 9. Precios e IVA (Java)

```java
BigDecimal total = suma(cantidad * precio_unitario); // precios con IVA incluido
BigDecimal sub = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
BigDecimal iva = total.subtract(sub);
if (total.compareTo(sub.add(iva)) != 0) throw new IllegalStateException(" descuadre IVA");
```
Probar con 1000, 10000, 11500, 19999.

---

## 10. Borrado lógico (Java, sin triggers)

| Entidad | Acción |
| ------- | ------ |
| pelicula, sala, producto, taquillero, administrador, tipo_sala | `setActivo(false)` / `setDisponible(false)`. Nunca `delete()` si tiene historia. `delete()` solo permitido en pruebas sin ventas. |
| funcion | Con ventas → `setActiva(false)`. Sin ventas → permitido `delete()`. Verificar con `existsByFuncion()` + `if`. |
| compra/factura | Nunca `delete()`. Solo `anular()` §8. |

Consultas de venta filtran `where activa=true / disponible=true`.

---

## 11. JPA mínimo + validación

```java
@Entity
public class Producto {
  @Id @GeneratedValue(strategy = IDENTITY) private Long id;
  @NotBlank private String nombre;
  @DecimalMin("0.01") private BigDecimal precio;
  private Boolean disponible = true;
}
```
Servicios lanzan `NegocioException` → controlador devuelve `409/422` para `ASIENTO_OCUPADO, SOLAPE, VENTANA_CERRADA, INACTIVO`, `500` para violación de `UNIQUE` inesperada (bug).

`spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`.

---

## 12. properties + flujo pull

```properties
spring.datasource.url=jdbc:mysql://<host>:3306/<bd>?useSSL=true&serverTimezone=America/Bogota&characterEncoding=utf8mb4
spring.datasource.username=<usuario>
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.thymeleaf.cache=true
```
Prohibido `update/create-drop`.

### 12.1 Flujo manual por pull (simple)

```
db/
  01_init.sql
  02_seed.sql
  03_alter_<que>_<fecha>.sql
```
Tabla `schema_version(version INT PK, script VARCHAR(100), aplicado_en TIMESTAMP)`.

- Responsable: crea `03_*.sql` simple, lo prueba, commit/push, termina con `INSERT INTO schema_version`.
- Resto: `git pull` → `SELECT * FROM schema_version` → aplicar con `mysql < db/03_*.sql` los faltantes en orden → arrancar (si `validate` falla, falta un script).
- Nunca editar un script ya en `main`. Producción se actualiza a mano antes del jar.

---

## 13. DDL ejemplo (plano, copiar a `db/01_init.sql`)

```sql
CREATE TABLE sala (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE,
  id_tipo_sala BIGINT UNSIGNED NOT NULL,
  capacidad INT NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_sala_tipo FOREIGN KEY (id_tipo_sala)
    REFERENCES tipo_sala(id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE asiento_vendido (
  id_funcion BIGINT UNSIGNED NOT NULL,
  numero_asiento INT NOT NULL,
  id_compra BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_funcion, numero_asiento),
  CONSTRAINT fk_asv_funcion FOREIGN KEY (id_funcion) REFERENCES funcion(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_asv_compra FOREIGN KEY (id_compra) REFERENCES compra(id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Resto de tablas siguen el mismo patrón §5, sin CHECK ni TRIGGER.
```

---

## 14. Semillas (`db/02_seed.sql`, con `INSERT IGNORE`)

1. SuperAdmin (`es_super_admin=TRUE`, BCrypt).
2. `configuracion_precio (1, base, 19.00)`.
3. `tipo_sala` + `sala`.
4. `taquillero` con documento.
5. `pelicula, producto`.

---

## 15. Matriz: qué valida cada método Java (contrato del equipo)

| Método | Valida (con `if` + queries) | Error |
| ------ | ---------------------------- | ----- |
| `TipoSalaService.crear/actualizar` | nombre no vacío, `recargo>=0` | `422` |
| `SalaService.crear/actualizar` | nombre no vacío, tipo existe+activo, `capacidad 1..500`, no reducir con funciones futuras (`existsFutura`) | `422/409` |
| `PeliculaService.crear` | título no vacío, `duracion 1..600`, `anio 1895..2100` si viene, `imagen_url↔imagen_tipo` pareados | `422` |
| `ProductoService.crear/vender` | nombre no vacío, `precio>0`, `disponible==true` al vender | `422/409` |
| `FuncionService.programar` | sala+película activas, `inicio` futuro, `fin>inicio`, `fin==inicio+duracion`, `precio>=0`, sin solape (`findSolape` con ±15min) | `422/409 SOLAPE` |
| `VentaService.vender` | taquillero activo, 1 método pago, ≥1 detalle, reglas §5.10 por tipo, asientos `1..capacidad` sin duplicados ni ocupados, productos disponibles, ventana 20min, totales IVA §9, snapshots factura §5.13, consecutivo §5.12. Captura `DuplicateKey→ASIENTO_OCUPADO`, `Duplicate numero→reintento` | `409/422` |
| `VentaService.anular` | compra `PAGADA`, motivo obligatorio, borra `asiento_vendido`, factura → `ANULADA` | `409` |
| `FacturaService.mostrar` | solo lectura `findByNumero + findDetalles`. Sin `update/delete` | — |
| `AdminService.crear` | `username>=4`, hash `>=60`, un solo `super_admin` (`count+if`) | `422/409` |
| `ConfigService.actualizar` | siempre `id=1`, `base>0`, `iva 0..100` | `422` |

Si violan un `UNIQUE/FK` de BD es bug Java → `500` + log, salvo `ASIENTO_OCUPADO` que es `409` esperado.

---

## 16. Pendientes y qué sería producción

- [ ] `db/01_init.sql` completo + probado en 2 PCs desde `git clone`.
- [ ] Entidades + servicios según §15 + test doble-venta (2 hilos mismo asiento → 1 ok + 1 `409`).
- [ ] DTO solo desde factura + PDF + test IVA.
- [ ] Backup `mysqldump` + roles (app sin `ALTER/DROP`).

Fin del Documento 02 v1.2-lite.
