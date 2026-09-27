# ADR-005. Algoritmo para calcular la disponibilidad

El endpoint de disponibilidad es el que más se va a usar de toda la aplicación: es el tablón donde el usuario ve los huecos libres, así que se consulta constantemente y antes de cualquier reserva. Por eso merecía la pena pensar el cálculo en lugar de escribir lo primero que saliera.

## La forma sencilla

Lo directo era, para cada turno de la rejilla, recorrer la lista de reservas comprobando si alguna lo pisa. Se puede cortar en cuanto encuentras una que se solapa (el turno ya está ocupado y no hace falta seguir), pero eso solo mejora el caso bueno: en el peor caso sigues recorriendo todas las reservas por cada turno, o sea **O(n · m)** con n turnos y m reservas.

Y el peor caso no es raro aquí: los turnos libres, que son la mayoría de lo que se consulta, obligan a recorrer la lista entera para confirmar que ninguna reserva los pisa. Para el endpoint más usado del sistema, no parecía razonable.

## El algoritmo elegido

Aprovechamos que **las dos secuencias están ordenadas**: la rejilla de turnos se genera en orden y las reservas llegan ordenadas por fecha de inicio desde la base de datos.

Con eso se pueden recorrer las dos a la vez con dos punteros que solo avanzan, mirando únicamente la primera reserva de la cola:

- Si el turno acaba antes de que empiece esa reserva → **libre**, y avanza el turno.
- Si el turno empieza después de que la reserva haya terminado → esa reserva ya no puede molestar a nadie más → **se descola**, sin avanzar el turno (hay que volver a comprobarlo contra la siguiente).
- En cualquier otro caso se solapan → **ocupado**, y avanza el turno.
- Si no quedan reservas de ese día, el resto del día es libre directamente.

La propiedad que lo hace correcto es esta: **como los turnos también avanzan en orden, una reserva que queda por detrás del turno actual no puede afectar a ningún turno posterior**. Por eso se puede descartar sin mirar atrás.

Es el mismo enfoque que el paso de mezcla del mergesort, o un barrido de línea sobre intervalos. La complejidad baja a **O(n + m)**: cada turno se emite una vez y cada reserva se descola una vez.

## El índice es parte de la decisión

El algoritmo exige que las reservas lleguen ordenadas, y eso **solo es gratis si existe el índice adecuado**:

```sql
CREATE UNIQUE INDEX uk_court_start ON bookings (court_id, init_date_time);
```

Al estar el índice ordenado por esas dos columnas, la consulta obtiene de una vez tres cosas: filtrar por pista, recorrer el rango de fechas y **devolver las filas ya ordenadas por hora de inicio**, sin ningún paso de ordenación.

Sin ese índice, la base de datos tendría que ordenar el resultado (`filesort`), que es O(m log m), y parte de la ganancia se perdería justo en la consulta más frecuente del sistema. Además, ese mismo índice es el que se usa para impedir la doble reserva, así que hace tres trabajos a la vez.

Un detalle pendiente: como la consulta también necesita `end_date_time`, que no está en el índice, hay que leer las filas. Si en algún momento el volumen lo justificara, añadir esa columna al índice lo convertiría en cobertor y la consulta se resolvería sin tocar la tabla.

## Consecuencias

- El coste pasa a ser lineal en lugar de cuadrático, y el endpoint más consultado del sistema deja de degradarse con el número de reservas.
- El algoritmo **depende de que la lista llegue ordenada**. Si algún día se cambia la consulta y se pierde el `order by`, o se borra el índice, el resultado es incorrecto sin dar ningún error. Está escrito como precondición.
- Es más difícil de leer que el bucle anidado, y tiene casos límite (turno que no cabe antes del cierre, reserva que ocupa varios turnos, reserva de otro día, cambio de día). Por eso vive en su propia clase, `AvailabilityCalculator`, sin dependencias de Spring ni de la base de datos, para poder probar cada caso por separado.
- Solo funciona con horarios que no cruzan la medianoche. Si algún club cerrara a las 02:00, habría que replantearlo.

## Pseudocódigo

```
*pre:
*   horaApertura: Hora,
*   horaCierre: Hora, // Debe ser posterior a horaApertura
*   desde: FechaHora, // Su hora debe estar puesta a la hora de apertura y el dia no puede ser anterior al actual
*   hasta: FechaHora, // Su hora debe estar puesta a la hora de cierre y no puede superar el app.booking.max-days-ahead de properties
*   duracionTurno: Entero,
*   listaReservas: Lista de Reservas // Debe venir ordenada por fecha

mientras (desde < hasta)
    si (desde.hora + duracionTurno > horaCierre):
        desde.dias = desde.dias + 1
        desde.hora = horaApertura

    en caso contrario:
        si (reservas no está vacio && desde.dia == reservas[0].dia):
            si ([desde.hora + duracionTurno] <= reservas[0].inicio):
                turno libre
                desde.hora = desde.hora + duracionTurno
            en caso contrario:
                si (desde.hora >= reservas[0].fin):
                    descolar reservas[0]
                en caso contrario:
                    turno ocupado
                    desde.hora = desde.hora + duracionTurno

        en caso contrario:
            mientras (desde.hora + duracionTurno <= horaCierre)
                turno libre
                desde.hora = desde.hora + duracionTurno
```

Nota de implementación: en Java, las comparaciones con la hora de cierre se hacen sobre `LocalDateTime` y no sobre `LocalTime`, porque `LocalTime` da la vuelta al pasar de las 23:59 y un turno que se sale del día parecería caber.
