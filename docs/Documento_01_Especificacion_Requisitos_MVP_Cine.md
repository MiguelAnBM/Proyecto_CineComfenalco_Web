# CineComfenalco -Sistema Web de Gestión Integral para un cine

## Documento 01 · Especificación de Requisitos del MVP (Sujeto a cambios)

| Versión | 0.1                                                           |
| ------- | ------------------------------------------------------------- |
| Tipo    | Especificación de requisitos                                  |
| Equipo  | 3 integrantes                                                 |
| Stack   | Java · Spring Boot · Thymeleaf · REST · JPA/Hibernate · MySQL |
| Estado  | Aprobado                                                      |

> Propósito del documento: Establecer, de forma clara y verificable, qué debe hacer el sistema, qué reglas debe respetar, qué queda fuera del MVP y cuáles son las condiciones que deben cumplirse antes de comenzar la implementación.

Este documento constituye la referencia funcional inicial del proyecto. Las decisiones de implementación detalladas (modelo relacional definitivo, arquitectura de paquetes, endpoints, transacciones, seguridad y estrategia de concurrencia) se desarrollarán en documentos técnicos posteriores.

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

# Propósito y contexto

El sistema tiene como finalidad centralizar la operación de un cine que actualmente gestiona de forma manual y dispersa la venta de entradas, la selección de asientos, la comercialización de productos de confitería, la facturación y el seguimiento de ingresos.

La situación actual genera errores de registro, pérdida de tiempo, dificultad para relacionar las ventas con sus facturas y dependencia de procesos manuales o archivos independientes. El sistema propuesto busca proporcionar un único entorno web para ejecutar y consultar estas operaciones.

# Objetivos del sistema

## Objetivo general

Construir un sistema web que permita gestionar íntegramente la operación de taquilla y administración de un cine, centralizando programación, ventas, confitería, facturación, precios y reportes en una única aplicación.

## Objetivos específicos

- Analizar los requerimientos funcionales y no funcionales para…
- Diseñar un sistema web para…
- Codificar el sistema web en Java para…
- Verificar el sistema web en Java para…
- Implementar el sistema web en Java para…

# Alcance del MVP

El MVP debe cubrir el ciclo completo de operación de venta y las funciones administrativas fundamentales.

| **Área**   | **Incluido**                                                             |
| ---------- | ------------------------------------------------------------------------ |
| Películas  | CRUD, clasificación, género, duración, sinopsis e imagen                 |
| Cartelera  | Consulta de películas; una película puede existir sin funciones          |
| Funciones  | CRUD, programación por sala/fecha/hora, cálculo de finalización y precio |
| Salas      | CRUD administrativo, tipo administrable y disponibilidad                 |
| Asientos   | Distribución uniforme predefinida y selección manual                     |
| Confitería | CRUD operativo, precios e imágenes                                       |
| Ventas         | Entradas, confitería o compra mixta                             |
| Pagos          | Efectivo o tarjeta, un único método por compra (Pago simulado)  |
| Facturación    | Generación, consulta, detalle e impresión                       |
| Estadísticas   | Ingresos, ventas, ocupación y desgloses relevantes con gráficos |
| Administración | Login, salas, taquilleros, precios, estadísticas e historial    |
| Taquilleros    | CRUD administrativo e identificación al realizar ventas         |

# Fuera de alcance

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
- Tickets de ingreso independientes de la factura.
- Auditoría detallada de acciones administrativas.
- Generación de PDF como requisito del sistema.

# Actores y permisos

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

El administrador debe autenticarse. Todas las cuentas administrativas comparten el rol ADMIN.

- Todas las operaciones disponibles para el taquillero.
- CRUD de salas.
- CRUD de taquilleros.
- Gestión de precios.
- Consulta de reportes de su sucursal.
- Consulta del historial de facturas.

## Súper Administrador

El súper administrador es un rol con una única cuenta con usuario y contraseña específicos. Debe ingresar
en el portal de logueo de administrador corriente, sin embargo, al ingresar las credenciales
del súper administrador se revelará un apartado especial para este rol que permite gestionar a los
administradores registrados y descargar el reporte de sucursal de ellos.

