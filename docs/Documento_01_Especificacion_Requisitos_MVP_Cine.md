# CineComfenalco - Sistema Web de Gestión Integral para un cine

## Documento 01 · Especificación de Requisitos del MVP (Sujeto a cambios)

| Versión | 0.1                                                                 |
| ------- | ------------------------------------------------------------------- |
| Tipo    | Especificación de requisitos                                        |
| Equipo  | 3 integrantes                                                       |
| Stack   | Java · Spring Boot · Thymeleaf · REST · JPA/Hibernate · MySQL       |
| Estado  | Revisado · pendiente de confirmar las reglas marcadas con †         |

> Propósito del documento: Establecer, de forma clara y verificable, qué debe hacer el sistema, qué reglas debe respetar, qué queda fuera y cuáles son las condiciones que deben cumplirse antes de comenzar la implementación.

Este documento constituye la referencia funcional inicial del proyecto. Las decisiones de implementación detalladas (modelo relacional definitivo, arquitectura de paquetes, endpoints, transacciones, seguridad y estrategia de concurrencia) se desarrollarán en documentos posteriores.


# Tabla de contenido

- 1\. Propósito y contexto
- 2\. Objetivos del sistema
- 3\. Alcance del MVP
- 4\. Fuera de alcance
- 5\. Actores y permisos
- 6\. Conceptos fundamentales del dominio
- 7\. Requisitos funcionales
- 8\. Reglas de negocio
- 9\. Flujos operativos
- 10\. Facturación, pagos e IVA
- 11\. Asientos y concurrencia
- 12\. Estadísticas y reportes
- 13\. Impresión
- 14\. Requisitos de datos e integridad histórica
- 15\. Requisitos no funcionales
- 16\. Restricciones tecnológicas
- 17\. Criterios de aceptación del MVP
- 18\. Casos límite y comportamientos esperados
- 19\. Decisiones consolidadas
- 20\. Pendientes para el diseño técnico

# 1. Propósito y contexto

El sistema tiene como finalidad centralizar la operación de un cine que actualmente gestiona de forma manual y dispersa la venta de entradas, la selección de asientos, la comercialización de productos de confitería, la facturación y el seguimiento de ingresos.

La situación actual genera errores de registro, pérdida de tiempo, dificultad para relacionar las ventas con sus facturas y dependencia de procesos manuales o archivos independientes. El sistema propuesto busca proporcionar un único entorno web para ejecutar y consultar estas operaciones.

# 2. Objetivos del sistema

## Objetivo general

Construir un sistema web que permita gestionar íntegramente la operación de taquilla y administración de un cine, centralizando programación, ventas, confitería, facturación, precios y reportes en una única aplicación.

## Objetivos específicos

- Analizar los requerimientos funcionales y no funcionales para el desarrollo de un sistema web con Java para la gestión y control de la venta de boletas y servicios de cafetería en CineComfenalco.
- Diseñar un sistema web con Java para la gestión y control de la venta de boletas y servicios de cafetería en CineComfenalco.
- Codificar el software en el lenguaje de programación Java para el desarrollo de un sistema web para la gestión y control de la venta de boletas y servicios de cafetería en CineComfenalco.
- Verificar el sistema web con Java para la gestión y control de la venta de boletas y servicios de cafetería en CineComfenalco.
- Implementar el sistema web con Java para la gestión y control de la venta de boletas y servicios de cafetería en CineComfenalco.

# 3. Alcance del MVP

| **Área**       | **Incluido**                                                                                     |
| -------------- | ------------------------------------------------------------------------------------------------ |
| Películas      | CRUD, clasificación, género (catálogo), duración, sinopsis, imagen y retiro de la cartelera      |
| Cartelera      | Consulta de las películas activas; una película puede existir sin funciones                      |
| Funciones      | CRUD, programación por sala/fecha/hora y cálculo automático de la hora de finalización           |
| Salas          | CRUD administrativo, tipo administrable y disponibilidad                                         |
| Asientos       | Distribución uniforme predefinida (8 filas × 10 asientos) y selección manual                     |
| Confitería     | CRUD operativo, precios e imágenes                                                               |
| Ventas         | Sólo entradas, sólo confitería o compra mixta                                                    |
| Pagos          | Efectivo o tarjeta, un único método por compra (pago simulado)                                   |
| Facturación    | Generación, consulta, detalle e impresión                                                        |
| Estadísticas   | Ingresos, ventas, ocupación y desgloses relevantes con gráficos                                  |
| Administración | Login, salas, taquilleros, precios e IVA, estadísticas e historial de facturas                   |
| Taquilleros    | CRUD administrativo e identificación al realizar ventas                                          |
| Administradores | CRUD de cuentas administrativas, exclusivo del rol SUPER_ADMIN                                  |

