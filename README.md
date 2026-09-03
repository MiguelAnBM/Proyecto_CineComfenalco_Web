# 🎬 CineComfenalco

> **Sistema web de gestión integral para una sucursal de cine**, orientado a taquilleros y administradores. Centraliza programación, venta de entradas, selección de asientos, confitería, pagos, facturación y estadísticas en una sola aplicación.

---

## 📌 Sobre el proyecto

CineComfenalco nace para reemplazar procesos manuales y dispersos de una sucursal de cine por un entorno web único. El MVP cubre el ciclo operativo desde la programación de películas y funciones hasta la venta, pago, facturación e impresión, además de herramientas administrativas y estadísticas.

> **Objetivo:** ejecutar una operación de venta de principio a fin con información consistente, control de asientos, pago registrado, factura inmutable y datos disponibles para análisis.

### 🎯 Alcance del MVP

| Módulo | Funcionalidades principales |
|---|---|
| 🎞️ **Películas** | CRUD, género, clasificación, duración, sinopsis e imagen |
| 🎬 **Cartelera** | Consulta de películas |
| 🕐 **Funciones** | CRUD, sala/fecha/hora, cálculo de finalización y precio histórico |
| 🏛️ **Salas** | CRUD administrativo, tipos configurables, recargos y disponibilidad |
| 💺 **Asientos** | Distribución uniforme y selección manual |
| 🍿 **Confitería** | CRUD, precios e imágenes  |
| 🛒 **Ventas** | Entradas, confitería o compra mixta |
| 💳 **Pagos** | Efectivo o tarjeta; pago simulado y un único método por compra |
| 🧾 **Facturación** | Generación, consulta, detalle e impresión |
| 📊 **Estadísticas** | Ingresos, ventas, productos, películas, funciones y taquilleros |
| 👥 **Administración** | Login, salas, taquilleros, precios e historial de facturas y reportes |

---

## 👤 Roles y permisos

### 🎟️ Taquillero
No requiere contraseña. Antes de una venta selecciona su nombre para asociarlo a la operación, utiliza el rol `TAQUILLERO`.

- Consultar películas, cartelera y funciones.
- Seleccionar y vender asientos.
- Añadir productos de confitería.
- Realizar compras únicamente de confitería.
- Registrar datos opcionales del cliente.
- Seleccionar efectivo o tarjeta.
- Generar e imprimir facturas.

### 🔐 Administrador
Requiere autenticación y utiliza el rol `ADMIN`.

- Todas las operaciones del taquillero.
- Gestionar salas y taquilleros.
- Gestionar precios.
- Consultar estadísticas y gráficos.
- Consultar historial de facturas.

### 👑 Súper Administrador
Cuenta única con credenciales propias y acceso al apartado especial de administración, utiliza el rol `SUPERADMIN`.

- CRUD y desactivación de administradores.
- Consulta de reportes por administrador.

---

## 🔄 Flujo principal de venta

```text
🎟️ Taquillero
     │
     ▼
🎬 Película / Función
     │
     ▼
💺 Selección de asientos
     │
     ├───────────────┐
     ▼               ▼
🍿 Confitería      👤 Cliente
   (opcional)       (opcional)
     │               │
     └───────┬───────┘
             ▼
       💳 Pago simulado
       ┌─────┴─────┐
       ▼           ▼
   ✅ Aprobado   ❌ Fallido
       │           │
       ▼           ▼
💺 Asientos   🔓Cancelar
   vendidos       venta
       │
       ▼
🧾 Factura
       │
       ▼
🖨️ Impresión
```

También se permiten compras **únicamente de confitería**, sin función ni entradas.

---

## 📐 Historias de Usuario

| ID | Regla |
|---|---|
| **HU-01** | _______________________________________________________ |
| **HU-02** | _______________________________________________________ |

---

## 💰 Facturación, precios e IVA

- Los precios configurados se consideran **precios finales con IVA incluido**.
- El sistema presenta conceptualmente la base y el componente de IVA del **19 %**.
- El precio de una entrada se compone de:
  - **Precio base**
  - **Recargo según el tipo de sala**
- Los cambios futuros de precios **no modifican operaciones históricas**.
- La factura conserva el precio aplicado en cada línea de venta.
- Una factura aprobada contiene identificación, operación, entradas, confitería, totales y método de pago.

### 💳 Métodos de pago

Solo se admiten:

- 💵 **Efectivo**
- 💳 **Tarjeta**

El pago es **simulado** y no existe integración con una pasarela externa ni almacenamiento de información bancaria sensible.

---

## 📊 Estadísticas y reportes

El panel administrativo debe presentar **gráficos y tablas** con información calculada a partir de los datos persistidos.

| Categoría | Indicadores |
|---|---|
| 💰 **Ingresos** | Entradas, confitería y total |
| 🛒 **Ventas** | Compras realizadas y entradas vendidas |
| 🎞️ **Películas** | Películas con mayor cantidad de entradas |
| 🕐 **Funciones** | Funciones con mayor cantidad de entradas |
| 🍿 **Productos** | Cantidad vendida y productos más vendidos |
| 🏛️ **Salas** | Ocupación por sala y función |
| 🎟️ **Taquilleros** | Ventas asociadas |
| 🧾 **Facturas** | Historial y detalle |

**Filtros:** hoy · últimos 7 días · este mes · rango personalizado.

---

## 🖨️ Impresión

La factura se mostrará como una **vista HTML preparada para impresión** mediante el navegador, por ejemplo `window.print()`.

