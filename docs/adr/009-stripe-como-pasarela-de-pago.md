# ADR-009. Stripe como única pasarela de pago

El proyecto se integra con **una única pasarela, Stripe**, en lugar del puerto de pagos genérico con implementación simulada que planteaba el roadmap. El club que despliegue el proyecto tendrá que usar Stripe.

## Motivo

Mantener el sistema independiente del proveedor obliga a diseñar una abstracción sin conocer aún las necesidades reales de cada pasarela, y a mantener una implementación simulada. Fijar Stripe simplifica el desarrollo y acorta los plazos.

## Consecuencias

- El cobro (crear la sesión de pago) y el handler de los eventos de Stripe se mantienen en piezas bien separadas del resto de la lógica de reservas, para que cambiar de proveedor suponga reescribir esas piezas y no el dominio.
- El vencimiento de las reservas pendientes lo gestiona Stripe con el evento `checkout.session.expired`, sin un job propio.