# 4. Fuera de alcance

- Aplicación móvil.
- Diseño responsive.
- Pasarela de pago real.
- Almacenamiento de números de tarjeta, CVV u otros datos bancarios.
- Facturación electrónica.
- Integración con autoridades tributarias.
- Inteligencia artificial.
- Múltiples sucursales.
- Control de inventario o stock.
- Reservas anticipadas para clientes.
- Bloqueo temporal de asientos antes de confirmar la compra.
- Registro de compras pendientes, canceladas o fallidas.
- Tickets de ingreso independientes de la factura.
- Auditoría detallada de acciones administrativas.
- Generación de PDF como requisito del sistema.
- Salas con generación de asientos personalizados.
- Gestión y conexión entre sucursales.
- Gestión de cuentas SUPER_ADMIN desde el sistema (la cuenta se crea mediante un script de inicialización).

# 5. Actores y permisos

Existen tres roles: TAQUILLERO, ADMIN y SUPER_ADMIN.

## Taquillero

El taquillero no requiere autenticación mediante contraseña. Antes de realizar una operación deberá seleccionar su nombre. Su identidad se asociará a la compra y, por extensión, a la factura.

- Gestionar películas.
- Gestionar funciones.
- Gestionar productos de confitería.
- Realizar ventas.
- Seleccionar asientos.
- Registrar datos opcionales del cliente.
- Seleccionar método de pago.
- Generar e imprimir facturas.

## Administrador

El administrador debe autenticarse. Todas las cuentas administrativas comunes comparten el rol ADMIN. Cuando realiza una venta, su identidad es la de su sesión (no selecciona un nombre).

- Todas las operaciones disponibles para el taquillero.
- CRUD de salas y de tipos de sala.
- CRUD de taquilleros.
- Gestión de precios: precio base de la entrada, recargo por tipo de sala e IVA.
- Consulta de reportes y estadísticas.
- Consulta del historial de facturas.

## Super administrador

El super administrador debe autenticarse y tiene el rol SUPER_ADMIN. Su única función es gestionar las cuentas de administradores.

- CRUD de administradores.
- No realiza ventas, no gestiona precios y no consulta reportes ni facturas.

> Regla: no existen roles adicionales dentro del MVP. La separación de permisos se basa en operación de taquilla (TAQUILLERO), funciones administrativas (ADMIN) y gestión de cuentas administrativas (SUPER_ADMIN).

# 6. Conceptos fundamentales del dominio

## Película

Producción cinematográfica registrada en el sistema. Tiene un género tomado de un catálogo y puede estar activa o inactiva. Las películas activas aparecen en cartelera, aunque no tengan ninguna función.

## Función

Programación de una película en una sala, con fecha y hora determinadas. Su hora de finalización se calcula a partir de la duración de la película. Una función está **vigente** mientras su hora de finalización no haya ocurrido (incluso si ya empezó).

## Sala

Espacio físico donde se proyecta una función. El administrador puede crear tantas salas como necesite. Su tipo determina un recargo sobre el precio base. Todas las salas tienen la misma distribución de asientos.

## Tipo de sala

Categoría administrable de salas (por ejemplo, 2D o 3D) con un recargo configurable.

## Asiento

Posición dentro de una sala, identificada por fila (A–H) y número (1–10).

## Compra

Conjunto de bienes adquiridos en una única operación. Puede pertenecer a una función cuando contiene entradas, y puede contener productos de confitería. También puede contener únicamente confitería. Una compra sólo existe cuando su pago fue aprobado; no existen compras pendientes, canceladas ni fallidas.

## Factura

Documento final de una compra aprobada. Toda compra aprobada genera exactamente una factura. Una vez generada no puede modificarse.

## Cartelera

Vista de las películas activas; no constituye una entidad independiente.

# 7. Requisitos funcionales

## RF-01 · Gestión de películas

- El sistema debe permitir crear, consultar y editar películas.
- El sistema debe permitir eliminar películas que no tengan ninguna función asociada.
- Una película que tenga funciones (históricas o vigentes) no se elimina: se retira de la cartelera marcándola como inactiva.
- Una película debe tener título, sinopsis, duración, género, clasificación e imagen.
- Cada película tendrá exactamente un género, tomado de un catálogo de géneros. La administración del catálogo desde la interfaz no forma parte del MVP; el catálogo se carga de forma inicial.
- La clasificación debe pertenecer a 0+, 7+, 15+ o 18+.
- La imagen podrá provenir de una carga desde el dispositivo o de una URL.
- Una película puede existir sin ninguna función.
- Sólo las películas activas aparecen en cartelera y pueden programarse en nuevas funciones.
- No se puede desactivar una película con funciones vigentes. 
- No se puede modificar la duración de una película con funciones vigentes. 

