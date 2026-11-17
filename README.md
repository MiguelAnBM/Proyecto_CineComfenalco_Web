# 🎬 CineComfenalco

> **Sistema web de gestión integral para una sucursal de cine**, orientado a taquilleros y administradores. Centraliza programación, venta de entradas, selección de asientos, confitería, pagos, facturación y estadísticas en una sola aplicación.

---

## 📌 Sobre el proyecto

CineComfenalco nace para reemplazar procesos manuales y dispersos de una sucursal de cine por un entorno web único. El MVP cubre el ciclo operativo desde la programación de películas y funciones hasta la venta, pago, facturación e impresión, además de herramientas administrativas y estadísticas.

### 🎯 Alcance del MVP

| Módulo | Funcionalidades principales | Estado |
|---|---|---|
| 🎞️ **Películas** | CRUD, género, clasificación, duración, sinopsis e imagen | ✅ Implementado |
| 🎬 **Cartelera** | Consulta de películas y funciones | ✅ Implementado |
| 🕐 **Funciones** | CRUD, sala/fecha/hora, cálculo de finalización, bloqueo con boletos vendidos | ✅ Implementado |
| 🏛️ **Salas** | CRUD administrativo, tipos (2D, 3D, IMAX), precio por sala y disponibilidad | ✅ Implementado |
| 💺 **Asientos** | Mapa 8×10 (A–H × 1–10, 80 asientos), selección manual, venta sin doble asignación | ✅ Implementado |
| 🍿 **Confitería** | CRUD, categorías, precios e imágenes | ✅ Implementado |
| 🛒 **Ventas** | Entradas, confitería | ✅ Implementado |
| 💳 **Pagos** | Efectivo o tarjeta; pago simulado y un único método por compra | ✅ Implementado |
| 🧾 **Facturación** | Generación, consulta, detalle, impresión y descarga TXT | ✅ Implementado |
| 📊 **Estadísticas** | Ingresos, ventas, productos, películas, funciones, salas y taquilleros | ✅ Implementado |
| 👥 **Administración** | Login de administrador, salas, taquilleros, historial de facturas, anulaciones y reportes | ✅ Implementado |

---

## 👤 Roles y permisos

El sistema tiene dos roles (`RolUsuario`): **TAQUILLERO** y **ADMINISTRADOR**.

### 🎟️ Taquillero

No requiere contraseña ni cuenta con login: atiende en mostrador y queda asociado a cada venta mediante `idTaquillero` en la petición. El área de venta (cartelera, funciones, boletos, cafetería, inicio) es pública.

- Consultar películas, cartelera y funciones.
- Seleccionar y vender asientos.
- Añadir productos de confitería.
- Realizar compras únicamente de confitería.
- Registrar datos opcionales del cliente.
- Seleccionar efectivo o tarjeta.
- Generar e imprimir facturas.

### 🔐 Administrador

Requiere autenticación (Spring Security + BCrypt, formulario `/login`). Solo el ADMINISTRADOR puede entrar: `/administracion/**` y `/reportes/**` están protegidas.

- Todas las operaciones del taquillero.
- Gestionar salas y taquilleros.
- Consultar historial de facturas y anular facturas (la anulación libera los asientos).
- Consultar estadísticas y gráficos.

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
     │
     ▼               
   👤 Cliente      
   (opcional)     
     │               
     └───────┐
             ▼
       💳 Pago simulado
       ┌─────┴─────┐
       ▼           ▼
   ✅ Exitoso   ❌ Fallido
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

---

### 💳 Métodos de pago

Solo se admiten:

- 💵 **Efectivo**
- 💳 **Tarjeta**

El pago es **simulado**.

---

## 📊 Estadísticas y reportes

Panel administrativo (`/reportes`, solo ADMINISTRADOR) con gráficos y tablas calculados sobre los datos persistidos.

| Categoría | Indicadores |
|---|---|
| 💰 **Ingresos** | Entradas, confitería y total |
| 🛒 **Ventas** | Compras realizadas y entradas vendidas |
| 🎞️ **Películas** | Películas con mayor cantidad de entradas |
| 🕐 **Funciones** | Funciones con mayor cantidad de entradas |
| 🍿 **Productos** | Cantidad vendida y productos más vendidos |
| 🏛️ **Salas** | Ocupación por sala |
| 🎟️ **Taquilleros** | Ventas asociadas |
| 🧾 **Facturas** | Historial y detalle |