- CRUD de administradores.
- Consulta de reportes por administrador.

> Regla: no existe un rol adicional dentro del MVP. La separación de permisos se basa en operación de taquilla frente a funciones administrativas.

# Conceptos fundamentales

## Película

Producción cinematográfica registrada en el sistema. Puede aparecer en cartelera, aunque no tenga ninguna función.

## Función

Programación de una película en una sala, con fecha y hora determinadas. Su hora de finalización se calcula a partir de la duración de la película.

## Sala

Espacio físico donde se proyecta una función. El administrador puede crear tantas salas como necesite. Su tipo determina un recargo sobre el precio base.

## Compra

Conjunto de bienes adquiridos en una única operación. Una compra puede pertenecer a una función cuando contiene entradas, y puede contener productos de confitería. También puede contener únicamente confitería.

## Factura

Documento final de una compra. Una vez generada no puede modificarse.

## Cartelera

Vista de las películas registradas; no constituye una entidad independiente.

# Requisitos funcionales

## RF-01 · Gestión de películas

- El sistema debe permitir crear películas.
- El sistema debe permitir consultar películas.
- El sistema debe permitir editar películas.
- El sistema debe permitir eliminar películas cuando no existan dependencias que impidan la eliminación.
- Una película debe tener título, sinopsis, duración, género, clasificación e imagen.
- Cada película tendrá exactamente un género.
- La clasificación debe pertenecer a 0+, 7+, 15+ o 18+.
- La imagen podrá provenir de una carga desde el dispositivo o de una URL.
- Una película puede existir sin ninguna función.

## RF-02 · Gestión de funciones

- El sistema debe permitir crear funciones.
- El sistema debe permitir consultar funciones.
- El sistema debe permitir editar funciones cuando las reglas de negocio lo permitan.
- El sistema debe permitir eliminar funciones que no tengan ventas.
- Una función debe referenciar exactamente una película y una sala.
- La hora de finalización debe calcularse automáticamente.
- No se deben crear funciones en fechas pasadas.
- No se deben crear funciones que se solapen en la misma sala.
- Debe respetarse un intervalo de limpieza de 15 minutos entre funciones.

## RF-03 · Gestión de salas

- El administrador debe poder crear, consultar, editar y eliminar salas según las reglas de integridad.
- Las salas pueden ser de tipos administrables.
- Cada tipo de sala tendrá un recargo configurable.
- Una sala puede estar activa o deshabilitada.
- No se podrá crear una función en una sala deshabilitada.
- No se podrá deshabilitar una sala que tenga funciones futuras.

## RF-04 · Gestión de productos

- El sistema debe permitir crear, consultar, editar y retirar productos.
- Cada producto debe tener nombre, descripción, precio e imagen.
- El sistema no gestionará inventario.
- Un producto podrá estar disponible o no disponible.

## RF-05 · Gestión de taquilleros

- El administrador debe poder crear, consultar, editar y eliminar taquilleros.
- Un taquillero con historial no debe eliminarse físicamente; debe desactivarse.
- El taquillero seleccionará su nombre al realizar una venta.

## RF-06 · Venta

- El sistema debe permitir seleccionar una función.
- Debe permitir seleccionar manualmente uno o varios asientos.
- Debe permitir añadir productos de confitería opcionalmente.
- Debe permitir realizar compras únicamente de confitería.
- Debe permitir registrar datos del cliente como opcionales.
- Debe permitir seleccionar efectivo o tarjeta.
- Una compra utilizará un único método de pago.
- No se permitirá una compra completamente vacía.

## RF-07 · Facturación

- Una compra aprobada debe generar una factura.
- Una compra cancelada o con error no debe generar factura.
- La factura debe conservar los precios históricos.
- La factura debe incluir el detalle completo de la operación.
- Una factura generada no puede modificarse.

## RF-08 · Administración de precios

