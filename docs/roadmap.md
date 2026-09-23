# Roadmap

En este documento detallaremos los requisitos previstos y cómo abordarlos.

El trabajo se agrupa en **épicas** (`EP-XX`), que son bloques funcionales del sistema, y estas a su vez en **historias de usuario** (`HU-XX`), que son las unidades de trabajo que se implementan y se prueban por separado. Los códigos son únicos y no se reutilizan: si una historia se descarta, su código se retira.

Al final del documento se indica el **orden de implementación**, que no coincide con el orden en que aparecen las épicas.

## EP-01. Gestión de la sesión

La aplicación **no gestiona credenciales por su cuenta**. Implementar bien un registro propio implica almacenamiento seguro de contraseñas, protección contra fuerza bruta, recuperación de contraseña, verificación del correo y gestión de sesiones, y todo eso es un coste altísimo frente a lo que aporta al proyecto.

En su lugar, la autenticación se delega en un proveedor externo (Firebase Authentication, Auth0, Keycloak o similar). El backend queda preparado para **validar los tokens que emita ese proveedor** y para autorizar los endpoints con Spring Security, de forma que quien despliegue el sistema solo tenga que enchufar la pieza que falta.

### HU-01. Validación de tokens

La API solo atiende peticiones que presenten un token válido emitido por el proveedor configurado.

- Sin token, o con un token expirado, mal firmado o de otro emisor, la respuesta es **401**.
- **Toda ruta exige autenticación salvo las declaradas públicas de forma explícita.** El valor por defecto es denegar, de modo que cualquier endpoint que se añada en el futuro nace protegido.
- La verificación se configura contra el proveedor (clave pública o `issuer-uri`). El sistema no almacena ni comprueba credenciales.
- Existe un modo de desarrollo que permite trabajar sin proveedor externo, sin tocar el código de la aplicación.

### HU-02. Sincronización del usuario

La primera vez que alguien accede con un token válido, el sistema crea su usuario local para poder asociarle reservas, participaciones e inscripciones.

- El usuario se identifica por el `sub` del token, nunca por el correo.
- Si el `sub` no existe en la base de datos, se crea el usuario con los datos disponibles en el token y el rol básico. El rol **nunca** se toma del token.
- El correo puede no venir en el token, y en ese caso el usuario se crea igualmente.
- No existe un endpoint de registro: el alta en el proveedor es el registro, y los datos que el token no trae se completan en HU-04.

### HU-03. Autorización por roles

El sistema distingue entre usuario y administrador, de forma que solo un administrador pueda gestionar pistas y torneos.

- El rol se lee de la base de datos, no del token, para que el club pueda administrarlo sin depender del proveedor.
- Un cambio de rol tiene efecto en la siguiente petición, sin esperar a que caduque el token.
- Un usuario autenticado sin el rol necesario recibe **403**, frente al **401** de quien no se ha autenticado.

### HU-04. Gestión del perfil

El usuario consulta y modifica sus datos personales (nombre, teléfono y correo).

- `GET /users/me` y `PUT /users/me` operan siempre sobre el usuario del token: la ruta no admite identificadores de otros usuarios.
- Es el punto donde se completan los datos que el token no proporciona, como el teléfono.
- Los datos de entrada se validan antes de guardarse.

## EP-02. Gestión de pistas y disponibilidad

Configuración de las pistas del club y cálculo de los huecos libres. Es la base sobre la que se apoya todo lo demás.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-05 | Alta de una pista | Un administrador da de alta una pista con su nombre, precio por turno, duración del turno y horario de apertura y cierre. |
| HU-06 | Baja de una pista | Un administrador retira una pista del sistema. Si hubiese reservas ya hechas habría que gestionar la cancelación. |
| HU-07 | Modificación de una pista | Un administrador cambia los datos de una pista. Modificar el horario o la duración del turno afecta a la disponibilidad futura, no a las reservas ya realizadas. |
| HU-08 | Consulta de disponibilidad | Dada una pista y una fecha, el sistema devuelve los turnos de ese día y cuáles están libres. Los turnos se calculan a partir del horario y la duración configurados en la pista; no se almacenan en base de datos. |

## EP-03. Gestión de reservas

El flujo principal de la aplicación: reservar una pista pagando. Incluye el ciclo de vida completo de la reserva, desde que se aparta el turno hasta que se confirma, vence o se cancela.

Al igual que con la sesión, **el sistema no implementa ninguna pasarela de pago concreta**. Se define un puerto de pagos (iniciar un cobro, confirmarlo y reembolsarlo) junto a una implementación simulada para desarrollo, de forma que quien despliegue el proyecto solo tenga que escribir el adaptador de Stripe, Redsys o la plataforma que prefiera. Lo que sí es responsabilidad del sistema, y no del adaptador, es que confirmar un pago dos veces no confirme la reserva dos veces.

