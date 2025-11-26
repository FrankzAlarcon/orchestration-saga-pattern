# Saga Pattern Spring Boot Demo

Demonstración del patrón de diseño **SAGA Orchestration** utilizando Spring Boot y Apache Kafka para gestionar transacciones distribuidas en un sistema de microservicios.

## 📋 Descripción

Este proyecto implementa un sistema de gestión de órdenes distribuido que utiliza el patrón Saga para coordinar transacciones entre múltiples microservicios. El sistema maneja el proceso completo de una orden: desde su creación, pasando por la reserva de productos, el procesamiento de pagos, hasta la aprobación o rechazo de la orden.

## 🏗️ Arquitectura

El proyecto implementa el patrón **Saga Orchestration** donde un orquestador central (OrderSaga) coordina los pasos de la transacción distribuida mediante eventos y comandos a través de Kafka.

### Servicios

El proyecto está compuesto por los siguientes microservicios:

1. **orders-service** (Puerto 8080)
   - Gestiona las órdenes de compra
   - Contiene el orquestador de la saga (OrderSaga)
   - Mantiene el historial de cambios de estado de las órdenes

2. **products-service** (Puerto 8081)
   - Gestiona el catálogo de productos
   - Maneja la reserva y cancelación de reservas de productos
   - Utiliza H2 como base de datos en memoria

3. **payments-service** (Puerto 8082)
   - Procesa los pagos de las órdenes
   - Se comunica con el servicio de procesamiento de tarjetas de crédito

4. **credit-card-processor-service** (Puerto 8084)
   - Simula el procesamiento de tarjetas de crédito
   - Servicio externo que procesa las solicitudes de pago

5. **core** (Módulo compartido)
   - Contiene los DTOs, eventos, comandos y tipos compartidos entre servicios
   - Define el contrato común para la comunicación entre servicios

## 🛠️ Tecnologías

- **Java 17**
- **Spring Boot 3.2.5**
- **Apache Kafka** (KRaft mode - sin Zookeeper)
- **Spring Kafka** - Integración con Kafka
- **Spring Data JPA** - Persistencia de datos
- **H2 Database** - Base de datos en memoria
- **Maven** - Gestión de dependencias
- **Docker Compose** - Orquestación de Kafka

## 📁 Estructura del Proyecto

```
saga-pattern-spring-boot-demo/
├── core/                          # Módulo compartido
│   └── src/main/java/com/appsdeveloperblog/core/
│       ├── dto/                   # DTOs compartidos
│       ├── dto/commands/          # Comandos
│       ├── dto/events/            # Eventos
│       └── types/                 # Tipos enumerados
├── orders-service/                # Servicio de órdenes
│   └── src/main/java/com/appsdeveloperblog/orders/
│       ├── saga/                  # Orquestador de la saga
│       ├── service/               # Lógica de negocio
│       └── web/controller/        # Controladores REST
├── products-service/              # Servicio de productos
├── payments-service/              # Servicio de pagos
├── credit-card-processor-service/ # Procesador de tarjetas
├── docker-compose.yml             # Configuración de Kafka
└── pom.xml                        # POM padre
```

## 🔄 Flujo de la Saga

### Flujo Exitoso

1. **Cliente crea una orden** → `POST /orders`
   - Se crea la orden con estado `CREATED`
   - Se publica evento `OrderCreatedEvent`

2. **OrderSaga recibe OrderCreatedEvent**
   - Envía comando `ReserveProductCommand` al products-service
   - Registra estado `CREATED` en el historial

3. **Products-service procesa ReserveProductCommand**
   - Reserva el producto (reduce cantidad disponible)
   - Publica evento `ProductReservedEvent`

4. **OrderSaga recibe ProductReservedEvent**
   - Envía comando `ProcessPaymentCommand` al payments-service

5. **Payments-service procesa ProcessPaymentCommand**
   - Llama al credit-card-processor-service
   - Publica evento `PaymentProcessedEvent` si es exitoso

6. **OrderSaga recibe PaymentProcessedEvent**
   - Envía comando `ApprovedOrderCommand` al orders-service

7. **Orders-service procesa ApprovedOrderCommand**
   - Cambia el estado de la orden a `APPROVED`
   - Publica evento `OrderApprovedEvent`
   - OrderSaga registra estado `APPROVED` en el historial

### Flujo de Compensación (Rollback)

Si el pago falla:

1. **Payments-service publica PaymentFailedEvent**
2. **OrderSaga recibe PaymentFailedEvent**
   - Envía comando `CancelProductReservationCommand` al products-service
3. **Products-service procesa CancelProductReservationCommand**
   - Cancela la reserva (restaura cantidad disponible)
   - Publica evento `ProductReservationCancelledEvent`
4. **OrderSaga recibe ProductReservationCancelledEvent**
   - Envía comando `RejectOrderCommand` al orders-service
   - Registra estado `REJECTED` en el historial
5. **Orders-service procesa RejectOrderCommand**
   - Cambia el estado de la orden a `REJECTED`

## 📡 Topics de Kafka