## RF-02 · Gestión de funciones

- El sistema debe permitir crear, consultar y editar funciones.
- El sistema debe permitir eliminar funciones que no tengan ventas.
- Una función debe referenciar exactamente una película activa y una sala activa.
- La hora de finalización se calcula automáticamente: hora de inicio + duración de la película.
- La sala permanece ocupada hasta la hora de finalización más 15 minutos de limpieza.
- No se deben crear funciones con inicio anterior al momento actual.
- No se deben crear funciones que se solapen en la misma sala, considerando la ocupación hasta el fin más los 15 minutos de limpieza.
- Una función con ventas no puede cambiar de película, sala, fecha ni hora. 
- Al editar una función sin ventas, se vuelven a validar todas las reglas anteriores.
- El precio de la entrada no se guarda en la función: se calcula al momento de la venta (ver RF-08 y RN-23).

## RF-03 · Gestión de salas y tipos de sala

- El administrador debe poder crear, consultar, editar y eliminar salas según las reglas de integridad.
- Una sala sin función alguna (pasada o vigente) puede eliminarse; si tiene historial sólo puede deshabilitarse.
- El administrador debe poder crear, consultar, editar y eliminar tipos de sala. Un tipo en uso por alguna sala no puede eliminarse.
- Cada tipo de sala tendrá un recargo configurable, expresado como monto fijo sin IVA.
- Una sala puede estar activa o deshabilitada.
- No se podrá crear una función en una sala deshabilitada.
- No se podrá deshabilitar una sala que tenga funciones vigentes, hayan o no tenido ventas.
- Todas las salas tienen la misma distribución de 80 asientos: filas A–H y números 1–10.

## RF-04 · Gestión de productos

- El sistema debe permitir crear, consultar, editar y retirar productos.
- Cada producto debe tener nombre, descripción, precio (sin IVA) e imagen.
- El sistema no gestionará inventario.
- Un producto podrá estar o no disponible. Retirar un producto lo marca como no disponible; no se elimina físicamente.

## RF-05 · Gestión de taquilleros

- El administrador debe poder crear, consultar, editar y eliminar taquilleros.
- El nombre de cada taquillero debe ser único.
- Un taquillero con historial no debe eliminarse físicamente; debe desactivarse. Un taquillero sin historial puede eliminarse.
- Un taquillero desactivado no aparece en la lista de selección ni puede realizar ventas.
- El taquillero seleccionará su nombre al realizar una venta.

## RF-06 · Venta

- El sistema debe permitir seleccionar una función vigente: se pueden vender entradas hasta que la función termine, aunque ya haya empezado.
- Debe permitir seleccionar manualmente uno o varios asientos.
- Debe permitir añadir productos de confitería opcionalmente, con cantidades enteras mayores o iguales a 1.
- Debe permitir realizar compras únicamente de confitería.
- Debe permitir registrar datos del cliente como opcionales: nombre, documento y correo electrónico. Si se informa el correo, debe tener un formato válido.
- Debe permitir seleccionar efectivo o tarjeta.
- Una compra utilizará un único método de pago.
- No se permitirá una compra completamente vacía.
- Al confirmar, el sistema verifica que los asientos sigan disponibles; si alguno ya fue vendido en otra compra, rechaza la operación y el vendedor debe elegir otro asiento.

## RF-07 · Facturación

- Una compra aprobada debe generar una factura.
- Una compra cancelada o con pago fallido no se registra y no genera factura.
- La factura debe conservar los precios históricos, la tasa de IVA aplicada y los datos descriptivos de la operación (ver sección 14).
- La factura debe incluir el detalle completo de la operación.
- Una factura generada no puede modificarse.

## RF-08 · Administración de precios e IVA

- El administrador debe poder modificar el precio base de entrada.
- El administrador debe poder modificar el recargo de cada tipo de sala.
- El administrador debe poder modificar el porcentaje de IVA.
- Todos los precios configurados (base, recargos y productos) se registran sin IVA; el IVA vigente se suma al calcular el precio final (ver sección 10).
- Los cambios se aplican a las ventas posteriores y no modifican facturas existentes.

## RF-09 · Gestión de administradores