- No se requiere generación de PDF.
- La impresión por Bluetooth dependerá de que el dispositivo y sistema operativo reconozcan la impresora como disponible.
- El backend no administrará directamente el protocolo Bluetooth.

---

## 🏗️ Arquitectura

El proyecto utiliza una **arquitectura en capas** para separar presentación, lógica de negocio y persistencia.

```text
                           👤 FUNCIONARIO
                                   │
                                   ▼
                          ┌─────────────────┐
                          │    THYMELEAF    │
                          │  Interfaz Web   │
                          └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │   CONTROLLER    │
                          │    API REST     │
                          └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │     SERVICE     │
                          │  Lógica negocio │
                          └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │   REPOSITORY    │
                          │   Persistencia  │
                          └────────┬────────┘
                                   │
                                   ▼
                          ┌─────────────────┐
                          │      MYSQL      │
                          │  Base de datos  │
                          └─────────────────┘
```

### Responsabilidades

- **Controller:** recibe solicitudes y expone la API REST.
- **Service:** concentra las reglas y lógica de negocio.
- **Repository:** gestiona la persistencia.
- **MySQL:** almacena los datos críticos del sistema.
- **Thymeleaf:** renderiza la interfaz web.

---

## 🛠️ Stack tecnológico

| Tecnología | Uso |
|---|---|
| ☕ **Java** | Lenguaje principal |
| 🌱 **Spring Boot** | Backend y estructura de la aplicación |
| 🔗 **API REST** | Comunicación entre interfaz y backend |
| 🎨 **Thymeleaf** | Interfaz y renderizado web |
| 🧩 **Spring Data JPA / Hibernate** | Persistencia y ORM |
| 🗄️ **MySQL** | Base de datos |
| 📦 **Maven** | Dependencias y construcción |
| 🚂 **Railway** | Hosting definido para el proyecto |
| 🪟 **Windows** | Entorno principal de desarrollo |

---

## 🚫 Fuera del MVP

Para mantener el alcance controlado, **no forman parte del MVP**:

- 📱 Aplicación móvil.
- 📐 Diseño responsive.
- 💳 Pasarela de pago real.
- 🔐 Almacenamiento de números de tarjeta, CVV u otros datos bancarios.
- 🧾 Facturación electrónica.
- 🏛️ Integración con autoridades tributarias.
- 🤖 Inteligencia artificial.
- 📦 Control de inventario o stock.
- 📅 Reservas anticipadas para clientes.
- 🎟️ Tickets independientes de la factura.
- 📝 Auditoría detallada de acciones administrativas.
- 📄 Generación de PDF como requisito del sistema.

---

## 📁 Integridad histórica

El sistema debe poder reconstruir una operación histórica indicando **qué se vendió, cuándo, por cuánto, en qué función, en qué sala y quién atendió la operación**.

Por ello:

- Los precios aplicados deben conservarse en las líneas de venta/factura.
- Las facturas no pueden editarse.
- Los registros con historial no deben eliminarse físicamente cuando esto rompa la trazabilidad.
- Las imágenes se almacenarán mediante una referencia a su ubicación; no como BLOB en MySQL.
- La estrategia concreta para almacenar archivos cargados se definirá durante el diseño técnico.

---

## 🚀 Instalación y ejecución

### Requisitos

- ☕ Java — **versión por definir**
- 📦 Maven — **versión por definir**
- 🗄️ MySQL — **versión por definir**
- 🔧 Git
- 💻 IDE compatible con Spring Boot

### Clonar el repositorio

```bash
git clone https://github.com/MiguelAnBM/Proyecto_CineComfenalco_Web.git
cd Proyecto_CineComfenalco_Web
```

### Configuración de Base de Datos
 
> ⚙️ La configuración de la base de datos, variables de entorno, migraciones y demás parámetros de ejecución se definirá durante el diseño técnico.

---

## 🗺️ Roadmap

- [x] Análisis y definición del MVP
- [x] Definición del enfoque web
- [x] Selección del stack tecnológico
- [ ] Diseño del modelo de dominio
- [ ] Diseño de la base de datos
- [ ] Definición de API REST y DTOs
- [ ] Estrategia de reservas y concurrencia
- [ ] Implementación del backend
- [ ] Implementación de la API REST
- [ ] Desarrollo de vistas Thymeleaf
- [ ] Módulo de películas y cartelera
- [ ] Módulo de funciones y salas
- [ ] Selección y control de asientos
- [ ] Módulo de confitería
- [ ] Flujo de ventas y pagos
- [ ] Facturación e impresión
- [ ] Administración y autenticación
- [ ] Estadísticas y reportes
- [ ] Pruebas
- [ ] Despliegue

---

## 👥 Equipo

| Integrante | Rol |
|---|---|
| **Daniel Galvis Gómez** | Por definir |
| **Delany Yulieth Mendoza Castillo** | Por definir |
| **Miguel Angel Benitez Moncaleano** | Por definir |

---

## 🎓 Información académica

- **Proyecto:** CineComfenalco
- **Asignatura:** Desarrollo de Software
- **Programa:** Tecnología en Desarrollo de Software
- **Institución:** Fundación Universitaria Tecnológico Comfenalco
- **Periodo:** 4.º semestre · 2026-2
- **Equipo:** 3 integrantes

---

<p align="center">
  🎬 <strong>CineComfenalco</strong><br>
  <em>Una plataforma para gestionar el cine de forma simple, eficiente y organizada.</em>
</p>