- El administrador debe poder modificar el precio base de entrada.
- El administrador debe poder modificar el recargo de cada tipo de sala.
- El administrador debe poder modificar el precio de los productos.
- Los cambios no deben modificar facturas existentes.
- Las funciones conservarán el precio que les corresponda conforme a la regla histórica definida.

## RF-09 · Gestión de Administradores

- El súper administrador debe poder crear, consultar, editar y desactivar administradores.
- Un administrador con historial no debe eliminarse físicamente; debe desactivarse.
- El súper administrador debe poder descargar los reportes de venta de los administradores

# Reglas de negocio

| **ID** | **Regla**                                                                                        |
| ------ | ------------------------------------------------------------------------------------------------ |
| RN-01  | Una compra puede corresponder a una única función.                                               |
| RN-02  | Una compra puede contener entradas, productos o ambos.                                           |
| RN-03  | Una compra puede contener únicamente productos de confitería.                                    |
| RN-04  | Una compra vacía no es válida.                                                                   |
| RN-05  | Un asiento comprado no puede volver a venderse en la misma función.                              |
| RN-06  | Si una operación falla o se cancela, los asientos vuelven a estar disponibles.                   |
| RN-07  | Una función puede vender entradas hasta 20 minutos después de su hora de inicio.                 |
| RN-08  | No se pueden programar funciones en el pasado.                                                   |
| RN-09  | No se permiten solapamientos en una misma sala.                                                  |
| RN-10  | Debe existir un intervalo de limpieza de 15 minutos entre funciones.                             |
| RN-11  | Una sala deshabilitada no puede recibir nuevas funciones.                                        |
| RN-12  | Una sala con funciones futuras no puede deshabilitarse.                                          |
| RN-13  | Una función con ventas no puede eliminarse.                                                      |
| RN-14  | Un taquillero con historial no puede eliminarse físicamente.                                     |
| RN-15  | Una película con dependencias históricas no debe eliminarse de forma que se pierda el historial. |
| RN-16  | Cada compra utiliza un único método de pago.                                                     |
| RN-17  | El IVA es del 19% y los precios configurados son precios finales con IVA incluido.               |
| RN-28  | Las facturas son inmutables una vez generadas.                                                   |
| RN-19  | Los cambios de precio futuros no alteran operaciones históricas.                                 |
| RN-20  | No se almacenarán datos sensibles de tarjetas.                                                   |

| RN-21 | Las estadísticas se calculan a partir de los datos persistidos. |
| ----- | --------------------------------------------------------------- |

# Flujos operativos

## Venta de entradas

1. Acceder al módulo de cafetería.
2. Seleccionar taquillero.
3. Seleccionar función.
4. Consultar disponibilidad.
5. Seleccionar uno o varios asientos.
6. Añadir productos de confitería (opcional).
7. Registrar datos del cliente (opcional).
8. Revisar el resumen.
9. Seleccionar efectivo o tarjeta.
10. Ejecutar pago simulado.
11. Si falla: cancelar operación y liberar asientos.
12. Si tiene éxito: confirmar venta y generar factura.
13. Mostrar factura y opción de impresión.

## Compra únicamente de confitería

1. Acceder al módulo de confitería.
2. Seleccionar taquillero.
3. Seleccionar productos y cantidades.
4. Registrar cliente (opcional).
5. Revisar compra.
6. Seleccionar método de pago.
7. Ejecutar pago simulado.
8. Generar factura si el pago es aprobado.
9. Mostrar e imprimir factura.

## Administración

1. Abrir Administración.
2. Autenticarse.
3. Acceder al panel.
4. Gestionar salas, taquilleros o precios.
5. Consultar estadísticas.
6. Consultar historial de facturas.

# Facturación, pagos e IVA

## Métodos de pago

Se soportan únicamente EFECTIVO y TARJETA. El pago es simulado. No existe conexión con una pasarela externa.

## IVA

Los precios configurados se consideran finales con IVA incluido. El sistema deberá separar conceptualmente el valor base y el componente de IVA para la presentación de la factura.