- El SUPER_ADMIN debe poder crear, consultar y editar cuentas de administradores, incluido el restablecimiento de contraseña.
- El SUPER_ADMIN debe poder desactivar y reactivar administradores.
- Un administrador con historial (ventas) no se elimina físicamente; se desactiva. Un administrador sin historial puede eliminarse.
- Un administrador desactivado no puede iniciar sesión.

## RF-10 · Autenticación

- Los roles ADMIN y SUPER_ADMIN deben autenticarse para acceder a sus funciones.
- Las contraseñas se almacenan con un mecanismo de hash seguro.
- Los taquilleros no se autentican; se identifican seleccionando su nombre.

# 8. Reglas de negocio

| **ID** | **Regla**                                                                                                                                              |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| RN-01  | Una compra puede corresponder a una única función.                                                                                                     |
| RN-02  | Una compra puede contener entradas, productos o ambos.                                                                                                 |
| RN-03  | Una compra puede contener únicamente productos de confitería.                                                                                          |
| RN-04  | Una compra vacía no es válida.                                                                                                                         |
| RN-05  | Un asiento vendido no puede volver a venderse en la misma función. Si dos compras compiten por el mismo asiento, sólo se acepta la primera en confirmarse. |
| RN-06  | No se pueden programar funciones con inicio anterior al momento actual.                                                                                |
| RN-07  | No se permiten solapamientos en una misma sala; la ocupación de una función va desde su inicio hasta su fin más 15 minutos.                            |
| RN-08  | Debe existir un intervalo de limpieza de 15 minutos entre el fin de una función y el inicio de la siguiente en la misma sala.                          |
| RN-09  | Una sala deshabilitada no puede recibir nuevas funciones.                                                                                              |
| RN-10  | Una sala con funciones vigentes no puede deshabilitarse.                                                                                               |
| RN-11  | Una función con ventas no puede eliminarse.                                                                                                            |
| RN-12  | Un taquillero con historial no puede eliminarse físicamente; se desactiva.                                                                             |
| RN-13  | Una película con funciones no puede eliminarse; se retira de la cartelera marcándola como inactiva.                                                    |
| RN-14  | Cada compra utiliza un único método de pago.                                                                                                           |
| RN-15  | Los precios configurados (base de entrada, recargos y productos) no incluyen IVA; el IVA vigente se suma al calcular el precio final.                  |
| RN-16  | Las facturas son inmutables una vez generadas.                                                                                                         |
| RN-17  | Los cambios posteriores de precios o de IVA no alteran operaciones históricas.                                                                         |
| RN-18  | Las estadísticas se calculan a partir de los datos persistidos.                                                                                        |
| RN-19  | Una compra sólo se registra si su pago es aprobado. Un pago fallido o una operación cancelada no registran compra, no venden asientos y no generan factura. |
| RN-20  | Toda compra aprobada genera exactamente una factura.                                                                                                   |
| RN-21  | Las entradas pueden venderse para funciones vigentes, es decir, hasta que la función termine (aunque ya haya empezado).                                |
| RN-22  | Todas las salas comparten la misma distribución de 80 asientos (filas A–H, números 1–10).                                                              |
| RN-23† | El precio de la entrada sin IVA es el precio base vigente más el recargo vigente del tipo de sala, calculado al momento de la venta y conservado en la factura. |
| RN-24  | Una única tasa de IVA vigente se aplica a entradas y confitería; es modificable por el ADMIN y cada factura conserva la tasa que aplicó.               |
| RN-25  | Un taquillero o administrador desactivado no puede realizar ventas ni, en el caso del administrador, iniciar sesión.                                   |
| RN-26  | El SUPER_ADMIN sólo gestiona cuentas de administradores; no vende ni consulta reportes.                                                                |
| RN-27  | Un administrador con historial no puede eliminarse físicamente; se desactiva.                                                                          |
| RN-28  | Una función con ventas no puede cambiar de película, sala, fecha ni hora.                                                                              |
| RN-29  | No se puede desactivar una película con funciones vigentes.                                                                                            |

# 9. Flujos operativos

## Venta de entradas

1. Acceder al módulo de venta.
2. Identificar al vendedor: el taquillero selecciona su nombre; el administrador usa su sesión.
3. Seleccionar una función vigente.
4. Consultar disponibilidad de asientos.
5. Seleccionar uno o varios asientos.
6. Añadir productos de confitería (opcional).
7. Registrar datos del cliente (opcional).
8. Revisar el resumen (subtotal, IVA y total).
9. Seleccionar efectivo o tarjeta.
10. Confirmar la compra: el sistema verifica que los asientos sigan disponibles y ejecuta el pago simulado.
11. Si algún asiento ya fue vendido: se rechaza la operación y se debe elegir otro asiento.
12. Si el pago falla: se cancela la operación y no se registra nada.
13. Si tiene éxito: se registra la compra, los asientos quedan vendidos y se genera la factura.
14. Mostrar factura y opción de impresión.