### Topics de Comandos
- `products.commands` - Comandos para el servicio de productos
- `payments.commands` - Comandos para el servicio de pagos
- `orders.commands` - Comandos para el servicio de órdenes

### Topics de Eventos
- `orders.events` - Eventos del servicio de órdenes
- `products.events` - Eventos del servicio de productos
- `payments.events` - Eventos del servicio de pagos

Cada topic está configurado con:
- **3 particiones**
- **3 réplicas** (factor de replicación)

## 🔌 Endpoints REST

### Orders Service (Puerto 8080)

- `POST /orders` - Crear una nueva orden
  ```json
  {
    "customerId": "uuid",
    "productId": "uuid",
    "productQuantity": 2
  }
  ```

- `GET /orders/{orderId}/history` - Obtener el historial de estados de una orden

### Products Service (Puerto 8081)

- `GET /products` - Listar todos los productos
- `POST /products` - Crear un nuevo producto
  ```json
  {
    "name": "Product Name",
    "price": 99.99,
    "quantity": 10
  }
  ```

### Credit Card Processor Service (Puerto 8084)

- `POST /ccp/process` - Procesar pago con tarjeta de crédito
  ```json
  {
    "orderId": "uuid",
    "productId": "uuid",
    "productPrice": 99.99,
    "productQuantity": 2
  }
  ```

## 🚀 Cómo Ejecutar el Proyecto

### Prerrequisitos

- Java 17 o superior
- Maven 3.6+
- Docker y Docker Compose

### Pasos

1. **Iniciar Kafka con Docker Compose**
   ```bash
   docker-compose up -d
   ```
   Esto iniciará un cluster de Kafka con 3 brokers en modo KRaft:
   - Broker 1: `localhost:9091`
   - Broker 2: `localhost:9092`
   - Broker 3: `localhost:9093`

2. **Compilar el proyecto**
   ```bash
   mvn clean install
   ```

3. **Ejecutar los servicios** (en orden recomendado):
   
   ```bash
   # Terminal 1 - Products Service
   cd products-service
   mvn spring-boot:run
   
   # Terminal 2 - Credit Card Processor Service
   cd credit-card-processor-service
   mvn spring-boot:run
   
   # Terminal 3 - Payments Service
   cd payments-service
   mvn spring-boot:run
   
   # Terminal 4 - Orders Service
   cd orders-service
   mvn spring-boot:run
   ```

   O ejecutar todos desde el directorio raíz:
   ```bash
   mvn spring-boot:run -pl products-service &
   mvn spring-boot:run -pl credit-card-processor-service &
   mvn spring-boot:run -pl payments-service &
   mvn spring-boot:run -pl orders-service
   ```

## 🧪 Probar el Sistema

### 1. Crear un producto
```bash
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 999.99,
    "quantity": 5
  }'
```

### 2. Obtener el ID del producto creado
```bash
curl http://localhost:8081/products
```

### 3. Crear una orden
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "productId": "<PRODUCT_ID>",
    "productQuantity": 2
  }'
```

### 4. Verificar el historial de la orden
```bash
curl http://localhost:8080/orders/<ORDER_ID>/history
```

## 📊 Comandos y Eventos

### Comandos
- `ReserveProductCommand` - Reservar un producto
- `CancelProductReservationCommand` - Cancelar reserva de producto
- `ProcessPaymentCommand` - Procesar un pago
- `ApprovedOrderCommand` - Aprobar una orden
- `RejectOrderCommand` - Rechazar una orden

### Eventos
- `OrderCreatedEvent` - Orden creada
- `ProductReservedEvent` - Producto reservado
- `ProductReservationCancelledEvent` - Reserva de producto cancelada
- `PaymentProcessedEvent` - Pago procesado exitosamente
- `PaymentFailedEvent` - Pago fallido
- `OrderApprovedEvent` - Orden aprobada

## 🔧 Configuración

Los servicios están configurados para conectarse al cluster de Kafka en:
- `localhost:9091`
- `localhost:9092`
- `localhost:9093`

La configuración de Kafka incluye:
- Serialización JSON para valores
- Idempotencia habilitada
- `acks=all` para garantizar durabilidad
- Deserialización con paquetes confiables configurados

## 📝 Notas

- Los servicios utilizan bases de datos H2 en memoria, por lo que los datos se pierden al reiniciar
- El credit-card-processor-service simula el procesamiento y siempre acepta las solicitudes
- Los topics de Kafka se crean automáticamente al iniciar los servicios (según configuración)
- El sistema implementa el patrón Saga Orchestration, donde el OrderSaga actúa como orquestador central

## 🎯 Objetivos de Aprendizaje

Este proyecto demuestra:
- Implementación del patrón Saga para transacciones distribuidas
- Uso de Apache Kafka para comunicación asíncrona entre microservicios
- Separación de comandos y eventos (CQRS-like)
- Manejo de compensación (rollback) en transacciones distribuidas
- Arquitectura de microservicios con Spring Boot
- Configuración de Kafka en modo KRaft

## 📄 Licencia

Este es un proyecto de demostración y aprendizaje.