| **Sección**    | **Contenido**                                                 |
| -------------- | ------------------------------------------------------------- |
| Identificación | Número, fecha y hora                                          |
| Operación      | Taquillero y datos opcionales del cliente                     |
| Entradas       | Película, función, sala, asientos, cantidad y precio aplicado |
| Confitería     | Producto, cantidad y precio aplicado                          |
| Totales        | Subtotal/base, IVA 19% y total final                          |
| Pago           | Método utilizado                                              |

## Estructura conceptual de factura

## Inmutabilidad

La factura representa el estado definitivo de la operación. No se deben editar sus datos después de generarla. Las relaciones históricas deberán diseñarse para que cambios posteriores en películas, productos, salas, taquilleros o precios no alteren el contenido histórico.

# Asientos

## Concurrencia

Si dos taquilleros intentan comprar un mismo asiento éste deberá reservarse sólo para aquel que confirme la compra primero, lo que conlleva a que el taquillero que no logró confirmar la compra reciba un error y deba seleccionar otro asiento.

# Estadísticas y reportes

Las estadísticas deben incluir gráficos y tablas que permitan al administrador entender el comportamiento del negocio.

| **Categoría** | **Indicadores**                                   |
| ------------- | ------------------------------------------------- |
| Ingresos      | Entradas, confitería y total                      |
| Ventas        | Compras realizadas y entradas vendidas            |
| Películas     | Películas con mayor cantidad de entradas vendidas |
| Funciones     | Funciones con mayor cantidad de entradas vendidas |
| Productos     | Cantidad vendida y productos más vendidos         |

| Salas       | Ocupación por sala y función       |
| ----------- | ---------------------------------- |
| Taquilleros | Ventas asociadas a cada taquillero |
| Facturas    | Historial y detalle                |

Filtros temporales: hoy, últimos 7 días, este mes y rango personalizado.

# Impresión

La factura se presentará como una vista HTML preparada para impresión. El sistema utilizará el mecanismo de impresión del navegador, por ejemplo window.print(). No se considera necesaria la generación de PDF.

La compatibilidad con una impresora Bluetooth dependerá de que el dispositivo y sistema operativo la expongan como impresora disponible. El backend no administrará directamente el protocolo Bluetooth.

# Requisitos de datos e integridad histórica

## Identificación histórica

Las operaciones de negocio deberán conservar referencias suficientes para reconstruir qué se vendió, cuándo, por cuánto, en qué función, en qué sala y quién atendió la operación.

## Borrado lógico o protección histórica

Cuando un registro ya participa en operaciones históricas, deberá evitarse su eliminación física si esto rompe la trazabilidad. La solución técnica podrá utilizar estados de disponibilidad/activación o restricciones de borrado.

## Precios

Las líneas de venta y factura deberán conservar el precio aplicado en el momento de la operación. Nunca se deberá consultar únicamente el precio actual de la entidad Producto o de la configuración de precios para reconstruir una factura antigua.

## Imágenes

Las imágenes no se almacenarán como BLOB en MySQL. Se conservará una referencia a su ubicación. La estrategia concreta de almacenamiento para archivos cargados se definirá durante el diseño técnico y despliegue.

# Requisitos no funcionales

| **Categoría** | **Requisito**                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------ |
| Arquitectura  | Separación clara de responsabilidades y capas.                                                   |
| Seguridad     | Las funciones administrativas deben requerir autenticación.                                      |
| Contraseñas   | Deben almacenarse mediante un mecanismo de hash seguro; nunca en texto plano.                    |
| Integridad    | Las ventas, reservas y facturas deben ejecutarse mediante operaciones transaccionales apropiadas |
| Concurrencia  | No se debe permitir doble venta de un mismo asiento.                                             |


# Restricciones tecnológicas

| **Elemento**      | **Decisión**    |
| ----------------- | --------------- |
| Lenguaje          | Java            |
| Backend           | Spring Boot     |
| API               | REST propia     |
| Frontend          | Thymeleaf       |
| Persistencia      | JPA + Hibernate |
| Base de datos     | MySQL           |
| Entorno principal | Windows         |
| Hosting           | Railway         |

