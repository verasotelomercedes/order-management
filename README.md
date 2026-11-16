# order-management

Servicio de gestión  cálculo de costes de envío de pedidos con integración de un proveedor logístico externo.

Tecnologías: Spring Boot 4 / Java 17, maven, H2 en memoria.

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/clientes` | Crear un cliente |
| GET | `/api/clientes/{id}` | Obtener un cliente |
| POST | `/api/pedidos` | Crear un pedido (nace en `PENDING_CONFIRMATION`) |
| GET | `/api/pedidos/{id}` | Obtener un pedido |
| POST | `/api/pedidos/{id}/confirmacion` | Confirmar un pedido: pide el coste de envío al proveedor |

Consola de H2: `http://localhost:8080/h2-console` (`jdbc:h2:mem:ordermanagement`, usuario `sa`, contraseña vacía).

## Estados del pedido

- `PENDING_CONFIRMATION`: creado, coste de envío desconocido, `total = importe de productos`.
- `CONFIRMING`: hay una confirmación en curso (se está llamando al proveedor).
- `CONFIRMED`: coste de envío guardado, `total = importe de productos + coste de envío`.

## Manejo de fallos del proveedor logístico

Contrato: `POST /api/tarifas` con `Authorization: Bearer <token>`. En producción el proveedor puede tardar
hasta 5 segundos, devuelve errores 503 de forma intermitente y exige un token.

| Aspecto | Decisión |
| --- | --- |
| Respuestas lentas | `connect-timeout: 2s`, `read-timeout: 6s` (1 s de margen sobre los 5 s que puede tardar). Pasado ese tiempo la llamada falla. |
| 503 intermitentes | Hasta `max-retries: 2` reintentos (3 intentos) con `retry-backoff: 300ms`. También se reintentan los timeouts y los errores de red. Los demás errores (401, 400, 500...) **no** se reintentan. Pedir una tarifa no tiene efectos secundarios, así que reintentar es seguro. |
| El proveedor sigue fallando | `ShippingRateException` -> HTTP **502**. El pedido vuelve a `PENDING_CONFIRMATION` sin coste de envío, y se puede pedir la confirmación otra vez. |
| Duración en el peor caso | Unos 18 s (3 intentos de 6 s más las esperas). Si es demasiado, bajar `max-retries` o `read-timeout`. |
| Token | Nunca en el código fuente: `logistics.token` se lee de la variable de entorno `LOGISTICS_TOKEN`. Con el token vacío el proveedor responde 401 y la confirmación termina en 502. |

Ninguna transacción de base de datos permanece abierta durante la llamada HTTP. `OrderService.confirm` no es
transaccional; cada cambio de estado es una transacción corta e independiente (`OrderStateManager`,
`REQUIRES_NEW`):

1. `startConfirmation`: `PENDING_CONFIRMATION` -> `CONFIRMING` (commit).
2. Llamada HTTP al proveedor (con timeouts y reintentos), sin transacción.
3. `completeConfirmation` (-> `CONFIRMED`, guarda el coste de envío) o `cancelConfirmation` (-> `PENDING_CONFIRMATION`).

Si la aplicación se cae entre el paso 1 y el 3, el pedido se queda en `CONFIRMING` y no hay recuperación
automática (un proceso programado que devuelva a `PENDING_CONFIRMATION` los pedidos que lleven demasiado
tiempo en `CONFIRMING` sería el siguiente paso).

## Confirmaciones simultáneas del mismo pedido

Dos confirmaciones del mismo pedido nunca provocan dos llamadas al proveedor:

- Solo un pedido en `PENDING_CONFIRMATION` puede iniciar una confirmación; la segunda petición lo encuentra
  en `CONFIRMING` o `CONFIRMED` y falla con `IllegalStateException` (HTTP **409**).
- Condición de carrera: si ambas peticiones leen el pedido como `PENDING_CONFIRMATION` antes de que ninguna
  lo guarde como `CONFIRMING`, las dos pasarían la comprobación de estado y llamarían al proveedor. Para
  evitarlo, `OrderEntity` tiene una columna `@Version` (bloqueo optimista): el `UPDATE` incluye
  `WHERE version = ?`, así que solo una actualización gana y la otra falla con un error de bloqueo
  optimista (HTTP **409**).

## Precisión decimal

Ningún `double`/`float`. Todos los importes son `BigDecimal` redondeados con `RoundingMode.HALF_UP`:

| Valor | Escala | Dónde |
| --- | --- | --- |
| Precio unitario | 4 | `OrderLine` lo normaliza; la columna `unit_price` es `DECIMAL(19,4)` |
| Peso unitario (kg) | 3 | `OrderLine`; columna `DECIMAL(10,3)` |
| Importe de línea, importe de productos, coste de envío, total | 2 | `OrderLine`, `OrderCalculator`, `Order`; columnas `DECIMAL(19,2)` |

Cada importe de línea se redondea a 2 decimales y después se suman las líneas. El dominio normaliza los
valores de entrada a la misma escala que guarda la base de datos, de modo que un pedido tiene los mismos
importes antes de guardarse y después de recargarse.

## Configuración

| Propiedad | Variable de entorno | Valor por defecto |
| --- | --- | --- |
| `logistics.base-url` | `LOGISTICS_BASE_URL` | `http://localhost:9090` |
| `logistics.token` | `LOGISTICS_TOKEN` | vacío |
| `logistics.connect-timeout` | | `2s` |
| `logistics.read-timeout` | | `6s` |
| `logistics.max-retries` | | `2` |
| `logistics.retry-backoff` | | `300ms` |

## Cómo arrancar la aplicación y los tests