## Compra únicamente de confitería

1. Acceder al módulo de confitería.
2. Identificar al vendedor.
3. Seleccionar productos y cantidades.
4. Registrar datos del cliente (opcional).
5. Revisar el resumen.
6. Seleccionar efectivo o tarjeta.
7. Ejecutar pago simulado.
8. Si falla: se cancela la operación y no se registra nada.
9. Si tiene éxito: se registra la compra y se genera la factura.
10. Mostrar factura y opción de impresión.

## Administración (ADMIN)

1. Iniciar sesión.
2. Acceder al panel de administración.
3. Gestionar salas, tipos de sala, taquilleros, precios o IVA.
4. Consultar estadísticas.
5. Consultar historial de facturas.

## Gestión de administradores (SUPER_ADMIN)

1. Iniciar sesión.
2. Acceder al módulo de administradores.
3. Crear, editar, desactivar, reactivar o eliminar administradores según las reglas.

# 10. Facturación, pagos e IVA

## Métodos de pago

Se soportan únicamente EFECTIVO y TARJETA. El pago es simulado. No existe conexión con una pasarela externa.

## Cálculo de precios

- Precio de entrada sin IVA = precio base vigente + recargo vigente del tipo de sala.
- Precio de producto sin IVA = precio configurado del producto.
- Precio final unitario = precio sin IVA × (1 + tasa de IVA vigente), redondeado a 2 decimales.
- Total de una línea = cantidad × precio final unitario.
- Total de la factura = suma de los totales de línea.
- Subtotal (base) = suma de (cantidad × precio sin IVA).
- IVA = total − subtotal.

La tasa de IVA es única, modificable por el ADMIN y se conserva en cada factura.

## Estructura conceptual de factura

| **Sección**    | **Contenido**                                                                                      |
| -------------- | -------------------------------------------------------------------------------------------------- |
| Identificación | Número, fecha y hora                                                                               |
| Operación      | Vendedor (nombre) y datos opcionales del cliente (nombre, documento, correo)                       |
| Entradas       | Película, fecha y hora de la función, sala, asientos, cantidad, precio sin IVA y precio final      |
| Confitería     | Producto, cantidad, precio sin IVA y precio final                                                  |
| Totales        | Subtotal/base, IVA (con la tasa aplicada) y total final                                            |
| Pago           | Método utilizado                                                                                   |

## Inmutabilidad

La factura representa el estado definitivo de la operación, no se deben editar sus datos después de generarla. Las relaciones históricas deberán diseñarse para que cambios posteriores en películas, productos, salas, taquilleros, precios o IVA no alteren el contenido histórico.

# 11. Asientos y concurrencia

## Distribución

Todas las salas tienen la misma distribución: 8 filas (A–H) con 10 asientos cada una (1–10), para un total de 80 asientos por función. La selección es manual.

## Venta de asientos

Un asiento sólo pasa a estar vendido cuando la compra es aprobada. No existe reserva ni bloqueo temporal: mientras la compra no se confirme, el asiento permanece disponible para cualquier otro vendedor.

## Concurrencia

Si dos vendedores intentan comprar el mismo asiento al mismo tiempo, el asiento se vende sólo a quien confirme la compra primero. El otro vendedor recibe un error, no se registra su compra y debe seleccionar otro asiento.

# 12. Estadísticas y reportes

Las estadísticas deben incluir gráficos y tablas que permitan al administrador (ADMIN) entender el comportamiento del negocio.

| **Categoría**   | **Indicadores**                                                           |
| --------------- | ------------------------------------------------------------------------- |
| Ingresos        | Entradas, confitería y total                                              |
| Ventas          | Compras realizadas y entradas vendidas                                    |
| Películas       | Películas con mayor cantidad de entradas vendidas                         |
| Funciones       | Funciones con mayor cantidad de entradas vendidas                         |
| Productos       | Cantidad vendida y productos más vendidos                                 |
| Salas           | Ocupación por sala (asientos vendidos sobre la capacidad de sus funciones) |
| Taquilleros     | Ventas asociadas a cada taquillero                                        |
| Administradores | Ventas asociadas a cada administrador                                     |
| Facturas        | Historial y detalle                                                       |