La arquitectura técnica detallada se definirá en un documento posterior. Este documento fija el qué; el diseño técnico fijará el cómo.

# Criterios de aceptación del MVP

- Se puede crear una película y verla en la cartelera.
- Se puede crear una función válida para una película y sala.
- El sistema rechaza funciones en fechas pasadas.
- El sistema rechaza solapamientos y respeta los 15 minutos de limpieza.
- El sistema permite crear y administrar tipos de sala y sus recargos.
- El administrador puede deshabilitar una sala sin funciones futuras.
- El sistema permite seleccionar asientos manualmente.
- Un asiento comprado no puede ser comprado simultáneamente por otra operación..

| **Categoría**   | **Requisito**                                                                                              |
| --------------- | ---------------------------------------------------------------------------------------------------------- |
| Persistencia    | Los datos críticos deben almacenarse en MySQL.                                                             |
| Usabilidad      | La interfaz de taquilla debe minimizar pasos innecesarios y estar orientada a operación rápida             |
| Mantenibilidad  | El código debe seguir convenciones consistentes y separar presentación, negocio y persistencia             |
| Disponibilidad  | El sistema debe poder ejecutarse en infraestructura gratuita compatible con el stack definido.             |
| Compatibilidad  | Desarrollo dirigido a Windows y navegadores webs modernos.                                                 |
| Escalabilidad   | El diseño debe permitir ampliar salas, productos, películas, funciones y cuentas sin cambios estructurales |
| Observabilidad  | Los errores relevantes deben poder identificarse mediante logs y respuestas controladas.                   |
| Datos sensibles | No almacenar información bancaria innecesaria.                                                             |

- Una función deja de aceptar ventas 20 minutos después de iniciar.
- Se puede vender una compra solo con entradas, solo con confitería o mixta.
- El sistema permite efectivo o tarjeta, pero no pagos divididos.
- Un pago aprobado genera factura.
- Un pago fallido no genera factura.
- Una factura generada no puede editarse.
- Los precios históricos permanecen intactos ante cambios futuros.
- El administrador puede consultar estadísticas y gráficos.
- El administrador puede consultar el historial de facturas.
- La factura puede descargarse y enviarse a impresión desde el navegador.
- Los datos de prueba pueden cargarse posteriormente sin modificar el modelo funcional.

# Casos límite y comportamientos esperados

| **Caso**                                                | **Comportamiento esperado**                                                          |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Cierre del navegador durante reserva                    | La reserva permanece persistida hasta su expiración.                                 |
| Reinicio del servidor durante reserva                   | La reserva persiste en MySQL y conserva su expiración.                               |
| Dos taquilleros seleccionan el mismo asiento            | Sólo una operación puede confirmar la venta.                                         |
| Pago fallido                                            | No se genera factura.                                                                |
| Compra cancelada antes del pago                         | No se genera factura.                                                                |
| Producto retirado                                       | No aparece como disponible para nuevas ventas, pero sus ventas históricas permanecen |
| Taquillero con ventas                                   | No se elimina físicamente; se desactiva.                                             |
| Sala con funciones futuras                              | No puede deshabilitarse.                                                             |
| Función con ventas                                      | No puede eliminarse.                                                                 |
| Película con historial                                  | No debe eliminarse de forma que se pierda la referencia histórica.                   |
| Precio cambiado después de programar<br><br>una función | No modifica el precio histórico aplicable a esa función.                             |
| Factura antigua consultada                              | Debe mostrar los valores históricos, no los precios actuales.                        |
| Venta 19 minutos después del inicio                     | Permitida si los demás requisitos se cumplen.                                        |
| Venta 21 minutos después del inicio                     | Rechazada.                                                                           |

# Decisiones consolidadas

Las siguientes decisiones quedan establecidas como base funcional del MVP:

