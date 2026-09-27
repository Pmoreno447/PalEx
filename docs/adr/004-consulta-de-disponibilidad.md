# ADR-004. Consulta de disponibilidad por rango y con ventana de reserva

Teníamos que decidir cómo funciona el endpoint que muestra los turnos libres de una pista. Hay dos problemas distintos: qué recibe como argumento y qué hacemos si alguien manipula ese argumento.

## Un día o un rango

La primera idea fue pedir un solo día: `?date=2026-09-28`. El problema es que el frontend va a mostrar una semana, así que serían siete peticiones y siete consultas a la base de datos para pintar una sola pantalla.

Elegimos **un rango**: `?from=...&to=...`. Todas las reservas del rango se traen con una única consulta, y la rejilla de turnos se genera en memoria agrupando por día. Un solo recorrido del índice `(court_id, start_time)`, que además contiene todo lo que la consulta necesita, así que ni siquiera hace falta leer las filas de la tabla.

## Qué pasa si alguien cambia la fecha de la URL

El argumento viaja en la URL, así que cualquiera puede pedir lo que quiera. Dos cosas que nos incomodan:

- Consultando días pasados se puede ir recopilando qué turnos se ocuparon y cuáles no. No es información sensible (no se ve quién reservó), pero tampoco tenemos motivo para regalarla.
- Se podrían intentar reservas de turnos que ya han pasado, lo cual no tiene ningún sentido.

Lo mínimo sería proteger solo la creación de la reserva, que es donde está el daño real. Pero hemos decidido **proteger también la consulta de disponibilidad**, con una ventana definida en configuración:

```properties
app.booking.max-days-ahead=14
```

La ventana válida es `[hoy, hoy + maxDaysAhead]`, y **la usan los dos endpoints**: el de disponibilidad y el de reserva. Fuera de ella, la petición se rechaza. Solo el límite superior es configurable; el inferior es siempre hoy y no necesita propiedad.

El límite superior no es solo defensa: es una regla de negocio. El club no quiere que alguien aparte la pista de Nochevieja en marzo, y el número concreto de días lo decide cada club en su configuración.

## Consecuencias

- Una sola consulta por pantalla, en lugar de una por día.
- La validación de la fecha queda en los dos endpoints. La de la reserva es la que de verdad importa, porque la disponibilidad es solo una foto informativa y nada obliga a pedirla antes de reservar.
- Al estar la ventana acotada por los dos extremos, el tamaño de la respuesta también queda acotado sin necesidad de un límite aparte.
- Perdemos la posibilidad de consultar días pasados. Si algún día hace falta (estadísticas del club, o que un usuario vea su histórico), será otro endpoint, con sus propios permisos y limitado a los datos de quien pregunta.
- Esto no protege de alguien que lance miles de peticiones dentro de la ventana válida. Para eso hace falta limitar la tasa de peticiones, que está pendiente para producción.
