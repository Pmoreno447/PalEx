# ADR-008. Límite de reservas pendientes por usuario

Un usuario no puede tener más de **`app.booking.max-pending`** reservas en estado `PENDING` a la vez (2 por defecto). Si ya las tiene, una nueva reserva responde **409 Conflict**.

## Motivo

Una reserva aparta el turno **antes** de pagarse, y queda pendiente hasta que llega el pago o vence (HU-12). Sin límite, un script que reserve sin pagar una y otra vez puede **mantener bloqueadas todas las pistas** sin gastar nada: es el llamado acaparamiento de inventario (*inventory hoarding*).

Como cada petición va autenticada, el límite se puede aplicar por usuario, que es la defensa más sencilla y directa.

## Consecuencias

- Un usuario legítimo que quiera reservar varios turnos tiene que pagar los anteriores antes de seguir. Con un límite de 2 se considera aceptable.
- El valor es configurable en `application.properties`, para que cada club lo ajuste sin tocar código.
- El límite por sí solo no basta si el atacante crea muchas cuentas. Ese coste depende del proveedor de identidad (verificación de correo, captcha). Si hiciera falta más protección, se añadiría rate limiting y un vencimiento corto de las reservas pendientes.
