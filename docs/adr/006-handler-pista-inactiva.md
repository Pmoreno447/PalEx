# ADR-006. Respuesta al consultar una pista inactiva

Cuando se pide la disponibilidad de una pista dada de baja (`active = false`), la API responde **404 Not Found con el mismo mensaje que si la pista no existiera**, en lugar de un 409 que diga que está inactiva.

## Motivo

- **No revelar pistas ocultas.** Una pista inactiva puede ser una pista nueva que todavía no se ha anunciado. Si la API respondiera "está dada de baja", cualquiera podría descubrir su existencia probando ids.
- **Coherencia con el listado.** `GET /courts` ya oculta las pistas inactivas a los usuarios que no son admin. Para ellos esas pistas no existen, así que el resto de endpoints debe comportarse igual.
- **Está permitido por HTTP.** RFC 9110 contempla responder 404 en lugar de 403 cuando el servidor no quiere revelar que el recurso existe. Es lo mismo que hace GitHub con los repositorios privados.

## Consecuencias

- Las dos respuestas (no existe / inactiva) tienen que ser **idénticas**: mismo código y mismo `detail`. Cualquier diferencia volvería a filtrar la información. El test `shouldRejectInactiveCourt` lo comprueba.
- El admin tampoco distingue ahora los dos casos. Si en el futuro lo necesita (por ejemplo, para revisar una pista antes de activarla), habrá que pasar el rol al service, como ya se hace en `getCourts`.
- A un usuario que ya conociera la pista (por ejemplo, porque tuvo reservas en ella), un 404 le puede resultar confuso. Se acepta, porque de momento no hay historial de reservas visible.