Filtros temporales: hoy, últimos 7 días, este mes y rango personalizado. La fecha de referencia es la fecha de la factura.

# 13. Impresión

La factura se presentará como una vista HTML preparada para impresión. El sistema utilizará el mecanismo de impresión del navegador, por ejemplo window.print(). No se considera necesaria la generación de PDF.

La compatibilidad con una impresora Bluetooth dependerá de que el dispositivo y sistema operativo la expongan como impresora disponible. El backend no administrará directamente el protocolo Bluetooth.

# 14. Requisitos de datos e integridad histórica

## Identificación histórica

Las operaciones de negocio deberán conservar referencias suficientes para reconstruir qué se vendió, cuándo, por cuánto, en qué función, en qué sala y quién atendió la operación.

## Borrado lógico o protección histórica

Cuando un registro ya participa en operaciones históricas, deberá evitarse su eliminación física si esto rompe la trazabilidad. Aplica a películas (se desactivan), productos (se retiran), salas (se deshabilitan), taquilleros y administradores (se desactivan). La solución técnica podrá utilizar estados de disponibilidad/activación o restricciones de borrado.

## Precios e IVA

Las líneas de venta y factura deberán conservar el precio sin IVA, la tasa de IVA y el precio final aplicados en el momento de la operación. Nunca se deberá consultar únicamente el precio actual de la entidad Producto, del tipo de sala o de la configuración de precios para reconstruir una factura antigua.

## Datos descriptivos en la factura

Para que cambios posteriores no alteren el contenido histórico, la factura debe conservar como texto, además de las referencias: el título de la película, la sala, la fecha y hora de la función, el nombre del producto y el nombre del vendedor.

## Imágenes

Las imágenes no se almacenarán como BLOB en MySQL. Se conservará una referencia a su ubicación (URL o ruta). Los archivos cargados desde el dispositivo se guardarán en un volumen persistente de Railway.

## Fecha y hora

Todas las fechas y horas del negocio se interpretan en la zona horaria America/Bogota.

# 15. Requisitos no funcionales

| **Categoría**   | **Requisito**                                                                                              |
| --------------- | ---------------------------------------------------------------------------------------------------------- |
| Arquitectura    | Separación clara de responsabilidades y capas.                                                             |
| Seguridad       | Las funciones administrativas deben requerir autenticación.                                                |
| Contraseñas     | Deben almacenarse mediante un mecanismo de hash seguro; nunca en texto plano.                              |
| Integridad      | Las ventas y facturas deben ejecutarse mediante operaciones transaccionales apropiadas.                    |
| Concurrencia    | No se debe permitir doble venta de un mismo asiento, garantizado también a nivel de base de datos.         |
| Persistencia    | Los datos críticos deben almacenarse en MySQL.                                                             |
| Usabilidad      | La interfaz de taquilla debe minimizar pasos innecesarios y estar orientada a operación rápida.            |
| Mantenibilidad  | El código debe seguir convenciones consistentes y separar presentación, negocio y persistencia.            |
| Disponibilidad  | El sistema debe poder ejecutarse en infraestructura gratuita compatible con el stack definido.             |
| Compatibilidad  | Desarrollo dirigido a Windows y navegadores web modernos.                                                  |
| Escalabilidad   | El diseño debe permitir ampliar salas, productos, películas, funciones y cuentas sin cambios estructurales. |
| Observabilidad  | Los errores relevantes deben poder identificarse mediante logs y respuestas controladas.                   |
| Datos sensibles | No almacenar información bancaria innecesaria.                                                             |
| Zona horaria    | Fechas y horas interpretadas en America/Bogota, de forma consistente entre aplicación y base de datos.     |

# 16. Restricciones tecnológicas

| **Elemento**      | **Decisión**                      |
| ----------------- | --------------------------------- |
| Lenguaje          | Java                              |
| Backend           | Spring Boot                       |
| API               | REST propia                       |
| Frontend          | Thymeleaf                         |
| Persistencia      | JPA + Hibernate                   |
| Base de datos     | MySQL                             |
| Entorno principal | Windows                           |
| Hosting           | Railway (con volumen persistente) |

# 17. Criterios de aceptación del MVP