---

## 🖨️ Impresión

La factura se muestra como una **vista HTML preparada para impresión** mediante el navegador (`window.print()`), en `/facturas/{id}/imprimir`.

- No se requiere generación de PDF.
- Además se puede descargar un comprobante en texto plano (`/facturas/{id}/descargar-txt`).
- La impresión por Bluetooth dependerá de que el dispositivo y sistema operativo reconozcan la impresora como disponible.
- El backend no administra directamente el protocolo Bluetooth.

---

## 🏗️ Arquitectura

El proyecto utiliza una **arquitectura en capas** para separar presentación, lógica de negocio y persistencia. Es un monolito server-rendered: Spring MVC + Thymeleaf, sin API REST separada.

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
                          │   Spring MVC    │
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

- **Controller:** recibe solicitudes HTTP y devuelve vistas Thymeleaf.
- **Service:** concentra las reglas y lógica de negocio (precios, disponibilidad de asientos, anulaciones, reportes).
- **Repository:** gestiona la persistencia con Spring Data JPA. Las interfaces de facturación extienden `Repository` directamente para no exponer operaciones de borrado.
- **MySQL:** almacena los datos críticos del sistema.
- **Thymeleaf:** renderiza la interfaz web.
- **Spring Security:** protege `/administracion/**` y `/reportes/**` (rol ADMINISTRADOR); el resto del mostrador es público.

### 🧩 Estructuras de datos en uso

Detalle completo en `docs/Anexo 6 Estructura de datos.docx`: `ArrayList` (colecciones de factura y reportes), `LinkedHashMap` (carrito de confitería `@SessionScope`, merge de cantidades), `HashSet` (validación de asientos A1–H10 y detección de duplicados), `HashMap` (agregación en memoria de filas de reportes).

---

## 🛠️ Stack tecnológico

| Tecnología | Uso |
|---|---|
| ☕ **Java 21** | Lenguaje principal |
| 🌱 **Spring Boot 4.1.2** | Backend y estructura de la aplicación |
| 🔗 **Spring MVC** | Controllers y enrutamiento |
| 🎨 **Thymeleaf** | Interfaz y renderizado web |
| 🧩 **Spring Data JPA / Hibernate** | Persistencia y ORM |
| 🔐 **Spring Security** | Autenticación (form login + BCrypt) y autorización por roles |
| ✅ **Bean Validation** | Validación de DTOs y entidades (`@Valid`, `@NotNull`, `@Min`, …) |
| 🗄️ **MySQL 8.0.16+** | Base de datos (requiere 8.0.16+ para los `CHECK`) |
| 📦 **Maven** | Dependencias y construcción (`mvnw` incluido) |
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
- 💰 Desglose de IVA o recargos por tipo de sala (el precio ya lo fija la sala).

---

## 📁 Integridad histórica

El sistema puede reconstruir una operación histórica indicando **qué se vendió, cuándo, por cuánto, en qué función, en qué sala y quién atendió la operación**.

Por ello:

- Los precios aplicados se conservan en las líneas de venta/factura.
- Las facturas no pueden editarse ni eliminarse físicamente.
- Los registros con historial no deben eliminarse físicamente cuando esto rompa la trazabilidad.
- Las imágenes se almacenan mediante una **referencia a su ubicación** (`imagen_url`); no como BLOB en MySQL.

---

## 🚀 Instalación y ejecución

### Requisitos

- ☕ Java — **21**
- 📦 Spring Boot — **4.1.2**
- 🗄️ MySQL — **8.0.16 o superior**
- 🔧 Git
- 💻 IDE compatible con Spring Boot

### Clonar el repositorio

```bash
git clone https://github.com/MiguelAnBM/Proyecto_CineComfenalco_Web.git
cd Proyecto_CineComfenalco_Web
```

### Configuración de base de datos