- El sistema es web y está orientado a taquilleros y administradores.
- Un administrador debe iniciar sesión para acceder al sistema web.
- Los taquilleros no requieren login; se identifican seleccionando su nombre.
- Los administradores requieren autenticación y utilizan el rol ADMIN.
- Una compra pertenece a una única función cuando contiene entradas.
- Una compra puede contener entradas, confitería, ambos o únicamente confitería.
- Los asientos se seleccionan manualmente.
- No existen tickets independientes; únicamente factura.
- No existe inventario.
- Los productos tienen imagen, pero no stock ni estado de inventario.
- Las películas tienen imagen, pero no fecha de estreno ni director.
- Cartelera es una vista de películas, no una entidad independiente.
- Una película puede existir sin funciones.
- Las salas tienen distribución uniforme.
- Los tipos de sala son administrables.
- El precio se compone de precio base + recargo por tipo de sala.
- El administrador controla precios.
- Los precios son finales con IVA incluido y el IVA es del 19%.
- Las funciones conservan su precio histórico.
- Las funciones deben respetar 15 minutos de limpieza.
- No se permiten funciones en el pasado.
- No se permiten funciones solapadas.
- No se venden entradas más de 20 minutos después del inicio.
- No se pueden eliminar funciones con ventas.
- Los datos del cliente son opcionales y no crean una entidad Cliente.
- El pago es efectivo o tarjeta, con un único método por compra.
- La factura es inmutable.
- Se permite impresión mediante el navegador.
- Se mostrarán estadísticas con gráficos.
- No habrá ticket, pago real, facturación electrónica, inventario, IA ni múltiples sucursales.

# Pendientes para el diseño técnico

El levantamiento funcional está suficientemente cerrado para comenzar el diseño de software. Los siguientes puntos no representan vacíos del negocio, sino decisiones de implementación que deben definirse antes del código.

- Modelo entidad-relación definitivo.
- Modelo de asientos por sala y por función.
- Estrategia exacta para reservas temporales y expiración.
- Estrategia de bloqueo/concurrencia en MySQL y JPA.
- Estados internos de compra y pago.
- Entidades versus enums versus tablas de configuración.
- Modelo de precios y snapshots históricos.
- Modelo de factura y sus detalles históricos.
- Restricciones, claves únicas e índices.
- API REST y contratos de request/response.
- DTOs y validaciones.
- Autenticación y autorización administrativa.
- Hash de contraseñas y gestión de sesión.
- Configuración de CSRF si las vistas Thymeleaf realizan llamadas REST.
- Migraciones de base de datos.
- Estructura de paquetes del proyecto.
- Estrategia de pruebas unitarias e integración.
- Configuración de despliegue gratuito.

# Anexo A · Resumen del flujo de negocio

```text
TAQUILLA

            Taquillero
                ↓
        Película / Función
                ↓
             Asientos
                ↓
      Confitería (opcional)
                ↓
        Cliente (opcional)
                ↓
    Pago: efectivo / tarjeta
                ↓

┌───────────────┬─────────────────┐
│ Pago aprobado │   Pago fallido  │
│      ↓        │        ↓        │
│    Asientos   │     Liberar     │
│    vendidos   │     asientos    │
│      ↓        │        ↓        │
│    Factura    │   No factura    │
│      ↓        │                 │
│    Imprimir   │                 │
└───────────────┴─────────────────┘

ADMINISTRACIÓN

          Administración
                ↓ 
              Login
                ↓
            Panel ADMIN
            ├── Salas
            ├── Taquilleros
            ├── Precios
            ├── Estadísticas
            └── Facturas
```

**Anexo B · Definición conceptual de éxito**

El proyecto habrá cumplido su propósito funcional cuando una operación que antes requería registros manuales pueda ejecutarse de principio a fin dentro del sistema, con información consistente, asiento controlado, pago registrado, factura generada y datos disponibles para consulta estadística.

La siguiente fase del proyecto será el diseño técnico: modelo de dominio, entidades, relaciones, base de datos, reglas de persistencia, API REST, arquitectura Spring Boot/Thymeleaf y estrategia de concurrencia.