La historia central de la épica es `HU-09`; `HU-10`, `HU-11` y `HU-12` son las piezas que la sostienen y se listan aparte por ser trabajo independiente y verificable por separado.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-09 | Realizar una reserva | El usuario reserva un turno libre y lo paga. La reserva queda primero en estado pendiente y se confirma cuando el pago se completa. El servidor valida que el turno encaje en el horario y en la rejilla de la pista, y garantiza que no se reserve dos veces el mismo turno. |
| HU-10 | Puerto de pagos | Definición de la interfaz de cobro y de una implementación simulada que permita desarrollar y probar el flujo completo sin depender de una pasarela real. |
| HU-11 | Confirmación del pago | El sistema atiende la notificación de pago completado y confirma la reserva. El mismo aviso puede llegar varias veces, así que el proceso no debe confirmar ni cobrar dos veces. |
| HU-12 | Vencimiento de reservas pendientes | Una reserva que no se paga en un plazo determinado se libera automáticamente, para que el turno no quede bloqueado indefinidamente. |
| HU-13 | Cancelación y reembolso | Una reserva confirmada puede cancelarse, lo que libera el turno y devuelve el importe a través del puerto de pagos. |
| HU-14 | Registro de los jugadores | Quien reserva indica con quién juega y contra quién, guardando el equipo de cada participante. Es el dato que después alimenta las estadísticas de EP-06. |

## EP-04. Gestión de torneos

Creación de competiciones por parte del club e inscripción de los usuarios.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-15 | Alta de un torneo | Un administrador crea un torneo con sus fechas, precio de inscripción y número de plazas. |
| HU-16 | Modificación de un torneo | Un administrador cambia los datos del torneo. Hay que definir qué ocurre si ya tiene inscripciones. |
| HU-17 | Baja de un torneo | Un administrador cancela un torneo, se notifica a los inscritos y se les devuelve el importe. |
| HU-18 | Inscripción en un torneo | El usuario se inscribe pagando. La inscripción sigue el mismo ciclo que la reserva: pendiente hasta que se confirma el pago. Se controla que no se superen las plazas disponibles. |
| HU-19 | Ocupación de pistas | Un torneo bloquea las pistas que va a usar y cancela las reservas que se solapen con él, devolviendo su importe. |
| HU-20 | Generación del cuadro | El sistema genera y muestra el desarrollo del torneo. **Pendiente de definir el formato**: liguilla, eliminatoria, sistema suizo o varios a elegir. |

## EP-05. Gestión del acceso a la pista

Entrega de la llave mediante un código que abre la taquilla. Es un módulo **opcional**: un club que ya gestione sus llaves puede desactivarlo y usar el resto del sistema.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-21 | Generación del código | Al confirmarse una reserva se genera un código de acceso válido únicamente durante la franja reservada. |
| HU-22 | Validación del código | El sistema valida un código presentado en la taquilla y autoriza o deniega la apertura, rechazando los caducados, los ya usados y los de reservas canceladas. |
| HU-23 | Activación del módulo | El club puede activar o desactivar la gestión de accesos. Con el módulo desactivado no se generan códigos y el resto del sistema funciona igual. |

## EP-06. Estadísticas H2H

El valor diferencial de la aplicación: saber qué tal se te da un rival antes de jugar contra él.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-24 | Registro del resultado | Al terminar el partido, quien hizo la reserva introduce el marcador y el equipo ganador. |
| HU-25 | Consulta del historial | El usuario consulta sus enfrentamientos previos contra un jugador o una pareja, con el número de partidos jugados y su porcentaje de victorias. |

## EP-07. Administración y facturación

Funciones de apoyo que no pertenecen a ningún flujo concreto.

| Código | Historia | Descripción |
| --- | --- | --- |
| HU-26 | Factura de un pago | El usuario descarga la factura de un pago, tanto de una reserva como de una inscripción. |
| HU-27 | Gestión de usuarios | Un administrador da de alta y de baja usuarios del club. |

## Orden de implementación

El orden lo marcan las dependencias: cada fase necesita la anterior, y la primera deja el sistema ya utilizable.

### Fase 1. Reservar y cobrar

`HU-01` → `HU-02` → `HU-03` → `HU-05` → `HU-07` → `HU-08` → `HU-09` → `HU-10` → `HU-11` → `HU-12`

Es el mínimo que resuelve el problema original: reservar una pista y pagarla sin llamar a nadie. La seguridad va primero porque todo lo demás cuelga de saber quién hace cada petición, y las pistas antes que las reservas porque la disponibilidad se calcula a partir de su configuración. El cobro se prueba contra la implementación simulada de `HU-10`, de modo que la fase se puede cerrar sin contratar ninguna pasarela. `HU-12` va al final: sin el vencimiento, cualquier usuario que abandone el pago deja un turno bloqueado para siempre.

### Fase 2. Cerrar el ciclo de la reserva

`HU-06` → `HU-13` → `HU-04` → `HU-14`

Los casos que la fase 1 deja abiertos: cancelar, dar de baja una pista con reservas vivas y gestionar el perfil. `HU-14` se adelanta aquí, aunque las estadísticas lleguen mucho después, porque los datos de los jugadores hay que empezar a recogerlos cuanto antes: sin partidos registrados, EP-06 no tendría nada que mostrar el día que se implemente.

### Fase 3. Torneos

`HU-15` → `HU-16` → `HU-18` → `HU-17` → `HU-19`

Se apoya en el cobro y en el reembolso ya resueltos en las fases anteriores, de ahí que vaya después. `HU-20` queda fuera hasta decidir el formato de competición.

### Fase 4. Acceso a la pista

`HU-21` → `HU-22` → `HU-23`

Depende de que la reserva se confirme correctamente, así que necesita la fase 1 cerrada. Es autónomo respecto a los torneos, de modo que puede adelantarse si interesa más que la fase 3.

### Fase 5. Mejoras

`HU-24` → `HU-25` → `HU-26` → `HU-27` → `HU-20`

Funcionalidades que aportan valor pero que no impiden usar el sistema. `HU-25` es la única que arrastra una dependencia real, ya que necesita los resultados de `HU-24` y los jugadores de `HU-14`.