El archivo `cine/src/main/resources/application.properties` solo define el nombre de la aplicación; las credenciales no se versionan (ver `.gitignore`). Crea un `application-local.properties` (o define variables de entorno) con tus datos:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cinecomfenalco_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=cine_app
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
```

Pasos:

1. Crea el usuario y la base en MySQL:

   ```sql
   CREATE USER 'cine_app'@'localhost' IDENTIFIED BY 'cine';
   CREATE DATABASE IF NOT EXISTS cinecomfenalco_db
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   GRANT ALL PRIVILEGES ON cinecomfenalco_db.* TO 'cine_app'@'localhost';
   ```

2. Ejecuta `docs/cinecomfenalco_schema.sql` para crear las tablas y cargar los datos de catálogo (80 asientos, géneros, tipos de sala y configuración de precio/IVA). El script crea la base `cinecomfenalco`; renómbrala o apunta `spring.datasource.url` a ella según prefieras. Está basado en el Documento 01 v0.1 y requiere MySQL 8.0.16+.

3. El script no trae usuarios de prueba: el insert del administrador viene comentado (requiere un hash BCrypt). Crea el administrador manualmente insertando su nombre, el rol ADMINISTRADOR y un hash generado (por ejemplo, con `BCryptPasswordEncoder`). Hazlo solo en local.

### Ejecutar

```bash
cd cine
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`. Rutas públicas: inicio, cartelera, funciones, boletos, cafetería. Rutas protegidas (requieren login de ADMINISTRADOR): `/administracion/**` y `/reportes/**` (`/login`).

---

## 👥 Equipo y reparto del trabajo

División vertical por dominio:

| Integrante | Corte | Alcance |
|---|---|---|
| **José Daniel Galvis Gómez** | Catálogo y Programación | Películas, salas, funciones, cartelera |
| **Delany Yulieth Mendoza Castillo** | Punto de Venta | Boletos, confitería, facturas, asientos, carrito |
| **Miguel Angel Benitez Moncaleano** | Usuarios, Administración y Reportes | Usuario y autenticación, panel de administración, anulaciones, reportes, inicio |

Los archivos transversales (`config/`, `exception/`, `layout.html`, fragments y recursos estáticos) se gestionan aparte del reparto por cortes.

---

## 📚 Documentación

Todo vive en `docs/`:

| Documento | Contenido |
|---|---|
| `docs/Informe_Metodológico_TDS.docx` | Informe metodológico: problema, estado del arte, marco legal, metodología, muestra y cronograma |
| `docs/Documento_01_Especificacion_Requisitos_MVP_Cine.md` | Especificación inicial de requisitos del MVP (v0.1) |
| `docs/Anexo 1 Contextualización.xlsx` | Contextualización y delimitación conceptual (POO, MVC, Spring Boot) |
| `docs/Anexo 2 Observacion.xlsx` | Observación del proceso de venta (5W2H, árbol de problema) |
| `docs/Anexo 3 Rastreo comercial.xlsx` | Rastreo comercial: idea, competencia, insumos y proveedor |
| `docs/Anexo 4 Historias de usuario.docx` | Historias de usuario del sistema (RF001–RF065) |
| `docs/Anexo 5 Diagrama de clases.docx` | Diagrama de clases del modelo de dominio y enumeraciones |
| `docs/Anexo 6 Estructura de datos.docx` | Estructuras de datos usadas (`ArrayList`, `LinkedHashMap`, `HashSet`, `HashMap`) |
| `docs/cinecomfenalco_schema.sql` | Esquema de referencia de la base de datos |

---

## 🎓 Información académica

- **Proyecto:** CineComfenalco
- **Asignatura:** Desarrollo de Software
- **Programa:** Tecnología en Desarrollo de Software
- **Institución:** Fundación Universitaria Tecnológico Comfenalco
- **Profesor:** Antonio José de la Valle
- **Periodo:** 4.º semestre · 2026-2
- **Equipo:** 3 integrantes
- **Informe metodológico:** 16-11-2026 (`docs/Informe_Metodológico_TDS.docx`)

---

<p align="center">
  🎬 <strong>CineComfenalco</strong><br>
  <em>Una plataforma para gestionar el cine de forma simple, eficiente y organizada.</em>
</p>