- Se puede crear una película y verla en la cartelera.
- Una película inactiva no aparece en cartelera ni puede programarse.
- Una película con funciones no puede eliminarse; puede retirarse de la cartelera si no tiene funciones vigentes.
- Se puede crear una función válida para una película y sala.
- El sistema rechaza funciones con inicio en el pasado.
- El sistema rechaza solapamientos y respeta los 15 minutos de limpieza.
- El sistema permite crear y administrar tipos de sala y sus recargos.
- El sistema rechaza deshabilitar una sala con funciones vigentes y permite deshabilitarla cuando no tiene ninguna.
- El sistema permite seleccionar asientos manualmente sobre la distribución de 80 asientos.
- Dos compras simultáneas del mismo asiento: sólo una se registra y la otra recibe un error.
- Se puede vender una compra sólo con entradas, sólo con confitería o mixta.
- Se pueden vender entradas de una función ya iniciada y no se pueden vender de una función terminada.
- El sistema permite efectivo o tarjeta, pero no pagos divididos.
- Un pago exitoso genera factura.
- Un pago fallido no genera factura, no registra compra y no vende asientos.
- Una factura generada no puede editarse.
- Los datos del cliente (nombre, documento, correo) son opcionales.
- El administrador puede modificar el precio base, los recargos y el IVA, y las nuevas ventas usan los valores vigentes.
- Los precios y el IVA históricos permanecen intactos en las facturas ante cambios futuros.
- El administrador puede consultar estadísticas y gráficos.
- El administrador puede consultar el historial de facturas.
- El SUPER_ADMIN puede crear, editar, desactivar y eliminar administradores, y no puede acceder a ventas ni reportes.
- La factura puede descargarse y enviarse a impresión desde el navegador.
- Los datos de prueba pueden cargarse posteriormente sin modificar el modelo funcional.

# 18. Casos límite y comportamientos esperados

| **Caso**                                                    | **Comportamiento esperado**                                                                          |
| ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Cierre del navegador antes de confirmar la compra           | No queda ningún registro ni reserva; los asientos siguen disponibles.                                |
| Dos vendedores seleccionan el mismo asiento                 | Sólo quien confirme primero lo compra; el otro recibe un error y debe elegir otro asiento.           |
| Pago fallido                                                | No se registra compra ni factura; los asientos no se venden.                                         |
| Compra cancelada antes del pago                             | No se registra compra ni factura.                                                                    |
| Producto retirado                                           | No aparece como disponible para nuevas ventas, pero sus ventas históricas permanecen.                |
| Taquillero o administrador con ventas                       | No se elimina físicamente; se desactiva.                                                             |
| Sala con funciones vigentes                                 | No puede deshabilitarse.                                                                             |
| Función con ventas                                          | No puede eliminarse ni cambiar de película, sala, fecha u hora.                                      |
| Película con funciones                                      | No se elimina; se retira de la cartelera (inactiva), salvo que tenga funciones vigentes.             |
| Película inactiva                                           | No aparece en cartelera y no puede programarse en nuevas funciones.                                  |
| Precio base, recargo o IVA cambiado                         | Las nuevas ventas usan los valores vigentes; las facturas existentes no cambian.                     |
| Factura antigua consultada                                  | Debe mostrar los valores históricos, no los precios ni el IVA actuales.                              |
| Función ya iniciada pero no terminada                       | Se pueden vender entradas.                                                                           |
| Función terminada                                           | No se pueden vender entradas.                                                                        |
| Cliente sin datos                                           | La factura se genera sin datos de cliente.                                                           |
| Correo del cliente con formato inválido                     | Se rechaza el dato antes de confirmar la compra.                                                     |
| Administrador desactivado                                   | No puede iniciar sesión ni realizar ventas.                                                          |

# 19. Decisiones consolidadas

Las siguientes decisiones quedan establecidas como base funcional del MVP:

