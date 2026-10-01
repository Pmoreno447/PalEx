# ADR-001. Monolito

La aplicación está pensada para clubes pequeños y ayuntamientos, donde como mucho puede haber unos 20 usuarios a la vez. Podríamos separar los cuatro módulos (reservas, torneos, accesos y estadísticas) en servicios independientes, pero elegimos un solo servicio con el código separado por responsabilidades.

Lo hacemos porque a este volumen no vamos a aprovechar nada de lo que dan los microservicios: no necesitamos escalar un módulo por su cuenta ni desplegarlos por separado. A cambio nos ahorramos el descubrimiento de servicios, el servidor de configuración y las trazas distribuidas.

Además, al haber una sola base de datos, todo lo que toca varias entidades a la vez (crear una reserva con su pago, por ejemplo) se resuelve con una transacción y no coordinando servicios.

Si algún día un módulo necesitase escalar solo, tenerlo separado por responsabilidades facilita sacarlo entonces.
