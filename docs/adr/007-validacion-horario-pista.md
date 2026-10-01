# ADR-007. Validación del horario de apertura y cierre de las pistas

Nos dimos cuenta de que la API permitía guardar pistas con la hora de cierre anterior a la de apertura, por ejemplo una pista que abre a las 16:00 y cierra a las 10:00. Nada lo impedía: ni el DTO, ni el service, ni la base de datos. Una pista así no tiene ningún turno, y el cálculo de disponibilidad devuelve siempre una lista vacía sin dar ningún error.

## Las dos opciones

**Un validador en el DTO.** Bean Validation no trae ninguna anotación para comparar dos campos entre sí, pero se puede resolver con un método anotado con `@AssertTrue` o con una anotación de clase propia y su `ConstraintValidator`. Es la opción más limpia: la regla queda declarada junto a los datos y el error sale como cualquier otro fallo de validación.

**Un método auxiliar en el service.** `CourtService` comprueba el horario y lanza una `BadRequestException`, que el `GlobalExceptionHandler` convierte en un 400.

## Por qué el service

El problema está en que `CourtRequest` se usa tanto para crear como para actualizar, y en la actualización todos los campos son opcionales: `null` significa "no lo cambies". Si un admin manda solo:

```json
{ "closTime": "10:00" }
```

el DTO no conoce la hora de apertura actual de la pista, que está en la base de datos. El validador tendría que devolver `true` al no tener nada con qué comparar, y la pista acabaría abriendo a las 16:00 y cerrando a las 10:00.

El único sitio donde están los dos valores definitivos es el service, **después** de aplicar los cambios del request a la pista. Por eso la comprobación vive en un método auxiliar que se llama desde `createCourt`, antes de guardar, y desde `updateCourt`, después de aplicar los cambios.

## Consecuencias

- La regla está en un solo sitio y cubre los dos casos. Se prueba en `CourtServiceTests` sin levantar Spring, incluido el caso de actualizar un solo campo, que era el que ningún validador de DTO podía detectar.
- Perdemos la validación declarativa: el error sale del service y no junto al resto de errores de validación del request.
- Al analizarlo vimos que compartir el DTO entre crear y actualizar es el origen del problema. Al crear, todos los campos deberían ser obligatorios, y hoy nada impide mandar una pista sin horario. Se separará en un DTO de creación, sin nulos y con sus propias validaciones, y otro de actualización con todo opcional. Se implementa en el commit `feat: dtoCreate`, con el que se da por terminada la EP-02.
- Aun con los DTOs separados, la comprobación del service se mantiene: la actualización parcial seguirá pudiendo cambiar solo una de las dos horas.