- El sistema es web y está orientado a taquilleros, administradores y un super administrador.
- Los administradores y el super administrador deben iniciar sesión; utilizan los roles ADMIN y SUPER_ADMIN.
- Los taquilleros no requieren login; se identifican seleccionando su nombre.
- El SUPER_ADMIN sólo gestiona el CRUD de administradores. Su cuenta se crea por script de inicialización.
- Una compra pertenece a una única función cuando contiene entradas.
- Una compra puede contener entradas, confitería, ambos o únicamente confitería.
- Una compra sólo existe si su pago fue aprobado; no hay estados de compra ni reserva previa de asientos.
- Los asientos se seleccionan manualmente; gana quien confirma la compra primero.
- Todas las salas tienen la misma distribución de 80 asientos (A–H × 1–10).
- No existen tickets independientes; únicamente factura.
- No existe inventario.
- Los productos tienen imagen, pero no stock ni estado de inventario.
- Las películas tienen imagen, género (catálogo), estado activa/inactiva, pero no fecha de estreno ni director.
- Cartelera es una vista de películas activas, no una entidad independiente.
- Una película puede existir sin funciones.
- Los tipos de sala son administrables, con recargo como monto fijo.
- Los precios configurados no incluyen IVA; el IVA es una tasa única, modificable y se suma al calcular el precio final.
- El administrador controla el precio base de la entrada, los recargos por tipo de sala y el IVA.
- El precio de la entrada se calcula al momento de la venta con los valores vigentes y se conserva en la factura.
- Las funciones deben respetar 15 minutos de limpieza.
- No se permiten funciones con inicio en el pasado ni solapadas.
- Se pueden vender entradas hasta que la función termine.
- No se pueden eliminar funciones con ventas.
- Una sala con funciones vigentes no puede deshabilitarse.
- Los datos del cliente (nombre, documento y correo) son opcionales y no crean una entidad Cliente.
- El pago es efectivo o tarjeta, con un único método por compra.
- La factura es inmutable.
- El número de factura es un identificador único generado por el sistema.
- El nombre de cada taquillero es único.
- Se permite impresión mediante el navegador.
- Se mostrarán estadísticas con gráficos.
- Las imágenes cargadas se guardan en un volumen persistente de Railway.
- Las fechas y horas se interpretan en America/Bogota.
- No habrá ticket, pago real, facturación electrónica, inventario, IA ni múltiples sucursales.

# 20. Pendientes para el diseño técnico

El levantamiento funcional está suficientemente cerrado para comenzar el diseño de software. Los siguientes puntos no representan vacíos del negocio, sino decisiones de implementación que deben definirse antes del código.

- Modelo entidad-relación definitivo, incluida la evaluación de si compra y factura se modelan como una sola entidad (toda compra aprobada genera exactamente una factura).
- Restricción única por función y asiento como garantía de no doble venta, y orden de las operaciones dentro de la transacción de venta (verificar y registrar asientos antes del pago simulado).
- Comportamiento si el precio o el IVA cambian entre el resumen y la confirmación de la compra.
- Validación de solapamiento de funciones con control de concurrencia (bloqueo transaccional de la sala).
- Modelo de precios: configuración vigente (precio base, IVA) y datos conservados en líneas y factura.
- Modelo de factura y sus detalles históricos (textos descriptivos incluidos).
- Datos iniciales: catálogo de géneros, tipos de sala, 80 asientos, cuenta SUPER_ADMIN y configuración de precios.
- Restricciones, claves únicas e índices.
- API REST y contratos de request/response.
- DTOs y validaciones.
- Autenticación y autorización (TAQUILLERO, ADMIN, SUPER_ADMIN).
- Hash de contraseñas y gestión de sesión.
- Zona horaria America/Bogota en aplicación y base de datos.
- Configuración del volumen persistente de Railway para imágenes.
- Estructura de paquetes del proyecto.
- Estrategia de pruebas unitarias e integración.
- Configuración de despliegue gratuito.

# Anexo A · Resumen del flujo de negocio

```text
TAQUILLA

         Vendedor (taquillero o administrador)
                ↓
        Película / Función vigente
                ↓
             Asientos
                ↓
      Confitería (opcional)
                ↓
        Cliente (opcional)
                ↓
    Confirmar: verificar asientos
    y pago simulado (efectivo / tarjeta)
                ↓

┌───────────────────┬────────────────────┐
│  Pago aprobado    │  Asiento ocupado   │
│                   │  o pago fallido    │
│        ↓          │        ↓           │
│  Se registra la   │  No se registra    │
│  compra y los     │  nada: no hay      │
│  asientos quedan  │  compra, venta de  │
│  vendidos         │  asientos ni       │
│        ↓          │  factura           │
│     Factura       │                    │
│        ↓          │                    │
│     Imprimir      │                    │
└───────────────────┴────────────────────┘

ADMINISTRACIÓN

          Administrador (ADMIN)
                ↓
              Login
                ↓
            Panel ADMIN
            ├── Salas y tipos de sala
            ├── Taquilleros
            ├── Precios e IVA
            ├── Estadísticas
            └── Facturas

          Super administrador (SUPER_ADMIN)
                ↓
              Login
                ↓
          Administradores (CRUD)
```

# Anexo B · Definición conceptual de éxito

El proyecto habrá cumplido su propósito funcional cuando una operación que antes requería registros manuales pueda ejecutarse de principio a fin dentro del sistema, con información consistente, asiento controlado, pago registrado, factura generada y datos disponibles para consulta estadística.

La siguiente fase del proyecto será el diseño técnico: modelo de dominio, entidades, relaciones, base de datos, reglas de persistencia, API REST, arquitectura Spring Boot/Thymeleaf y estrategia de concurrencia.

