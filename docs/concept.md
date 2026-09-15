# PalEx

## ¿En qué consiste?
PalEx es un backend desarrollado con Spring Boot que da soporte a un sistema de reservas para clubes con una o varias pistas de pádel. Incluye las siguientes funcionalidades:

- **Reservas con pago.** Para reservar una franja horaria en una pista, el usuario abona el importe correspondiente en el momento.
- **Torneos y ligas.** Los administradores del club pueden crear torneos o liguillas y los usuarios pueden inscribirse. Además, la aplicación facilita el seguimiento de cada competición.
- **Clasificación H2H entre amigos.** Al reservar, el usuario puede indicar quién es su pareja y contra quién juega. Al terminar el partido, quien hizo la reserva introduce el resultado y, en futuras reservas, puede consultar estadísticas de ese enfrentamiento, como el porcentaje de victorias.
- **Acceso a la llave de la pista.** Con la reserva se genera un código QR o de barras que permite abrir una taquilla y recoger la llave de la pista.

Estos módulos son independientes entre sí. Por ejemplo, un club puede usar el sistema de reservas y prescindir de la gestión de llaves si ya dispone de su propio método.

## ¿De dónde surge la idea?
Este año construyeron en mi pueblo unas pistas de pádel de pago. Fue una gran noticia, pero su gestión, en mi opinión, se ha quedado anticuada.

El proceso actual es el siguiente:

1. Para reservar hay que contactar directamente con el alcalde, que comprueba si el horario está libre y confirma la reserva.
2. La llave se recoge en el ayuntamiento o en alguna tienda del pueblo, lo que obliga a que el alcalde y la tienda también se coordinen.
3. Al acabar el partido hay que devolver la llave, con el paseo extra que eso supone.
4. El pago se realiza en el ayuntamiento después de jugar, pero solo abre entre semana por la mañana. Si reservas un viernes por la tarde, tienes que acordarte de pagar el lunes.

En resumen, un auténtico lío.

## Stack tecnológico
- **Arquitectura monolítica.** La aplicación está pensada para ayuntamientos y clubes pequeños, donde en el peor de los casos puede haber unos 20 usuarios simultáneos. En este contexto, las ventajas de los microservicios, como la escalabilidad independiente, no compensan la complejidad que añaden (service discovery con Eureka, servidor de configuración, etc.). Un monolito con el código bien separado por responsabilidades es suficiente.
- **Spring Boot.** Elegí Spring por su estabilidad y madurez a largo plazo, y Spring Boot porque simplifica mucho la configuración. Es cierto que su consumo de memoria y su tiempo de arranque son mayores que los de alternativas como Quarkus o Node.js, pero no es relevante en este caso: al tratarse de un monolito, habrá una única instancia que arranca pocas veces y permanece en ejecución.
- **MySQL.** El modelo de datos es claramente relacional: las reservas están asociadas a usuarios, y tanto el H2H como los torneos requieren relaciones entre usuarios. Entre las opciones relacionales, MySQL y PostgreSQL serían igual de válidas para este caso: con tan pocos usuarios, el rendimiento no es un factor diferencial. Me decanté por MySQL porque es con la que tengo más soltura y me permite arrancar el proyecto con más agilidad.
