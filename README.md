# PalEx

Backend en Spring Boot para la gestión de clubes de pádel: reservas con pago, torneos, estadísticas entre amigos y acceso a las pistas mediante QR.

La idea nace de un problema real: en mi pueblo, reservar una pista pasa por llamar al alcalde, recoger la llave en una tienda, devolverla al terminar y pagar en el ayuntamiento al día siguiente. PalEx resuelve todo eso desde el móvil.

Sus cuatro módulos son **independientes**, así que cada club activa solo los que necesita. Un club que ya gestione sus llaves puede usar únicamente las reservas.

## Estado del proyecto

En desarrollo, sin código todavía. Ahora mismo hay documentación de diseño y un roadmap por fases.

## Stack

- **Java + Spring Boot**, como monolito modular.
- **MySQL** como base de datos.
- **Autenticación y pagos delegados.** El proyecto no implementa un proveedor concreto: valida los tokens de un proveedor externo (Firebase, Auth0, Keycloak…) y define un puerto de pagos con una implementación simulada, de forma que cada despliegue enchufe Stripe, Redsys o lo que prefiera.

## Documentación

| Documento | Qué encontrarás |
| --- | --- |
| [Concepto](./docs/concept.md) | Qué hace la aplicación, de dónde surge la idea y por qué se ha elegido este stack. |
| [Arquitectura](./docs/architecture.md) | Modelo de datos, casos de uso, flujos principales y organización del código. |
| [Roadmap](./docs/roadmap.md) | Las épicas e historias de usuario previstas, con su orden de implementación. |