Requisitos: Java 17

No existe un proveedor real. En `tools/logistics-stub` hay un stub que comprueba el token (por defecto
`testToken`; se cambia con `STUB_TOKEN`, y el puerto con `STUB_PORT`, por defecto 9090):

```bash
python3 -I tools/logistics-stub/logistics_stub.py
```

En otra terminal, la aplicación (puerto 8080):

```bash
LOGISTICS_TOKEN=testToken ./mvnw spring-boot:run
```

Los tests no necesitan el stub ni el token: levantan su propio proveedor simulado.

```bash
./mvnw test
```

- Unitarios: `OrderCalculatorTest` y `OrderTest` (importes, redondeo, total con envío, estados),
  `OrderServiceTest`, `OrderStateManagerTest`, `CustomerServiceTest`.
- `LogisticsRestClientTest`: contrato, token, reintentos ante 503, timeout, contra un servidor HTTP simulado.
- `OrderConfirmationIntegrationTest` (contexto de Spring + H2 + proveedor simulado): confirmación correcta,
  recuperación de 503 intermitentes, 503 persistente, timeout del proveedor (el pedido se queda en
  `PENDING_CONFIRMATION`), volver a confirmar tras un fallo y confirmaciones simultáneas (una sola llamada
  al proveedor).

## Decisiones de diseño y por qué

- **Arquitectura hexagonal.** El dominio (`Order`, `OrderLine`, `Customer`) es Java puro, sin JPA ni
  Spring. Los puertos de salida (`OrderRepositoryPort`, `CustomerRepositoryPort`, `ShippingRatePort`) están en
  `domain/ports/outbound` y sus adaptadores en `infrastructure`. Así las reglas de negocio se prueban sin
  base de datos ni HTTP, y cambiar de base de datos o de proveedor no toca el dominio.
- **Estados del pedido con un estado intermedio (`CONFIRMING`).** Confirmar implica una llamada externa lenta.
  Marcar el pedido como `CONFIRMING` antes de llamar permite rechazar una segunda confirmación sin esperar al
  proveedor y deja constancia de que hay una en curso.
- **Sin transacción durante la llamada HTTP.** Una transacción abierta durante hasta 18 s retendría una
  conexión de base de datos y bloquearía filas. Por eso `OrderService.confirm` no es transaccional y cada
  cambio de estado es una transacción corta (`REQUIRES_NEW`). A cambio, si falla algo entre pasos hay que
  compensar a mano (`cancelConfirmation`).
- **Reintentos dentro del cliente HTTP, solo ante 503, timeout y errores de red.** Un 401 o un 400 no mejoran
  reintentando. Pedir una tarifa no tiene efectos secundarios, así que reintentar es seguro. El fallo final se
  traduce a un error del dominio (`ShippingRateException`, HTTP 502) para que los servicios no dependan de
  Spring ni de HTTP.
- **Concurrencia: máquina de estados más bloqueo optimista, sin bloqueos de base de datos.** Solo un pedido en
  `PENDING_CONFIRMATION` puede empezar a confirmarse, y `@Version` resuelve la carrera cuando dos peticiones
  leen el mismo estado. No se mantiene ningún bloqueo durante la llamada externa. Se demuestra con un test de
  5 hilos simultáneos que ve una sola llamada al proveedor.
- **Importes con `BigDecimal` y escalas explícitas**, normalizadas en el dominio a la misma escala que guarda
  la base de datos (ver "Precisión decimal"). Lo descubrió el test de integración: un precio de 10.005 volvía
  de la base de datos como 10.01 y el total cambiaba al recargar el pedido.
- **Validación manual, sin librería de validación.** El formato de la petición se valida en `OrderDtoMapper`
  y las reglas de negocio en el dominio y los servicios (`IllegalArgumentException` -> 400).
- **Configuración con `@Value` en una clase `@Configuration`** (`LogisticsConfiguration`) y el token por
  variable de entorno: sencillo y sin claves en el repositorio.

## Qué ha quedado fuera y cómo lo abordaría

- **Token cifrado con Jasypt.** Hoy el token viene de una variable de entorno. Para guardarlo cifrado
  (por ejemplo en el `application.yml` como `ENC(...)`, o en base de datos) usaría la librería Jasypt, con la
  contraseña de cifrado fuera del repositorio.
- **Recuperación automática de confirmaciones atascadas.** Un proceso programado (`@Scheduled`) que cada cierto tiempo devuelva a `PENDING_CONFIRMATION` los pedidos que lleven más de unos minutos en `CONFIRMING` (la aplicación cayó a mitad de confirmación). Es lo más importante que falta.
- **Reintento automático de pedidos pendientes.** Un job que reintente la confirmación de los pedidos sin confirmar. Habría que añadir un estado
  o un indicador (por ejemplo `CONFIRMATION_FAILED`) para que el job solo toque
  los pedidos cuya confirmación se pidió y falló, con un máximo de intentos.


## Uso de IA

Me he apoyado en Claude Code modelo Sonnet 5.5 para generar la estructura y los tests, y lo he ido revisando.

Lo que cambié respecto a lo que propuso:

- Pedí arquitectura hexagonal estricta: dominio sin anotaciones de JPA y puertos movidos a
  `domain/ports/outbound`.
- Quité la librería de validación porque ensuciaba mucho el dto: la validación es manual.
- Servicios concretos (`OrderService`, `CustomerService`), sin interfaces de casos de uso.
- Pedí que el proveedor logístico fuera un puerto del dominio de pedidos y no un módulo aparte.
- Eliminé exceso de comentarios.
- Cambios varios por legibilidad del código.
- Agregué logs
